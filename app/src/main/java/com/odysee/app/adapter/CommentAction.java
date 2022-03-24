package com.odysee.app.adapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.odysee.app.R;
import com.odysee.app.dialog.EditCommentDialogFragment;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Claim;
import com.odysee.app.model.Comment;
import com.odysee.app.utils.Comments;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Response;
import okhttp3.ResponseBody;

public enum CommentAction {
    PIN(R.id.action_pin, R.string.pin_comment, R.drawable.ic_delete, "Pin") {
        @Override
        protected boolean isAvailable(Comment comment, final Claim claim) {
            return false;
        }

        @Override
        public void performAction(CommentListAdapter adapter, Comment comment) {

        }

        @Override
        public boolean isSuccess(JSONObject responseJson) {
            return false;
        }
    },
    EDIT(R.id.action_edit, R.string.edit, R.drawable.ic_delete, "Edit") {
        @Override
        protected boolean isAvailable(Comment comment, Claim claim) {
            if ( isCommentAuthoredByUsersCurrentChannel(comment, claim) ) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void performAction(CommentListAdapter adapter, Comment comment) {
            final EditCommentDialogFragment editCommentDialog = EditCommentDialogFragment.newInstance();
            editCommentDialog.setCommentListAdapter(adapter);
            editCommentDialog.setComment(comment);

            final FragmentActivity activity = (FragmentActivity)adapter.getContext();
            editCommentDialog.show(activity.getSupportFragmentManager(), EditCommentDialogFragment.TAG);
        }

        @Override
        public boolean isSuccess(JSONObject responseJson) {
            // Don't think success needs to get more specific than this.
            return responseJson.has("result");
        }
    },
    ADD_AS_MODERATOR(R.id.action_add_as_moderator, R.string.add_as_moderator, R.drawable.ic_delete, "AddModerator") {
        @Override
        protected boolean isAvailable(Comment comment, Claim claim) {
            return false;
        }

        @Override
        public void performAction(CommentListAdapter adapter, Comment comment) {

        }

        @Override
        public boolean isSuccess(JSONObject responseJson) {
            return false;
        }
    },
    REMOVE(R.id.action_remove, R.string.remove_comment, R.drawable.ic_delete, "Abandon") {
        @Override
        protected boolean isAvailable(Comment comment, Claim claim) {

            if ( isCommentAuthoredByUsersCurrentChannel(comment, claim) ) {
                return true;
            }

            for ( final Claim ithOwnChannel : Lbry.ownChannels ) {
                if ( ithOwnChannel != null && claim.getSigningChannel() != null) {
                    if (ithOwnChannel.getClaimId().equals(claim.getSigningChannel().getClaimId())) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public void performAction(CommentListAdapter adapter, Comment comment) {
            AlertDialog.Builder builder = new AlertDialog.Builder(adapter.getContext()).
            setTitle(R.string.confirm_comment_remove_dialog_title).
            setMessage(R.string.confirm_comment_remove_dialog_message)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    CommentAction.executeRemoveCommentTask(adapter, comment, new Runnable() {
                        @Override
                        public void run() {
                            // Nothing to do here.
                        }
                    });
                }
            }).setNegativeButton(R.string.no, null);

            builder.show();
        }

        @Override
        public boolean isSuccess(JSONObject responseJson) throws JSONException {
            return responseJson.getJSONObject("result").getBoolean("abandoned") == true;
        }
    },
    BLOCK(R.id.action_block, R.string.block_from_comment, R.drawable.ic_delete, "Block") {
        @Override
        protected boolean isAvailable(Comment comment, Claim claim) {
            return false;
        }

        @Override
        public void performAction(CommentListAdapter adapter, Comment comment) {

        }

        @Override
        public boolean isSuccess(JSONObject responseJson) throws JSONException  {
            return false;
        }
    },
    MUTE(R.id.action_mute, R.string.mute, R.drawable.ic_delete, "Mute") {
        @Override
        protected boolean isAvailable(Comment comment, Claim claim) {
            return false;
        }

        @Override
        public void performAction(CommentListAdapter adapter, Comment comment) {

        }

        @Override
        public boolean isSuccess(JSONObject responseJson) throws JSONException  {
            return false;
        }
    },
    COPY_LINK(R.id.action_copy_link, R.string.copy_link, R.drawable.ic_delete, "json_rpc_not_applicable") {
        @Override
        protected boolean isAvailable(Comment comment, Claim claim) {
            return false;
        }

        @Override
        public void performAction(CommentListAdapter adapter, Comment comment) {

        }

        @Override
        public boolean isSuccess(JSONObject responseJson) throws JSONException  {
            return false;
        }
    };

    public interface CommentActionTaskHandler {
        void fillJsonRpcParams(JSONObject params) throws JSONException;
        void onSuccess();
        void onError(Exception error);
    }

    public static void executeRemoveCommentTask(CommentListAdapter adapter, Comment comment, final Runnable onComplete) {
        executeCommentActionTask(adapter, comment, REMOVE, comment.getId(), new CommentActionTaskHandler() {
            @Override
            public void fillJsonRpcParams(JSONObject params) throws JSONException {
                params.put("mod_channel_id", comment.getChannelId());
                params.put("mod_channel_name", comment.getChannelName());
            }

            @Override
            public void onSuccess() {
                adapter.removeComment(comment);

                onComplete.run();
            }

            @Override
            public void onError(Exception error) {
                adapter.showError(adapter.getContext().getString(R.string.unable_to_delete_comment));

                onComplete.run();
            }
        });
    }

    public static void executeCommentActionTask(CommentListAdapter adapter, Comment comment, CommentAction action, String hexDataSource, CommentActionTaskHandler taskHandler) {
        AccountManager am            = AccountManager.get(adapter.getContext());
        Account        odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
        final String   authToken     = am.peekAuthToken(odyseeAccount, "auth_token_type");

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Comment              createdComment = null;
                ResponseBody responseBody   = null;
                try {
                    // check comments status endpoint
                    Comments.checkCommentsEndpointStatus();

                    JSONObject comment_body = new JSONObject();

                    if (!Helper.isNullOrEmpty(comment.getParentId())) {
                        comment_body.put("parent_id", comment.getParentId());
                    }

                    comment_body.put("comment_id", comment.getId());

                    taskHandler.fillJsonRpcParams(comment_body);

                    if (authToken != null) {
                        comment_body.put("auth_token", authToken);
                    }

                    JSONObject jsonChannelSign = Comments.channelSignWithCommentData(comment_body, comment, hexDataSource);

                    if (jsonChannelSign.has("signature") && jsonChannelSign.has("signing_ts")) {
                        comment_body.put("signature", jsonChannelSign.getString("signature"));
                        comment_body.put("signing_ts", jsonChannelSign.getString("signing_ts"));
                    }

                    Response resp = Comments.performRequest(comment_body, "comment." + action.jsonRpcMethod);
                    responseBody = resp.body();
                    if (responseBody != null) {
                        String responseString = responseBody.string();
                        resp.close();
                        JSONObject jsonResponse = new JSONObject(responseString);

                        if ( action.isSuccess(jsonResponse) == false ) {
                            final Exception error = new Exception("JSONRPC call failed.");
                            onCommentActionTaskError(adapter, taskHandler, error);
                        } else {
                            ((Activity)adapter.getContext()).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    taskHandler.onSuccess();
                                }
                            });
                        }
                    }
                } catch (ApiCallException | ClassCastException | IOException | JSONException ex) {
                    onCommentActionTaskError(adapter, taskHandler, ex);
                } finally {
                    if (responseBody != null) {
                        responseBody.close();
                    }
                }
            }
        });

        thread.start();
    }

    private static void onCommentActionTaskError(CommentListAdapter adapter, final CommentActionTaskHandler handler, final Exception error) {
        ((Activity)adapter.getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                handler.onError(error);
            }
        });
    }

    /**
     * This should reflect the current "commenting as" state. For now it doesn't appear
     * the app has a global way of determining the current channel and it's stored in the UI.
     * TODO: Right now this will return true for any channel that the current user owns,
     * which means network calls will fail if e.g. removing a comment that was not made by your "current channel".
     */
    private static boolean isCommentAuthoredByUsersCurrentChannel(final Comment comment, final Claim claim) {
        for ( final Claim ithOwnChannel : Lbry.ownChannels ) {
            if ( ithOwnChannel.getClaimId().equals(comment.getChannelId()) ) {
                return true;
            }
        }

        return false;
    }

    public static boolean areAnyActionsAvailable(Comment comment, Claim claim) {
        for ( final CommentAction ith : CommentAction.values() ) {
            if ( ith.isAvailable(comment, claim) ) {
                return true;
            }
        }

        return false;
    }

    public static CommentAction fromActionId(int actionId) {
        for ( final CommentAction ith : CommentAction.values() ) {
            if ( ith.actionId == actionId ) {
                return ith;
            }
        }

        return null;
    }

    protected final int actionId;
    protected final int stringId;
    protected final int iconId;
    public final String jsonRpcMethod;

    CommentAction(final int actionId, final int stringId, final int iconId, final String jsonRpcMethod) {
        this.actionId = actionId;
        this.stringId = stringId;
        this.iconId   = iconId;
        this.jsonRpcMethod = jsonRpcMethod;
    }

    protected abstract boolean isAvailable(Comment comment, final Claim claim);

    protected abstract void performAction(CommentListAdapter adapter, Comment comment);

    public abstract boolean isSuccess(final JSONObject responseJson) throws JSONException;
}
