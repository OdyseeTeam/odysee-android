package com.odysee.app.ui.findcontent;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.ClaimListAdapter;
import com.odysee.app.dialog.ContentFromDialogFragment;
import com.odysee.app.dialog.ContentScopeDialogFragment;
import com.odysee.app.dialog.ContentSortDialogFragment;
import com.odysee.app.dialog.CustomizeTagsDialogFragment;
import com.odysee.app.listener.DownloadActionListener;
import com.odysee.app.listener.TagListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.LbryFile;
import com.odysee.app.model.OdyseeCollection;
import com.odysee.app.model.Tag;
import com.odysee.app.tasks.claim.ClaimSearchResultHandler;
import com.odysee.app.tasks.claim.ClaimSearchTask;
import com.odysee.app.tasks.FollowUnfollowTagTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.ContentSources;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;
import com.odysee.app.utils.Predefined;
import lombok.Getter;

// TODO: Similar code to FollowingFragment and Channel page fragment. Probably make common operations (sorting/filtering) into a control
public class AllContentFragment extends BaseFragment implements DownloadActionListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public static int ALL_CONTENT_CONTEXT_GROUP_ID = 1;

    @Getter
    private boolean singleTagView;
    private List<String> tags;
    private View layoutFilterContainer;
    private View customizeLink;
    private View sortLink;
    private View contentFromLink;
    private TextView titleView;
    private TextView sortLinkText;
    private TextView contentFromLinkText;
    private TextView scopeLinkText;
    private RecyclerView contentList;
    private int currentSortBy;
    private int currentContentFrom;
    @Getter
    private int currentContentScope;
    private String contentReleaseTime;
    private List<String> contentSortOrder;
    private View fromPrefix;
    private View forPrefix;
    private View contentLoading;
    private View bigContentLoading;
    private View noContentView;
    private ClaimListAdapter contentListAdapter;
    private boolean contentClaimSearchLoading;
    private boolean contentHasReachedEnd;
    private int currentClaimSearchPage;
    private ClaimSearchTask contentClaimSearchTask;

    private List<ContentSources.Category> dynamicCategories;
    private boolean contentCategoriesDisplayed;
    private int currentCategoryId;
    private String[] currentChannelIdList;
    private ChipGroup categorySelection;
    private int wildWestIndex = -1;
    private int moviesIndex = -1;

    private void buildAndDisplayContentCategories() {
        if (contentCategoriesDisplayed) {
            return;
        }

        Context context = getContext();
        View view = getView();
        if (context != null && view != null) {
            dynamicCategories = new ArrayList<>(ContentSources.DYNAMIC_CONTENT_CATEGORIES);
            ContentSources.Category wildWest = new ContentSources.Category();
            wildWest.setName("wildwest");
            wildWest.setLabel(getString(R.string.wildwest_category));
            wildWest.setChannelIds(null);
            dynamicCategories.add(wildWest);

            wildWestIndex = dynamicCategories.size() - 1;

            ChipGroup group = view.findViewById(R.id.category_selection_chipgroup);
            if (group.getChildCount() == 0) {
                for (int i = 0; i < dynamicCategories.size(); i++) {
                    ContentSources.Category category = dynamicCategories.get(i);
                    if ("movies".equalsIgnoreCase(category.getName())) {
                        moviesIndex = i;
                    }

                    Chip chip = new Chip(context);
                    chip.setChipBackgroundColor(ContextCompat.getColorStateList(context, R.color.chip_background));
                    chip.setText(category.getLabel());
                    chip.setCheckable(true);
                    chip.setCheckedIconVisible(false);
                    chip.setChecked(i == 0);
                    group.addView(chip);
                }
            }
        }

        currentCategoryId = 0;
        onCategoryChanged();

        contentCategoriesDisplayed = true;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_all_content, container, false);

        // All content page is sorted by trending by default, past week if sort is top
        currentSortBy = ContentSortDialogFragment.ITEM_SORT_BY_TRENDING;
        currentContentFrom = ContentFromDialogFragment.ITEM_FROM_PAST_WEEK;

        layoutFilterContainer = root.findViewById(R.id.all_content_filter_container);
        sortLink = root.findViewById(R.id.all_content_sort_link);
        contentFromLink = root.findViewById(R.id.all_content_time_link);
        customizeLink = root.findViewById(R.id.all_content_customize_link);
        fromPrefix = root.findViewById(R.id.all_content_from_prefix);

        sortLinkText = root.findViewById(R.id.all_content_sort_link_text);
        contentFromLinkText = root.findViewById(R.id.all_content_time_link_text);

        bigContentLoading = root.findViewById(R.id.all_content_main_progress);
        contentLoading = root.findViewById(R.id.all_content_load_progress);
        noContentView = root.findViewById(R.id.all_content_no_claim_search_content);

        categorySelection = root.findViewById(R.id.category_selection_chipgroup);
        categorySelection.setOnCheckedChangeListener(new ChipGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, int checkedId) {
                Chip chip = group.findViewById(checkedId);
                if (chip != null && chip.isChecked()) {
                    // Use child index, because getTag() doesn't work properly for some reason
                    currentCategoryId = categorySelection.indexOfChild(chip);
                    currentChannelIdList = dynamicCategories.get(currentCategoryId).getChannelIds();
                    onCategoryChanged();
                }
            }
        });

        contentList = root.findViewById(R.id.all_content_list);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        contentList.setLayoutManager(llm);
        contentList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (contentClaimSearchLoading) {
                    return;
                }

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null) {
                    int visibleItemCount = lm.getChildCount();
                    int totalItemCount = lm.getItemCount();
                    int pastVisibleItems = lm.findFirstVisibleItemPosition();
                    if (pastVisibleItems + visibleItemCount >= totalItemCount) {
                        if (!contentHasReachedEnd) {
                            // load more
                            currentClaimSearchPage++;
                            fetchClaimSearchContent();
                        }
                    }
                }
            }
        });

        sortLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContentSortDialogFragment dialog = ContentSortDialogFragment.newInstance();
                dialog.setCurrentSortByItem(currentSortBy);
                dialog.setSortByListener(new ContentSortDialogFragment.SortByListener() {
                    @Override
                    public void onSortByItemSelected(int sortBy) {
                        onSortByChanged(sortBy);
                    }
                });

                Context context = getContext();
                if (context instanceof MainActivity) {
                    MainActivity activity = (MainActivity) context;
                    dialog.show(activity.getSupportFragmentManager(), ContentSortDialogFragment.TAG);
                }
            }
        });
        contentFromLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContentFromDialogFragment dialog = ContentFromDialogFragment.newInstance();
                dialog.setCurrentFromItem(currentContentFrom);
                dialog.setContentFromListener(new ContentFromDialogFragment.ContentFromListener() {
                    @Override
                    public void onContentFromItemSelected(int contentFromItem) {
                        onContentFromChanged(contentFromItem);
                    }
                });
                Context context = getContext();
                if (context instanceof MainActivity) {
                    MainActivity activity = (MainActivity) context;
                    dialog.show(activity.getSupportFragmentManager(), ContentFromDialogFragment.TAG);
                }
            }
        });
        customizeLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCustomizeTagsDialog();
            }
        });

        checkParams(false);
        return root;
    }

    public void setParams(Map<String, Object> params) {
        super.setParams(params);
        if (getView() != null) {
            checkParams(true);
        }
    }

    private void checkParams(boolean reload) {
        Map<String, Object> params = getParams();
        if (params != null && params.containsKey("singleTag")) {
            String tagName = "";
            Object o = params.get("singleTag");
            if (o != null) {
                tagName = o.toString();
            }
            singleTagView = true;
            tags = Arrays.asList(tagName);
            titleView.setText(Helper.capitalize(tagName));
            Helper.setViewVisibility(customizeLink, View.GONE);
        } else {
            singleTagView = false;
            // default to followed Tags scope if any tags are followed
            tags = Helper.getTagsForTagObjects(Lbry.followedTags);
            if (tags.size() > 0) {
                currentContentScope = ContentScopeDialogFragment.ITEM_TAGS;
                Helper.setViewVisibility(customizeLink, View.VISIBLE);
            }
        }

        if (reload) {
            fetchClaimSearchContent(true);
        }
    }

    private void onContentFromChanged(int contentFrom) {
        currentContentFrom = contentFrom;

        // rebuild options and search
        updateContentFromLinkText();
        contentReleaseTime = Helper.buildReleaseTime(currentContentFrom);
        fetchClaimSearchContent(true);
    }

    private void onCategoryChanged() {
        Helper.setViewVisibility(layoutFilterContainer, currentCategoryId == wildWestIndex ? View.GONE : View.VISIBLE);
        fetchClaimSearchContent(true);
    }

    private void showCustomizeTagsDialog() {
        CustomizeTagsDialogFragment dialog = CustomizeTagsDialogFragment.newInstance();
        dialog.setListener(new TagListener() {
            @Override
            public void onTagAdded(Tag tag) {
                // heavy-lifting
                // save to local, save to wallet and then sync
                FollowUnfollowTagTask task = new FollowUnfollowTagTask(tag, false, getContext(), followUnfollowHandler);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            @Override
            public void onTagRemoved(Tag tag) {
                // heavy-lifting
                // save to local, save to wallet and then sync
                FollowUnfollowTagTask task = new FollowUnfollowTagTask(tag, true, getContext(), followUnfollowHandler);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            dialog.show(activity.getSupportFragmentManager(), CustomizeTagsDialogFragment.TAG);
        }
    }

    private final FollowUnfollowTagTask.FollowUnfollowTagHandler followUnfollowHandler = new FollowUnfollowTagTask.FollowUnfollowTagHandler() {
        @Override
        public void onSuccess(Tag tag, boolean unfollowing) {
            if (tags != null) {
                if (unfollowing) {
                    tags.remove(tag.getLowercaseName());
                } else {
                    tags.add(tag.getLowercaseName());
                }
                fetchClaimSearchContent(true);
            }

            Bundle bundle = new Bundle();
            bundle.putString("tag", tag.getLowercaseName());
            LbryAnalytics.logEvent(unfollowing ? LbryAnalytics.EVENT_TAG_UNFOLLOW : LbryAnalytics.EVENT_TAG_FOLLOW, bundle);

            Context context = getContext();
            if (context instanceof MainActivity) {
                ((MainActivity) context).saveSharedUserState();
            }
        }

        @Override
        public void onError(Exception error) {
            // pass
        }
    };

    private void onSortByChanged(int sortBy) {
        currentSortBy = sortBy;

        // rebuild options and search
        Helper.setViewVisibility(fromPrefix, currentSortBy == ContentSortDialogFragment.ITEM_SORT_BY_TOP ? View.VISIBLE : View.GONE);
        Helper.setViewVisibility(contentFromLink, currentSortBy == ContentSortDialogFragment.ITEM_SORT_BY_TOP ? View.VISIBLE : View.GONE);
        currentContentFrom = currentSortBy == ContentSortDialogFragment.ITEM_SORT_BY_TOP ?
                (currentContentFrom == 0 ? ContentFromDialogFragment.ITEM_FROM_PAST_WEEK : currentContentFrom) : 0;

        updateSortByLinkText();
        contentSortOrder = Helper.buildContentSortOrder(currentSortBy);
        contentReleaseTime = Helper.buildReleaseTime(currentContentFrom);
        fetchClaimSearchContent(true);
    }

    private void updateSortByLinkText() {
        int stringResourceId = -1;
        switch (currentSortBy) {
            case ContentSortDialogFragment.ITEM_SORT_BY_NEW: default: stringResourceId = R.string.new_text; break;
            case ContentSortDialogFragment.ITEM_SORT_BY_TOP: stringResourceId = R.string.top; break;
            case ContentSortDialogFragment.ITEM_SORT_BY_TRENDING: stringResourceId = R.string.trending; break;
        }

        Helper.setViewText(sortLinkText, stringResourceId);
    }

    private void updateContentScopeLinkText() {
        int stringResourceId = -1;
        switch (currentContentScope) {
            case ContentScopeDialogFragment.ITEM_EVERYONE: default: stringResourceId = R.string.everyone; break;
            case ContentScopeDialogFragment.ITEM_TAGS: stringResourceId = R.string.tags; break;
        }

        Helper.setViewText(scopeLinkText, stringResourceId);
    }

    private void updateContentFromLinkText() {
        int stringResourceId = -1;
        switch (currentContentFrom) {
            case ContentFromDialogFragment.ITEM_FROM_PAST_24_HOURS: stringResourceId = R.string.past_24_hours; break;
            case ContentFromDialogFragment.ITEM_FROM_PAST_WEEK: default: stringResourceId = R.string.past_week; break;
            case ContentFromDialogFragment.ITEM_FROM_PAST_MONTH: stringResourceId = R.string.past_month; break;
            case ContentFromDialogFragment.ITEM_FROM_PAST_YEAR: stringResourceId = R.string.past_year; break;
            case ContentFromDialogFragment.ITEM_FROM_ALL_TIME: stringResourceId = R.string.all_time; break;
        }

        Helper.setViewText(contentFromLinkText, stringResourceId);
    }

    public void onResume() {
        super.onResume();
        Helper.setWunderbarValue(null, getContext());
        checkParams(false);

        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            if (singleTagView) {
                LbryAnalytics.setCurrentScreen(activity, "Tag", "Tag");
            } else {
                LbryAnalytics.setCurrentScreen(activity, "All Content", "AllContent");
            }
            activity.addDownloadActionListener(this);

            if (activity.isInitialCategoriesLoaded()) {
                buildAndDisplayContentCategories();
            }

            PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
            updateContentFromLinkText();
            updateContentScopeLinkText();
            updateSortByLinkText();
        }

        applyFilterForBlockedChannels(Lbryio.blockedChannels);
    }

    public void onPause() {
        Context context = getContext();
        if (context != null) {
            ((MainActivity) context).removeDownloadActionListener(this);
        }
        PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this);
        contentCategoriesDisplayed = false;
        super.onPause();
    }

    private Map<String, Object> buildContentOptions() {
        Context context = getContext();
        boolean canShowMatureContent = false;
        if (context != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            canShowMatureContent = sp.getBoolean(MainActivity.PREFERENCE_KEY_SHOW_MATURE_CONTENT, false);
        }

        List<String> channelIdsForCategory;

        if (currentCategoryId == wildWestIndex) {
            channelIdsForCategory = Collections.emptyList();
        } else if (currentChannelIdList != null) {
            channelIdsForCategory = Arrays.asList(currentChannelIdList);
        } else {
            channelIdsForCategory = Arrays.asList(dynamicCategories.get(0).getChannelIds());
        }

        int claimsPerChannel = 0;

        if (currentCategoryId == moviesIndex) {
            claimsPerChannel = 20;
        } else if (currentCategoryId == wildWestIndex) {
            claimsPerChannel = 3;
        }
        return Lbry.buildClaimSearchOptions(
                null,
                (currentContentScope == ContentScopeDialogFragment.ITEM_EVERYONE) ? null : tags,
                canShowMatureContent ? null : new ArrayList<>(Predefined.MATURE_TAGS),
                null,
                channelIdsForCategory,
                null,
                getContentSortOrder(),
                currentCategoryId == wildWestIndex ? Helper.buildReleaseTime(ContentFromDialogFragment.ITEM_FROM_PAST_WEEK) : contentReleaseTime,
                0,
                claimsPerChannel,
                currentClaimSearchPage == 0 ? 1 : currentClaimSearchPage,
                Helper.CONTENT_PAGE_SIZE);
    }

    private List<String> getContentSortOrder() {
        if (contentSortOrder == null || currentCategoryId == wildWestIndex) {
            return Arrays.asList(Claim.ORDER_BY_TRENDING_GROUP, Claim.ORDER_BY_TRENDING_MIXED);
        }
        return contentSortOrder;
    }

    private View getLoadingView() {
        return (contentListAdapter == null || contentListAdapter.getItemCount() == 0) ? bigContentLoading : contentLoading;
    }

    private void fetchClaimSearchContent() {
        fetchClaimSearchContent(false);
    }

    public void fetchClaimSearchContent(boolean reset) {
        if (reset && contentListAdapter != null) {
            contentListAdapter.clearItems();
            currentClaimSearchPage = 1;
        }

        contentClaimSearchLoading = true;
        Helper.setViewVisibility(noContentView, View.GONE);
        Map<String, Object> claimSearchOptions = buildContentOptions();
        contentClaimSearchTask = new ClaimSearchTask(claimSearchOptions, Lbry.API_CONNECTION_STRING, getLoadingView(), new ClaimSearchResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                claims = Helper.filterClaimsByOutpoint(claims);
                claims = Helper.filterClaimsByBlockedChannels(claims, Lbryio.blockedChannels);

                if (contentListAdapter == null) {
                    Context context = getContext();
                    if (context != null) {
                        contentListAdapter = new ClaimListAdapter(claims, context);
                        contentListAdapter.setContextGroupId(ALL_CONTENT_CONTEXT_GROUP_ID);
                        contentListAdapter.setListener(new ClaimListAdapter.ClaimListItemListener() {
                            @Override
                            public void onClaimClicked(Claim claim) {
                                Context context = getContext();
                                if (context instanceof MainActivity) {
                                    MainActivity activity = (MainActivity) context;
                                    if (claim.getName().startsWith("@")) {
                                        // channel claim
                                        activity.openChannelClaim(claim);
                                    } else {
                                        activity.openFileClaim(claim);
                                    }
                                }
                            }
                        });
                    }
                } else {
                    contentListAdapter.addItems(claims);
                }

                if (contentList != null && contentList.getAdapter() == null) {
                    contentList.setAdapter(contentListAdapter);
                }

                contentHasReachedEnd = hasReachedEnd;
                contentClaimSearchLoading = false;
                checkNoContent();
            }

            @Override
            public void onError(Exception error) {
                contentClaimSearchLoading = false;
                checkNoContent();
            }
        });
        contentClaimSearchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void checkNoContent() {
        boolean noContent = contentListAdapter == null || contentListAdapter.getItemCount() == 0;
        Helper.setViewVisibility(noContentView, noContent ? View.VISIBLE : View.GONE);
    }

    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equalsIgnoreCase(MainActivity.PREFERENCE_KEY_SHOW_MATURE_CONTENT)) {
            fetchClaimSearchContent(true);
        }
    }

    public void onDownloadAction(String downloadAction, String uri, String outpoint, String fileInfoJson, double progress) {
        if ("abort".equals(downloadAction)) {
            if (contentListAdapter != null) {
                contentListAdapter.clearFileForClaimOrUrl(outpoint, uri);
            }
            return;
        }

        try {
            JSONObject fileInfo = new JSONObject(fileInfoJson);
            LbryFile claimFile = LbryFile.fromJSONObject(fileInfo);
            String claimId = claimFile.getClaimId();
            if (contentListAdapter != null) {
                contentListAdapter.updateFileForClaimByIdOrUrl(claimFile, claimId, uri);
            }
        } catch (JSONException ex) {
            // invalid file info for download
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == ALL_CONTENT_CONTEXT_GROUP_ID && item.getItemId() == R.id.action_block) {
            if (contentListAdapter != null) {
                int position = contentListAdapter.getPosition();
                Claim claim = contentListAdapter.getItems().get(position);
                if (claim != null && claim.getSigningChannel() != null) {
                    Claim channel = claim.getSigningChannel();
                    Context context = getContext();
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).handleBlockChannel(channel);
                    }
                }
            }
            return true;
        }

        if (item.getGroupId() ==  ALL_CONTENT_CONTEXT_GROUP_ID)  {
            if (contentListAdapter != null) {
                int position = contentListAdapter.getPosition();
                Claim claim = contentListAdapter.getItems().get(position);
                String url = claim.getPermanentUrl();

                Context context = getContext();
                if (context instanceof MainActivity) {
                    MainActivity activity = (MainActivity) context;
                    if (item.getItemId() == R.id.action_add_to_watch_later) {
                        activity.handleAddUrlToList(url, OdyseeCollection.BUILT_IN_ID_WATCHLATER);
                    } else if (item.getItemId() == R.id.action_add_to_favorites) {
                        activity.handleAddUrlToList(url, OdyseeCollection.BUILT_IN_ID_FAVORITES);
                    } else if (item.getItemId() == R.id.action_add_to_lists) {
                        activity.handleAddUrlToList(url, null);
                    }
                }
            }
        }

        return super.onContextItemSelected(item);
    }

    public void displayDynamicCategories() {
        buildAndDisplayContentCategories();
    }

    public void applyFilterForBlockedChannels(List<LbryUri> blockedChannels) {
        if (contentListAdapter != null) {
            contentListAdapter.filterBlockedChannels(blockedChannels);
        }
    }
}
