package com.odysee.app.ui.rewards;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.odysee.app.R;
import com.odysee.app.listener.VerificationListener;

import lombok.Setter;

public class RewardVerificationTwitterFragment extends Fragment {
    @Setter
    private VerificationListener listener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reward_verification_twitter, container, false);
    }
}