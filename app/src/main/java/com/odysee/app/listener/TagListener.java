package com.odysee.app.listener;

import com.odysee.app.model.Tag;

public interface TagListener {
    void onTagAdded(Tag tag);
    void onTagRemoved(Tag tag);
}
