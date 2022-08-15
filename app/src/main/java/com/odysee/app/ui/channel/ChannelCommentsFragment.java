package com.odysee.app.ui.channel;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.odysee.app.BuildConfig;
import com.odysee.app.MainActivity;
import com.odysee.app.OdyseeApp;
import com.odysee.app.R;
import com.odysee.app.adapter.ClaimListAdapter;
import com.odysee.app.adapter.CommentItemDecoration;
import com.odysee.app.adapter.CommentListAdapter;
import com.odysee.app.adapter.InlineChannelSpinnerAdapter;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Claim;
import com.odysee.app.model.Comment;
import com.odysee.app.model.lbryinc.CreatorSetting;
import com.odysee.app.tasks.CommentCreateTask;
import com.odysee.app.tasks.CommentListHandler;
import com.odysee.app.tasks.CommentListTask;
import com.odysee.app.tasks.claim.*;
import com.odysee.app.tasks.lbryinc.LogPublishTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Comments;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;
import com.odysee.app.utils.Utils;
import com.odysee.app.checkers.CommentEnabledCheck;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.Setter;
import okhttp3.ResponseBody;

public class ChannelCommentsFragment extends BaseFragment implements ChannelCreateDialogFragment.ChannelCreateListener {

    @Setter
    private Claim claim;
    @Setter
    private String commentHash;

    @Getter
    private CommentListAdapter commentListAdapter;
    private CommentEnabledCheck commentEnabledCheck;
    private CreatorSetting creatorSetting;

    private Comment replyToComment;
    private View containerReplyToComment;
    private TextView textReplyingTo;
    private TextView textReplyToBody;
    private View buttonClearReplyToComment;

