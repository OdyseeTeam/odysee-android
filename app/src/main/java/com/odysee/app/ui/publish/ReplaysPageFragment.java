package com.odysee.app.ui.publish;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.odysee.app.R;
import com.odysee.app.model.LivestreamReplay;
import com.odysee.app.utils.FormatTime;

import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Setter;

public class ReplaysPageFragment extends Fragment {
    @Setter
    private List<LivestreamReplay> replays;
    @Setter
    private ReplaySelectedListener listener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_replays_page, container, false);

        View layout1 = root.findViewById(R.id.replays_page_first);
        View layout2 = root.findViewById(R.id.replays_page_second);
        View layout3 = root.findViewById(R.id.replays_page_third);
        View layout4 = root.findViewById(R.id.replays_page_fourth);

        ImageView thumbnail1 = layout1.findViewById(R.id.livestream_replay_thumbnail);
        ImageView thumbnail2 = layout2.findViewById(R.id.livestream_replay_thumbnail);
        ImageView thumbnail3 = layout3.findViewById(R.id.livestream_replay_thumbnail);
        ImageView thumbnail4 = layout4.findViewById(R.id.livestream_replay_thumbnail);

        TextView textDuration1 = layout1.findViewById(R.id.livestream_replay_duration);
        TextView textDuration2 = layout2.findViewById(R.id.livestream_replay_duration);
        TextView textDuration3 = layout3.findViewById(R.id.livestream_replay_duration);
        TextView textDuration4 = layout4.findViewById(R.id.livestream_replay_duration);

        TextView textUploaded1 = layout1.findViewById(R.id.livestream_replay_uploaded);
        TextView textUploaded2 = layout2.findViewById(R.id.livestream_replay_uploaded);
        TextView textUploaded3 = layout3.findViewById(R.id.livestream_replay_uploaded);
        TextView textUploaded4 = layout4.findViewById(R.id.livestream_replay_uploaded);

        Context context = getContext();
        if (context != null) {
            // TODO: This crashes when changing Light/Dark mode
            initLayout(context, layout1, thumbnail1, textDuration1, textUploaded1, replays.get(0));
            if (replays.size() >= 2) initLayout(context, layout2, thumbnail2, textDuration2, textUploaded2, replays.get(1));
            if (replays.size() >= 3) initLayout(context, layout3, thumbnail3, textDuration3, textUploaded3, replays.get(2));
            if (replays.size() == 4) initLayout(context, layout4, thumbnail4, textDuration4, textUploaded4, replays.get(3));
        }

        RadioButton radio1 = layout1.findViewById(R.id.livestream_replay_radio_button);
        RadioButton radio2 = layout2.findViewById(R.id.livestream_replay_radio_button);
        RadioButton radio3 = layout3.findViewById(R.id.livestream_replay_radio_button);
        RadioButton radio4 = layout4.findViewById(R.id.livestream_replay_radio_button);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                radio1.setChecked(replays.get(0).selected);
                if (replays.size() >= 2) radio2.setChecked(replays.get(1).selected);
                if (replays.size() >= 3) radio3.setChecked(replays.get(2).selected);
                if (replays.size() == 4) radio4.setChecked(replays.get(3).selected);
            }
        });

        return root;
    }

    private void initLayout(Context context,
                            View layout,
                            ImageView thumbnail,
                            TextView textDuration,
                            TextView textUploaded,
                            LivestreamReplay replay) {
        layout.setVisibility(View.VISIBLE);

        if (replay.getStatus().equals("inprogress")) {
            layout.findViewById(R.id.livestream_replay_radio_button).setEnabled(false);

            textDuration.setText(getString(R.string.replay_processing, replay.getPercentComplete()));
        } else {
            layout.setOnClickListener(view -> listener.onReplaySelected(replay));

            Glide.with(context).load(replay.getThumbnailUrls().get(0)).into(thumbnail);

            int minutes = (int) TimeUnit.NANOSECONDS.toMinutes(replay.getDuration());
            textDuration.setText(getResources().getQuantityString(R.plurals.minutes, minutes, minutes));
        }

        textUploaded.setText(FormatTime.fromEpochMillis(replay.getCreated().getTime()));
    }

    public interface ReplaySelectedListener {
        void onReplaySelected(LivestreamReplay replay);
    }
}