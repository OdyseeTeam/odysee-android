package com.odysee.app.adapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;

import com.odysee.app.R;
import com.odysee.app.model.Claim;
import com.odysee.app.model.Comment;
import com.odysee.app.tasks.CommentOptionTask;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;

import org.json.JSONException;
import org.json.JSONObject;

public enum CommentOption {
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
            final EditText editText = new EditText(adapter.getContext());
            final int                    marginInPixels = Helper.getDimenAsPixels(adapter.getContext(), R.dimen.edit_text_dialog_margin);
            editText.setText(comment.getText());
            final FrameLayout editTextWrapper = new FrameLayout(adapter.getContext());
            editTextWrapper.setPadding(marginInPixels, 0, marginInPixels, 0);
            editTextWrapper.addView(editText);

            AlertDialog.Builder builder = new AlertDialog.Builder(adapter.getContext()).
                    setTitle(R.string.edit_comment_dialog_title).
                    setView(editTextWrapper)
                    .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            final String newCommentText = editText.getText().toString();

                            if ( newCommentText == null || newCommentText.isEmpty() ) {
                                CommentOption.executeRemoveCommentTask(adapter, comment);
                            } else if ( newCommentText.equals(comment.getText()) ) {
                                // Nothing to do. Automatically closes dialog and we're back we're started.
                            } else {
                                CommentOption.executeCommentOptionTask(adapter, comment, EDIT, newCommentText, new CommentOptionTask.CommentOptionTaskHandler() {
                                    @Override
                                    public void fillJsonRpcParams(JSONObject params) throws JSONException {
                                        params.put("comment", newCommentText);
                                    }

                                    @Override
                                    public void onSuccess() {
                                        adapter.updateCommentText(comment, newCommentText);
                                    }

                                    @Override
                                    public void onError(Exception error) {
                                        adapter.showError(adapter.getContext().getString(R.string.unable_to_edit_comment));
                                    }
                                });
                            }
                        }
                    }).setNegativeButton(R.string.cancel, null);

            builder.show();
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
                if ( ithOwnChannel.getClaimId().equals(claim.getSigningChannel().getClaimId()) ) {
                    return true;
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
                    CommentOption.executeRemoveCommentTask(adapter, comment);
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

    private static void executeRemoveCommentTask(CommentListAdapter adapter, Comment comment) {
        executeCommentOptionTask(adapter, comment, REMOVE, comment.getId(), new CommentOptionTask.CommentOptionTaskHandler() {
            @Override
            public void fillJsonRpcParams(JSONObject params) throws JSONException {
                params.put("mod_channel_id", comment.getChannelId());
                params.put("mod_channel_name", comment.getChannelName());
            }

            @Override
            public void onSuccess() {
                adapter.removeComment(comment);
            }

            @Override
            public void onError(Exception error) {
                adapter.showError(adapter.getContext().getString(R.string.unable_to_delete_comment));
            }
        });
    }

    private static void executeCommentOptionTask(CommentListAdapter adapter, Comment comment, CommentOption option, String hexDataSource, CommentOptionTask.CommentOptionTaskHandler taskHandler) {
        AccountManager am            = AccountManager.get(adapter.getContext());
        Account        odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
        final String   authToken     = am.peekAuthToken(odyseeAccount, "auth_token_type");

        CommentOptionTask task = new CommentOptionTask(comment, option, authToken, hexDataSource, taskHandler);

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    public static boolean areAnyOptionsAvailable(Comment comment, Claim claim) {
        for ( final CommentOption ith : CommentOption.values() ) {
            if ( ith.isAvailable(comment, claim) ) {
                return true;
            }
        }

        return false;
    }

    public static CommentOption fromActionId(int actionId) {
        for ( final CommentOption ith : CommentOption.values() ) {
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

    CommentOption(final int actionId, final int stringId, final int iconId, final String jsonRpcMethod) {
        this.actionId = actionId;
        this.stringId = stringId;
        this.iconId   = iconId;
        this.jsonRpcMethod = jsonRpcMethod;
    }

    protected abstract boolean isAvailable(Comment comment, final Claim claim);

    protected abstract void performAction(CommentListAdapter adapter, Comment comment);

    public abstract boolean isSuccess(final JSONObject responseJson) throws JSONException;
}
