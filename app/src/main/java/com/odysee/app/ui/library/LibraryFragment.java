package com.odysee.app.ui.library;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import com.odysee.app.adapter.CollectionListAdapter;
import com.odysee.app.adapter.PlaylistCollectionListAdapter;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.OdyseeCollection;
import com.odysee.app.runnable.DeleteViewHistoryItem;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.ClaimListAdapter;
import com.odysee.app.data.DatabaseHelper;
import com.odysee.app.listener.DownloadActionListener;
import com.odysee.app.listener.SelectionModeListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.LbryFile;
import com.odysee.app.model.ViewHistory;
import com.odysee.app.tasks.claim.ClaimListResultHandler;
import com.odysee.app.tasks.claim.ClaimListTask;
import com.odysee.app.tasks.claim.ClaimSearchResultHandler;
import com.odysee.app.tasks.claim.PurchaseListTask;
import com.odysee.app.tasks.claim.ResolveResultHandler;
import com.odysee.app.tasks.claim.ResolveTask;
import com.odysee.app.tasks.file.BulkDeleteFilesTask;
import com.odysee.app.tasks.file.FileListTask;
import com.odysee.app.tasks.localdata.FetchViewHistoryTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;

public class LibraryFragment extends BaseFragment implements
        ActionMode.Callback, DownloadActionListener, SelectionModeListener {

    private static final int FILTER_DOWNLOADS = 1;
    private static final int FILTER_PURCHASES = 2;
    private static final int FILTER_HISTORY = 3;
    private static final int PAGE_SIZE = 10;

    private ActionMode actionMode;
    private int currentFilter;
    private List<LbryFile> currentFiles;
    private RecyclerView contentList;
    private ClaimListAdapter contentListAdapter;
    private ProgressBar listLoading;
    private TextView linkFilterDownloads;
    private TextView linkFilterPurchases;
    private TextView linkFilterHistory;
    private View layoutListEmpty;
    private TextView textListEmpty;
    private int currentPage;
    private Date lastDate;
    private boolean listReachedEnd;
    private boolean contentListLoading;
    private boolean initialOwnClaimsFetched;
    private TextView textNoHistory;

    private CardView cardStats;
    private TextView linkStats;
    private TextView linkHide;
    private View viewStatsDistribution;
    private View viewVideoStatsBar;
    private View viewAudioStatsBar;
    private View viewImageStatsBar;
    private View viewOtherStatsBar;
    private TextView textStatsTotalSize;
    private TextView textStatsTotalSizeUnits;
    private TextView textStatsVideoSize;
    private TextView textStatsAudioSize;
    private TextView textStatsImageSize;
    private TextView textStatsOtherSize;
    private View legendVideo;
    private View legendAudio;
    private View legendImage;
    private View legendOther;

    private long totalBytes;
    private long totalVideoBytes;
    private long totalAudioBytes;
    private long totalImageBytes;
    private long totalOtherBytes;

    private RecyclerView recentList;
    private RecyclerView playlistsList;

    private View historyLinkView;
    private View favoritesLinkView;
    private View watchLaterLinkView;
    private ProgressBar playlistsLoading;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_library, container, false);

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        contentList = root.findViewById(R.id.library_list);
        contentList.setLayoutManager(llm);

        listLoading = root.findViewById(R.id.library_list_loading);
        linkFilterDownloads = root.findViewById(R.id.library_filter_link_downloads);
        linkFilterPurchases = root.findViewById(R.id.library_filter_link_purchases);
        linkFilterHistory = root.findViewById(R.id.library_filter_link_history);

        layoutListEmpty = root.findViewById(R.id.library_empty_container);
        textListEmpty = root.findViewById(R.id.library_list_empty_text);

        currentFilter = FILTER_HISTORY;
        linkFilterDownloads.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDownloads();
            }
        });
        linkFilterPurchases.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPurchases();
            }
        });
        linkFilterHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRecent();
            }
        });
        /*contentList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (contentListLoading) {
                    return;
                }

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null) {
                    int visibleItemCount = lm.getChildCount();
                    int totalItemCount = lm.getItemCount();
                    int pastVisibleItems = lm.findFirstVisibleItemPosition();
                    if (pastVisibleItems + visibleItemCount >= totalItemCount) {
                        if (!listReachedEnd) {
                            // load more
                            if (currentFilter == FILTER_DOWNLOADS) {
                                currentPage++;
                                fetchDownloads();
                            } else if (currentFilter == FILTER_HISTORY) {
                                fetchHistory();
                            }
                        }
                    }
                }
            }
        });*/

        // stats
        linkStats = root.findViewById(R.id.library_show_stats);
        linkHide = root.findViewById(R.id.library_hide_stats);
        cardStats = root.findViewById(R.id.library_storage_stats_card);
        viewStatsDistribution = root.findViewById(R.id.library_storage_stat_distribution);
        viewVideoStatsBar = root.findViewById(R.id.library_storage_stat_video_bar);
        viewAudioStatsBar = root.findViewById(R.id.library_storage_stat_audio_bar);
        viewImageStatsBar = root.findViewById(R.id.library_storage_stat_image_bar);
        viewOtherStatsBar = root.findViewById(R.id.library_storage_stat_other_bar);
        textStatsTotalSize = root.findViewById(R.id.library_storage_stat_used);
        textStatsTotalSizeUnits = root.findViewById(R.id.library_storage_stat_unit);
        textStatsVideoSize = root.findViewById(R.id.library_storage_stat_video_size);
        textStatsAudioSize = root.findViewById(R.id.library_storage_stat_audio_size);
        textStatsImageSize = root.findViewById(R.id.library_storage_stat_image_size);
        textStatsOtherSize = root.findViewById(R.id.library_storage_stat_other_size);
        legendVideo = root.findViewById(R.id.library_storage_legend_video);
        legendAudio = root.findViewById(R.id.library_storage_legend_audio);
        legendImage = root.findViewById(R.id.library_storage_legend_image);
        legendOther = root.findViewById(R.id.library_storage_legend_other);

        linkStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateStats();
                cardStats.setVisibility(View.VISIBLE);
                checkStatsLink();
            }
        });
        linkHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cardStats.setVisibility(View.GONE);
                checkStatsLink();
            }
        });

        LinearLayoutManager recentLLM = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recentList = root.findViewById(R.id.library_recent_list);
        recentList.setLayoutManager(recentLLM);

        LinearLayoutManager playlistsLLM = new LinearLayoutManager(getContext());
        playlistsList = root.findViewById(R.id.library_playlists_list);
        playlistsList.setLayoutManager(playlistsLLM);
        playlistsLoading = root.findViewById(R.id.library_playlists_loading);

        historyLinkView = root.findViewById(R.id.library_item_history);
        favoritesLinkView = root.findViewById(R.id.library_item_favorites);
        watchLaterLinkView = root.findViewById(R.id.library_item_watchlater);

        historyLinkView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // open history fragment
            }
        });

        favoritesLinkView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                if (context instanceof MainActivity) {
                    ((MainActivity) context).openPlaylistFragment(OdyseeCollection.BUILT_IN_ID_FAVORITES);
                }
            }
        });

        watchLaterLinkView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                if (context instanceof MainActivity) {
                    ((MainActivity) context).openPlaylistFragment(OdyseeCollection.BUILT_IN_ID_WATCHLATER);
                }
            }
        });

        return root;
    }

    public void onResume() {
        super.onResume();

        Context context = getContext();
        Helper.setWunderbarValue(null, context);
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            LbryAnalytics.setCurrentScreen(activity, "Library", "Library");
            //activity.addDownloadActionListener(this);
        }

        // renderFilter(); // Show tab according to selected filter
        showRecent();
        loadPlaylists();
    }

    private void loadPlaylists() {
        Helper.setViewVisibility(playlistsLoading, View.VISIBLE);
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Context context = getContext();
                if (context instanceof MainActivity) {
                    MainActivity activity = (MainActivity) context;

                    SQLiteDatabase db = activity.getDbHelper().getReadableDatabase();
                    Map<String, OdyseeCollection> collectionsMap = DatabaseHelper.loadAllCollections(db);
                    collectionsMap.remove(OdyseeCollection.BUILT_IN_ID_FAVORITES);
                    collectionsMap.remove(OdyseeCollection.BUILT_IN_ID_WATCHLATER);

                    List<OdyseeCollection> privateCollections = new ArrayList<>(collectionsMap.values());

                    // Also need to load published / public lists at this point
                    List<OdyseeCollection> collections = new ArrayList<>(privateCollections);
                    try {
                        List<OdyseeCollection> publicCollections = Lbry.loadOwnCollections(Lbryio.AUTH_TOKEN);
                        collections.addAll(publicCollections);
                    } catch (ApiCallException | JSONException ex) {
                        // pass
                    }
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            onPlaylistsLoaded(collections);
                        }
                    });
                }
            }
        });
    }

    private void onPlaylistsLoaded(List<OdyseeCollection> collections) {
        PlaylistCollectionListAdapter adapter = new PlaylistCollectionListAdapter(collections, getContext());
        adapter.setListener(new PlaylistCollectionListAdapter.ClickListener() {
            @Override
            public void onClick(OdyseeCollection collection, int position) {
                if (OdyseeCollection.PLACEHOLDER_ID_NEW.equalsIgnoreCase(collection.getId())) {
                    // new playlist
                    return;
                }
                // open the playlist
                Context context = getContext();
                if (context instanceof MainActivity) {
                    MainActivity activity = (MainActivity) context;
                    activity.openPlaylistFragment(collection.getId());
                }
            }
        });
        if (playlistsList != null) {
            playlistsList.setAdapter(adapter);
        }
        Helper.setViewVisibility(playlistsLoading, View.INVISIBLE);
    }

    public void onPause() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.removeDownloadActionListener(this);
        }
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity a = (MainActivity) context;
            a.switchClearViewHistoryButton(false);
        }
    }

    /**
     * Shows tab according to the filter which is selected
     */
    private void renderFilter() {
        if (currentFilter == FILTER_DOWNLOADS) {
            showDownloads();
        } else if (currentFilter == FILTER_HISTORY) {
            showRecent();
        } else if (currentFilter == FILTER_PURCHASES) {
            showPurchases();
        }
    }

    private void showDownloads() {
        currentFilter = FILTER_DOWNLOADS;
        linkFilterDownloads.setTypeface(null, Typeface.BOLD);
        linkFilterPurchases.setTypeface(null, Typeface.NORMAL);
        linkFilterHistory.setTypeface(null, Typeface.NORMAL);
        if (contentListAdapter != null) {
            contentListAdapter.setHideFee(false);
            contentListAdapter.clearItems();
            contentListAdapter.setCanEnterSelectionMode(true);
        }
        listReachedEnd = false;

        checkStatsLink();
        currentPage = 1;
        currentFiles = new ArrayList<>();
        if (Lbry.SDK_READY) {
            if (!initialOwnClaimsFetched) {
                fetchOwnClaimsAndShowDownloads();
            } else {
                fetchDownloads();
            }
        }
    }

    private void fetchOwnClaimsAndShowDownloads() {
        if (Lbry.ownClaims != null && Lbry.ownClaims.size() > 0) {
            initialOwnClaimsFetched = true;
            fetchDownloads();
            return;
        }

        linkStats.setVisibility(View.INVISIBLE);
        Map<String, Object> options = Lbry.buildClaimListOptions(
                Arrays.asList(Claim.TYPE_STREAM, Claim.TYPE_REPOST), 1, 999, true);
        ClaimListTask task = new ClaimListTask(options, listLoading, new ClaimListResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                Lbry.ownClaims = Helper.filterDeletedClaims(new ArrayList<>(claims));
                initialOwnClaimsFetched = true;
                if (currentFilter == FILTER_DOWNLOADS) {
                    fetchDownloads();
                }
                checkStatsLink();
            }

            @Override
            public void onError(Exception error) {
                initialOwnClaimsFetched = true;
                checkStatsLink();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void showPurchases() {
        currentFilter = FILTER_PURCHASES;
        linkFilterDownloads.setTypeface(null, Typeface.NORMAL);
        linkFilterPurchases.setTypeface(null, Typeface.BOLD);
        linkFilterHistory.setTypeface(null, Typeface.NORMAL);
        if (contentListAdapter != null) {
            contentListAdapter.setHideFee(true);
            contentListAdapter.clearItems();
            contentListAdapter.setCanEnterSelectionMode(false);
        }
        listReachedEnd = false;

        cardStats.setVisibility(View.GONE);
        checkStatsLink();

        currentPage = 1;
        if (Lbry.SDK_READY) {
            fetchPurchases();
        }
    }

    private void showRecent() {
        if (actionMode != null) {
            actionMode.finish();
        }
        listReachedEnd = false;

        cardStats.setVisibility(View.GONE);
        checkStatsLink();

        lastDate = null;
        loadRecent();
    }

    private void initRecentAdapter(List<Claim> claims) {
        Context context = getContext();
        if (context != null) {
            contentListAdapter = new ClaimListAdapter(claims, ClaimListAdapter.STYLE_SMALL_LIST_HORIZONTAL, context);
//          contentListAdapter.setCanEnterSelectionMode(currentFilter == FILTER_DOWNLOADS);
            contentListAdapter.setCanEnterSelectionMode(true);
            contentListAdapter.setSelectionModeListener(this);
            contentListAdapter.setListener(new ClaimListAdapter.ClaimListItemListener() {
                @Override
                public void onClaimClicked(Claim claim, int position) {
                    Context context = getContext();
                    if (context instanceof MainActivity) {
                        MainActivity activity = (MainActivity) context;
                        if (claim.getName().startsWith("@")) {
                            activity.openChannelUrl(claim.getPermanentUrl());
                        } else {
                            activity.openFileUrl(claim.getPermanentUrl());
                        }
                    }
                }
            });
        }
    }

    private void initContentListAdapter(List<Claim> claims) {
        Context context = getContext();
        if (context != null) {
            contentListAdapter = new ClaimListAdapter(claims, context);
//            contentListAdapter.setCanEnterSelectionMode(currentFilter == FILTER_DOWNLOADS);
            contentListAdapter.setCanEnterSelectionMode(true);
            contentListAdapter.setSelectionModeListener(this);
            contentListAdapter.setHideFee(currentFilter != FILTER_PURCHASES);
            contentListAdapter.setListener(new ClaimListAdapter.ClaimListItemListener() {
                @Override
                public void onClaimClicked(Claim claim, int position) {
                    Context context = getContext();
                    if (context instanceof MainActivity) {
                        MainActivity activity = (MainActivity) context;
                        if (claim.getName().startsWith("@")) {
                            activity.openChannelUrl(claim.getPermanentUrl());
                        } else {
                            activity.openFileUrl(claim.getPermanentUrl());
                        }
                    }
                }
            });
        }
    }

    private void fetchDownloads() {
        contentListLoading  = true;
        Helper.setViewVisibility(linkStats, View.GONE);
        Helper.setViewVisibility(layoutListEmpty, View.GONE);
        FileListTask task = new FileListTask(currentPage, PAGE_SIZE, true, listLoading, new FileListTask.FileListResultHandler() {
            @Override
            public void onSuccess(List<LbryFile> files, boolean hasReachedEnd) {
                listReachedEnd = hasReachedEnd;
                List<LbryFile> filteredFiles = Helper.filterDownloads(files);
                List<Claim> claims = Helper.claimsFromFiles(filteredFiles);
                addFiles(filteredFiles);
                updateStats();
                checkStatsLink();

                if (contentListAdapter == null) {
                    initContentListAdapter(claims);
                } else {
                    contentListAdapter.addItems(claims);
                }
                if (contentListAdapter != null && contentList.getAdapter() == null) {
                    contentList.setAdapter(contentListAdapter);
                }
                resolveMissingChannelNames(buildUrlsToResolve(claims));
                checkListEmpty();
                contentListLoading = false;
            }

            @Override
            public void onError(Exception error) {
                // pass
                checkStatsLink();
                checkListEmpty();
                contentListLoading = false;
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void fetchPurchases() {
        contentListLoading  = true;
        Helper.setViewVisibility(linkStats, View.GONE);
        Helper.setViewVisibility(layoutListEmpty, View.GONE);
        PurchaseListTask task = new PurchaseListTask(currentPage, PAGE_SIZE, listLoading, new ClaimSearchResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                listReachedEnd = hasReachedEnd;
                if (contentListAdapter == null) {
                    initContentListAdapter(claims);
                } else {
                    contentListAdapter.addItems(claims);
                }
                if (contentListAdapter != null && contentList.getAdapter() == null) {
                    contentList.setAdapter(contentListAdapter);
                }
                checkListEmpty();
                contentListLoading = false;
            }

            @Override
            public void onError(Exception error) {
                checkStatsLink();
                checkListEmpty();
                contentListLoading = false;
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void loadRecent() {
        contentListLoading = true;
        Helper.setViewVisibility(layoutListEmpty, View.GONE);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance();
        if (dbHelper != null) {
            FetchViewHistoryTask task = new FetchViewHistoryTask(lastDate, PAGE_SIZE, dbHelper, new FetchViewHistoryTask.FetchViewHistoryHandler() {
                @Override
                public void onSuccess(List<ViewHistory> history, boolean hasReachedEnd) {
                    listReachedEnd = hasReachedEnd;
                    if (history.size() > 0) {
                        lastDate = history.get(history.size() - 1).getTimestamp();
                    }
                    List<Claim> claims = Helper.claimsFromViewHistory(history);
                    if (contentListAdapter == null) {
                        initRecentAdapter(claims);
                    }
                    if (contentListAdapter != null && recentList.getAdapter() == null) {
                        recentList.setAdapter(contentListAdapter);
                    }
                    checkListEmpty();
                    contentListLoading = false;
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            checkListEmpty();
            contentListLoading = false;
        }
    }

    public void onDownloadAction(String downloadAction, String uri, String outpoint, String fileInfoJson, double progress) {
        if ("abort".equals(downloadAction)) {
            if (contentListAdapter != null) {
                contentListAdapter.clearFileForClaimOrUrl(outpoint, uri, currentFilter == FILTER_DOWNLOADS);
            }
            return;
        }

        try {
            JSONObject fileInfo = new JSONObject(fileInfoJson);
            LbryFile claimFile = LbryFile.fromJSONObject(fileInfo);
            String claimId = claimFile.getClaimId();
            if (contentListAdapter != null) {
                contentListAdapter.updateFileForClaimByIdOrUrl(claimFile, claimId, uri, true);
            }
        } catch (JSONException ex) {
            // invalid file info for download
        }
    }

    private void checkListEmpty() {
        boolean hasRecent = contentListAdapter != null && contentListAdapter.getItemCount() > 0;
        Helper.setViewVisibility(recentList, hasRecent ? View.VISIBLE : View.GONE);
        Helper.setViewVisibility(textNoHistory, !hasRecent ? View.VISIBLE : View.GONE);

        // The "Clear All" button is shown at the main app toolbar, so its visibility is changed from MainActivity instance
        MainActivity a = (MainActivity) getActivity();
        if (a != null) {
            a.switchClearViewHistoryButton(currentFilter == FILTER_HISTORY && contentListAdapter != null && contentListAdapter.getItemCount() > 0);
        }
    }

    @MainThread
    public void onViewHistoryCleared() {
        contentListAdapter.removeItems(contentListAdapter.getItems());
        checkListEmpty();
    }

    private void addFiles(List<LbryFile> files) {
        if (currentFiles == null) {
            currentFiles = new ArrayList<>();
        }
        for  (LbryFile file : files) {
            if (!currentFiles.contains(file)) {
                currentFiles.add(file);
            }
        }
    }

    private void updateStats() {
        totalBytes = 0;
        totalVideoBytes = 0;
        totalAudioBytes = 0;
        totalImageBytes = 0;
        totalOtherBytes = 0;
        if (currentFiles != null) {
            for (LbryFile file : currentFiles) {
                long writtenBytes = file.getWrittenBytes();
                String mime = file.getMimeType();
                if (mime != null) {
                    if (mime.startsWith("video/")) {
                        totalVideoBytes += writtenBytes;
                    } else if (mime.startsWith("audio/")) {
                        totalAudioBytes += writtenBytes;
                    } else if (mime.startsWith("image/")) {
                        totalImageBytes += writtenBytes;
                    } else {
                        totalOtherBytes += writtenBytes;
                    }
                }

                totalBytes += writtenBytes;
            }
        }

        renderStats();
    }

    private void renderStats() {
        String[] totalSizeParts = Helper.formatBytesParts(totalBytes, false);
        textStatsTotalSize.setText(totalSizeParts[0]);
        textStatsTotalSizeUnits.setText(totalSizeParts[1]);

        viewStatsDistribution.setVisibility(totalBytes > 0 ? View.VISIBLE : View.GONE);

        int percentVideo = normalizePercent((double) totalVideoBytes / (double) totalBytes * 100.0);
        legendVideo.setVisibility(totalVideoBytes > 0 ? View.VISIBLE : View.GONE);
        textStatsVideoSize.setText(Helper.formatBytes(totalVideoBytes, false));
        applyLayoutWeight(viewVideoStatsBar, percentVideo);

        int percentAudio = normalizePercent((double) totalAudioBytes / (double) totalBytes * 100.0);
        legendAudio.setVisibility(totalAudioBytes > 0 ? View.VISIBLE : View.GONE);
        textStatsAudioSize.setText(Helper.formatBytes(totalAudioBytes, false));
        applyLayoutWeight(viewAudioStatsBar, percentAudio);

        int percentImage = normalizePercent((double) totalImageBytes / (double) totalBytes * 100.0);
        legendImage.setVisibility(totalImageBytes > 0 ? View.VISIBLE : View.GONE);
        textStatsImageSize.setText(Helper.formatBytes(totalImageBytes, false));
        applyLayoutWeight(viewImageStatsBar, percentImage);

        int percentOther = normalizePercent((double) totalOtherBytes / (double) totalBytes * 100.0);
        legendOther.setVisibility(totalOtherBytes > 0 ? View.VISIBLE : View.GONE);
        textStatsOtherSize.setText(Helper.formatBytes(totalOtherBytes, false));
        applyLayoutWeight(viewOtherStatsBar, percentOther);

        // We have to get to 100 (or adjust the container accordingly)
        int totalPercent = percentVideo + percentAudio + percentImage + percentOther;
        ((LinearLayout) viewStatsDistribution).setWeightSum(totalPercent);
    }

    private void applyLayoutWeight(View view, int weight) {
        LinearLayout.LayoutParams params =  (LinearLayout.LayoutParams) view.getLayoutParams();
        params.weight = weight;
    }

    private static int normalizePercent(double value) {
        if (value > 0 && value < 1) {
            return 1;
        }
        return Double.valueOf(Math.floor(value)).intValue();
    }

    private void checkStatsLink() {
        linkStats.setVisibility(cardStats.getVisibility() == View.VISIBLE ||
                        listLoading.getVisibility() == View.VISIBLE ||
                        currentFilter != FILTER_DOWNLOADS ||
                        !Lbry.SDK_READY ?
                View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        this.actionMode = actionMode;
        actionMode.getMenuInflater().inflate(R.menu.menu_claim_list, menu);
        return true;
    }
    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        if (contentListAdapter != null) {
            contentListAdapter.clearSelectedItems();
            contentListAdapter.setInSelectionMode(false);
            contentListAdapter.notifyDataSetChanged();
        }
        this.actionMode = null;
    }

    @Override
    public boolean onPrepareActionMode(androidx.appcompat.view.ActionMode actionMode, Menu menu) {
        menu.findItem(R.id.action_edit).setVisible(false);
        return true;
    }

    @Override
    public boolean onActionItemClicked(androidx.appcompat.view.ActionMode actionMode, MenuItem menuItem) {
        if (R.id.action_delete == menuItem.getItemId()) {
            if (contentListAdapter != null && contentListAdapter.getSelectedCount() > 0) {
                Context context = getContext();
                if (context != null) {
                    final List<Claim> selectedClaims = new ArrayList<>(contentListAdapter.getSelectedItems());
                    if (currentFilter == FILTER_DOWNLOADS) {
                        String message = getResources().getQuantityString(R.plurals.confirm_delete_files, selectedClaims.size());
                        AlertDialog.Builder builder = new AlertDialog.Builder(context).
                                setTitle(R.string.delete_selection).
                                setMessage(message)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        handleDeleteSelectedClaims(selectedClaims);
                                    }
                                }).setNegativeButton(R.string.no, null);
                        builder.show();
                    } else if (currentFilter == FILTER_HISTORY) {
                        handleDeleteSelectedClaims(selectedClaims);
                    }
                }
                return true;
            }
        }

        return false;
    }

    private void handleDeleteSelectedClaims(List<Claim> selectedClaims) {
        List<String> claimIds = new ArrayList<>();
        for (Claim claim : selectedClaims) {
            claimIds.add(claim.getClaimId());
        }

        if (currentFilter == FILTER_DOWNLOADS) {
            new BulkDeleteFilesTask(claimIds).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            Lbry.unsetFilesForCachedClaims(claimIds);
            contentListAdapter.removeItems(selectedClaims);

            if (actionMode != null) {
                actionMode.finish();
            }
            View root = getView();
            if (root != null) {
                showMessage(getResources().getQuantityString(R.plurals.files_deleted, claimIds.size()));
            }
        } else if (currentFilter == FILTER_HISTORY) {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Activity a = getActivity();
            String message = getResources().getQuantityString(R.plurals.confirm_delete_files, selectedClaims.size());
            AlertDialog.Builder builder;
            if (a != null) {
                builder = new AlertDialog.Builder(a);
                builder.setTitle(R.string.delete_selection).setMessage(message)
                       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialogInterface, int i) {
                               Thread t = new Thread(new Runnable() {
                                   @Override
                                   public void run() {
                                       for (Claim c : selectedClaims) {
                                           try {
                                               Runnable r = new DeleteViewHistoryItem(c.getPermanentUrl());
                                               Future<?> f = executorService.submit(r);
                                               f.get();
                                               a.runOnUiThread(new Runnable() {
                                                   @Override
                                                   public void run() {
                                                       contentListAdapter.removeItem(c);
                                                   }
                                               });
                                           } catch (Exception e) {
                                               e.printStackTrace();
                                           }
                                       }

                                       a.runOnUiThread(new Runnable() {
                                           @Override
                                           public void run() {
                                               if (actionMode != null) {
                                                   actionMode.finish();
                                               }

                                               if (executorService != null && !executorService.isShutdown()) {
                                                   executorService.shutdown();
                                               }
                                           }
                                       });
                                   }
                               });
                               t.start();
                           }
                       })
                       .setNegativeButton(R.string.no, null);
                builder.show();
            }
        }
    }

    private List<String> buildUrlsToResolve(List<Claim> claims) {
        List<String> urls = new ArrayList<>();
        for (Claim claim : claims) {
            Claim channel = claim.getSigningChannel();
            if (channel != null && Helper.isNullOrEmpty(channel.getName()) && !Helper.isNullOrEmpty(channel.getClaimId())) {
                LbryUri uri = LbryUri.tryParse(String.format("%s#%s", claim.getName(), claim.getClaimId()));
                if (uri != null) {
                    urls.add(uri.toString());
                }
            }
        }
        return urls;
    }

    private void resolveMissingChannelNames(List<String> urls) {
        if (urls.size() > 0) {
            ResolveTask task = new ResolveTask(urls, Lbry.API_CONNECTION_STRING, null, new ResolveResultHandler() {
                @Override
                public void onSuccess(List<Claim> claims) {
                    boolean updated = false;
                    for (Claim claim : claims) {
                        if (claim.getClaimId() == null) {
                            continue;
                        }

                        if (contentListAdapter != null) {
                            contentListAdapter.updateSigningChannelForClaim(claim);
                            updated = true;
                        }
                    }
                    if (updated) {
                        contentListAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onError(Exception error) {

                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public void onEnterSelectionMode() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.startSupportActionMode(this);
        }
    }
    public void onItemSelectionToggled() {
        if (actionMode != null) {
            actionMode.setTitle(String.valueOf(contentListAdapter.getSelectedCount()));
            actionMode.invalidate();
        }
    }
    public void onExitSelectionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }
}
