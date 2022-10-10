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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.odysee.app.MainActivity;
import com.odysee.app.OdyseeApp;
import com.odysee.app.R;
import com.odysee.app.adapter.ClaimListAdapter;
import com.odysee.app.data.DatabaseHelper;
import com.odysee.app.dialog.PublishPlaylistDialogFragment;
import com.odysee.app.dialog.RepostClaimDialogFragment;
import com.odysee.app.listener.DownloadActionListener;
import com.odysee.app.listener.SelectionModeListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.OdyseeCollection;
import com.odysee.app.model.Tag;
import com.odysee.app.model.lbryinc.Subscription;
import com.odysee.app.tasks.claim.ResolveResultHandler;
import com.odysee.app.tasks.claim.ResolveTask;
import com.odysee.app.tasks.wallet.LoadSharedUserStateTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class PlaylistFragment extends BaseFragment implements
        ActionMode.Callback, DownloadActionListener, SelectionModeListener {

    private ClaimListAdapter adapter;
    private RecyclerView playlistList;
    private MaterialButton playButton;
    private MaterialButton publishButton;
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
        setupDragAndDropForReordering();

        playButton = root.findViewById(R.id.playlist_play);
        publishButton = root.findViewById(R.id.playlist_publish);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                if (currentCollection != null && context instanceof MainActivity) {
                    MainActivity activity = (MainActivity) context;
                    // openPrivatePlaylist also works for public (published) collections, because we've already loaded and resolved
                    // so we use it, because it's faster, instead of  having to re-resolve
                    activity.openPrivatePlaylist(currentCollection);
                }
            }
        });

        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PublishPlaylistDialogFragment dialog = PublishPlaylistDialogFragment.newInstance(
                        currentCollection, new PublishPlaylistDialogFragment.PublishPlaylistListener() {
                    @Override
                    public void onPlaylistPublished(Claim claim) {
                        //showMessage(R.string.playlist_successfully_published);
                        Helper.setViewVisibility(publishButton, View.GONE);

                        // remove the playlist from editedCollections
                        removeFromEditedCollections(claim.getClaimId());
                    }
                });
                Context context = getContext();
                if (context instanceof MainActivity) {
                    dialog.show(((MainActivity) context).getSupportFragmentManager(), PublishPlaylistDialogFragment.TAG);
                }
            }
        });

        return root;
    }

    private void setupDragAndDropForReordering() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                        ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.START | ItemTouchHelper.END);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if (adapter != null) {
                    Collections.swap(adapter.getUnderlyingItems(), viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                    adapter.notifyItemMoved(viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                    adapter.recalculateItemOrders();

                    if (currentCollection != null) {
                        currentCollection.setItemsFromStringList(adapter.getItems().stream().map(claim -> claim.getPermanentUrl()).collect(Collectors.toList()));

                        // save the collection
                        Context context = getContext();
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).handleSaveCollection(currentCollection);
                        }
                    }

                    return true;
                }

                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            }
        };

        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(playlistList);
    }

    private void removeFromEditedCollections(String claimId) {

    }

    private void loadPlaylist() {
        Map<String, Object> params = getParams();
        String collectionIdParam = null;
        if (params.containsKey("collectionId")) {
            collectionIdParam = (String) params.get("collectionId");
        }

        Context context = getContext();
        final String collectionId = collectionIdParam;

        if (context instanceof MainActivity) {
            ExecutorService executor = ((OdyseeApp) ((MainActivity) context).getApplication()).getExecutor();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Try to load from local state first
                        SQLiteDatabase db = MainActivity.getDatabaseHelper().getReadableDatabase();
                        OdyseeCollection collection = DatabaseHelper.loadCollection(collectionId, db);

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                boolean playlistFound = false;
                                if (collection != null) {
                                    // Additionally, get the actualClaim if it's a published playlist and we have an existing reference
                                    if (collection.getVisibility() == OdyseeCollection.VISIBILITY_PUBLIC &&
                                            Lbry.isOwnedCollection(collectionId)) {
                                        OdyseeCollection remoteCollection = Lbry.getOwnCollectionById(collectionId);
                                        if (remoteCollection != null) {
                                            collection.setActualClaim(remoteCollection.getActualClaim());
                                        }
                                    }

                                    onPlaylistLoaded(collection);
                                    playlistFound = true;
                                } else {
                                    if (Lbry.isOwnedCollection(collectionId)) {
                                        OdyseeCollection collection = Lbry.getOwnCollectionById(collectionId);
                                        if (collection != null) {
                                            onPlaylistLoaded(collection);
                                            playlistFound = true;
                                        }
                                    }
                                }

                                if (!playlistFound) {
                                    showError(getString(R.string.could_not_load_playlist));
                                }
                            }
                        });
                    } catch (SQLiteException ex) {
                        showError(getString(R.string.could_not_load_playlist));
                    }
                }
            });
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

        checkCanPublishPlaylist();

        // load the claims
        List<String> collectionItemUrls = collection.getItems().stream().map(OdyseeCollection.Item::getUrl).collect(Collectors.toList());
        ResolveTask task = new ResolveTask(collectionItemUrls, Lbry.API_CONNECTION_STRING, playlistItemsLoading, new ResolveResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims) {
                // reorder the claims based on the order in the playlist collection, TODO: find a more efficient way to do this
                Map<String, Claim> playlistClaimMap = new LinkedHashMap<>();
                List<String> claimIds = new ArrayList<>();
                List<OdyseeCollection.Item> collectionItems = collection.getItems();
                for (int i = 0; i < collectionItems.size(); i++) {
                    LbryUri url = LbryUri.tryParse(collectionItems.get(i).getUrl());
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
                adapter.setLongClickForContextMenu(true);
                adapter.setOwnCollection(true);
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

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (currentCollection != null && item.getItemId() == R.id.action_remove_from_list) {
            String id = currentCollection.getId();
            int position = adapter.getCurrentPosition();
            Claim claim = adapter.getItems().get(position);
            String url = claim.getPermanentUrl();

            DatabaseHelper.removeCollectionItem(id, url, DatabaseHelper.getInstance().getWritableDatabase());

            loadPlaylist();
            Context context = getContext();
            if (context instanceof MainActivity) {
                MainActivity activity = (MainActivity) context;
                activity.saveSharedUserState();

                Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag("LIBRARY");
                if (fragment instanceof LibraryFragment) {
                    ((LibraryFragment) fragment).loadPlaylists();
                }
            }
        }
        return super.onContextItemSelected(item);
    }

    private void checkCanPublishPlaylist() {
        if (currentCollection.getItems() == null || currentCollection.getItems().size() == 0) {
            Helper.setViewVisibility(publishButton, View.GONE);
            return;
        }

        // publish button should only be shown if it's a private playlist
        // or if the playlist has been updated (check for ID in the wallet state's editedCollections)
        boolean canPublish = Helper.isNullOrEmpty(currentCollection.getClaimId());
        if (canPublish) {
            Helper.setViewVisibility(publishButton, View.VISIBLE);
            return;
        }

        // retrieve the user state to check if the claimId can be found in editedCollections
        Context context = getContext();
        LoadSharedUserStateTask task = new LoadSharedUserStateTask(context, new LoadSharedUserStateTask.LoadSharedUserStateHandler() {
            @Override
            public void onSuccess(List<Subscription> subscriptions, List<Tag> followedTags, List<LbryUri> blockedChannels,
                                  List<String> editedCollectionClaimIds) {
                boolean hasPendingChanges = editedCollectionClaimIds != null && editedCollectionClaimIds.contains(currentCollection.getClaimId());
                Helper.setViewVisibility(publishButton, hasPendingChanges ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception error) {

            }
        }, Lbryio.AUTH_TOKEN);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
