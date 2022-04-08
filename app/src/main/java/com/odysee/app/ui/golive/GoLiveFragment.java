package com.odysee.app.ui.golive;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

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
import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.InlineChannelSpinnerAdapter;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.listener.CameraPermissionListener;
import com.odysee.app.listener.FilePickerListener;
import com.odysee.app.listener.StoragePermissionListener;
import com.odysee.app.model.Claim;
import com.odysee.app.tasks.UploadImageTask;
import com.odysee.app.tasks.claim.ClaimListResultHandler;
import com.odysee.app.tasks.claim.ClaimListTask;
import com.odysee.app.tasks.claim.ClaimResultHandler;
import com.odysee.app.tasks.claim.PublishClaimTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoLiveFragment extends BaseFragment implements
        CameraPermissionListener,
        StoragePermissionListener,
        FilePickerListener {
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

    private boolean launchPickerPending;
    private boolean uploadingThumbnail;
    private String uploadedThumbnailUrl;
    private String lastSelectedThumbnailFile;
    private boolean isStreaming = false;
    private boolean startingStream = false;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_go_live, container, false);

        imagePrecheckSpaceman = root.findViewById(R.id.livestream_precheck_spaceman_image);
        textPrecheckStatus = root.findViewById(R.id.livestream_precheck_status_text);
        selectChannelSpinner = root.findViewById(R.id.livestream_options_select_channel_spinner);
        textChannelError = root.findViewById(R.id.livestream_options_channel_error_text);
        inputTitle = root.findViewById(R.id.livestream_options_title_input);
        progress = root.findViewById(R.id.livestream_progress);
        imageThumbnail = root.findViewById(R.id.livestream_options_thumbnail_preview);
        thumbnailUploadProgress = root.findViewById(R.id.livestream_options_thumbnail_upload_progress);

        livestreamPrecheckView = root.findViewById(R.id.livestream_precheck);
        livestreamOptionsView = root.findViewById(R.id.livestream_options);
        livestreamControlsView = root.findViewById(R.id.livestream_controls);

        HkSurfaceView livestreamControlsCameraView = root.findViewById(R.id.livestream_controls_camera_view);
        livestreamControlsCameraView.attachStream(stream);

        MaterialButton buttonContinue = root.findViewById(R.id.livestream_options_continue_button);
        buttonToggleStreaming = root.findViewById(R.id.livestream_controls_toggle_streaming_button);
        ImageButton buttonSwitchCamera = root.findViewById(R.id.livestream_controls_switch_camera_button);

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
                livestreamControlsView.setVisibility(View.VISIBLE);
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
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                    startingStream = true;
                    connection.connect(RTMP_URL);
                    buttonToggleStreaming.setText(R.string.stop_streaming);
                } else {
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
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

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            LbryAnalytics.setCurrentScreen(activity, "Go Live", "GoLive");
            activity.addCameraPermissionListener(this);
            activity.addStoragePermissionListener(this);
            activity.addFilePickerListener(this);
        }
        checkCameraPermissionAndOpenCameraSource();
        fetchChannels();
    }

    @Override
    public void onStop() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.removeCameraPermissionListener(this);
            activity.removeStoragePermissionListener(this);
            if (!MainActivity.startingFilePickerActivity) {
                activity.removeFilePickerListener(this);
            }
        }
        super.onStop();
    }

    @Override
    public void onFilePicked(String filePath) {
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

    @Override
    public void onFilePickerCancelled() {

    }

    @Override
    public void onCameraPermissionGranted() {
        openCameraSource();
    }

    @Override
    public void onCameraPermissionRefused() {
        showError(getString(R.string.camera_permission_rationale_livestream));
    }

    @Override
    public void onStoragePermissionGranted() {
        if (launchPickerPending) {
            launchPickerPending = false;
            launchFilePicker();
        }
    }

    @Override
    public void onStoragePermissionRefused() {
        showError(getString(R.string.storage_permission_rationale_images));
        launchPickerPending = false;
    }

    @Override
    public void onRecordAudioPermissionGranted() {

    }

    @Override
    public void onRecordAudioPermissionRefused() {

    }

    @Override
    public boolean shouldSuspendGlobalPlayer() {
        return true;
    }

    private void uploadThumbnail(String thumbnailPath) {
        if (uploadingThumbnail) {
            View view = getView();
            if (view != null) {
                Snackbar.make(view, R.string.wait_for_upload, Snackbar.LENGTH_LONG).show();
            }
            return;
        }

        Context context = getContext();
        if (context != null) {
            Glide.with(context.getApplicationContext()).load(thumbnailPath).centerCrop().into(imageThumbnail);
        }

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
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity.startingFilePickerActivity = true;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            ((MainActivity) context).startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.select_thumbnail)),
                    MainActivity.REQUEST_FILE_PICKER);
        }
    }

    private void checkStoragePermissionAndLaunchFilePicker() {
        Context context = getContext();
        if (!MainActivity.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE, context)) {
            launchPickerPending = true;
            MainActivity.requestPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    MainActivity.REQUEST_STORAGE_PERMISSION,
                    getString(R.string.storage_permission_rationale_images),
                    context,
                    true);
        } else {
            launchPickerPending = false;
            launchFilePicker();
        }
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
                    getString(R.string.camera_permission_rationale_livestream),
                    context,
                    true);
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

                    AccountManager am = AccountManager.get(getContext());
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
        AccountManager am = AccountManager.get(getContext());
        String authToken = am.peekAuthToken(Helper.getOdyseeAccount(am.getAccounts()), "auth_token_type");
        PublishClaimTask task = new PublishClaimTask(claim, "", null, authToken, new ClaimResultHandler() {
            @Override
            public void beforeStart() {
                buttonToggleStreaming.setEnabled(false);
            }

            @Override
            public void onSuccess(Claim claimResult) {
                signAndSetupStream();
                progress.setVisibility(View.GONE);
                buttonToggleStreaming.setEnabled(true);
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
        selectChannelSpinner.setAdapter(new InlineChannelSpinnerAdapter(getContext(), R.layout.spinner_item_channel, channels));

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

    private String createStreamKey(String signature, String signingTs) {
        String hexData = Helper.toHexString(selectedChannel.getName());
        return selectedChannel.getClaimId()
                + "?d=" + hexData
                + "&s=" + signature
                + "&t=" + signingTs;
    }
}
