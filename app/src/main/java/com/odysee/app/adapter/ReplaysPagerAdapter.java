package com.odysee.app.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.odysee.app.model.LivestreamReplay;
import com.odysee.app.ui.publish.ReplaysPageFragment;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ReplaysPagerAdapter extends FragmentStateAdapter {
    private static final int PAGE_SIZE = 4;

    private final List<LivestreamReplay> replays;
    private final SelectedReplayManager manager;

    // Needed because default getItemId() uses position so fragments
    // don't get changed when calling notifyDataSetChanged().
    private long idCounter = 0;
    private List<Long> itemIds;

    public ReplaysPagerAdapter(FragmentActivity activity, List<LivestreamReplay> replays, SelectedReplayManager manager) {
        super(activity);
        this.replays = replays;
        this.manager = manager;

        LivestreamReplay selectedReplay = manager.getSelectedReplay();
        if (selectedReplay != null) {
            for (LivestreamReplay replay : replays) {
                if (replay.getUrl().equals(selectedReplay.getUrl())) {
                    replay.setSelected(true);
                }
            }
        }

        this.itemIds = generateItemIds();
        RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                itemIds = generateItemIds();
            }
        };
        registerAdapterDataObserver(observer);
    }

    private List<Long> generateItemIds() {
        return IntStream.range(0, getItemCount())
                .mapToObj(unused -> idCounter++)
                .collect(Collectors.toList());
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        ReplaysPageFragment fragment = new ReplaysPageFragment();
        fragment.setReplays(replays.subList(
                position * 4, Math.min((position * 4) + 4, replays.size())));
        fragment.setListener(new ReplaysPageFragment.ReplaySelectedListener() {
            @Override
            public void onReplaySelected(LivestreamReplay replay) {
                LivestreamReplay selectedReplay = manager.getSelectedReplay();
                if (selectedReplay != null) {
                    selectedReplay.setSelected(false);
                }
                replay.setSelected(true);
                manager.setSelectedReplay(replay);
                notifyDataSetChanged();
            }
        });
        return fragment;
    }

    @Override
    public int getItemCount() {
        return (int) Math.ceil(replays.size() / (double) PAGE_SIZE);
    }

    @Override
    public long getItemId(int position) {
        return itemIds.get(position);
    }

    @Override
    public boolean containsItem(long itemId) {
        return itemIds.contains(itemId);
    }

    public interface SelectedReplayManager {
        LivestreamReplay getSelectedReplay();
        void setSelectedReplay(LivestreamReplay replay);
    }
}
