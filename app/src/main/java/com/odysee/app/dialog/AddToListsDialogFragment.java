package com.odysee.app.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.CollectionListAdapter;
import com.odysee.app.data.DatabaseHelper;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.OdyseeCollection;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.Lbryio;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import lombok.Setter;

public class AddToListsDialogFragment extends BottomSheetDialogFragment {
    public static final String TAG = "AddToListsDialog";
    private RecyclerView collectionList;
    private View buttonAddList;
    private MaterialButton doneButton;
    private ProgressBar loadProgress;

    @Setter
    private String url;

    public static AddToListsDialogFragment newInstance() {
        return new AddToListsDialogFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_playlists, container,false);
        collectionList = view.findViewById(R.id.playlist_list);
        loadProgress = view.findViewById(R.id.playlist_load_progress);
        buttonAddList = view.findViewById(R.id.playlist_create_list);
        doneButton = view.findViewById(R.id.playlist_done);

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        collectionList.setLayoutManager(llm);

        buttonAddList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // show create playlist dialog with url
                Context context = getContext();
                if (context instanceof MainActivity) {
                    MainActivity activity = (MainActivity) context;
                    activity.handleAddUrlToCustomList(url);
                    dismiss();
                }

            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        return view;
    }

    public void onResume() {
        super.onResume();
        loadPlaylists();
    }

    private void loadPlaylists(){
        Context context = getContext();
        if (context instanceof MainActivity) {
            Helper.setViewVisibility(loadProgress, View.VISIBLE);

            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    SQLiteDatabase db = MainActivity.getDatabaseHelper().getReadableDatabase();
                    List<OdyseeCollection> privateCollections = DatabaseHelper.getSimpleCollections(db);
                    List<OdyseeCollection> publicCollections = new ArrayList<>();
                    try {
                         publicCollections = Lbry.loadOwnCollections(Lbryio.AUTH_TOKEN);
                    }  catch  (ApiCallException | JSONException ex) {
                        // pass
                    }

                    // Also need to load published / public lists at this point
                    List<OdyseeCollection> collections = new ArrayList<>(privateCollections);
                    collections.addAll(publicCollections);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            onPlaylistsLoaded(collections);
                        }
                    });
                }
            });
        }
    }

    private void onPlaylistsLoaded(List<OdyseeCollection> collections) {
        Helper.setViewVisibility(loadProgress, View.INVISIBLE);
        CollectionListAdapter adapter = new CollectionListAdapter(collections, getContext());
        adapter.setListener(new CollectionListAdapter.CollectionListItemCheckChangedListener() {
            @Override
            public void onCheckedChanged(OdyseeCollection collection, boolean checked) {
                if (!Helper.isNullOrEmpty(url)) {
                    Context context = getContext();
                    if (context instanceof MainActivity) {
                        MainActivity activity = (MainActivity) context;
                        if (checked) {
                            activity.handleAddUrlToList(url, collection, false);
                        } else {
                            activity.handleRemoveUrlFromList(url, collection);
                        }
                    }
                }
            }
        });
        if (collectionList != null) {
            collectionList.setAdapter(adapter);
        }
    }
}
