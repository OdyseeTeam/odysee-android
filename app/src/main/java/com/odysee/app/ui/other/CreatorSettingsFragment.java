package com.odysee.app.ui.other;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.ChannelWithThumbnailListSpinnerAdapter;
import com.odysee.app.adapter.FilteredWordListAdapter;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Claim;
import com.odysee.app.model.lbryinc.CreatorSetting;
import com.odysee.app.tasks.claim.ClaimListResultHandler;
import com.odysee.app.tasks.claim.ClaimListTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Comments;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.Lbryio;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.ResponseBody;

public class CreatorSettingsFragment extends BaseFragment {
    private static final String METHOD_UPDATE_SETTING = "setting.Update";
    private static final String DURATION_PATTERN = "^([0-9]+)([m|h|d|M|y]{1})$";
    private static final int ONE_EIGHTY_DAYS_IN_MINUTES = 259200;

    private AppCompatSpinner channelSelector;
    private ChannelWithThumbnailListSpinnerAdapter channelAdapter;
    private Claim currentChannel;
    private boolean autoloadingSettings;

    private SwitchMaterial switchCommentsEnabled;
    private TextInputEditText inputSlowModeValue;
    private TextInputEditText inputMinChannelAgeValue;
    private TextInputEditText inputMinTipAmountComments;
    private TextInputEditText inputMinTipAmountHyperchats;
    private TextInputEditText inputWordsToBlock;

    private MaterialButton buttonAddModerator;
    private MaterialButton buttonAddWords;

    private FilteredWordListAdapter mutedWordsAdapter;

    private RecyclerView listModerators;
    private RecyclerView listMutedWords;

    private ProgressBar loadingProgress;
    private boolean requestInProgress;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Context context = getContext();
        View root = inflater.inflate(R.layout.fragment_creator_settings, container, false);

        channelSelector = root.findViewById(R.id.channel_setting_spinner);

        switchCommentsEnabled = root.findViewById(R.id.switch_enable_comments);
        inputSlowModeValue = root.findViewById(R.id.input_slow_mode);
        inputMinChannelAgeValue = root.findViewById(R.id.input_min_channel_age);
        inputMinTipAmountComments = root.findViewById(R.id.input_min_tip_comments);
        inputMinTipAmountHyperchats = root.findViewById(R.id.input_min_tip_hyperchats);
        inputWordsToBlock = root.findViewById(R.id.input_add_words);

        buttonAddModerator = root.findViewById(R.id.button_add_moderator);
        buttonAddWords = root.findViewById(R.id.button_add_words);

        listModerators = root.findViewById(R.id.added_moderators);
        listMutedWords = root.findViewById(R.id.added_words);

        loadingProgress = root.findViewById(R.id.creator_settings_loading_progress);

        FlexboxLayoutManager flm1 = new FlexboxLayoutManager(context);
        listModerators.setLayoutManager(flm1);

        FlexboxLayoutManager flm2 = new FlexboxLayoutManager(context);
        listMutedWords.setLayoutManager(flm2);

        channelSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Object item = adapterView.getAdapter().getItem(i);
                if (item instanceof Claim) {
                    didSelectChannel((Claim) item);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        buttonAddWords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String value = Helper.getValue(inputWordsToBlock.getText());
                if (Helper.isNullOrEmpty(value)) {
                    showError(getString(R.string.no_words_to_block), Snackbar.LENGTH_SHORT);
                    return;
                }

                addBlockedWords(value);
            }
        });

        switchCommentsEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (autoloadingSettings) {
                    return;
                }

                updateCommentsEnabledSetting(checked);
            }
        });

        inputSlowModeValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (autoloadingSettings) {
                    return;
                }
                updateSlowModeSetting(Helper.getValue(charSequence));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        inputMinChannelAgeValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (autoloadingSettings) {
                    return;
                }

                updateMinChannelAgeValueSetting(Helper.getValue(charSequence));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        inputMinTipAmountComments.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (autoloadingSettings) {
                    return;
                }
                updateMinTipAmountCommentSetting(Helper.getValue(charSequence));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        inputMinTipAmountHyperchats.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (autoloadingSettings) {
                    return;
                }
                updateMinTipAmountHyperchatSetting(Helper.getValue(charSequence));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        return root;
    }

    public void onResume() {
        super.onResume();
        loadChannels();
    }

    private void addBlockedWords(final String value) {
        if (requestInProgress) {
            return;
        }

        toggleControlsEnabled(false);
        requestInProgress = true;
        Helper.setViewVisibility(loadingProgress, View.VISIBLE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Object> options = buildBaseOptions();
                    options.put("words", value);

                    okhttp3.Response response = Comments.performRequest(Lbry.buildJsonParams(options), "setting.BlockWord");
                    ResponseBody responseBody = response.body();
                    JSONObject jsonResponse = new JSONObject(responseBody.string());
                    if (!jsonResponse.has("result") || jsonResponse.isNull("result")) {
                        throw new ApiCallException("invalid json response");
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // finished saving, load the new values
                            didBlockWords();
                        }
                    });
                } catch (JSONException | ApiCallException | IOException ex) {
                    // error
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            failedToSaveSettings();
                        }
                    });
                }
            }
        });
    }

    private void removeBlockedWord(final String value) {
        if (requestInProgress) {
            return;
        }

        toggleControlsEnabled(false);
        requestInProgress = true;
        Helper.setViewVisibility(loadingProgress, View.VISIBLE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Object> options = buildBaseOptions();
                    options.put("words", value);

                    okhttp3.Response response = Comments.performRequest(Lbry.buildJsonParams(options), "setting.UnBlockWord");
                    ResponseBody responseBody = response.body();
                    JSONObject jsonResponse = new JSONObject(responseBody.string());
                    if (!jsonResponse.has("result") || jsonResponse.isNull("result")) {
                        throw new ApiCallException("invalid json response");
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // finished saving, load the new values
                            didUnblockWord(value);
                        }
                    });
                } catch (JSONException | ApiCallException | IOException ex) {
                    // error
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            failedToSaveSettings();
                        }
                    });
                }
            }
        });
    }

    private void updateCommentsEnabledSetting(final boolean value) {
        if (requestInProgress) {
            return;
        }

        toggleControlsEnabled(false);
        requestInProgress = true;
        Helper.setViewVisibility(loadingProgress, View.VISIBLE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Object> options = buildBaseOptions();
                    options.put("comments_enabled", value);

                    okhttp3.Response response = Comments.performRequest(Lbry.buildJsonParams(options), METHOD_UPDATE_SETTING);
                    ResponseBody responseBody = response.body();
                    JSONObject jsonResponse = new JSONObject(responseBody.string());
                    if (!jsonResponse.has("result") || jsonResponse.isNull("result")) {
                        throw new ApiCallException("invalid json response");
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // finished saving, load the new values
                            didSaveSettings();
                        }
                    });
                } catch (JSONException | ApiCallException | IOException ex) {
                    // error
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            failedToSaveSettings();
                        }
                    });
                }
            }
        });
    }

    private void updateSlowModeSetting(String value) {
        if (Helper.isNullOrEmpty(value)) {
            return;
        }

        // check that the value is a valid integer
        int intValue = 0;
        try {
            intValue = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            showError(getString(R.string.slow_mode_value_invalid), Snackbar.LENGTH_SHORT);
            return;
        }

        final int actualValue = intValue;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Object> options = buildBaseOptions();
                    options.put("slow_mode_min_gap", actualValue);

                    okhttp3.Response response = Comments.performRequest(Lbry.buildJsonParams(options), METHOD_UPDATE_SETTING);
                    ResponseBody responseBody = response.body();
                    JSONObject jsonResponse = new JSONObject(responseBody.string());
                    if (!jsonResponse.has("result") || jsonResponse.isNull("result")) {
                        throw new ApiCallException("invalid json response");
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // finished saving, load the new values
                            didSaveSettings();
                        }
                    });
                } catch (JSONException | ApiCallException | IOException ex) {
                    // error
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            failedToSaveSettings();
                        }
                    });
                }
            }
        });
    }

    private void checkMinTipAmountCommentSetting() {
        String value = Helper.getValue(inputMinTipAmountComments.getText());
        inputMinTipAmountHyperchats.setEnabled(Helper.isNullOrEmpty(value));
        if (!Helper.isNullOrEmpty(value)) {
            inputMinTipAmountHyperchats.setText(value);
        }
    }

    private void updateMinTipAmountCommentSetting(String value) {
        if (Helper.isNullOrEmpty(value)) {
            return;
        }

        // check that the value is a valid integer
        BigDecimal bd = new BigDecimal(0);
        try {
            bd = new BigDecimal(value);
        } catch (NumberFormatException ex) {
            showError(getString(R.string.min_tip_amount_invalid), Snackbar.LENGTH_SHORT);
            return;
        }

        final BigDecimal actualValue = bd;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Object> options = buildBaseOptions();
                    options.put("min_tip_amount_comment", actualValue.doubleValue());

                    okhttp3.Response response = Comments.performRequest(Lbry.buildJsonParams(options), METHOD_UPDATE_SETTING);
                    ResponseBody responseBody = response.body();
                    JSONObject jsonResponse = new JSONObject(responseBody.string());
                    if (!jsonResponse.has("result") || jsonResponse.isNull("result")) {
                        throw new ApiCallException("invalid json response");
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // finished saving, load the new values
                            didSaveSettings();
                        }
                    });
                } catch (JSONException | ApiCallException | IOException ex) {
                    // error
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            failedToSaveSettings();
                        }
                    });
                }
            }
        });
    }

    private void updateMinChannelAgeValueSetting(String value) {
        if (Helper.isNullOrEmpty(value)) {
            return;
        }

        // parse the duration value
        Pattern pattern = Pattern.compile(DURATION_PATTERN);
        Matcher matcher = pattern.matcher(value.trim());
        if (!matcher.matches() || matcher.groupCount() < 2) {
            showError(getString(R.string.min_channel_age_invalid), Snackbar.LENGTH_SHORT);
            return;
        }

        int numMinutes = 0;
        try {
            int num = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2).trim();

            switch (unit) {
                case "m":
                    numMinutes = num;
                    break;
                case "h":
                    numMinutes = num * 60;
                    break;
                case "d":
                    numMinutes = num * 60 * 24;
                    break;
                case "M":
                    numMinutes = num * 60 * 24 * 30;
                    break;
                default:
                    numMinutes = num * 60 * 24 * 365;
                    break;
            }
        } catch (NumberFormatException ex) {
            // shouldn't happen, but if it does, handle anyway
            showError(getString(R.string.min_channel_age_invalid), Snackbar.LENGTH_SHORT);
            return;
        }

        if (numMinutes > ONE_EIGHTY_DAYS_IN_MINUTES) {
            showError(getString(R.string.min_channel_age_180D), Snackbar.LENGTH_SHORT);
            return;
        }

        final int actualValue = numMinutes;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Object> options = buildBaseOptions();
                    options.put("time_since_first_comment", actualValue);

                    okhttp3.Response response = Comments.performRequest(Lbry.buildJsonParams(options), METHOD_UPDATE_SETTING);
                    ResponseBody responseBody = response.body();
                    JSONObject jsonResponse = new JSONObject(responseBody.string());
                    if (!jsonResponse.has("result") || jsonResponse.isNull("result")) {
                        throw new ApiCallException("invalid json response");
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // finished saving, load the new values
                            didSaveSettings();
                        }
                    });
                } catch (JSONException | ApiCallException | IOException ex) {
                    // error
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            failedToSaveSettings();
                        }
                    });
                }
            }
        });
    }

    private void updateMinTipAmountHyperchatSetting(String value) {
        if (Helper.isNullOrEmpty(value)) {
            return;
        }

        // check that the value is a valid integer
        BigDecimal bd = new BigDecimal(0);
        try {
            bd = new BigDecimal(value);
        } catch (NumberFormatException ex) {
            showError(getString(R.string.min_tip_amount_invalid), Snackbar.LENGTH_SHORT);
            return;
        }

        final BigDecimal actualValue = bd;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Object> options = buildBaseOptions();
                    options.put("min_tip_amount_super_chat", actualValue.doubleValue());

                    okhttp3.Response response = Comments.performRequest(Lbry.buildJsonParams(options), METHOD_UPDATE_SETTING);
                    ResponseBody responseBody = response.body();
                    JSONObject jsonResponse = new JSONObject(responseBody.string());
                    if (!jsonResponse.has("result") || jsonResponse.isNull("result")) {
                        throw new ApiCallException("invalid json response");
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // finished saving, load the new values
                            didSaveSettings();
                        }
                    });
                } catch (JSONException | ApiCallException | IOException ex) {
                    // error
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            failedToSaveSettings();
                        }
                    });
                }
            }
        });
    }

    private void returnToMain() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.showMessage(getString(R.string.no_channel_for_creator_settings));
            activity.navigateBackToMain();
        }
    }

    private void loadChannels() {
        toggleControlsEnabled(false);
        Map<String, Object> options = Lbry.buildClaimListOptions(Claim.TYPE_CHANNEL, 1, 999, true);
        ClaimListTask task = new ClaimListTask(options, Lbryio.AUTH_TOKEN, loadingProgress, new ClaimListResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                if (claims.size() == 0) {
                    returnToMain();
                    return;
                }

                didLoadChannels(claims);
            }

            @Override
            public void onError(Exception error) {
                returnToMain();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void toggleControlsEnabled(boolean enabled) {
        channelSelector.setEnabled(enabled);

        switchCommentsEnabled.setEnabled(enabled);
        inputSlowModeValue.setEnabled(enabled);
        inputMinChannelAgeValue.setEnabled(enabled);
        inputMinTipAmountComments.setEnabled(enabled);
        if (enabled) {
            inputMinTipAmountHyperchats.setEnabled(Helper.isNullOrEmpty(Helper.getValue(inputMinTipAmountComments.getText())));
        }
        inputWordsToBlock.setEnabled(enabled);

        buttonAddModerator.setEnabled(enabled);
        buttonAddWords.setEnabled(enabled);
    }

    private void didLoadChannels(List<Claim> channels) {
        if (channelAdapter == null || channelAdapter.getItemCount() != channels.size()) {
            channelAdapter = new ChannelWithThumbnailListSpinnerAdapter(getContext(), R.layout.list_item_channel_small_thumbnail, channels);
            channelSelector.setAdapter(channelAdapter);
        }
    }

    private void didSelectChannel(Claim channel) {
        currentChannel = channel;
        loadSettings();
    }

    private void loadSettings() {
        if (requestInProgress || currentChannel == null) {
            return;
        }

        toggleControlsEnabled(false);
        requestInProgress = true;
        Helper.setViewVisibility(loadingProgress, View.VISIBLE);

        if (mutedWordsAdapter != null) {
            mutedWordsAdapter.clear();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject params = new JSONObject();
                    params.put("channel_id", currentChannel.getClaimId());
                    params.put("channel_name", currentChannel.getName());
                    params.put("auth_token", Lbryio.AUTH_TOKEN);
                    JSONObject jsonChannelSign = Comments.channelSignName(params, currentChannel.getClaimId(), currentChannel.getName());

                    Map<String, Object> options = new HashMap<>();
                    options.put("channel_id", currentChannel.getClaimId());
                    options.put("channel_name", currentChannel.getName());
                    options.put("signature", Helper.getJSONString("signature", "", jsonChannelSign));
                    options.put("signing_ts", Helper.getJSONString("signing_ts", "", jsonChannelSign));

                    okhttp3.Response response = Comments.performRequest(Lbry.buildJsonParams(options), "setting.List");
                    ResponseBody responseBody = response.body();
                    JSONObject jsonResponse = new JSONObject(responseBody.string());
                    if (!jsonResponse.has("result") || jsonResponse.isNull("result")) {
                        throw new ApiCallException("invalid json response");
                    }

                    JSONObject result = Helper.getJSONObject("result", jsonResponse);
                    Type type = new TypeToken<CreatorSetting>(){}.getType();
                    Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
                    CreatorSetting creatorSetting = gson.fromJson(result.toString(), type);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            didLoadSettings(creatorSetting);
                        }
                    });
                } catch (JSONException | ApiCallException | IOException ex) {
                    // error
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            failedToLoadSettings();
                        }
                    });
                }
            }
        });
    }

    private void didLoadSettings(CreatorSetting creatorSetting) {
        requestInProgress = false;
        Helper.setViewVisibility(loadingProgress, View.INVISIBLE);
        toggleControlsEnabled(true);

        autoloadingSettings = true;

        switchCommentsEnabled.setChecked(creatorSetting.isCommentsEnabled());
        inputSlowModeValue.setText(
                creatorSetting.getSlowModeMinGap() > 0 ? String.valueOf(creatorSetting.getSlowModeMinGap()) : "");
        inputMinChannelAgeValue.setText(creatorSetting.getTimeSinceFirstCommentString());
        inputMinTipAmountComments.setText(creatorSetting.getMinTipAmountComment() != null ?
                String.valueOf(creatorSetting.getMinTipAmountComment()) : "");
        inputMinTipAmountHyperchats.setText(creatorSetting.getMinTipAmountSuperChat() != null ?
                String.valueOf(creatorSetting.getMinTipAmountSuperChat()) : "");
        checkMinTipAmountCommentSetting();

        // parse words into adapter
        if (!Helper.isNullOrEmpty(creatorSetting.getWords())) {
            List<String> words = Arrays.asList(creatorSetting.getWords().split(","));
            mutedWordsAdapter = new FilteredWordListAdapter(words, getContext());
            mutedWordsAdapter.setRemoveItemListener(new FilteredWordListAdapter.RemoveItemListener() {
                @Override
                public void onRemoveItem(String value) {
                    removeBlockedWord(value);
                }
            });

            listMutedWords.setAdapter(mutedWordsAdapter);
        }

        autoloadingSettings = false;
    }

    private void failedToLoadSettings() {
        requestInProgress = false;
        Helper.setViewVisibility(loadingProgress, View.INVISIBLE);
        showError(getString(R.string.creator_settings_load_failed, currentChannel.getName()));
    }

    private void didBlockWords() {
        inputWordsToBlock.setText(null);
        didSaveSettings();
    }

    private void didUnblockWord(String value) {
        if (mutedWordsAdapter != null) {
            mutedWordsAdapter.removeWord(value);
        }
        didSaveSettings();
    }

    private void didSaveSettings() {
        requestInProgress = false;
        toggleControlsEnabled(true);
        checkMinTipAmountCommentSetting();
        Helper.setViewVisibility(loadingProgress, View.INVISIBLE);
        loadSettings();
    }

    private void failedToSaveSettings() {
        requestInProgress = false;
        toggleControlsEnabled(true);
        Helper.setViewVisibility(loadingProgress, View.INVISIBLE);
        showError(getString(R.string.creator_settings_save_failed, currentChannel.getName()));
    }

    private void addModeratorDelegate() {
        if (currentChannel == null) {
            return;
        }
    }

    private Map<String, Object> buildBaseOptions() throws ApiCallException, JSONException {
        JSONObject params = new JSONObject();
        params.put("channel_id", currentChannel.getClaimId());
        params.put("channel_name", currentChannel.getName());
        params.put("auth_token", Lbryio.AUTH_TOKEN);
        JSONObject jsonChannelSign = Comments.channelSignName(params, currentChannel.getClaimId(), currentChannel.getName());

        Map<String, Object> options = new HashMap<>();
        options.put("channel_id", currentChannel.getClaimId());
        options.put("channel_name", currentChannel.getName());
        options.put("signature", Helper.getJSONString("signature", "", jsonChannelSign));
        options.put("signing_ts", Helper.getJSONString("signing_ts", "", jsonChannelSign));


        return options;
    }
}
