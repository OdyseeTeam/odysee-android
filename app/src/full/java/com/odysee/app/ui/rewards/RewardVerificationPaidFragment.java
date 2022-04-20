package com.odysee.app.ui.rewards;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.odysee.app.R;
import com.odysee.app.listener.VerificationListener;

import lombok.Setter;

public class RewardVerificationPaidFragment extends Fragment {
    @Setter
    private VerificationListener listener;

    private static final double SKIP_QUEUE_PRICE = 4.99;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_reward_verification_paid, container, false);

        MaterialButton button = root.findViewById(R.id.reward_verification_skip_queue_button);
        button.setText(getString(R.string.skip_queue_button_text, String.valueOf(SKIP_QUEUE_PRICE)));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onSkipQueueAction();
                }
            }
        });

        return root;
    }
}
