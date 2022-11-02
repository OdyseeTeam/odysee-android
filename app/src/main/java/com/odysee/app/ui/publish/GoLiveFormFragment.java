package com.odysee.app.ui.publish;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.odysee.app.BuildConfig;
import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.InlineChannelSpinnerAdapter;
import com.odysee.app.adapter.LanguageSpinnerAdapter;
import com.odysee.app.adapter.LicenseSpinnerAdapter;
import com.odysee.app.adapter.ReplaysPagerAdapter;
import com.odysee.app.adapter.TagListAdapter;
import com.odysee.app.listener.FilePickerListener;
import com.odysee.app.listener.StoragePermissionListener;
import com.odysee.app.listener.WalletBalanceListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.Language;
import com.odysee.app.model.License;
import com.odysee.app.model.LivestreamReplay;
import com.odysee.app.model.Tag;
import com.odysee.app.model.WalletBalance;
import com.odysee.app.tasks.LivestreamReplaysResultHandler;
import com.odysee.app.tasks.LivestreamReplaysTask;
import com.odysee.app.tasks.UpdateSuggestedTagsTask;
import com.odysee.app.tasks.UploadImageTask;
import com.odysee.app.tasks.claim.ClaimListResultHandler;
import com.odysee.app.tasks.claim.ClaimListTask;
import com.odysee.app.tasks.claim.ClaimResultHandler;
import com.odysee.app.tasks.claim.PublishClaimTask;
import com.odysee.app.tasks.claim.ReplayPublishTask;
import com.odysee.app.tasks.lbryinc.LogPublishTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.ui.channel.ChannelCreateDialogFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;
import com.odysee.app.utils.Predefined;
import com.odysee.app.utils.Utils;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GoLiveFormFragment extends BaseFragment implements
        FilePickerListener, StoragePermissionListener, TagListAdapter.TagClickListener,
        WalletBalanceListener, ChannelCreateDialogFragment.ChannelCreateListener {
    private static final int SUGGESTED_TAGS_LIMIT = 8;
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final String SCHEDULED_LIVESTREAM_TAG = "c:scheduled-livestream";

    private ScrollView scrollView;

    private TextInputEditText inputTitle;
    private TextInputEditText inputAddress;
    private TextInputEditText inputDescription;
    private TextInputEditText inputTagFilter;
    private TextInputEditText inputOtherLicenseDescription;
    private TextInputEditText inputDeposit;

    private TextView textAddressChannel;
    private TextView textInlineAddressInvalid;
    private TextView textDateInfo;
    private TextView textReplaysTitle;
    private TextView textInlineDepositBalance;
    private TextView textReleaseTimePastNotAllowed;

    private TextView linkShowExtraFields;
    private TextView linkReleaseDate;
    private TextView linkReleaseTime;
    private TextView linkCancel;

    private ImageView imageThumbnail;

    private MaterialButtonToggleGroup createModeToggleGroup;
    private MaterialButtonToggleGroup dateToggleGroup;

    private MaterialButton buttonReleaseTimeDefault;
    private MaterialButton buttonCreate;

    private ImageButton buttonReloadReplays;

    private View noTagsView;
    private View noTagResultsView;
    private View mediaContainer;
    private View progressThumbnailUploading;
    private View progressLoadingChannels;
    private View progressLoadingReplays;
    private View progressCreating;
    private View layoutLivestreamDate;
    private View layoutScheduledPicker;
    private View layoutLivestreamReplays;
    private View layoutLivestreamReplaysList;
    private View layoutExtraFields;
    private View layoutOtherLicenseDescription;
    private View inlineDepositBalanceContainer;

    private AppCompatSpinner channelSpinner;
    private AppCompatSpinner languageSpinner;
    private AppCompatSpinner licenseSpinner;

    private RecyclerView suggestedTagsList;

    private ViewPager2 replaysViewPager;

    private TabLayout replaysTabLayout;

    private TagListAdapter addedTagsAdapter;
    private TagListAdapter suggestedTagsAdapter;

    private InlineChannelSpinnerAdapter channelSpinnerAdapter;

    private ReplaysPagerAdapter replaysAdapter;

    // In progress variables
    private boolean launchPickerPending;
    private boolean uploadingThumbnail;
    private boolean fetchingChannels;
    private boolean saveInProgress;

    // State variables
    private String lastSelectedThumbnailFile;
    private String uploadedThumbnailUrl;
    private boolean storageRefusedOnce;
    private boolean editFieldsLoaded;
    private boolean editMode;
    private boolean modeLivestream = true;
    private boolean anytimeStream = true;
    private Claim currentClaim;
    private String currentTagFilter;
    private LivestreamReplay selectedLivestreamReplay;
    private long releaseDateMillis = Long.MIN_VALUE;
    private int releaseTimeHours = -1;
    private int releaseTimeMinutes = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_go_live_form, container, false);

        scrollView = root.findViewById(R.id.go_live_form_scroll_view);

        inputTitle = root.findViewById(R.id.go_live_form_input_title);
        inputAddress = root.findViewById(R.id.go_live_form_input_address);
        inputDescription = root.findViewById(R.id.go_live_form_input_description);
        inputTagFilter = root.findViewById(R.id.form_tag_filter_input);
        inputOtherLicenseDescription = root.findViewById(R.id.go_live_form_input_license_other);
        inputDeposit = root.findViewById(R.id.go_live_form_input_deposit);

        textAddressChannel = root.findViewById(R.id.go_live_form_address_channel);
        textInlineAddressInvalid = root.findViewById(R.id.go_live_form_inline_address_invalid);
        textDateInfo = root.findViewById(R.id.go_live_form_date_info);
        textReplaysTitle = root.findViewById(R.id.go_live_form_replays_title);
        textInlineDepositBalance = root.findViewById(R.id.go_live_form_inline_balance_value);
        textReleaseTimePastNotAllowed = root.findViewById(R.id.go_live_form_release_time_past_not_allowed);

        linkShowExtraFields = root.findViewById(R.id.go_live_form_toggle_extra);
        linkReleaseDate = root.findViewById(R.id.go_live_form_release_date);
        linkReleaseTime = root.findViewById(R.id.go_live_form_release_time);
        linkCancel = root.findViewById(R.id.go_live_form_cancel);

        imageThumbnail = root.findViewById(R.id.go_live_form_thumbnail_preview);

        createModeToggleGroup = root.findViewById(R.id.go_live_form_create_mode_toggle_group);
        dateToggleGroup = root.findViewById(R.id.go_live_form_date_toggle_group);

        buttonReleaseTimeDefault = root.findViewById(R.id.go_live_form_release_time_default);
        buttonCreate = root.findViewById(R.id.go_live_form_create_button);

        buttonReloadReplays = root.findViewById(R.id.go_live_form_replays_reload);

        noTagsView = root.findViewById(R.id.form_no_added_tags);
        noTagResultsView = root.findViewById(R.id.form_no_tag_results);
        mediaContainer = root.findViewById(R.id.go_live_form_media_container);
        progressThumbnailUploading = root.findViewById(R.id.go_live_form_thumbnail_upload_progress);
        progressLoadingChannels = root.findViewById(R.id.go_live_form_loading_channels);
        progressLoadingReplays = root.findViewById(R.id.go_live_form_replays_progress);
        progressCreating = root.findViewById(R.id.go_live_form_creating);
        layoutLivestreamDate = root.findViewById(R.id.go_live_form_date_container);
        layoutScheduledPicker = root.findViewById(R.id.go_live_form_scheduled_picker);
        layoutLivestreamReplays = root.findViewById(R.id.go_live_form_replays_container);
        layoutLivestreamReplaysList = root.findViewById(R.id.go_live_form_replays_list);
        layoutExtraFields = root.findViewById(R.id.go_live_form_extra_options_container);
        layoutOtherLicenseDescription = root.findViewById(R.id.go_live_form_license_other_layout);
        inlineDepositBalanceContainer = root.findViewById(R.id.go_live_form_inline_balance_container);

        channelSpinner = root.findViewById(R.id.go_live_form_channel_spinner);
        languageSpinner = root.findViewById(R.id.go_live_form_language_spinner);
        licenseSpinner = root.findViewById(R.id.go_live_form_license_spinner);

        Context context = getContext();
        FlexboxLayoutManager flm1 = new FlexboxLayoutManager(context);
        FlexboxLayoutManager flm2 = new FlexboxLayoutManager(context);
        RecyclerView addedTagsList = root.findViewById(R.id.form_added_tags);
        addedTagsList.setLayoutManager(flm1);
        suggestedTagsList = root.findViewById(R.id.form_suggested_tags);
        suggestedTagsList.setLayoutManager(flm2);

        addedTagsAdapter = new TagListAdapter(new ArrayList<>(), context);
        addedTagsAdapter.setCustomizeMode(TagListAdapter.CUSTOMIZE_MODE_REMOVE);
        addedTagsAdapter.setClickListener(this);
        addedTagsList.setAdapter(addedTagsAdapter);

        suggestedTagsAdapter = new TagListAdapter(new ArrayList<>(), context);
        suggestedTagsAdapter.setCustomizeMode(TagListAdapter.CUSTOMIZE_MODE_ADD);
        suggestedTagsAdapter.setClickListener(this);
        suggestedTagsList.setAdapter(suggestedTagsAdapter);

        replaysViewPager = root.findViewById(R.id.go_live_form_replays_pager);

        replaysTabLayout = root.findViewById(R.id.go_live_form_replays_tab_layout);

        initUi();

        return root;
    }

    private void initUi() {
        Context context = getContext();

        inputAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String value = Helper.getValue(charSequence);
                boolean invalid = !Helper.isNullOrEmpty(value) && !LbryUri.isNameValid(value);
                Helper.setViewVisibility(textInlineAddressInvalid, invalid ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        buttonReloadReplays.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                replaysViewPager.setAdapter(null);
                fetchLivestreamReplays();
            }
        });

        inputTagFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String value = Helper.getValue(charSequence);
                setTagFilter(value);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mediaContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkStoragePermissionAndLaunchFilePicker();
            }
        });

        channelSpinnerAdapter = new InlineChannelSpinnerAdapter(getContext(), R.layout.spinner_item_channel, new ArrayList<>());
        channelSpinnerAdapter.addPlaceholder(false);

        channelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                Object item = adapterView.getItemAtPosition(position);
                if (item instanceof Claim) {
                    Claim claim = (Claim) item;
                    if (claim.isPlaceholder()) {
                        if (!fetchingChannels) {
                            showChannelCreator();
                        }
                        textAddressChannel.setText(R.string.url_anonymous_prefix);
                    } else {
                        textAddressChannel.setText(getString(R.string.url_channel_prefix, claim.getName()));
                    }
                    fetchLivestreamReplays();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        createModeToggleGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                modeLivestream = checkedId == R.id.go_live_form_new_livestream;
                layoutLivestreamDate.setVisibility(modeLivestream ? View.VISIBLE : View.GONE);
                layoutLivestreamReplays.setVisibility(modeLivestream ? View.GONE : View.VISIBLE);
            }
        });

        dateToggleGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                anytimeStream = checkedId == R.id.go_live_form_anytime;
                textDateInfo.setText(anytimeStream ? R.string.anytime_info : R.string.scheduled_info);
                layoutScheduledPicker.setVisibility(anytimeStream ? View.GONE : View.VISIBLE);
            }
        });

        linkShowExtraFields.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layoutExtraFields.getVisibility() != View.VISIBLE) {
                    layoutExtraFields.setVisibility(View.VISIBLE);
                    linkShowExtraFields.setText(R.string.hide_extra_fields);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                } else {
                    layoutExtraFields.setVisibility(View.GONE);
                    linkShowExtraFields.setText(R.string.show_extra_fields);
                }
            }
        });

        setDefaultScheduled();

        linkReleaseDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (context instanceof MainActivity) {
                    MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                            .setSelection(releaseDateMillis > Long.MIN_VALUE
                                    ? releaseDateMillis : MaterialDatePicker.todayInUtcMilliseconds())
                            .setCalendarConstraints(
                                    new CalendarConstraints.Builder()
                                            .setValidator(DateValidatorPointForward.now())
                                            .build()
                            )
                            .build();
                    datePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
                        @Override
                        public void onPositiveButtonClick(Long millis) {
                            long localMillis = LocalDateTime
                                    .ofInstant(Instant.ofEpochMilli(millis), UTC_ZONE)
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli();
                            linkReleaseDate.setText(DateUtils.formatDateTime(context, localMillis, DateUtils.FORMAT_SHOW_DATE));
                            releaseDateMillis = millis;
                            checkReleaseTimeInvalid();
                        }
                    });
                    datePicker.show(((MainActivity) context).getSupportFragmentManager(), "DatePicker");
                }
            }
        });

        linkReleaseTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (context instanceof MainActivity) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.add(Calendar.HOUR_OF_DAY, 1);

                    MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                            .setTimeFormat(DateFormat.is24HourFormat(context) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                            .setHour(releaseTimeHours > -1 ? releaseTimeHours : calendar.get(Calendar.HOUR_OF_DAY))
                            .setMinute(releaseTimeMinutes > -1 ? releaseTimeMinutes : calendar.get(Calendar.MINUTE))
                            .build();
                    timePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            releaseTimeHours = timePicker.getHour();
                            releaseTimeMinutes = timePicker.getMinute();

                            Calendar calendar = Calendar.getInstance();
                            calendar.set(Calendar.HOUR_OF_DAY, releaseTimeHours);
                            calendar.set(Calendar.MINUTE, releaseTimeMinutes);

                            linkReleaseTime.setText(checkReleaseTimeInvalid() ? getString(R.string.time_default)
                                    : DateUtils.formatDateTime(context, calendar.getTimeInMillis(), DateUtils.FORMAT_SHOW_TIME));
                        }
                    });
                    timePicker.show(((MainActivity) context).getSupportFragmentManager(), "TimePicker");
                }
            }
        });

        buttonReleaseTimeDefault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDefaultScheduled();
                textReleaseTimePastNotAllowed.setVisibility(View.GONE);
            }
        });

        languageSpinner.setAdapter(new LanguageSpinnerAdapter(context, R.layout.spinner_item_generic));
        licenseSpinner.setAdapter(new LicenseSpinnerAdapter(context, R.layout.spinner_item_generic));

        licenseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                License license = (License) adapterView.getAdapter().getItem(position);
                boolean otherLicense = Arrays.asList(
                        Predefined.LICENSE_COPYRIGHTED.toLowerCase(),
                        Predefined.LICENSE_OTHER.toLowerCase()).contains(license.getName().toLowerCase());
                Helper.setViewVisibility(layoutOtherLicenseDescription, otherLicense ? View.VISIBLE : View.GONE);
                if (!otherLicense) {
                    inputOtherLicenseDescription.setText(null);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        inputDeposit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                Helper.setViewVisibility(inlineDepositBalanceContainer, hasFocus ? View.VISIBLE : View.GONE);
            }
        });

        linkCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                if (context instanceof MainActivity) {
                    ((MainActivity) context).onBackPressed();
                }
            }
        });

        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uploadingThumbnail) {
                    showMessage(R.string.publish_thumbnail_in_progress);
                    return;
                } else if (Helper.isNullOrEmpty(uploadedThumbnailUrl)) {
                    showError(getString(R.string.publish_no_thumbnail));
                    return;
                }

                // check minimum deposit
                String depositString = Helper.getValue(inputDeposit.getText());
                double depositAmount;
                try {
                    depositAmount = Double.parseDouble(depositString);
                } catch (NumberFormatException ex) {
                    // pass
                    showError(getString(R.string.please_enter_valid_deposit));
                    return;
                }
                if (depositAmount < Helper.MIN_DEPOSIT) {
                    showError(getResources().getQuantityString(R.plurals.min_deposit_required, depositAmount == 1 ? 1 : 2, String.valueOf(Helper.MIN_DEPOSIT)));
                    return;
                }
                if (Lbry.getAvailableBalance() < depositAmount) {
                    showError(getString(R.string.deposit_more_than_balance));
                    return;
                }

                if (releaseDateMillis > Long.MIN_VALUE && (releaseTimeHours == -1 || releaseTimeMinutes == -1)) {
                    showError(getString(R.string.invalid_release_time));
                    return;
                }

                Claim claim = buildLivestreamClaim();
                if (validateLivestreamClaim(claim)) {
                    publishLivestream(claim);
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity activity = (MainActivity) getContext();
        if (activity != null) {
            activity.hideSearchBar();

            activity.addFilePickerListener(this);
            activity.addWalletBalanceListener(this);

            activity.setActionBarTitle(R.string.go_live);
        }
    }

    @Override
    public void onStop() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) getContext();
            if (!MainActivity.startingFilePickerActivity) {
                activity.removeWalletBalanceListener(this);
                activity.removeFilePickerListener(this);
            }
        }

        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        checkParams();
        checkRewardsDriver();
        updateFieldsFromCurrentClaim();
        fetchChannels();

        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            LbryAnalytics.setCurrentScreen(activity, "Go Live Form", "GoLiveForm");
            activity.addStoragePermissionListener(this);
            if (editMode) {
                activity.setActionBarTitle(R.string.edit_livestream);
                buttonCreate.setText(R.string.save);
            }
        }

        String filterText = Helper.getValue(inputTagFilter.getText());
        updateSuggestedTags(filterText, SUGGESTED_TAGS_LIMIT, true);
    }

    private void checkParams() {
        Map<String, Object> params = getParams();
        if (params != null) {
            if (params.containsKey("claim")) {
                Claim claim = (Claim) params.get("claim");
                if (claim != null && !claim.equals(currentClaim)) {
                    currentClaim = claim;
                    editFieldsLoaded = false;
                }
            }
        }
    }

    private void updateFieldsFromCurrentClaim() {
        if (currentClaim != null && !editFieldsLoaded) {
            Context context = getContext();
            try {
                Claim.StreamMetadata metadata = (Claim.StreamMetadata) currentClaim.getValue();
                if (context != null) {
                    uploadedThumbnailUrl = currentClaim.getThumbnailUrl(Utils.STREAM_THUMBNAIL_WIDTH, Utils.STREAM_THUMBNAIL_HEIGHT, Utils.STREAM_THUMBNAIL_Q);
                }
                if (context != null && !Helper.isNullOrEmpty(uploadedThumbnailUrl)) {
                    Glide.with(context.getApplicationContext()).load(uploadedThumbnailUrl).centerCrop().into(imageThumbnail);
                }

                inputTitle.setText(currentClaim.getTitle());
                inputDescription.setText(currentClaim.getDescription());
                if (addedTagsAdapter != null && currentClaim.getTagObjects() != null) {
                    addedTagsAdapter.addTags(currentClaim.getTagObjects());
                    updateSuggestedTags(currentTagFilter, SUGGESTED_TAGS_LIMIT, true);
                }

                inputAddress.setText(currentClaim.getName());
                inputDeposit.setText(currentClaim.getAmount());

                if (metadata.getLanguages() != null && metadata.getLanguages().size() > 0) {
                    // get the first language
                    String langCode = metadata.getLanguages().get(0);
                    int langCodePosition = ((LanguageSpinnerAdapter) languageSpinner.getAdapter()).getItemPosition(langCode);
                    if (langCodePosition > -1) {
                        languageSpinner.setSelection(langCodePosition);
                    }
                }

                if (!Helper.isNullOrEmpty(metadata.getLicense())) {
                    LicenseSpinnerAdapter adapter = (LicenseSpinnerAdapter) licenseSpinner.getAdapter();
                    int licPosition = adapter.getItemPosition(metadata.getLicense());
                    if (licPosition == -1) {
                        licPosition = adapter.getItemPosition(Predefined.LICENSE_OTHER);
                    }
                    if (licPosition > -1) {
                        licenseSpinner.setSelection(licPosition);
                    }

                    License selectedLicense = (License) licenseSpinner.getSelectedItem();
                    boolean otherLicense = Arrays.asList(
                            Predefined.LICENSE_COPYRIGHTED.toLowerCase(),
                            Predefined.LICENSE_OTHER.toLowerCase()).contains(selectedLicense.getName().toLowerCase());
                    inputOtherLicenseDescription.setText(otherLicense ? metadata.getLicense() : null);
                }

                inputAddress.setEnabled(false);
                editMode = true;
                editFieldsLoaded = true;
            } catch (ClassCastException ex) {
                // invalid claim value type
                cancelOnFatalCondition(getString(R.string.publish_invalid_claim_type));
            }
        }
    }

    // region: Channels
    private void fetchChannels() {
        if (Lbry.ownChannels != null && Lbry.ownChannels.size() > 0) {
            updateChannelList(Lbry.ownChannels);
            return;
        }

        fetchingChannels = true;
        disableChannelSpinner();
        Map<String, Object> options = Lbry.buildClaimListOptions(Claim.TYPE_CHANNEL, 1, 999, true);
        ClaimListTask task = new ClaimListTask(options, Lbryio.AUTH_TOKEN, progressLoadingChannels, new ClaimListResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                Lbry.ownChannels = new ArrayList<>(claims);
                updateChannelList(Lbry.ownChannels);
                enableChannelSpinner();
                fetchingChannels = false;
            }

            @Override
            public void onError(Exception error) {
                enableChannelSpinner();
                fetchingChannels = false;
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void disableChannelSpinner() {
        Helper.setViewEnabled(channelSpinner, false);
    }

    private void enableChannelSpinner() {
        Helper.setViewEnabled(channelSpinner, true);
    }

    private void showChannelCreator() {
        MainActivity activity = (MainActivity) getActivity();

        if (activity != null) {
            activity.showChannelCreator(this);
        }
    }

    private void updateChannelList(List<Claim> channels) {
        Context context = getContext();
        if (channelSpinnerAdapter == null) {
            if (context != null) {
                channelSpinnerAdapter = new InlineChannelSpinnerAdapter(context, R.layout.spinner_item_channel, new ArrayList<>(channels));
                channelSpinnerAdapter.addPlaceholder(false);
                channelSpinnerAdapter.notifyDataSetChanged();
            }
        } else {
            channelSpinnerAdapter.clear();
            channelSpinnerAdapter.addPlaceholder(false);
            channelSpinnerAdapter.addAll(channels);
            channelSpinnerAdapter.notifyDataSetChanged();
        }

        if (channelSpinner != null) {
            channelSpinner.setAdapter(channelSpinnerAdapter);
        }

        if (channelSpinnerAdapter != null && channelSpinner != null) {
            if (editMode) {
                if (currentClaim.getSigningChannel() != null) {
                    int position = channelSpinnerAdapter.getItemPosition(currentClaim.getSigningChannel());
                    if (position > -1) {
                        channelSpinner.setSelection(position);
                    }
                }
            } else {
                if (channelSpinnerAdapter.getCount() > 1) {
                    String defaultChannelName = Helper.getDefaultChannelName(context);

                    List<Claim> defaultChannel = channels.stream().filter(c -> c != null && c.getName().equalsIgnoreCase(defaultChannelName)).collect(Collectors.toList());

                    if (defaultChannel.size() > 0) {
                        channelSpinner.setSelection(channelSpinnerAdapter.getItemPosition(defaultChannel.get(0)));
                    } else {
                        // Always select something as livestreaming needs a channel
                        channelSpinner.setSelection(1);
                    }
                }
            }
        }
    }
    // endregion

    // region: Tags
    private void setTagFilter(String filter) {
        currentTagFilter = filter;
        updateSuggestedTags(currentTagFilter, SUGGESTED_TAGS_LIMIT, true);
    }

    private void checkNoAddedTags() {
        Helper.setViewVisibility(noTagsView, addedTagsAdapter == null || addedTagsAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void checkNoTagResults() {
        Helper.setViewVisibility(noTagResultsView, suggestedTagsAdapter == null || suggestedTagsAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void addTag(Tag tag) {
        if (saveInProgress) {
            return;
        }

        if (addedTagsAdapter.getTags().contains(tag)) {
            showMessage(getString(R.string.tag_already_added, tag.getName()));
            return;
        }
        if (addedTagsAdapter.getItemCount() == 5) {
            showMessage(R.string.tag_limit_reached);
            return;
        }

        addedTagsAdapter.addTag(tag);
        if (suggestedTagsAdapter != null) {
            suggestedTagsAdapter.removeTag(tag);
        }
        updateSuggestedTags(currentTagFilter, SUGGESTED_TAGS_LIMIT, false);

        checkNoAddedTags();
        checkNoTagResults();
    }

    public void removeTag(Tag tag) {
        if (saveInProgress) {
            return;
        }
        addedTagsAdapter.removeTag(tag);
        updateSuggestedTags(currentTagFilter, SUGGESTED_TAGS_LIMIT, false);
        checkNoAddedTags();
        checkNoTagResults();
    }

    private void updateSuggestedTags(String filter, int limit, boolean clearPrevious) {
        UpdateSuggestedTagsTask task = new UpdateSuggestedTagsTask(
                filter,
                limit,
                addedTagsAdapter,
                suggestedTagsAdapter,
                clearPrevious,
                true, new UpdateSuggestedTagsTask.KnownTagsHandler() {
            @Override
            public void onSuccess(List<Tag> tags) {
                if (suggestedTagsAdapter == null) {
                    suggestedTagsAdapter = new TagListAdapter(tags, getContext());
                    suggestedTagsAdapter.setCustomizeMode(TagListAdapter.CUSTOMIZE_MODE_ADD);
                    suggestedTagsAdapter.setClickListener(GoLiveFormFragment.this);
                    if (suggestedTagsList != null) {
                        suggestedTagsList.setAdapter(suggestedTagsAdapter);
                    }
                } else {
                    suggestedTagsAdapter.setTags(tags);
                }

                checkNoAddedTags();
                checkNoTagResults();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    // endregion

    // region: Replays
    private void fetchLivestreamReplays() {
        Claim channel = (Claim) channelSpinner.getSelectedItem();
        if (channel.isPlaceholder()) {
            layoutLivestreamReplaysList.setVisibility(View.GONE);
            textReplaysTitle.setText(R.string.no_replays);
            return;
        }
        LivestreamReplaysTask task = new LivestreamReplaysTask(channel, progressLoadingReplays, Lbryio.AUTH_TOKEN, new LivestreamReplaysResultHandler() {
            @Override
            public void onSuccess(List<LivestreamReplay> replays) {
                if (replays.size() != 0) {
                    layoutLivestreamReplaysList.setVisibility(View.VISIBLE);
                    textReplaysTitle.setText(R.string.select_replay);
                } else {
                    layoutLivestreamReplaysList.setVisibility(View.GONE);
                    textReplaysTitle.setText(R.string.no_replays);
                    return;
                }

                replaysAdapter = new ReplaysPagerAdapter(getActivity(), replays, new ReplaysPagerAdapter.SelectedReplayManager() {
                    @Override
                    public LivestreamReplay getSelectedReplay() {
                        return selectedLivestreamReplay;
                    }

                    @Override
                    public void setSelectedReplay(LivestreamReplay replay) {
                        selectedLivestreamReplay = replay;
                    }
                });
                replaysViewPager.setAdapter(replaysAdapter);
                new TabLayoutMediator(replaysTabLayout, replaysViewPager, new TabLayoutMediator.TabConfigurationStrategy() {
                    @Override
                    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                        tab.setText(String.valueOf(position + 1));
                    }
                }).attach();
            }

            @Override
            public void onError(Exception error) {
                showError(error.getMessage());
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    // endregion

    // region: Claim creation and publish
    private Claim buildLivestreamClaim() {
        Claim claim = new Claim();

        claim.setName(Helper.getValue(inputAddress.getText()));
        claim.setAmount(Helper.getValue(inputDeposit.getText()));

        Claim.StreamMetadata metadata = new Claim.StreamMetadata();
        metadata.setTitle(Helper.getValue(inputTitle.getText()));
        metadata.setDescription(Helper.getValue(inputDescription.getText()));

        List<String> tags = Helper.getTagsForTagObjects(addedTagsAdapter.getTags());
        if (!anytimeStream) {
            tags.add(SCHEDULED_LIVESTREAM_TAG);
        }
        metadata.setTags(tags);

        Claim selectedChannel = (Claim) channelSpinner.getSelectedItem();
        if (selectedChannel != null && !selectedChannel.isPlaceholder() && !selectedChannel.isPlaceholderAnonymous()) {
            claim.setSigningChannel(selectedChannel);
        }

        if (!Helper.isNullOrEmpty(uploadedThumbnailUrl)) {
            Claim.Resource thumbnail = new Claim.Resource();
            thumbnail.setUrl(uploadedThumbnailUrl);
            metadata.setThumbnail(thumbnail);
        }

        Language selectedLanguage = (Language) languageSpinner.getSelectedItem();
        if (selectedLanguage != null) {
            metadata.setLanguages(Collections.singletonList(selectedLanguage.getCode()));
        }

        License selectedLicense = (License) licenseSpinner.getSelectedItem();
        if (selectedLicense != null) {
            boolean otherLicense = Arrays.asList(
                    Predefined.LICENSE_COPYRIGHTED.toLowerCase(),
                    Predefined.LICENSE_OTHER.toLowerCase()).contains(selectedLicense.getName().toLowerCase());
            metadata.setLicense(otherLicense ? Helper.getValue(inputOtherLicenseDescription.getText()) : selectedLicense.getName());
            metadata.setLicenseUrl(selectedLicense.getUrl());
        }

        if (!anytimeStream &&
                releaseDateMillis > Long.MIN_VALUE &&
                releaseTimeHours > -1 &&
                releaseTimeMinutes > -1) {
            long secs = LocalDateTime
                    .ofInstant(Instant.ofEpochMilli(releaseDateMillis), UTC_ZONE)
                    .plusHours(releaseTimeHours)
                    .plusMinutes(releaseTimeMinutes)
                    .atZone(ZoneId.systemDefault())
                    .toEpochSecond();
            metadata.setReleaseTime(secs);
        }

        claim.setValueType(Claim.TYPE_STREAM);
        claim.setValue(metadata);

        return claim;
    }

    private boolean validateLivestreamClaim(Claim claim) {
        if (Helper.isNullOrEmpty(claim.getTitle())) {
            showError(getString(R.string.please_provide_title));
            return false;
        }
        if (Helper.isNullOrEmpty(claim.getName())) {
            showError(getString(R.string.please_specify_address));
            return false;
        }
        if (!LbryUri.isNameValid(claim.getName())) {
            showError(getString(R.string.address_invalid_characters));
            return false;
        }
        if (claim.getSigningChannel() == null
                || claim.getSigningChannel().isPlaceholder()
                || claim.getSigningChannel().isPlaceholderAnonymous()) {
            showError(getString(R.string.livestream_select_channel));
            return false;
        }
        if (!editMode && Helper.claimNameExists(claim.getName())) {
            showError(getString(R.string.address_already_used));
            return false;
        }

        return true;
    }

    private void publishLivestream(Claim claim) {
        saveInProgress = true;
        ClaimResultHandler handler = new ClaimResultHandler() {
            @Override
            public void beforeStart() {
                preSave();
            }

            @Override
            public void onSuccess(Claim claimResult) {
                postSave();
                showMessage(modeLivestream ? R.string.livestream_pending
                        : (editMode ? R.string.update_successful : R.string.publish_successful));

                // Run the logPublish task
                if (!BuildConfig.DEBUG) {
                    claimResult.setSigningChannel(claim.getSigningChannel());
                    LogPublishTask logPublish = new LogPublishTask(claimResult);
                    logPublish.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                // publish done
                Bundle bundle = new Bundle();
                bundle.putString("claim_id", claimResult.getClaimId());
                bundle.putString("claim_name", claimResult.getName());
                LbryAnalytics.logEvent(editMode ? LbryAnalytics.EVENT_PUBLISH_UPDATE : LbryAnalytics.EVENT_PUBLISH, bundle);

                Context context = getContext();
                if (context instanceof MainActivity) {
                    MainActivity activity = (MainActivity) context;
                    activity.sendBroadcast(new Intent(modeLivestream
                            ? MainActivity.ACTION_LIVESTREAM_PUBLISH_SUCCESSFUL : MainActivity.ACTION_PUBLISH_SUCCESSFUL));
                }
            }

            @Override
            public void onError(Exception error) {
                showError(error.getMessage());
                postSave();
            }
        };
        if (modeLivestream) {
            PublishClaimTask task = new PublishClaimTask(claim, null, Lbryio.AUTH_TOKEN, handler);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            String url = selectedLivestreamReplay != null ? selectedLivestreamReplay.getUrl() : null;
            ReplayPublishTask task = new ReplayPublishTask(claim, url, null, Lbryio.AUTH_TOKEN, handler);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void preSave() {
        saveInProgress = true;

        Helper.setViewVisibility(progressCreating, View.VISIBLE);

        // disable input views
        Helper.setViewEnabled(inputTitle, false);
        Helper.setViewEnabled(inputAddress, false);
        Helper.setViewEnabled(inputDescription, false);
        Helper.setViewEnabled(inputTagFilter, false);
        Helper.setViewEnabled(inputOtherLicenseDescription, false);
        Helper.setViewEnabled(inputDeposit, false);

        Helper.setViewEnabled(channelSpinner, false);
        Helper.setViewEnabled(languageSpinner, false);
        Helper.setViewEnabled(licenseSpinner, false);

        Helper.setViewEnabled(linkShowExtraFields, false);
        Helper.setViewEnabled(linkCancel, false);
        Helper.setViewEnabled(buttonCreate,  false);
    }

    private void postSave() {
        Helper.setViewEnabled(inputTitle, true);
        Helper.setViewEnabled(inputAddress, true);
        Helper.setViewEnabled(inputDescription, true);
        Helper.setViewEnabled(inputTagFilter, true);
        Helper.setViewEnabled(inputOtherLicenseDescription, true);
        Helper.setViewEnabled(inputDeposit, true);

        Helper.setViewEnabled(channelSpinner, true);
        Helper.setViewEnabled(languageSpinner, true);
        Helper.setViewEnabled(licenseSpinner, true);

        Helper.setViewEnabled(linkShowExtraFields, true);
        Helper.setViewEnabled(linkCancel, true);
        Helper.setViewEnabled(buttonCreate,  true);

        Helper.setViewVisibility(progressCreating, View.GONE);

        saveInProgress = false;
    }
    // endregion

    private void checkStoragePermissionAndLaunchFilePicker() {
        Context context = getContext();
        if (MainActivity.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, context)) {
            launchPickerPending = false;
            launchFilePicker();
        } else {
            launchPickerPending = true;
            MainActivity.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    MainActivity.REQUEST_STORAGE_PERMISSION,
                    getString(R.string.storage_permission_rationale_images),
                    context,
                    true);
        }
    }

    private void launchFilePicker() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity.startingFilePickerActivity = true;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            ((MainActivity) context).startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.select_thumbnail)),
                    MainActivity.REQUEST_FILE_PICKER);
        }
    }

    private void uploadThumbnail(String thumbnailPath) {
        if (uploadingThumbnail) {
            View view = getView();
            if (view != null) {
                showMessage(R.string.wait_for_upload);
            }
            return;
        }

        Context context = getContext();
        if (context != null) {
            Glide.with(context.getApplicationContext()).load(thumbnailPath).centerCrop().into(imageThumbnail);
        }

        uploadingThumbnail = true;
        uploadedThumbnailUrl = null;
        UploadImageTask task = new UploadImageTask(thumbnailPath, progressThumbnailUploading, new UploadImageTask.UploadThumbnailHandler() {
            @Override
            public void onSuccess(String url) {
                lastSelectedThumbnailFile = thumbnailPath;
                uploadedThumbnailUrl = url;
                uploadingThumbnail = false;
            }

            @Override
            public void onError(Exception error) {
                View view = getView();
                if (context != null && view != null) {
                    showError(getString(R.string.image_upload_failed));
                }
                lastSelectedThumbnailFile = null;
                imageThumbnail.setImageDrawable(null);
                uploadingThumbnail = false;
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void cancelOnFatalCondition(String message) {
        Context context = getContext();
        if (context instanceof MainActivity) {
            showError(message);
            MainActivity activity = (MainActivity) context;
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    activity.onBackPressed();
                }
            }, 100);
        }
    }

    private void checkRewardsDriver() {
        Context ctx = getContext();
        if (ctx != null) {
            String rewardsDriverText = String.format("%s\n%s",
                    getString(R.string.livestreaming_requires_credits), getString(R.string.tap_here_to_get_some));
            checkRewardsDriverCard(rewardsDriverText, Helper.MIN_DEPOSIT);
        }
    }

    private void setDefaultScheduled() {
        releaseDateMillis = MaterialDatePicker.todayInUtcMilliseconds();

        Context context = getContext();
        if (context != null) {
            linkReleaseDate.setText(DateUtils.formatDateTime(
                    context, releaseDateMillis, DateUtils.FORMAT_SHOW_DATE));
        }

        setDefaultScheduledTime();
    }

    private void setDefaultScheduledTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        releaseTimeHours = calendar.get(Calendar.HOUR_OF_DAY);
        releaseTimeMinutes = calendar.get(Calendar.MINUTE);

        Context context = getContext();
        if (context != null) {
            linkReleaseTime.setText(DateUtils.formatDateTime(
                    context, calendar.getTimeInMillis(), DateUtils.FORMAT_SHOW_TIME));
        }
    }

    private boolean checkReleaseTimeInvalid() {
        boolean isReleaseTimeInvalid = releaseTimeHours > -1 && releaseTimeMinutes > -1
                && LocalDateTime
                        .ofInstant(Instant.ofEpochMilli(releaseDateMillis), UTC_ZONE)
                        .plusHours(releaseTimeHours)
                        .plusMinutes(releaseTimeMinutes)
                        .isBefore(LocalDateTime.now().minusMinutes(1));
        textReleaseTimePastNotAllowed.setVisibility(isReleaseTimeInvalid ? View.VISIBLE : View.GONE);
        if (isReleaseTimeInvalid) {
            releaseTimeHours = -1;
            releaseTimeMinutes = -1;
        }
        return isReleaseTimeInvalid;
    }

    // region: Callbacks
    @Override
    public void onFilePicked(String filePath) {
        if (Helper.isNullOrEmpty(filePath)) {
            View view = getView();
            if (view != null) {
                showError(getString(R.string.undetermined_image_filepath));
            }
            return;
        }

        Context context = getContext();
        if (context != null) {
            if (filePath.equalsIgnoreCase(lastSelectedThumbnailFile)) {
                // previous selected cover was uploaded successfully
                return;
            }

            Uri fileUri = Uri.fromFile(new File(filePath));
            Glide.with(context.getApplicationContext()).load(fileUri).centerCrop().into(imageThumbnail);
            uploadThumbnail(filePath);
        }
    }

    @Override
    public void onFilePickerCancelled() {
        // nothing to do here
        // At some point in the future, allow file picking for publish file?
    }

    @Override
    public void onStoragePermissionGranted() {
        if (launchPickerPending) {
            launchPickerPending = false;
            launchFilePicker();
        }
    }

    @Override
    public void onStoragePermissionRefused() {
        if (!storageRefusedOnce) {
            try {
                showError(getString(R.string.storage_permission_rationale_images));
            } catch (IllegalStateException ex) {
                // pass
            }
            storageRefusedOnce = true;
        }
        launchPickerPending = false;
    }

    @Override
    public void onTagClicked(Tag tag, int customizeMode) {
        if (customizeMode == TagListAdapter.CUSTOMIZE_MODE_ADD) {
            addTag(tag);
        } else if (customizeMode == TagListAdapter.CUSTOMIZE_MODE_REMOVE) {
            removeTag(tag);
        }
    }

    @Override
    public void onWalletBalanceUpdated(WalletBalance walletBalance) {
        if (walletBalance != null && textInlineDepositBalance != null) {
            textInlineDepositBalance.setText(Helper.shortCurrencyFormat(walletBalance.getAvailable().doubleValue()));
        }
        checkRewardsDriver();
    }

    @Override
    public void onChannelCreated(Claim claimResult) {
        // add the claim to the channel list and set it as the selected item
        if (channelSpinnerAdapter != null) {
            channelSpinnerAdapter.add(claimResult);
        } else {
            updateChannelList(Collections.singletonList(claimResult));
        }
        if (channelSpinner != null && channelSpinnerAdapter != null) {
            // Ensure adapter is set for the spinner
            if (channelSpinner.getAdapter() == null) {
                channelSpinner.setAdapter(channelSpinnerAdapter);
            }

            int adapterCount = channelSpinnerAdapter.getCount();

            if (adapterCount == 1) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.saveSharedUserState();
                }
            }

            channelSpinner.setSelection(adapterCount - 1);
        }
    }
    // endregion
}