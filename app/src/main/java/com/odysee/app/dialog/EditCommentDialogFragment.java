package com.odysee.app.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.odysee.app.R;
import com.odysee.app.adapter.CommentAction;
import com.odysee.app.adapter.CommentListAdapter;
import com.odysee.app.model.Comment;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Setter;

public class EditCommentDialogFragment extends BottomSheetDialogFragment {
    public static final String TAG = "EditCommentDialog";

    @Setter
    private Comment            comment;
    @Setter
    private CommentListAdapter commentListAdapter;

    public static EditCommentDialogFragment newInstance() {
        return new EditCommentDialogFragment();
    }

    @Override
    public void onResume() {
        super.onResume();

        final EditText editText = getView().findViewById(R.id.comment_edit_text);

        // Keybboard doesn't show unless it's done in this runnable.
        // Probably simply because there's a brief delay. I tried countless
        // other stackoverflow solutions but something with the dialog fragment
        // architecture makes this a really sticky problem. Many others on SO having issues.
        editText.post(new Runnable() {
            @Override
            public void run() {
                editText.requestFocus();

                final InputMethodManager inputManager = (InputMethodManager)editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputManager != null) {
                    inputManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.dialog_edit_comment, container,false);

        final EditText editText = view.findViewById(R.id.comment_edit_text);
        editText.setText(comment.getText());

        final View doneButton = view.findViewById(R.id.done_editing_comment);
        final View progressSpinner = view.findViewById(R.id.comment_form_edit_progress);

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                doneButton.setVisibility(View.INVISIBLE);
                progressSpinner.setVisibility(View.VISIBLE);

                final String newCommentText = editText.getText().toString();

                if ( newCommentText == null || newCommentText.isEmpty() ) {
                    CommentAction.executeRemoveCommentTask(commentListAdapter, comment, new Runnable() {
                        @Override
                        public void run() {
                            dismiss();
                        }
                    });
                } else if ( newCommentText.equals(comment.getText()) ) {
                    dismiss();
                } else {
                    CommentAction.executeCommentActionTask(commentListAdapter, comment, CommentAction.EDIT, newCommentText, new CommentAction.CommentActionTaskHandler() {
                        @Override
                        public void fillJsonRpcParams(JSONObject params) throws JSONException {
                            params.put("comment", newCommentText);
                        }

                        @Override
                        public void onSuccess() {
                            commentListAdapter.updateCommentText(comment, newCommentText);

                            dismiss();
                        }

                        @Override
                        public void onError(Exception error) {
                            commentListAdapter.showError(commentListAdapter.getContext().getString(R.string.unable_to_edit_comment));

                            dismiss();
                        }
                    });
                }
            }
        });

        return view;
    }
}
