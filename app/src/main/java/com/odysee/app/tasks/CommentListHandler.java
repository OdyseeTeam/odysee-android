package com.odysee.app.tasks;

import java.util.List;

import com.odysee.app.model.Comment;

public interface CommentListHandler {
    void onSuccess(List<Comment> comments, boolean hasReachedEnd);
    void onError(Exception error);
}
