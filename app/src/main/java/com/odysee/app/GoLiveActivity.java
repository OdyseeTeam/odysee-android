package com.odysee.app;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraCharacteristics;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.haishinkit.event.Event;
import com.haishinkit.event.IEventListener;
import com.haishinkit.media.AudioRecordSource;
import com.haishinkit.media.Camera2Source;
import com.haishinkit.rtmp.RtmpConnection;
import com.haishinkit.rtmp.RtmpStream;
import com.haishinkit.view.HkSurfaceView;
import com.odysee.app.adapter.InlineChannelSpinnerAdapter;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Claim;
import com.odysee.app.supplier.ClaimListSupplier;
import com.odysee.app.tasks.UploadImageTask;
import com.odysee.app.tasks.claim.ClaimListResultHandler;
import com.odysee.app.tasks.claim.ClaimListTask;
import com.odysee.app.tasks.claim.ClaimResultHandler;
import com.odysee.app.tasks.claim.PublishClaimTask;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class GoLiveActivity extends AppCompatActivity {
    private final String RTMP_URL = "rtmp://stream.odysee.com/live";
    private final double MIN_STREAM_STAKE = 50;

    private ImageView imagePrecheckSpaceman;
    private TextView textPrecheckStatus;
    private Spinner selectChannelSpinner;
    private TextView textChannelError;
    private TextInputEditText inputTitle;
    private View progress;
    private ImageView imageThumbnail;
    private View thumbnailUploadProgress;
    private MaterialButton buttonToggleStreaming;

    private View livestreamPrecheckView;
    private View livestreamOptionsView;
    private View livestreamControlsView;

    private Claim selectedChannel;
    private String streamKey;
    private RtmpConnection connection;
    private RtmpStream stream;
    private Camera2Source cameraSource;
    private BroadcastReceiver screenOnReceiver;
    private ScheduledExecutorService waitForConfirmationScheduler;

    private boolean uploadingThumbnail;
    private String uploadedThumbnailUrl;
    private String lastSelectedThumbnailFile;
    private boolean isStreaming;
    private boolean startingStream;
    private boolean screenTurnedOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isDarkMode()) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        setContentView(R.layout.activity_go_live);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {})
                    .launch(Manifest.permission.RECORD_AUDIO);
        }

        connection = new RtmpConnection();
        stream = new RtmpStream(connection);
        stream.attachAudio(new AudioRecordSource());
        cameraSource = new Camera2Source(this, null, false);
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

        imagePrecheckSpaceman = findViewById(R.id.livestream_precheck_spaceman_image);
        textPrecheckStatus = findViewById(R.id.livestream_precheck_status_text);
        selectChannelSpinner = findViewById(R.id.livestream_options_select_channel_spinner);
        textChannelError = findViewById(R.id.livestream_options_channel_error_text);
        inputTitle = findViewById(R.id.livestream_options_title_input);
        progress = findViewById(R.id.livestream_progress);
        imageThumbnail = findViewById(R.id.livestream_options_thumbnail_preview);
        thumbnailUploadProgress = findViewById(R.id.livestream_options_thumbnail_upload_progress);

        livestreamPrecheckView = findViewById(R.id.livestream_precheck);
        livestreamOptionsView = findViewById(R.id.livestream_options);
        livestreamControlsView = findViewById(R.id.livestream_controls);

        HkSurfaceView livestreamControlsCameraView = findViewById(R.id.livestream_controls_camera_view);
        livestreamControlsCameraView.attachStream(stream);

        MaterialButton buttonContinue = findViewById(R.id.livestream_options_continue_button);
        buttonToggleStreaming = findViewById(R.id.livestream_controls_toggle_streaming_button);
        ImageButton buttonSwitchCamera = findViewById(R.id.livestream_controls_switch_camera_button);

        selectChannelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedChannel = (Claim) selectChannelSpinner.getSelectedItem();
                checkCanStreamOnChannel(selectedChannel);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        imageThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkStoragePermissionAndLaunchFilePicker();
            }
        });

        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uploadingThumbnail) {
                    showError(getString(R.string.publish_thumbnail_in_progress));
                    return;
                }
                if (!checkCanStreamOnChannel(selectedChannel)) {
                    showError(getString(R.string.need_valid_channel));
                    return;
                }
                if (Helper.isNullOrEmpty(Helper.getValue(inputTitle.getText()))) {
                    showError(getString(R.string.specify_title));
                    return;
                }

                progress.setVisibility(View.VISIBLE);
                livestreamOptionsView.setVisibility(View.GONE);
                textPrecheckStatus.setText(R.string.precheck_wait_confirmation);
                livestreamPrecheckView.setVisibility(View.VISIBLE);
                publishLivestreamClaim();
            }
        });

        buttonToggleStreaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (startingStream) {
                    return;
                }
                if (!isStreaming) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
                    screenOnReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            screenTurnedOn = true;
                        }
                    };
                    registerReceiver(screenOnReceiver, filter);
                    startingStream = true;
                    connection.connect(RTMP_URL);
                    buttonToggleStreaming.setText(R.string.stop_streaming);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    connection.close();
                    isStreaming = false;
                    startingStream = false;
                    buttonToggleStreaming.setText(R.string.start_streaming);
                }
            }
        });

        buttonSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraSource.switchCamera();
            }
        });
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
        fetchChannels();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connection.dispose();
        if (screenOnReceiver != null) {
            unregisterReceiver(screenOnReceiver);
        }
    }

    @Override
    public void onBackPressed() {
        if (isStreaming) {
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
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MainActivity.REQUEST_FILE_PICKER
                && resultCode == Activity.RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            String filePath = Helper.getRealPathFromURI_API19(this, fileUri);

            if (Helper.isNullOrEmpty(filePath)) {
                showError(getString(R.string.undetermined_image_filepath));
                return;
            }

            if (filePath.equalsIgnoreCase(lastSelectedThumbnailFile)) {
                // previous selected thumbnail was uploaded successfully
                return;
            }

            uploadThumbnail(filePath);
        }
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
        } else if (requestCode == MainActivity.REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchFilePicker();
            } else {
                showError(getString(R.string.storage_permission_rationale_images));
            }
        }
    }

    private void uploadThumbnail(String thumbnailPath) {
        if (uploadingThumbnail) {
            Snackbar.make(findViewById(R.id.livestream_main), R.string.wait_for_upload, Snackbar.LENGTH_LONG).show();
            return;
        }

        Glide.with(thumbnailUploadProgress).load(thumbnailPath).centerCrop().into(imageThumbnail);

        uploadingThumbnail = true;
        uploadedThumbnailUrl = null;
        UploadImageTask task = new UploadImageTask(thumbnailPath, thumbnailUploadProgress, new UploadImageTask.UploadThumbnailHandler() {
            @Override
            public void onSuccess(String url) {
                lastSelectedThumbnailFile = thumbnailPath;
                uploadedThumbnailUrl = url;
                uploadingThumbnail = false;
            }

            @Override
            public void onError(Exception error) {
                showError(getString(R.string.image_upload_failed));
                lastSelectedThumbnailFile = null;
                imageThumbnail.setImageDrawable(null);
                uploadingThumbnail = false;
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void launchFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_thumbnail)),
                MainActivity.REQUEST_FILE_PICKER);
    }

    private void checkStoragePermissionAndLaunchFilePicker() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, MainActivity.REQUEST_STORAGE_PERMISSION);
        } else {
            launchFilePicker();
        }
    }

    private void openCameraSource() {
        cameraSource.open(CameraCharacteristics.LENS_FACING_BACK);
    }

    private void checkCameraPermissionAndOpenCameraSource() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.CAMERA }, MainActivity.REQUEST_CAMERA_PERMISSION);
        } else {
            openCameraSource();
        }
    }

    private void showPrecheckError(String status) {
        livestreamPrecheckView.setVisibility(View.VISIBLE);
        imagePrecheckSpaceman.setImageResource(R.drawable.spaceman_sad);
        textPrecheckStatus.setText(status);
    }

    private void signAndSetupStream() {
        String hexData = Helper.toHexString(selectedChannel.getName());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Object> options = new HashMap<>(2);
                    options.put("channel_id", selectedChannel.getClaimId());
                    options.put("hexdata", hexData);

                    AccountManager am = AccountManager.get(getApplicationContext());
                    String authToken = am.peekAuthToken(Helper.getOdyseeAccount(am.getAccounts()), "auth_token_type");
                    JSONObject result = (JSONObject) Lbry.authenticatedGenericApiCall("channel_sign", options, authToken);
                    String signature = result.getString("signature");
                    String signingTs = result.getString("signing_ts");
                    if (!Helper.isNullOrEmpty(signature) && !Helper.isNullOrEmpty(signingTs)) {
                        streamKey = createStreamKey(signature, signingTs);
                    } else {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                livestreamControlsView.setVisibility(View.GONE);
                                showPrecheckError(getString(R.string.stream_key_not_generated));
                            }
                        });
                    }
                } catch (ApiCallException | JSONException ex) {
                    showError(ex.getMessage());
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            livestreamControlsView.setVisibility(View.GONE);
                            showPrecheckError(getString(R.string.error_publishing_livestream));
                        }
                    });
                }
            }
        });
        executor.shutdown();
    }

    // Poll every 30s waiting for claim to be confirmed before streaming
    private void waitForConfirmation(String txid) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        waitForConfirmationScheduler = Executors.newSingleThreadScheduledExecutor();
        waitForConfirmationScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Object> options = new HashMap<>(2);
                    options.put("type", Claim.TYPE_STREAM);
                    options.put("txid", txid);

                    AccountManager am = AccountManager.get(getApplicationContext());
                    String authToken = am.peekAuthToken(Helper.getOdyseeAccount(am.getAccounts()), "auth_token_type");
                    JSONObject result = (JSONObject) Lbry.authenticatedGenericApiCall(Lbry.METHOD_TXO_LIST, options, authToken);
                    JSONObject item = result.getJSONArray("items").getJSONObject(0);
                    int confirmations  = item.getInt("confirmations");
                    if (confirmations > 0) {
                        signAndSetupStream();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.setVisibility(View.GONE);
                                livestreamPrecheckView.setVisibility(View.GONE);
                                livestreamControlsView.setVisibility(View.VISIBLE);
                            }
                        });

                        waitForConfirmationScheduler.shutdownNow();
                    }
                } catch (ApiCallException | JSONException ex) {
                    // Do nothing, will retry in 30s
                    showError(ex.getMessage());
                }
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private Claim buildLivestreamClaim() {
        Claim claim = new Claim();

        claim.setName("livestream-" + System.currentTimeMillis());
        claim.setAmount("0.01");

        Claim.StreamMetadata metadata = new Claim.StreamMetadata();
        metadata.setTitle(Helper.getValue(inputTitle.getText()));

        claim.setSigningChannel(selectedChannel);

        if (!Helper.isNullOrEmpty(uploadedThumbnailUrl)) {
            Claim.Resource thumbnail = new Claim.Resource();
            thumbnail.setUrl(uploadedThumbnailUrl);
            metadata.setThumbnail(thumbnail);
        } else {
            // Use the channel thumbnail
            String channelThumbnailUrl = Optional.ofNullable(selectedChannel.getValue())
                    .map(Claim.GenericMetadata::getThumbnail)
                    .map(Claim.Resource::getUrl)
                    .orElse(null);
            if (!Helper.isNullOrEmpty(channelThumbnailUrl)) {
                metadata.setThumbnail(selectedChannel.getValue().getThumbnail());
            }
        }

        claim.setValueType(Claim.TYPE_STREAM);
        claim.setValue(metadata);

        return claim;
    }

    private void publishLivestreamClaim() {
        Claim claim = buildLivestreamClaim();
        AccountManager am = AccountManager.get(this);
        String authToken = am.peekAuthToken(Helper.getOdyseeAccount(am.getAccounts()), "auth_token_type");
        PublishClaimTask task = new PublishClaimTask(claim, "", null, authToken, new ClaimResultHandler() {
            @Override
            public void beforeStart() { }

            @Override
            public void onSuccess(Claim claimResult) {
                String txid = claimResult.getTxid();
                waitForConfirmation(txid);
            }

            @Override
            public void onError(Exception error) {
                if (error != null) {
                    showError(error.getMessage());
                }
                progress.setVisibility(View.GONE);
                livestreamControlsView.setVisibility(View.GONE);
                showPrecheckError(getString(R.string.error_publishing_livestream));
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Checks if it's possible to stream on {@code channel}, and update the
     * error text with the reason if not.
     *
     * @param channel The channel to check
     * @return True if it's possible to stream on the channel, false otherwise
     */
    private boolean checkCanStreamOnChannel(Claim channel) {
        if (channel == null || channel.isPlaceholder() || channel.isPlaceholderAnonymous()) {
            return false;
        }

        if (channel.getConfirmations() < 1) {
            textChannelError.setText(getString(R.string.channel_error_pending));
            textChannelError.setVisibility(View.VISIBLE);
            return false;
        }

        // Disabled due to lack of server side checking for now
//        double effectiveAmount = Double.parseDouble(channel.getMeta().getEffectiveAmount());
//        if (effectiveAmount < MIN_STREAM_STAKE) {
//            textChannelError.setText(getString(R.string.channel_error_need_minimum_credits,
//                    MIN_STREAM_STAKE, channel.getName()));
//            textChannelError.setVisibility(View.VISIBLE);
//            return false;
//        }

        textChannelError.setVisibility(View.GONE);
        return true;
    }

    private void showLivestreamingOptions() {
        progress.setVisibility(View.GONE);
        livestreamPrecheckView.setVisibility(View.GONE);
        livestreamOptionsView.setVisibility(View.VISIBLE);

        checkCanStreamOnChannel((Claim) selectChannelSpinner.getSelectedItem());
    }

    private void updateChannelList(List<Claim> channels) {
        selectChannelSpinner.setAdapter(new InlineChannelSpinnerAdapter(this, R.layout.spinner_item_channel, channels));

        if (channels.size() > 0) {
            showLivestreamingOptions();
        } else {
            progress.setVisibility(View.GONE);
            showPrecheckError(getString(R.string.precheck_need_channel));
        }
    }

    private void fetchChannels() {
        if (Lbry.ownChannels != null && Lbry.ownChannels.size() > 0) {
            updateChannelList(Lbry.ownChannels);
            return;
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Supplier<List<Claim>> s = new ClaimListSupplier(Collections.singletonList(Claim.TYPE_CHANNEL), null);
            CompletableFuture<List<Claim>> cf = CompletableFuture.supplyAsync(s);
            cf.whenComplete((result, e) -> {
                if (e != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Throwable t = e.getCause();
                            if (t != null) {
                                showError(t.getMessage());
                            }
                            progress.setVisibility(View.GONE);
                            showPrecheckError(getString(R.string.precheck_error_loading_channels));
                        }
                    });
                }

                if (result != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Lbry.ownChannels = new ArrayList<>(result);
                            updateChannelList(Lbry.ownChannels);
                        }
                    });
                }
            });
        } else {
            ClaimListTask task = new ClaimListTask(Claim.TYPE_CHANNEL, null, new ClaimListResultHandler() {
                @Override
                public void onSuccess(List<Claim> claims) {
                    Lbry.ownChannels = new ArrayList<>(claims);
                    updateChannelList(Lbry.ownChannels);
                }

                @Override
                public void onError(Exception error) {
                    if (error != null) {
                        showError(error.getMessage());
                    }
                    progress.setVisibility(View.GONE);
                    showPrecheckError(getString(R.string.precheck_error_loading_channels));
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private String createStreamKey(String signature, String signingTs) {
        String hexData = Helper.toHexString(selectedChannel.getName());
        return selectedChannel.getClaimId()
                + "?d=" + hexData
                + "&s=" + signature
                + "&t=" + signingTs;
    }

    private void showError(String message) {
        Snackbar.make(findViewById(R.id.livestream_main), message, Snackbar.LENGTH_LONG).
                setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
    }

    private boolean isDarkMode() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(MainActivity.PREFERENCE_KEY_DARK_MODE, false);
    }
}