    private boolean postingComment;
    private boolean fetchingChannels;
    private View progressLoadingChannels;
    private View progressPostComment;
    private InlineChannelSpinnerAdapter commentChannelSpinnerAdapter;
    private AppCompatSpinner commentChannelSpinner;
    private TextInputEditText inputComment;
    private TextView textCommentLimit;
    private MaterialButton buttonPostComment;
    private MaterialButton buttonCreateChannel;
    private ImageView commentPostAsThumbnail;
    private View commentPostAsNoThumbnail;
    private TextView commentPostAsAlpha;
    private MaterialButton buttonUserSignedInRequired;
    private View loadingCommentView;
    private View commentsNestedLayout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        commentEnabledCheck = new CommentEnabledCheck();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_channel_comments, container, false);

        containerReplyToComment = root.findViewById(R.id.comment_form_reply_to_container);
        textReplyingTo = root.findViewById(R.id.comment_form_replying_to_text);
        textReplyToBody = root.findViewById(R.id.comment_form_reply_to_body);
        buttonClearReplyToComment = root.findViewById(R.id.comment_form_clear_reply_to);

        commentChannelSpinner = root.findViewById(R.id.comment_form_channel_spinner);
        progressLoadingChannels = root.findViewById(R.id.comment_form_channels_loading);
        progressPostComment = root.findViewById(R.id.comment_form_post_progress);
        inputComment = root.findViewById(R.id.comment_form_body);
        textCommentLimit = root.findViewById(R.id.comment_form_text_limit);
        buttonPostComment = root.findViewById(R.id.comment_form_post);
        buttonCreateChannel = root.findViewById(R.id.create_channel_button);
        commentPostAsThumbnail = root.findViewById(R.id.comment_form_thumbnail);
        commentPostAsNoThumbnail = root.findViewById(R.id.comment_form_no_thumbnail);
        commentPostAsAlpha = root.findViewById(R.id.comment_form_thumbnail_alpha);
        buttonUserSignedInRequired = root.findViewById(R.id.sign_in_user_button);

        loadingCommentView = root.findViewById(R.id.channel_comments_loading_spinner);
        commentsNestedLayout = root.findViewById(R.id.channel_comments_area);

        RecyclerView commentList = root.findViewById(R.id.channel_comments_list);
        commentList.setLayoutManager(new LinearLayoutManager(getContext()));

        initCommentForm(root);

        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        commentsNestedLayout.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();

        fetchChannels();
        checkAndLoadComments();
        applyFilterForBlockedChannels(Lbryio.blockedChannels);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void checkAndLoadComments() {
        View root = getView();
        if (root != null) {
            View commentsDisabledText = root.findViewById(R.id.channel_disabled_comments);
            View commentForm = root.findViewById(R.id.container_comment_form);
            RecyclerView commentsList = root.findViewById(R.id.channel_comments_list);

            loadingCommentView.setVisibility(View.VISIBLE);
            commentEnabledCheck.checkCommentStatus(claim.getClaimId(), claim.getName(), isEnabled -> {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (isEnabled) {
                            showComments(commentsDisabledText, commentForm, commentsList);
                            Helper.setViewVisibility(commentsDisabledText, View.GONE);
                            Helper.setViewVisibility(commentForm, View.VISIBLE);
                            Helper.setViewVisibility(commentsList, View.VISIBLE);
                            if (commentsList == null || commentsList.getAdapter() == null || commentsList.getAdapter().getItemCount() == 0) {
                                loadComments();
                            }
                        } else {
                            hideComments(commentsDisabledText, commentForm, commentsList);
                        }
                        loadingCommentView.setVisibility(View.GONE);
                        commentsNestedLayout.setVisibility(View.VISIBLE);
                    });
                }
            });
        }
    }

    private void hideComments(View commentsDisabledText, View commentForm, View commentsList) {
        Helper.setViewVisibility(commentsDisabledText, View.VISIBLE);
        Helper.setViewVisibility(commentForm, View.GONE);
        Helper.setViewVisibility(commentsList, View.GONE);
    }

    private void showComments(View commentsDisabledText, View commentForm, View commentsList) {
        Helper.setViewVisibility(commentsDisabledText, View.GONE);
        Helper.setViewVisibility(commentForm, View.VISIBLE);
        Helper.setViewVisibility(commentsList, View.VISIBLE);
    }

    private void loadComments() {
        View root = getView();
        if (claim != null && root != null) {
            ProgressBar relatedLoading = root.findViewById(R.id.channel_comments_progress);
            CommentListTask task = new CommentListTask(1, 200, claim.getClaimId(), relatedLoading, new CommentListHandler() {
                @Override
                public void onSuccess(List<Comment> comments, boolean hasReachedEnd) {
                    Context ctx = getContext();
                    View root = getView();
                    if (ctx != null && root != null) {
                        ensureCommentListAdapterCreated(comments);
                    }
                }

                @Override
                public void onError(Exception error) {
                    // pass
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void ensureCommentListAdapterCreated(final List<Comment> comments) {
        if ( commentListAdapter == null ) {
            Context ctx = getContext();
            View root = getView();

            commentListAdapter = new CommentListAdapter(comments, ctx, claim, new CommentListAdapter.CommentListListener() {
                @Override
                public void onListChanged() {
                    checkNoComments();
                }

                @Override
                public void onCommentReactClicked(Comment c, boolean liked) {
                    // Not used for now.
                }

                @Override
                public void onReplyClicked(Comment comment) {
                    setReplyToComment(comment);
                }
            });
            commentListAdapter.setListener(new ClaimListAdapter.ClaimListItemListener() {
                @Override
                public void onClaimClicked(Claim claim, int position) {
                    if (!Helper.isNullOrEmpty(claim.getName()) &&
                            claim.getName().startsWith("@") &&
                            ctx instanceof MainActivity) {
                        ((MainActivity) ctx).openChannelClaim(claim);
                    }
                }
            });

            RecyclerView commentList = root.findViewById(R.id.channel_comments_list);
            int marginInPx = Math.round(40 * ((float) ctx.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
            commentList.addItemDecoration(new CommentItemDecoration(marginInPx));
            commentList.setAdapter(commentListAdapter);
            commentListAdapter.notifyItemRangeInserted(0, comments.size());
            commentListAdapter.setCollapsed(false);

            checkNoComments();
            resolveCommentPosters();
            scrollToCommentHash();
        }
    }

    private void scrollToCommentHash() {
        View root = getView();
        // check for the position of commentHash if set
        if (root != null && !Helper.isNullOrEmpty(commentHash) && commentListAdapter != null && commentListAdapter.getItemCount() > 0) {
            RecyclerView commentList = root.findViewById(R.id.channel_comments_list);
            int position = commentListAdapter.getPositionForComment(commentHash);
            if (position > -1 && commentList.getLayoutManager() != null) {
                NestedScrollView scrollView = root.findViewById(R.id.channel_comments_area);
                scrollView.requestChildFocus(commentList, commentList);
                commentList.getLayoutManager().scrollToPosition(position);
            }
        }
    }

    private void resolveCommentPosters() {
        if (commentListAdapter != null) {
            List<String> urlsToResolve = new ArrayList<>(commentListAdapter.getClaimUrlsToResolve());
            if (urlsToResolve.size() > 0) {
                ResolveTask task = new ResolveTask(urlsToResolve, Lbry.API_CONNECTION_STRING, null, new ResolveResultHandler() {
                    @Override
                    public void onSuccess(List<Claim> claims) {
                        if (commentListAdapter != null) {
                            for (Claim claim : claims) {
                                if (claim.getClaimId() != null) {
                                    commentListAdapter.updatePosterForComment(claim.getClaimId(), claim);
                                }
                            }

                            commentListAdapter.filterBlockedChannels(Lbryio.blockedChannels);

                            commentListAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        // pass
                    }
                });
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    private void fetchChannels() {
        if (Lbry.ownChannels != null && Lbry.ownChannels.size() > 0) {
            updateChannelList(Lbry.ownChannels);
            return;
        }

        fetchingChannels = true;
        disableChannelSpinner();
        Map<String, Object> options = Lbry.buildClaimListOptions(Claim.TYPE_CHANNEL, 1, 999, true);
        ClaimListTask task = new ClaimListTask(options, progressLoadingChannels, new ClaimListResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                Lbry.ownChannels = new ArrayList<>(claims);
                updateChannelList(Lbry.ownChannels);
                enableChannelSpinner();
                fetchingChannels = false;
            }

            @Override
            public void onError(Exception error) {
                enableChannelSpinner();
                fetchingChannels = false;
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    private void disableChannelSpinner() {
        Helper.setViewEnabled(commentChannelSpinner, false);
    }
    private void enableChannelSpinner() {
        Helper.setViewEnabled(commentChannelSpinner, true);
        if (commentChannelSpinner != null) {
            Claim selectedClaim = (Claim) commentChannelSpinner.getSelectedItem();
            if (selectedClaim != null) {
                if (selectedClaim.isPlaceholder()) {
                    showChannelCreator();
                }
            }
        }
    }
    private void showChannelCreator() {
        MainActivity activity = (MainActivity) getActivity();

        if (activity != null) {
            activity.showChannelCreator(this);
        }
    }

    @Override
    public void onChannelCreated(Claim claimResult) {
        // add the claim to the channel list and set it as the selected item
        if (commentChannelSpinnerAdapter != null) {
            commentChannelSpinnerAdapter.add(claimResult);
        } else {
            updateChannelList(Collections.singletonList(claimResult));
        }
        if (commentChannelSpinner != null && commentChannelSpinnerAdapter != null) {
            // Ensure adapter is set for the spinner
            if (commentChannelSpinner.getAdapter() == null) {
                commentChannelSpinner.setAdapter(commentChannelSpinnerAdapter);
            }
            commentChannelSpinner.setSelection(commentChannelSpinnerAdapter.getCount() - 1);
        }

        if (commentChannelSpinner != null) {
            View formRoot = (View) commentChannelSpinner.getParent().getParent();
            formRoot.setVisibility(View.VISIBLE);
            formRoot.findViewById(R.id.has_channels).setVisibility(View.VISIBLE);
            formRoot.findViewById(R.id.no_channels).setVisibility(View.GONE);
        }
    }

    private void updateChannelList(List<Claim> channels) {
        Context context = getContext();
        if (commentChannelSpinnerAdapter == null) {
            if (context != null) {
                commentChannelSpinnerAdapter = new InlineChannelSpinnerAdapter(context, R.layout.spinner_item_channel, new ArrayList<>(channels));
                commentChannelSpinnerAdapter.addPlaceholder(false);
                commentChannelSpinnerAdapter.notifyDataSetChanged();
            }
        } else {
            commentChannelSpinnerAdapter.clear();
            commentChannelSpinnerAdapter.addAll(channels);
            commentChannelSpinnerAdapter.addPlaceholder(false);
            commentChannelSpinnerAdapter.notifyDataSetChanged();
        }

        if (commentChannelSpinner != null) {
            commentChannelSpinner.setAdapter(commentChannelSpinnerAdapter);
        }

        if (commentChannelSpinnerAdapter != null && commentChannelSpinner != null) {
            if (commentChannelSpinnerAdapter.getCount() > 1) {
                String defaultChannelName = Helper.getDefaultChannelName(context);

                List<Claim> defaultChannel = channels.stream().filter(c -> c != null && c.getName().equalsIgnoreCase(defaultChannelName)).collect(Collectors.toList());

                if (defaultChannel.size() > 0) {
                    commentChannelSpinner.setSelection(commentChannelSpinnerAdapter.getItemPosition(defaultChannel.get(0)));
                } else {
                    commentChannelSpinner.setSelection(1);
                }
            }
        }
    }

    private void initCommentForm(View root) {
        MainActivity activity = (MainActivity) getActivity();

        if (activity != null) {
            activity.refreshChannelCreationRequired(root);
        }

        textCommentLimit.setText(String.format("%d / %d", Helper.getValue(inputComment.getText()).length(), Comment.MAX_LENGTH));

        buttonUserSignedInRequired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity activity = (MainActivity) getActivity();

                if (activity != null) {
                    activity.simpleSignIn(0);
                }
            }
        });

        buttonClearReplyToComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearReplyToComment();
            }
        });

        buttonPostComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateAndCheckPostComment();
            }
        });

        buttonCreateChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!fetchingChannels) {
                    showChannelCreator();
                }
            }
        });

        inputComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                int len = charSequence.length();
                textCommentLimit.setText(String.format("%d / %d", len, Comment.MAX_LENGTH));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        commentChannelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                Object item = adapterView.getItemAtPosition(position);
                if (item instanceof Claim) {
                    Claim claim = (Claim) item;
                    if (claim.isPlaceholder()) {
                        if (!fetchingChannels) {
                            showChannelCreator();
                            if (commentChannelSpinnerAdapter.getCount() > 1) {
                                commentChannelSpinner.setSelection(commentChannelSpinnerAdapter.getCount() - 1);
                            }
                        }
                    } else {
                        updatePostAsChannel(claim);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void validateAndCheckPostComment() {
        String comment = Helper.getValue(inputComment.getText());
        Claim channel = (Claim) commentChannelSpinner.getSelectedItem();

        if (Helper.isNullOrEmpty(comment)) {
            showError(getString(R.string.please_enter_comment));
            return;
        }
        if (channel == null || Helper.isNullOrEmpty(channel.getClaimId())) {
            showError(getString(R.string.please_select_channel));
            return;
        }
        if (creatorSetting != null && creatorSetting.getMinTipAmountCommentValue() > 0) {
            // TODO: Update comment form
            showError(getString(R.string.please_enter_min_tip_amount));
            return;
        }

        postComment();
    }

    private void updatePostAsChannel(Claim channel) {
        boolean hasThumbnail = !Helper.isNullOrEmpty(channel.getThumbnailUrl());
        Helper.setViewVisibility(commentPostAsThumbnail, hasThumbnail ? View.VISIBLE : View.INVISIBLE);
        Helper.setViewVisibility(commentPostAsNoThumbnail, !hasThumbnail ? View.VISIBLE : View.INVISIBLE);
        Helper.setViewText(commentPostAsAlpha, channel.getName() != null ? channel.getName().substring(1, 2).toUpperCase() : null);

        Context context = getContext();
        int bgColor = Helper.generateRandomColorForValue(channel.getClaimId());
        Helper.setIconViewBackgroundColor(commentPostAsNoThumbnail, bgColor, false, context);

        if (hasThumbnail && context != null) {
            Glide.with(context.getApplicationContext()).
                    asBitmap().
                    load(channel.getThumbnailUrl(Utils.CHANNEL_THUMBNAIL_WIDTH, Utils.CHANNEL_THUMBNAIL_HEIGHT, Utils.CHANNEL_THUMBNAIL_Q)).
                    apply(RequestOptions.circleCropTransform()).
                    into(commentPostAsThumbnail);
        }
    }

    private void beforePostComment() {
        postingComment = true;
        Helper.setViewEnabled(commentChannelSpinner, false);
        Helper.setViewEnabled(inputComment, false);
        Helper.setViewEnabled(buttonClearReplyToComment, false);
        Helper.setViewEnabled(buttonPostComment, false);
    }

    private void afterPostComment() {
        Helper.setViewEnabled(commentChannelSpinner, true);
        Helper.setViewEnabled(inputComment, true);
        Helper.setViewEnabled(buttonClearReplyToComment, true);
        Helper.setViewEnabled(buttonPostComment, true);
        postingComment = false;
    }

    private Comment buildPostComment() {
        Comment comment = new Comment();
        Claim channel = (Claim) commentChannelSpinner.getSelectedItem();
        comment.setClaimId(claim.getClaimId());
        comment.setChannelId(channel.getClaimId());
        comment.setChannelName(channel.getName());
        comment.setText(Helper.getValue(inputComment.getText()));
        comment.setPoster(channel);
        if (replyToComment != null) {
            comment.setParentId(replyToComment.getId());
        }

        return comment;
    }

    private void setReplyToComment(Comment comment) {
        replyToComment = comment;
        Helper.setViewText(textReplyingTo, getString(R.string.replying_to, comment.getChannelName()));
        Helper.setViewText(textReplyToBody, comment.getText());
        Helper.setViewVisibility(containerReplyToComment, View.VISIBLE);

        inputComment.requestFocus();
        Context context = getContext();
        if (context != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(inputComment, InputMethodManager.SHOW_FORCED);
        }
    }

    private void clearReplyToComment() {
        Helper.setViewText(textReplyingTo, null);
        Helper.setViewText(textReplyToBody, null);
        Helper.setViewVisibility(containerReplyToComment, View.GONE);
        replyToComment = null;
    }

    private void postComment() {
        if (postingComment) {
            return;
        }

        Comment comment = buildPostComment();

        beforePostComment();
        AccountManager am = AccountManager.get(getContext());
        Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());

        CommentCreateTask task = new CommentCreateTask(comment, am.peekAuthToken(odyseeAccount, "auth_token_type"), progressPostComment, new CommentCreateTask.CommentCreateWithTipHandler() {
            @Override
            public void onSuccess(Comment createdComment) {
                inputComment.setText(null);
                clearReplyToComment();

                ensureCommentListAdapterCreated(new ArrayList<Comment>());

                if (commentListAdapter != null) {
                    createdComment.setPoster(comment.getPoster());
                    if (!Helper.isNullOrEmpty(createdComment.getParentId())) {
                        commentListAdapter.addReply(createdComment);
                    } else {
                        commentListAdapter.insert(0, createdComment);
                    }
                }
                afterPostComment();
                checkNoComments();

                Bundle bundle = new Bundle();
                bundle.putString("claim_id", claim != null ? claim.getClaimId() : null);
                bundle.putString("claim_name", claim != null ? claim.getName() : null);
                LbryAnalytics.logEvent(LbryAnalytics.EVENT_COMMENT_CREATE, bundle);

                showMessage(R.string.comment_posted);
            }

            @Override
            public void onError(Exception error) {
                try {
                    showError(error != null ? error.getMessage() : getString(R.string.comment_error));
                } catch (IllegalStateException ex) {
                    // pass
                }
                afterPostComment();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void checkNoComments() {
        View root = getView();
        if (root != null) {
            Helper.setViewVisibility(root.findViewById(R.id.channel_no_comments),
                    commentListAdapter == null || commentListAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    public void applyFilterForBlockedChannels(List<LbryUri> blockedChannels) {
        if (commentListAdapter != null) {
            commentListAdapter.filterBlockedChannels(blockedChannels);
        }
    }

    private void setupInlineChannelCreator(
            View container,
            TextInputEditText inputChannelName,
            TextInputEditText inputDeposit,
            View inlineBalanceView,
            TextView inlineBalanceValue,
            View linkCancel,
            MaterialButton buttonCreate,
            View progressView,
            AppCompatSpinner channelSpinner,
            InlineChannelSpinnerAdapter channelSpinnerAdapter) {
        inputDeposit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                Helper.setViewVisibility(inlineBalanceView, hasFocus ? View.VISIBLE : View.INVISIBLE);
            }
        });

        linkCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Helper.setViewText(inputChannelName, null);
                Helper.setViewText(inputDeposit, null);
                Helper.setViewVisibility(container, View.GONE);
            }
        });

        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // validate deposit and channel name
                String channelNameString = Helper.normalizeChannelName(Helper.getValue(inputChannelName.getText()));
                Claim claimToSave = new Claim();
                claimToSave.setName(channelNameString);
                String channelName = claimToSave.getName().startsWith("@") ? claimToSave.getName().substring(1) : claimToSave.getName();
                String depositString = Helper.getValue(inputDeposit.getText());
                if ("@".equals(channelName) || Helper.isNullOrEmpty(channelName)) {
                    showError(getString(R.string.please_enter_channel_name));
                    return;
                }
                if (!LbryUri.isNameValid(channelName)) {
                    showError(getString(R.string.channel_name_invalid_characters));
                    return;
                }
                if (Helper.channelExists(channelName)) {
                    showError(getString(R.string.channel_name_already_created));
                    return;
                }

                double depositAmount = 0;
                try {
                    depositAmount = Double.valueOf(depositString);
                } catch (NumberFormatException ex) {
                    // pass
                    showError(getString(R.string.please_enter_valid_deposit));
                    return;
                }
                if (depositAmount <= 0.000001) {
                    String error = getResources().getQuantityString(R.plurals.min_deposit_required, Math.abs(depositAmount-1.0) <= 0.000001 ? 1 : 2, String.valueOf(Helper.MIN_DEPOSIT));
                    showError(error);
                    return;
                }
                if (Lbry.walletBalance == null || Lbry.getAvailableBalance() < depositAmount) {
                    showError(getString(R.string.deposit_more_than_balance));
                    return;
                }

                ChannelCreateUpdateTask task =  new ChannelCreateUpdateTask(
                        claimToSave, new BigDecimal(depositString), false, progressView, Lbryio.AUTH_TOKEN, new ClaimResultHandler() {
                    @Override
                    public void beforeStart() {
                        Helper.setViewEnabled(inputChannelName, false);
                        Helper.setViewEnabled(inputDeposit, false);
                        Helper.setViewEnabled(buttonCreate, false);
                        Helper.setViewEnabled(linkCancel, false);
                    }

                    @Override
                    public void onSuccess(Claim claimResult) {
                        if (!BuildConfig.DEBUG) {
                            LogPublishTask logPublishTask = new LogPublishTask(claimResult);
                            logPublishTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }

                        // channel created
                        Bundle bundle = new Bundle();
                        bundle.putString("claim_id", claimResult.getClaimId());
                        bundle.putString("claim_name", claimResult.getName());
                        LbryAnalytics.logEvent(LbryAnalytics.EVENT_CHANNEL_CREATE, bundle);

                        // add the claim to the channel list and set it as the selected item
                        if (channelSpinnerAdapter != null) {
                            channelSpinnerAdapter.add(claimResult);
                        }
                        if (channelSpinner != null && channelSpinnerAdapter != null) {
                            channelSpinner.setSelection(channelSpinnerAdapter.getCount() - 1);
                        }

                        Helper.setViewEnabled(inputChannelName, true);
                        Helper.setViewEnabled(inputDeposit, true);
                        Helper.setViewEnabled(buttonCreate, true);
                        Helper.setViewEnabled(linkCancel, true);
                    }

                    @Override
                    public void onError(Exception error) {
                        Helper.setViewEnabled(inputChannelName, true);
                        Helper.setViewEnabled(inputDeposit, true);
                        Helper.setViewEnabled(buttonCreate, true);
                        Helper.setViewEnabled(linkCancel, true);
                        showError(error.getMessage());
                    }
                });
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        Helper.setViewText(inlineBalanceValue, Helper.shortCurrencyFormat(Lbry.getAvailableBalance()));
    }

    private void loadChannelSettings(final String channelId) {
        Activity activity = getActivity();
        if (activity != null) {
            ((OdyseeApp) activity.getApplication()).getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Map<String, Object> options = new HashMap<>();
                        options.put("channel_id", channelId);
                        options.put(Lbryio.AUTH_TOKEN_PARAM, Lbryio.AUTH_TOKEN);

                        okhttp3.Response response = Comments.performRequest(Lbry.buildJsonParams(options), "setting.Get");
                        ResponseBody responseBody = response.body();
                        JSONObject jsonResponse = new JSONObject(responseBody.string());
                        if (!jsonResponse.has("result") || jsonResponse.isNull("result")) {
                            throw new ApiCallException("invalid json response");
                        }

                        JSONObject result = Helper.getJSONObject("result", jsonResponse);
                        Type type = new TypeToken<CreatorSetting>(){}.getType();
                        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
                        CreatorSetting creatorSetting = gson.fromJson(result.toString(), type);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                didLoadChannelSettings(creatorSetting);
                            }
                        });
                    } catch (JSONException | ApiCallException | IOException ex) {
                        // pass
                    }
                }
            });
        }
    }

    private void didLoadChannelSettings(CreatorSetting creatorSetting) {
        this.creatorSetting = creatorSetting;
    }
}
