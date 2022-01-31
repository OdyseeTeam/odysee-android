package com.odysee.app.ui.ytsync;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.odysee.app.R;
import com.odysee.app.listener.YouTubeSyncListener;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.LbryUri;

import lombok.Setter;

public class YouTubeSyncSetupFragment extends Fragment {
    @Setter
    private YouTubeSyncListener listener;

    private SwitchMaterial switchWantToSync;
    private TextView textHint;
    private MaterialButton skipButton;
    private MaterialButton claimNowButton;
    private TextInputEditText inputChannelName;
    private View viewInputError;
    private View setupProgress;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_youtube_sync_setup, container, false);

        switchWantToSync = root.findViewById(R.id.youtube_sync_switch_want_to_sync);
        //switchWantToSync.setMovementMethod(LinkMovementMethod.getInstance());
        switchWantToSync.setText(HtmlCompat.fromHtml(getString(R.string.i_want_to_sync), HtmlCompat.FROM_HTML_MODE_LEGACY));

        textHint = root.findViewById(R.id.youtube_sync_hint);
        textHint.setMovementMethod(LinkMovementMethod.getInstance());
        textHint.setText(HtmlCompat.fromHtml(getString(R.string.this_will_verify), HtmlCompat.FROM_HTML_MODE_LEGACY));

        inputChannelName = root.findViewById(R.id.youtube_sync_channel_name_input);
        viewInputError = root.findViewById(R.id.youtube_sync_channel_name_input_error);
        setupProgress = root.findViewById(R.id.youtube_sync_setup_progress);

        inputChannelName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String name = Helper.getValue(charSequence);
                if (name.startsWith("@")) {
                    name = name.substring(1);
                }

                if (!Helper.isNullOrEmpty(name)) {
                    boolean isValid = LbryUri.isNameValid(name);
                    viewInputError.setVisibility(isValid ? View.INVISIBLE : View.VISIBLE);
                    claimNowButton.setEnabled(isValid && switchWantToSync.isChecked());
                    return;
                }

                claimNowButton.setEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        skipButton = root.findViewById(R.id.youtube_sync_skip_button);
        claimNowButton = root.findViewById(R.id.youtube_sync_claim_now_button);

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onSkipPressed();
                }
            }
        });

        claimNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onClaimNowPressed(Helper.getValue(inputChannelName.getText()), skipButton, inputChannelName, view, setupProgress);
                }
            }
        });

        switchWantToSync.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                String name = Helper.getValue(inputChannelName.getText());
                if (name.startsWith("@")) {
                    name = name.substring(1);
                }

                boolean isValid = !Helper.isNullOrEmpty(name) && LbryUri.isNameValid(name);
                claimNowButton.setEnabled(isValid && checked);
            }
        });

        return root;
    }
}
