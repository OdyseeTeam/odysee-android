package com.odysee.app.ui.rewards;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import com.odysee.app.R;

public class RewardVerificationManualFragment extends Fragment {
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_reward_verification_manual, container, false);

        TextView desc = root.findViewById(R.id.reward_verification_manual_para1);
        desc.setText(HtmlCompat.fromHtml(getString(R.string.reward_verification_manual_desc), HtmlCompat.FROM_HTML_MODE_LEGACY));
        desc.setMovementMethod(LinkMovementMethod.getInstance());

        return root;
    }
}
