package com.odysee.app.ui.library;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.ClaimListAdapter;
import com.odysee.app.data.DatabaseHelper;
import com.odysee.app.listener.DownloadActionListener;
import com.odysee.app.listener.SelectionModeListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.OdyseeCollection;
import com.odysee.app.tasks.claim.ResolveResultHandler;
import com.odysee.app.tasks.claim.ResolveTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryUri;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class PlaylistFragment extends BaseFragment implements
        ActionMode.Callback, DownloadActionListener, SelectionModeListener {

    private ClaimListAdapter adapter;
    private RecyclerView playlistList;
    private MaterialButton playButton;
    private ProgressBar playlistItemsLoading;

    private TextView textTitle;
    private TextView textVideoCount;
    private ImageView visibilityIcon;
    private OdyseeCollection currentCollection;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_playlist, container, false);

        textTitle = root.findViewById(R.id.playlist_title);
        textVideoCount = root.findViewById(R.id.playlist_video_count);
        playlistItemsLoading = root.findViewById(R.id.playlist_items_loading);
        visibilityIcon = root.findViewById(R.id.playlist_icon);

        playlistList = root.findViewById(R.id.playlist_items);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        playlistList.setLayoutManager(llm);

        playButton = root.findViewById(R.id.playlist_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                if (currentCollection != null && context instanceof MainActivity) {
                    MainActivity activity = (MainActivity) context;
                    // openPrivatePlaylist also works for public (published) collections, because we've already  loaded and resolved
                    // so we use it, because it's faster, instead of  having to re-resolve
                    activity.openPrivatePlaylist(currentCollection);
                }
            }
        });

        return root;
    }

    private void loadPlaylist() {
        Map<String, Object> params = getParams();
        String collectionIdParam = null;
        if (params.containsKey("collectionId")) {
            collectionIdParam = (String) params.get("collectionId");
        }

        final String collectionId = collectionIdParam;
        if (Lbry.isOwnedCollection(collectionId)) {
            // Check if it's edited and return the updated one, otherwise, just get what we have

            OdyseeCollection collection = Lbry.getOwnCollectionById(collectionId);
            if (collection != null) {
                onPlaylistLoaded(collection);
            }
        } else {
            Context context = getContext();
            if (context instanceof MainActivity) {
                MainActivity activity = (MainActivity) context;

                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SQLiteDatabase db = activity.getDbHelper().getReadableDatabase();
                            OdyseeCollection collection = DatabaseHelper.loadCollection(collectionId, db);

                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    onPlaylistLoaded(collection);
                                }
                            });
                        } catch (SQLiteException ex) {
                            showError(getString(R.string.could_not_load_playlist));
                        }
                    }
                });
            }
        }
    }

    private void onPlaylistLoaded(OdyseeCollection collection) {
        currentCollection = collection;
        if (visibilityIcon != null) {
            visibilityIcon.setImageResource(collection.getVisibility() == OdyseeCollection.VISIBILITY_PRIVATE ?
                    R.drawable.ic_private : R.drawable.ic_public);
        }
        Helper.setViewText(textTitle, collection.getName());
        Helper.setViewText(textVideoCount, getResources().getQuantityString(R.plurals.video_count, collection.getItems().size(), collection.getItems().size()));

        // load the claims
        ResolveTask task = new ResolveTask(collection.getItems(), Lbry.API_CONNECTION_STRING, playlistItemsLoading, new ResolveResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims) {
                // reorder the claims based on the order in the playlist collection, TODO: find a more efficient way to do this
                Map<String, Claim> playlistClaimMap = new LinkedHashMap<>();
                List<String> claimIds = new ArrayList<>();
                List<String> collectionItems = collection.getItems();
                for (int i = 0; i < collectionItems.size(); i++) {
                    LbryUri url = LbryUri.tryParse(collectionItems.get(i));
                    if (url != null) {
                        claimIds.add(url.getClaimId());
                    }
                }
                for (String id : claimIds) {
                    for (Claim claim : claims) {
                        if (id.equalsIgnoreCase(claim.getClaimId())) {
                            playlistClaimMap.put(id, claim);
                            break;
                        }
                    }
                }

                collection.setClaims(new ArrayList<>(playlistClaimMap.values()));

                adapter = new ClaimListAdapter(collection.getClaims(), ClaimListAdapter.STYLE_SMALL_LIST, getContext());
                adapter.setListener(new ClaimListAdapter.ClaimListItemListener() {
                    @Override
                    public void onClaimClicked(Claim claim, int position) {
                        Context context = getContext();
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).openPrivatePlaylist(collection, claim, position);
                        }
                    }
                });
                if (playlistList != null) {
                    playlistList.setAdapter(adapter);
                }
            }

            @Override
            public void onError(Exception error) {
                // pass
                showError(getString(R.string.could_not_load_playlist));
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPlaylist();
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.updateMiniPlayerMargins(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.updateMiniPlayerMargins(true);
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }

    @Override
    public void onDownloadAction(String downloadAction, String uri, String outpoint, String fileInfoJson, double progress) {

    }

    @Override
    public void onEnterSelectionMode() {

    }

    @Override
    public void onExitSelectionMode() {

    }

    @Override
    public void onItemSelectionToggled() {

    }
}
