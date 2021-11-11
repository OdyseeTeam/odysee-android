package com.odysee.app.ui.channel;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.ClaimListAdapter;
import com.odysee.app.model.Claim;
import com.odysee.app.tasks.claim.ClaimSearchResultHandler;
import com.odysee.app.tasks.claim.ClaimSearchTask;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.Predefined;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.Setter;

public class ChannelPlaylistsFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Setter
    private String channelId;
    private RecyclerView playlistsList;
    private View contentLoading;
    private View bigPlaylistsLoading;
    private View noPlaylistsView;
    private ClaimListAdapter playlistsListAdapter;
    private boolean playlistsClaimSearchLoading;
    private boolean playlistsHasReachedEnd;
    private int currentClaimSearchPage;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_channel_playlists, container, false);

        bigPlaylistsLoading = root.findViewById(R.id.channel_playlists_list_progress);
        contentLoading = root.findViewById(R.id.channel_content_load_progress);
        noPlaylistsView = root.findViewById(R.id.channel_playlists_no_claim_search_lists);

        playlistsList = root.findViewById(R.id.channel_playlists_list);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        playlistsList.setLayoutManager(llm);
        playlistsList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (playlistsClaimSearchLoading) {
                    return;
                }

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null) {
                    int visibleItemCount = lm.getChildCount();
                    int totalItemCount = lm.getItemCount();
                    int pastVisibleItems = lm.findFirstVisibleItemPosition();
                    if (pastVisibleItems + visibleItemCount >= totalItemCount) {
                        if (!playlistsHasReachedEnd) {
                            // load more
                            currentClaimSearchPage++;
                            fetchClaimSearchPlaylists();
                        }
                    }
                }
            }
        });

        return root;
    }

    public void onResume() {
        super.onResume();
        Context context = getContext();
        if (context != null) {
            PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
        }
        fetchClaimSearchPlaylists();
    }

    public void onPause() {
        Context context = getContext();
        if (context != null) {
            PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
        }
        super.onPause();
    }

    private Map<String, Object> buildPlaylistsOptions() {
        Context context = getContext();
        boolean canShowMatureContent = false;
        if (context != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            canShowMatureContent = sp.getBoolean(MainActivity.PREFERENCE_KEY_SHOW_MATURE_CONTENT, false);
        }

        return Lbry.buildClaimSearchOptions(
                Collections.singletonList(Claim.TYPE_COLLECTION),
                null,
                canShowMatureContent ? null : new ArrayList<>(Predefined.MATURE_TAGS),
                !Helper.isNullOrEmpty(channelId) ? Collections.singletonList(channelId) : null,
                null,
                null,
                null,
                null,
                0,
                0,
                currentClaimSearchPage == 0 ? 1 : currentClaimSearchPage,
                Helper.CONTENT_PAGE_SIZE);
    }

    private View getLoadingView() {
        return (playlistsListAdapter == null || playlistsListAdapter.getItemCount() == 0) ? bigPlaylistsLoading : contentLoading;
    }

    private void fetchClaimSearchPlaylists() {
        fetchClaimSearchPlaylists(false);
    }

    private void fetchClaimSearchPlaylists(boolean reset) {
        if (reset && playlistsListAdapter != null) {
            playlistsListAdapter.clearItems();
            currentClaimSearchPage = 1;
        }

        playlistsClaimSearchLoading = true;
        Helper.setViewVisibility(noPlaylistsView, View.GONE);
        Map<String, Object> claimSearchOptions = buildPlaylistsOptions();
        // channel claim
        ClaimSearchTask playlistsClaimSearchTask = new ClaimSearchTask(claimSearchOptions, Lbry.API_CONNECTION_STRING, getLoadingView(), new ClaimSearchResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                claims = Helper.filterClaimsByOutpoint(claims);

                if (playlistsListAdapter == null) {
                    Context context = getContext();
                    if (context != null) {
                        playlistsListAdapter = new ClaimListAdapter(claims, context);
                        playlistsListAdapter.setListener(new ClaimListAdapter.ClaimListItemListener() {
                            @Override
                            public void onClaimClicked(Claim claim) {
                                Context context = getContext();
                                if (context instanceof MainActivity) {
                                    MainActivity activity = (MainActivity) context;
                                    activity.openFileClaim(claim);
                                }
                            }
                        });
                    }
                } else {
                    playlistsListAdapter.addItems(claims);
                }

                if (playlistsList != null && playlistsList.getAdapter() == null) {
                    playlistsList.setAdapter(playlistsListAdapter);
                }

                playlistsHasReachedEnd = hasReachedEnd;
                playlistsClaimSearchLoading = false;
                checkNoPlaylists();
            }

            @Override
            public void onError(Exception error) {
                playlistsClaimSearchLoading = false;
                checkNoPlaylists();
            }
        });
        playlistsClaimSearchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void checkNoPlaylists() {
        boolean noPlaylists = playlistsListAdapter == null || playlistsListAdapter.getItemCount() == 0;
        Helper.setViewVisibility(noPlaylistsView, noPlaylists ? View.VISIBLE : View.GONE);
    }

    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equalsIgnoreCase(MainActivity.PREFERENCE_KEY_SHOW_MATURE_CONTENT)) {
            fetchClaimSearchPlaylists(true);
        }
    }
}
