package com.odysee.app.ui.publish;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.odysee.app.BuildConfig;
import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.GalleryGridAdapter;
import com.odysee.app.listener.CameraPermissionListener;
import com.odysee.app.listener.FilePickerListener;
import com.odysee.app.listener.StoragePermissionListener;
import com.odysee.app.model.GalleryItem;
import com.odysee.app.tasks.localdata.LoadGalleryItemsTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.LbryAnalytics;

public class PublishFragment extends BaseFragment implements
        CameraPermissionListener, FilePickerListener, StoragePermissionListener {

    private boolean cameraPreviewInitialized;
    private boolean storagePermissionRefusedOnce;
    private PreviewView cameraPreview;
    private RecyclerView galleryGrid;
    private GalleryGridAdapter adapter;
    private TextView noVideosLoaded;
    private View loading;

    private View buttonRecord;
    private View buttonTakePhoto;
    private View buttonUpload;

    private boolean loadGalleryItemsPending;
    private boolean launchFilePickerPending;
    private boolean recordPending;
    private boolean takePhotoPending;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_publish, container, false);

        noVideosLoaded = root.findViewById(R.id.publish_grid_no_videos);
        loading = root.findViewById(R.id.publish_grid_loading);
        cameraPreview = root.findViewById(R.id.publish_camera_preview);

        Context context = getContext();
        galleryGrid = root.findViewById(R.id.publish_video_grid);
        GridLayoutManager glm = new GridLayoutManager(context, 3);
        galleryGrid.setLayoutManager(glm);
        galleryGrid.addItemDecoration(new GalleryGridAdapter.GalleryGridItemDecoration(
                3, Helper.getScaledValue(3, context.getResources().getDisplayMetrics().density)));

        buttonRecord = root.findViewById(R.id.publish_record_button);
        buttonTakePhoto = root.findViewById(R.id.publish_photo_button);
        buttonUpload = root.findViewById(R.id.publish_upload_button);

        buttonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkCameraPermissionAndRecord();
            }
        });
        buttonTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkCameraPermissionAndTakePhoto();
            }
        });
        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkStoragePermissionAndLaunchFilePicker();
            }
        });

        return root;
    }

    private boolean cameraAvailable() {
        Context context = getContext();
        return context != null && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    private void showCameraPreview() {
        buttonRecord.setBackgroundColor(Color.TRANSPARENT);
        buttonTakePhoto.setBackgroundColor(Color.TRANSPARENT);
        displayPreviewWithCameraX();
    }

    private void displayPreviewWithCameraX() {
        Context context = getContext();
        if (MainActivity.hasPermission(Manifest.permission.CAMERA, context)) {
            cameraProviderFuture = ProcessCameraProvider.getInstance(context);
            cameraProviderFuture.addListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        cameraProvider = cameraProviderFuture.get();
                        if (cameraProvider != null) {
                            Preview preview = new Preview.Builder().build();
                            CameraSelector cameraSelector = new CameraSelector.Builder()
                                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                    .build();
                            cameraPreview.setScaleType(PreviewView.ScaleType.FILL_START);
                            preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
                            cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, preview);
                            cameraPreviewInitialized = true;
                        }
                    } catch (ExecutionException | IllegalArgumentException | InterruptedException ex) {
                        // pass
                    }
                }
            }, ContextCompat.getMainExecutor(context));
        }
    }

    private void checkCameraPermissionAndRecord() {
        Context context = getContext();
        if (!MainActivity.hasPermission(Manifest.permission.CAMERA, context)) {
            recordPending = true;
            MainActivity.requestPermission(
                    Manifest.permission.CAMERA,
                    MainActivity.REQUEST_CAMERA_PERMISSION,
                    getString(R.string.camera_permission_rationale_record),
                    context,
                    true);
        } else  {
            record();
        }
    }

    private void checkCameraPermissionAndTakePhoto() {
        Context context = getContext();
        if (!MainActivity.hasPermission(Manifest.permission.CAMERA, context)) {
            takePhotoPending = true;
            MainActivity.requestPermission(
                    Manifest.permission.CAMERA,
                    MainActivity.REQUEST_CAMERA_PERMISSION,
                    getString(R.string.camera_permission_rationale_photo),
                    context,
                    true);
        } else {
            takePhoto();
        }
    }

    private void takePhoto() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            takePhotoPending = false;
            ((MainActivity) context).requestTakePhoto();
        }
    }

    private void record() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            recordPending = false;
            ((MainActivity) context).requestVideoCapture();
        }
    }

    private void checkStoragePermissionAndLaunchFilePicker() {
        Context context = getContext();

        // Android 13 granular media permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (MainActivity.hasPermission(Manifest.permission.READ_MEDIA_AUDIO, context) &&
                    MainActivity.hasPermission(Manifest.permission.READ_MEDIA_IMAGES, context) &&
                    MainActivity.hasPermission(Manifest.permission.READ_MEDIA_VIDEO, context)) {
                if (!Environment.isExternalStorageManager()) {
                    // request for file access
                    launchFilePickerPending = true;
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).setManageExternalStoragePending(true);
                    }
                    requestManageExternalStorage();
                } else {
                    launchFilePickerPending = false;
                    launchFilePicker();
                }
            } else {
                launchFilePickerPending = true;
                MainActivity.requestPermissions(
                        new String[] { Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO },
                        MainActivity.REQUEST_STORAGE_PERMISSION,
                        getString(R.string.storage_permission_rationale_videos),
                        context,
                        true);
            }

            return;
        }

        // Android 12 and below
        if (MainActivity.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE, context)) {
            launchFilePickerPending = false;
            launchFilePicker();
        } else {
            launchFilePickerPending = true;
            MainActivity.requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                    MainActivity.REQUEST_STORAGE_PERMISSION,
                    getString(R.string.storage_permission_rationale_images),
                    context,
                    true);
        }
    }

    private void launchFilePicker() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity.startingFilePickerActivity = true;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            ((MainActivity) context).startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.upload_file)),
                    MainActivity.REQUEST_FILE_PICKER);
        }
    }

    public void onResume() {
        super.onResume();
        Context context = getContext();
        Helper.setWunderbarValue(null, context);
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            LbryAnalytics.setCurrentScreen(activity, "Publish", "Publish");
            activity.addCameraPermissionListener(this);
            activity.addFilePickerListener(this);
            activity.addStoragePermissionListener(this);

            if (cameraAvailable() && MainActivity.hasPermission(Manifest.permission.CAMERA, context)) {
                showCameraPreview();
            }
        }

        if (!storagePermissionRefusedOnce) {
            checkStoragePermissionAndLoadVideos();
        }
    }

    @SuppressLint("RestrictedApi")
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
        if (cameraPreviewInitialized) {
            cameraProvider.unbindAll();
        }
        super.onStop();
    }

    private void checkStoragePermissionAndLoadVideos() {
        Context context = getContext();

        // Android 13 granular media permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (MainActivity.hasPermission(Manifest.permission.READ_MEDIA_AUDIO, context) &&
                MainActivity.hasPermission(Manifest.permission.READ_MEDIA_IMAGES, context) &&
                MainActivity.hasPermission(Manifest.permission.READ_MEDIA_VIDEO, context)) {
                loadGalleryItems();
            } else {
                loadGalleryItemsPending = true;
                MainActivity.requestPermissions(
                        new String[] { Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO },
                        MainActivity.REQUEST_STORAGE_PERMISSION,
                        getString(R.string.storage_permission_rationale_videos),
                        context,
                        true);
            }

            return;
        }

        // Android 12 and below
        if (MainActivity.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE, context)) {
            loadGalleryItems();
        } else {
            loadGalleryItemsPending = true;
            MainActivity.requestPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    MainActivity.REQUEST_STORAGE_PERMISSION,
                    getString(R.string.storage_permission_rationale_videos),
                    context,
                    true);
        }
    }

    private void loadGalleryItems() {
        Context context = getContext();
        Helper.setViewVisibility(noVideosLoaded, View.GONE);
        LoadGalleryItemsTask task = new LoadGalleryItemsTask(loading, context, new LoadGalleryItemsTask.LoadGalleryHandler() {
            @Override
            public void onItemLoaded(GalleryItem item) {
                if (context != null) {
                    if (adapter == null) {
                        adapter = new GalleryGridAdapter(Arrays.asList(item), context);
                        adapter.setListener(new GalleryGridAdapter.GalleryItemClickListener() {
                            @Override
                            public void onGalleryItemClicked(GalleryItem item) {
                                Context context = getContext();
                                if (context instanceof MainActivity) {
                                    Map<String, Object> params = new HashMap<>();
                                    params.put("galleryItem", item);
                                    params.put("suggestedUrl", getSuggestedPublishUrl());
                                    ((MainActivity) context).openFragment(PublishFormFragment.class, true, params);
                                }
                            }
                        });
                    } else {
                        adapter.addItem(item);
                    }

                    if (galleryGrid.getAdapter() == null) {
                        galleryGrid.setAdapter(adapter);
                    }
                    Helper.setViewVisibility(loading, adapter == null || adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onAllItemsLoaded(List<GalleryItem> items) {
                if (context != null) {
                    if (adapter == null) {
                        adapter = new GalleryGridAdapter(items, context);
                    } else {
                        adapter.addItems(items);
                    }

                    if (galleryGrid.getAdapter() == null) {
                        galleryGrid.setAdapter(adapter);
                    }
                }
                checkNoVideosLoaded();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void checkNoVideosLoaded() {
        Helper.setViewVisibility(noVideosLoaded, adapter == null || adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCameraPermissionGranted() {
        if (recordPending) {
            // record video
            record();
        }/* else if (takePhotoPending) {
            // take a photo
            takePhoto();
        }*/
    }

    @Override
    public void onCameraPermissionRefused() {
        /*if (takePhotoPending) {
            takePhotoPending = false;
            showError(getString(R.string.camera_permission_rationale_photo));
            return;
        }*/

        recordPending = false;
        showError(getString(R.string.camera_permission_rationale_record));
    }

    @Override
    public void onRecordAudioPermissionGranted() {

    }

    @Override
    public void onRecordAudioPermissionRefused() {

    }

    @Override
    public void onStoragePermissionGranted() {
        if (loadGalleryItemsPending) {
            loadGalleryItemsPending = false;
            loadGalleryItems();
        }
        if (launchFilePickerPending) {
            // one more permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!Environment.isExternalStorageManager()) {
                    launchFilePickerPending = true;
                    Context context = getContext();
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).setManageExternalStoragePending(true);
                    }
                    requestManageExternalStorage();
                } else {
                    launchFilePickerPending = false;
                    launchFilePicker();
                }
            } else {
                launchFilePickerPending = false;
                launchFilePicker();
            }
        }
    }

    private void requestManageExternalStorage() {
        try {
            // if for some reason, the package uri
            Uri packageUri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, packageUri);
            startActivityForResult(intent, MainActivity.REQUEST_MANAGE_STORAGE_PERMISSION);
        } catch (Exception ex){
            // If for some reason, the package uri was not found and it results in an error, show the screen for all permissions
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivityForResult(intent, MainActivity.REQUEST_MANAGE_STORAGE_PERMISSION);
        }
    }

    @Override
    public void onManageExternalStoragePermissionGranted() {
        launchFilePickerPending = false;
        launchFilePicker();
    }

    @Override
    public void onManageExternalStoragePermissionRefused() {
        storagePermissionRefusedOnce = true;
        View root = getView();
        if (root != null) {
            showError(getString(R.string.storage_permission_rationale_videos));
        }
    }

    @Override
    public void onStoragePermissionRefused() {
        storagePermissionRefusedOnce = true;
        View root = getView();
        if (root != null) {
            showError(getString(R.string.storage_permission_rationale_videos));
            Helper.setViewText(noVideosLoaded, R.string.storage_permission_rationale_videos);
        }

        checkNoVideosLoaded();
    }

    public String getSuggestedPublishUrl() {
        Map<String, Object> params = getParams();
        if (params != null && params.containsKey("suggestedUrl")) {
            return (String) params.get("suggestedUrl");
        }
        return null;
    }

    @Override
    public boolean shouldHideGlobalPlayer() {
        return true;
    }

    @Override
    public boolean shouldSuspendGlobalPlayer() {
        return true;
    }

    @Override
    public void onFilePicked(String filePath, Uri intentData) {
        Context context = getContext();
        if (context instanceof MainActivity) {
            Map<String, Object> params = new HashMap<>();
            params.put("directFilePath", filePath);
            params.put("suggestedUrl", getSuggestedPublishUrl());
            ((MainActivity) context).openFragment(PublishFormFragment.class, true, params);
        }
    }

    @Override
    public void onFilePickerCancelled() {

    }
}
