package com.odysee.app.ui.firstrun;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.odysee.app.R;
import com.odysee.app.utils.FirstRunStepHandler;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.LbryUri;

import lombok.Setter;

public class CreateChannelFragment extends Fragment {
    @Setter
    private FirstRunStepHandler firstRunStepHandler;

    private TextInputEditText inputChannelName;
    private View viewInputError;

    public View onCreateView(@NonNull LayoutInflater inflater,
                ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_create_channel, container, false);

        inputChannelName = root.findViewById(R.id.first_run_channel_name_input);
        viewInputError = root.findViewById(R.id.first_run_channel_name_input_error);

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
                    if (firstRunStepHandler != null) {
                        firstRunStepHandler.onChannelNameUpdated(isValid ? name : null);
                    }
                    return;
                }

                if (firstRunStepHandler != null) {
                    firstRunStepHandler.onChannelNameUpdated(null);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        return root;
    }

    public void onResume() {
        super.onResume();
        inputChannelName.requestFocus();
    }

}
