package com.odysee.app.ui.ytsync;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.odysee.app.R;
import com.odysee.app.listener.YouTubeSyncListener;

import lombok.Setter;

public class YouTubeSyncStatusFragment extends Fragment {
    @Setter
    private YouTubeSyncListener listener;

    private TextView textHint;
    private MaterialButton newSyncButton;
    private MaterialButton exploreOdyseeButton;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_youtube_sync_status, container, false);

        textHint = root.findViewById(R.id.youtube_sync_status_hint);
        textHint.setMovementMethod(LinkMovementMethod.getInstance());
        textHint.setText(HtmlCompat.fromHtml(getString(R.string.you_will_be_able), HtmlCompat.FROM_HTML_MODE_LEGACY));

        newSyncButton = root.findViewById(R.id.youtube_sync_new_sync);
        exploreOdyseeButton = root.findViewById(R.id.youtube_sync_explore_odysee);
        newSyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onNewSyncPressed();
                }
            }
        });

        exploreOdyseeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onDonePressed();
                }
            }
        });

        return root;
    }
}
