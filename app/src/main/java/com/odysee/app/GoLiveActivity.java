package com.odysee.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.haishinkit.event.Event;
import com.haishinkit.event.IEventListener;
import com.haishinkit.media.AudioRecordSource;
import com.haishinkit.media.Camera2Source;
import com.haishinkit.rtmp.RtmpConnection;
import com.haishinkit.rtmp.RtmpStream;
import com.haishinkit.view.HkSurfaceView;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.LbryAnalytics;

import java.util.HashMap;
import java.util.Map;

public class GoLiveActivity extends AppCompatActivity {
    private final String RTMP_URL = "rtmp://stream.odysee.com/live";

    private MaterialButton buttonToggleStreaming;

    private String streamKey;
    private boolean isStreaming;
    private boolean startingStream;
    private boolean screenTurnedOn;

    private RtmpConnection connection;
    private RtmpStream stream;
    private Camera2Source cameraSource;
    private BroadcastReceiver screenOnReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Change status bar text color depending on Night mode when app is running
        String darkModeAppSetting = ((OdyseeApp) getApplication()).getDarkModeAppSetting();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!darkModeAppSetting.equals(MainActivity.APP_SETTING_DARK_MODE_NIGHT) && AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES) {
                //noinspection deprecation
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        } else {
            int defaultNight = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (darkModeAppSetting.equals(MainActivity.APP_SETTING_DARK_MODE_NOTNIGHT) || (darkModeAppSetting.equals(MainActivity.APP_SETTING_DARK_MODE_SYSTEM) && defaultNight == Configuration.UI_MODE_NIGHT_NO)) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    getWindow().getDecorView().getWindowInsetsController().setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                } else {
                    //noinspection deprecation
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            } else {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    getWindow().getDecorView().getWindowInsetsController().setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                }
            }
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenOnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                screenTurnedOn = true;
            }
        };
        registerReceiver(screenOnReceiver, filter);

        setContentView(R.layout.activity_go_live);

        // If stream key not provided, fail
        Intent intent = getIntent();
        String extra = intent.getStringExtra("streamKey");
        if (Helper.isNullOrEmpty(extra)) {
            MainActivity.instance.showError(getString(R.string.no_stream_key_provided));
            finish();
            return;
        }
        streamKey = extra;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {})
                    .launch(Manifest.permission.RECORD_AUDIO);
        }

        connection = new RtmpConnection();
        stream = new RtmpStream(connection);
        stream.attachAudio(new AudioRecordSource());
        cameraSource = new Camera2Source(this, false);
        stream.attachVideo(cameraSource);
        connection.addEventListener(Event.RTMP_STATUS, new IEventListener() {
            @Override
            public void handleEvent(@NonNull Event event) {
                startingStream = false;
                Object data = event.getData();
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    HashMap<String, Object> dataMap = (HashMap<String, Object>) data;
                    String code = dataMap.get("code").toString();
                    if (code.equals(RtmpConnection.Code.CONNECT_SUCCESS.getRawValue())) {
                        isStreaming = true;
                        stream.publish(streamKey, RtmpStream.HowToPublish.LIVE);
                    } else if (code.equals(RtmpConnection.Code.CONNECT_FAILED.getRawValue())) {
                        isStreaming = false;
                        showError(getString(R.string.stream_connect_failed));
                    } else if (code.equals(RtmpConnection.Code.CONNECT_CLOSED.getRawValue())) {
                        // attempt connection retry?
                        isStreaming = false;
                    }
                }
            }
        });
        connection.addEventListener(Event.IO_ERROR, new IEventListener() {
            @Override
            public void handleEvent(@NonNull Event event) {
                // simply attempt to reconnect
                showError(getString(R.string.stream_connect_failed_retrying));
                connection.connect(RTMP_URL);
            }
        });

        ((HkSurfaceView) findViewById(R.id.livestream_controls_camera_view)).attachStream(stream);

        buttonToggleStreaming = findViewById(R.id.livestream_controls_toggle_streaming_button);

        buttonToggleStreaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startingStream) {
                    return;
                }
                if (!isStreaming) {
                    startingStream = true;
                    connection.connect(RTMP_URL);
                    buttonToggleStreaming.setText(R.string.stop_streaming);
                } else {
                    connection.close();
                    isStreaming = false;
                    startingStream = false;
                    buttonToggleStreaming.setText(R.string.start_streaming);
                }
            }
        });

        findViewById(R.id.livestream_controls_switch_camera_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraSource.switchCamera();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraSource.close();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (screenTurnedOn) {
            MainActivity.instance.showStreamStoppedMessage();
            finish();
        }

        LbryAnalytics.setCurrentScreen(this, "Go Live", "GoLive");
        checkCameraPermissionAndOpenCameraSource();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connection != null) {
            connection.dispose();
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (screenOnReceiver != null) {
            unregisterReceiver(screenOnReceiver);
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.stop_streaming)
                .setMessage(R.string.confirm_stop_message)
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GoLiveActivity.super.onBackPressed();
                    }
                }).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MainActivity.REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraSource();
            } else {
                showError(getString(R.string.camera_permission_rationale_livestream));
            }
        }
    }

    private void checkCameraPermissionAndOpenCameraSource() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.CAMERA }, MainActivity.REQUEST_CAMERA_PERMISSION);
        } else {
            openCameraSource();
        }
    }

    private void openCameraSource() {
        cameraSource.open(CameraCharacteristics.LENS_FACING_BACK);
    }

    private void showError(String message) {
        Snackbar.make(findViewById(R.id.livestream_main), message, Snackbar.LENGTH_LONG).
                setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
    }
}
