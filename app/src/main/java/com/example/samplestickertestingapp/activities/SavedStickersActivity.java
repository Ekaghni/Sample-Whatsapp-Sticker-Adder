package com.example.samplestickertestingapp.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.samplestickertestingapp.R;
import com.example.samplestickertestingapp.adapters.SavedStickerAdapter;
import com.example.samplestickertestingapp.models.CustomSticker;
import com.example.samplestickertestingapp.models.Sticker;
import com.example.samplestickertestingapp.models.StickerPack;
import com.example.samplestickertestingapp.utils.FileUtils;
import com.example.samplestickertestingapp.utils.StickerPackLoader;
import com.example.samplestickertestingapp.utils.StickerPackManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity to display and manage saved custom stickers.
 */
public class SavedStickersActivity extends AppCompatActivity implements SavedStickerAdapter.OnStickerClickListener {
    private static final String TAG = "SavedStickersActivityyyy";

    // Views
    private RecyclerView recyclerView;
    private TextView emptyView;
    private ProgressBar progressBar;
    private FloatingActionButton createPackFab;

    // Adapter
    private SavedStickerAdapter adapter;
    private List<CustomSticker> customStickers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_stickers);

        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.saved_stickers);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        recyclerView = findViewById(R.id.recycler_saved_stickers);
        emptyView = findViewById(R.id.text_empty_view);
        progressBar = findViewById(R.id.progress_bar);
        createPackFab = findViewById(R.id.fab_create_pack);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new SavedStickerAdapter(this, customStickers, this);
        recyclerView.setAdapter(adapter);

        // Set FAB click listener
        createPackFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreatePackDialog();
            }
        });

        // Load saved stickers
        loadSavedStickers();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload stickers when coming back to this activity
        loadSavedStickers();
    }

    /**
     * Load saved custom stickers.
     */
    private void loadSavedStickers() {
        showLoading(true);
        new LoadStickersTask(this).execute();
    }

    /**
     * Update UI based on whether stickers are available.
     *
     * @param stickers List of custom stickers
     */
    private void updateUI(List<CustomSticker> stickers) {
        showLoading(false);

        customStickers.clear();
        customStickers.addAll(stickers);
        adapter.notifyDataSetChanged();

        // Show empty view if no stickers
        if (customStickers.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            createPackFab.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            createPackFab.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Show loading indicator.
     *
     * @param loading Whether loading is in progress
     */
    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
        if (loading) {
            emptyView.setVisibility(View.GONE);
        }
    }

    /**
     * Show dialog to create a new sticker pack.
     */
    private void showCreatePackDialog() {
        if (customStickers.isEmpty()) {
            Toast.makeText(this, R.string.no_saved_stickers, Toast.LENGTH_SHORT).show();
            return;
        }

        // Create dialog with pack name input
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.create_new_pack);

        // Set up the input
        final EditText input = new EditText(this);
        input.setHint(R.string.enter_pack_name);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String packName = input.getText().toString().trim();
                if (packName.isEmpty()) {
                    packName = "My Custom Stickers";
                }
                createStickerPack(packName);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    /**
     * Create a new sticker pack with the saved stickers.
     *
     * @param packName Name for the sticker pack
     */
    private void createStickerPack(String packName) {
        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating sticker pack...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Create pack in background
        new CreateStickerPackTask(this, progressDialog).execute(packName);
    }

    @Override
    public void onStickerAddToWhatsAppClick(CustomSticker sticker) {
        // Show dialog to select the pack
        try {
            List<StickerPack> packs = StickerPackLoader.getStickerPacks(this);

            if (packs.isEmpty()) {
                Toast.makeText(this, "No sticker packs found. Create one first.", Toast.LENGTH_SHORT).show();
                return;
            }

            final String[] packNames = new String[packs.size()];
            final String[] packIds = new String[packs.size()];

            for (int i = 0; i < packs.size(); i++) {
                StickerPack pack = packs.get(i);
                packNames[i] = pack.name;
                packIds[i] = pack.identifier;
            }

            // Create dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.select_pack);
            builder.setItems(packNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Show progress dialog
                    final ProgressDialog progressDialog = new ProgressDialog(SavedStickersActivity.this);
                    progressDialog.setMessage("Adding sticker to pack...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    // Add sticker to selected pack
                    StickerPackManager.addStickerToPack(
                            SavedStickersActivity.this,
                            sticker,
                            packIds[which],
                            new StickerPackManager.AddStickerCallback() {
                                @Override
                                public void onStickerAdded(boolean success) {
                                    progressDialog.dismiss();

                                    if (success) {
                                        Toast.makeText(SavedStickersActivity.this,
                                                R.string.sticker_added_to_pack,
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(SavedStickersActivity.this,
                                                "Failed to add sticker to pack",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Toast.makeText(this, "Error loading sticker packs", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStickerDeleteClick(CustomSticker sticker) {
        // Confirm deletion
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_sticker)
                .setMessage("Are you sure you want to delete this sticker?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Delete in background
                        new DeleteStickerTask(SavedStickersActivity.this).execute(sticker);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * AsyncTask to load stickers in background.
     */
    private static class LoadStickersTask extends AsyncTask<Void, Void, List<CustomSticker>> {
        private final WeakReference<SavedStickersActivity> activityRef;

        LoadStickersTask(SavedStickersActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        protected List<CustomSticker> doInBackground(Void... voids) {
            SavedStickersActivity activity = activityRef.get();
            if (activity != null) {
                return FileUtils.loadCustomStickers(activity);
            }
            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<CustomSticker> stickers) {
            SavedStickersActivity activity = activityRef.get();
            if (activity != null) {
                activity.updateUI(stickers);
            }
        }
    }

    /**
     * AsyncTask to delete a sticker.
     */
    private static class DeleteStickerTask extends AsyncTask<CustomSticker, Void, Boolean> {
        private final WeakReference<SavedStickersActivity> activityRef;
        private CustomSticker deletedSticker;

        DeleteStickerTask(SavedStickersActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        protected Boolean doInBackground(CustomSticker... stickers) {
            SavedStickersActivity activity = activityRef.get();
            if (activity != null && stickers.length > 0) {
                deletedSticker = stickers[0];
                return FileUtils.deleteCustomSticker(activity, deletedSticker);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            SavedStickersActivity activity = activityRef.get();
            if (activity != null) {
                if (success) {
                    // Remove from list and update adapter
                    activity.customStickers.remove(deletedSticker);
                    activity.adapter.notifyDataSetChanged();

                    // Show empty view if no stickers left
                    if (activity.customStickers.isEmpty()) {
                        activity.emptyView.setVisibility(View.VISIBLE);
                        activity.createPackFab.setVisibility(View.GONE);
                    }

                    Toast.makeText(activity, R.string.sticker_deleted, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "Failed to delete sticker", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * AsyncTask to create a sticker pack.
     */
    private static class CreateStickerPackTask extends AsyncTask<String, Void, Boolean> {
        private final WeakReference<SavedStickersActivity> activityRef;
        private final WeakReference<ProgressDialog> dialogRef;
        private StickerPack createdPack;

        CreateStickerPackTask(SavedStickersActivity activity, ProgressDialog dialog) {
            this.activityRef = new WeakReference<>(activity);
            this.dialogRef = new WeakReference<>(dialog);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            SavedStickersActivity activity = activityRef.get();
            if (activity == null || params.length == 0) {
                return false;
            }

            try {
                String packName = params[0];

                // Create a new sticker pack
                createdPack = FileUtils.createCustomStickerPack(
                        activity,
                        packName,
                        "Created with " + activity.getString(R.string.app_name)
                );

                if (createdPack == null) {
                    return false;
                }

                // Add stickers to the pack (not fully implemented yet)
                // This would require copying files and updating the sticker pack

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            // Dismiss progress dialog
            ProgressDialog dialog = dialogRef.get();
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }

            SavedStickersActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }

            if (success && createdPack != null) {
                Toast.makeText(activity, "Sticker pack created successfully", Toast.LENGTH_SHORT).show();

                // Open the sticker pack details
                Intent intent = new Intent(activity, StickerPackDetailsActivity.class);
                intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_ID, createdPack.identifier);
                activity.startActivity(intent);
            } else {
                Toast.makeText(activity, "Failed to create sticker pack", Toast.LENGTH_SHORT).show();
            }
        }
    }
}