package com.odysee.app.ui.golive;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.haishinkit.event.Event;
import com.haishinkit.event.IEventListener;
import com.haishinkit.media.AudioRecordSource;
import com.haishinkit.media.Camera2Source;
import com.haishinkit.rtmp.RtmpConnection;
import com.haishinkit.rtmp.RtmpStream;
import com.haishinkit.view.HkSurfaceView;
import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.InlineChannelSpinnerAdapter;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.listener.CameraPermissionListener;
import com.odysee.app.model.Claim;
import com.odysee.app.tasks.claim.ClaimListResultHandler;
import com.odysee.app.tasks.claim.ClaimListTask;
import com.odysee.app.tasks.claim.ClaimResultHandler;
import com.odysee.app.tasks.claim.PublishClaimTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoLiveFragment extends BaseFragment implements CameraPermissionListener {
    private final String RTMP_URL = "rtmp://stream.odysee.com/live";

    private Spinner selectChannelSpinner;
    private TextInputEditText inputTitle;

    private View livestreamOptionsView;
    private View livestreamControlsView;

    private Claim selectedChannel;
    private String streamKey;
    private RtmpConnection connection;
    private RtmpStream stream;
    private Camera2Source cameraSource;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity() != null) {
            Activity activity = getActivity();
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {})
                        .launch(Manifest.permission.RECORD_AUDIO);
            }
        }

        connection = new RtmpConnection();
        stream = new RtmpStream(connection);
        stream.attachAudio(new AudioRecordSource());
        cameraSource = new Camera2Source(requireContext(), null, false);
        checkCameraPermissionAndOpenCameraSource();
        stream.attachVideo(cameraSource);
        connection.addEventListener(Event.RTMP_STATUS, new IEventListener() {
            @Override
            public void handleEvent(@NonNull Event event) {
                Object data = event.getData();
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    HashMap<String, Object> dataMap = (HashMap<String, Object>) data;
                    String code = dataMap.get("code").toString();
                    if (code.equals(RtmpConnection.Code.CONNECT_SUCCESS.getRawValue())) {
                        stream.publish(streamKey, RtmpStream.HowToPublish.LIVE);
                    }
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_go_live, container, false);

        selectChannelSpinner = root.findViewById(R.id.livestream_options_select_channel_spinner);
        inputTitle = root.findViewById(R.id.livestream_options_title_input);

        livestreamOptionsView = root.findViewById(R.id.livestream_options);
        livestreamControlsView = root.findViewById(R.id.livestream_controls);

        HkSurfaceView livestreamControlsCameraView = root.findViewById(R.id.livestream_controls_camera_view);
        livestreamControlsCameraView.attachStream(stream);

        MaterialButton buttonContinue = root.findViewById(R.id.livestream_options_continue_button);
        MaterialButton buttonToggleStreaming = root.findViewById(R.id.livestream_controls_toggle_streaming_button);
        ImageButton buttonSwitchCamera = root.findViewById(R.id.livestream_controls_switch_camera_button);

        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                livestreamOptionsView.setVisibility(View.GONE);
                publishLivestreamClaim();
                livestreamControlsView.setVisibility(View.VISIBLE);
            }
        });

        buttonToggleStreaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (buttonToggleStreaming.getText() == getString(R.string.start_streaming)) {
                    connection.connect(RTMP_URL);
                    buttonToggleStreaming.setText(R.string.stop_streaming);
                } else {
                    connection.close();
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

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchChannels();
    }

    @Override
    public void onCameraPermissionGranted() {
        openCameraSource();
    }

    @Override
    public void onCameraPermissionRefused() {
        showError(getString(R.string.camera_permission_rationale_record));
    }

    @Override
    public void onRecordAudioPermissionGranted() {

    }

    @Override
    public void onRecordAudioPermissionRefused() {

    }

    private void openCameraSource() {
        cameraSource.open(CameraCharacteristics.LENS_FACING_BACK);
    }

    private void checkCameraPermissionAndOpenCameraSource() {
        Context context = getContext();
        if (!MainActivity.hasPermission(Manifest.permission.CAMERA, context)) {
            MainActivity.requestPermission(
                    Manifest.permission.CAMERA,
                    MainActivity.REQUEST_CAMERA_PERMISSION,
                    getString(R.string.camera_permission_rationale_record),
                    context,
                    true);
        } else {
            openCameraSource();
        }
    }

    private void signAndSetupStream() {
        String hexData = toHexString(selectedChannel.getName());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Object> options = new HashMap<>(2);
                    options.put("channel_id", selectedChannel.getClaimId());
                    options.put("hexdata", hexData);

                    AccountManager am = AccountManager.get(getContext());
                    String authToken = am.peekAuthToken(Helper.getOdyseeAccount(am.getAccounts()), "auth_token_type");
                    JSONObject result = (JSONObject) Lbry.authenticatedGenericApiCall("channel_sign", options, authToken);
                    String signature = result.getString("signature");
                    String signingTs = result.getString("signing_ts");
                    if (!Helper.isNullOrEmpty(signature) && !Helper.isNullOrEmpty(signingTs)) {
                        streamKey = createStreamKey(signature, signingTs);
                        Log.d("MYLOG", streamKey);
                    } else {
                        showError(getString(R.string.stream_key_not_generated));
                    }
                } catch (ApiCallException | JSONException ex) {
                    showError(ex.getMessage());
                }
            }
        });
        executor.shutdown();
    }

    private Claim buildLivestreamClaim() {
        Claim claim = new Claim();

        claim.setName("livestream-" + System.currentTimeMillis());
        claim.setAmount("0.01");

        Claim.StreamMetadata metadata = new Claim.StreamMetadata();
        metadata.setTitle(Helper.getValue(inputTitle.getText()));

        Claim selectedChannel = (Claim) selectChannelSpinner.getSelectedItem();
        if (selectedChannel != null && !selectedChannel.isPlaceholder() && !selectedChannel.isPlaceholderAnonymous()) {
            this.selectedChannel = selectedChannel;
            claim.setSigningChannel(selectedChannel);
        }

        claim.setValueType(Claim.TYPE_STREAM);
        claim.setValue(metadata);

        return claim;
    }

    private void publishLivestreamClaim() {
        Claim claim = buildLivestreamClaim();
        AccountManager am = AccountManager.get(getContext());
        String authToken = am.peekAuthToken(Helper.getOdyseeAccount(am.getAccounts()), "auth_token_type");
        PublishClaimTask task = new PublishClaimTask(claim, "", null, authToken, new ClaimResultHandler() {
            @Override
            public void beforeStart() {

            }

            @Override
            public void onSuccess(Claim claimResult) {
                signAndSetupStream();
            }

            @Override
            public void onError(Exception error) {
                showError(error.getMessage());
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void updateChannelList(List<Claim> channels) {
        selectChannelSpinner.setAdapter(new InlineChannelSpinnerAdapter(getContext(), R.layout.spinner_item_channel, channels));
    }

    private void fetchChannels() {
        if (Lbry.ownChannels != null && Lbry.ownChannels.size() > 0) {
            updateChannelList(Lbry.ownChannels);
            return;
        }

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
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private String createStreamKey(String signature, String signingTs) {
        String hexData = toHexString(selectedChannel.getName());
        return selectedChannel.getClaimId()
                + "?d=" + hexData
                + "&s=" + signature
                + "&t=" + signingTs;
    }

    private String toHexString(final String value) {
        final byte[] commentBodyBytes = value.getBytes(StandardCharsets.UTF_8);

        final String hexString;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1)
            hexString = Hex.encodeHexString(commentBodyBytes, false);
        else
            hexString = new String(Hex.encodeHex(commentBodyBytes));

        return hexString;
    }
}