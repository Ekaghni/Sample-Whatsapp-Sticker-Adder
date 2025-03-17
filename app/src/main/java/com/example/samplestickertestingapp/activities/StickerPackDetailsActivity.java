package com.example.samplestickertestingapp.activities;


import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.samplestickertestingapp.R;
import com.example.samplestickertestingapp.adapters.StickerPreviewAdapter;
import com.example.samplestickertestingapp.models.StickerPack;
import com.example.samplestickertestingapp.utils.StickerPackLoader;
import com.example.samplestickertestingapp.utils.WhitelistCheck;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Activity to display the details of a sticker pack and its stickers.
 * Simplified to use standard ImageView instead of Fresco views.
 */
public class StickerPackDetailsActivity extends AppCompatActivity {
    private static final String TAG = "StickerPackDetails";

    // Intent extra keys
    public static final String EXTRA_STICKER_PACK_ID = "sticker_pack_id";

    private String stickerPackId;
    private StickerPack stickerPack;

    // Views
    private ImageView trayImage;
    private TextView packNameText;
    private TextView publisherText;
    private TextView packSizeText;
    private TextView stickersCountText;
    private Button addToWhatsAppButton;
    private TextView alreadyAddedText;
    private RecyclerView stickersRecyclerView;
    private View progressBar;

    private StickerPreviewAdapter stickerPreviewAdapter;
    private GridLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_pack_details);

        // Get the sticker pack ID from intent
        stickerPackId = getIntent().getStringExtra(EXTRA_STICKER_PACK_ID);
        if (stickerPackId == null) {
            Toast.makeText(this, R.string.error_invalid_sticker_pack, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Opening sticker pack details for: " + stickerPackId);

        // Setup action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_activity_sticker_pack_details);
        }

        // Initialize views
        trayImage = findViewById(R.id.tray_image);
        packNameText = findViewById(R.id.pack_name);
        publisherText = findViewById(R.id.publisher);
        packSizeText = findViewById(R.id.pack_size);
        stickersCountText = findViewById(R.id.stickers_count);
        addToWhatsAppButton = findViewById(R.id.add_to_whatsapp_button);
        alreadyAddedText = findViewById(R.id.already_added_text);
        stickersRecyclerView = findViewById(R.id.stickers_recycler);
        progressBar = findViewById(R.id.progress_bar);

        // Setup RecyclerView
        int numColumns = getResources().getInteger(R.integer.sticker_pack_details_columns);
        layoutManager = new GridLayoutManager(this, numColumns);
        stickersRecyclerView.setLayoutManager(layoutManager);

        // Set up button click listener
        addToWhatsAppButton.setOnClickListener(v -> addStickerPackToWhatsApp());

        // Load the sticker pack
        loadStickerPack();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if pack is already added to WhatsApp
        updateAddButtonStatus();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Load the sticker pack data.
     */
    private void loadStickerPack() {
        showLoading(true);
        new LoadStickerPackTask(this).execute(stickerPackId);
    }

    /**
     * Display the loaded sticker pack.
     */
    private void displayStickerPack() {
        if (stickerPack == null) {
            Toast.makeText(this, R.string.error_invalid_sticker_pack, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Displaying sticker pack: " + stickerPack.name + " with " +
                stickerPack.getStickers().size() + " stickers");

        // Set tray image (simplified using standard drawables)
        try {
            File trayFile = new File(new File(getFilesDir(), stickerPack.identifier), stickerPack.trayImageFile);
            if (trayFile.exists()) {
                trayImage.setImageBitmap(BitmapFactory.decodeFile(trayFile.getAbsolutePath()));
            } else {
                // Try loading from assets
                try {
                    InputStream is = getAssets().open(stickerPack.identifier + "/" + stickerPack.trayImageFile);
                    trayImage.setImageBitmap(BitmapFactory.decodeStream(is));
                    is.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error loading tray from assets: " + e.getMessage());
                    trayImage.setImageResource(R.drawable.ic_add);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading tray image: " + e.getMessage());
            trayImage.setImageResource(R.drawable.ic_add);
        }

        // Set text views
        packNameText.setText(stickerPack.name);
        publisherText.setText(stickerPack.publisher);

        // Format the pack size
        long totalSize = stickerPack.getTotalSize();
        packSizeText.setText(Formatter.formatShortFileSize(this, totalSize));

        // Show sticker count
        int stickerCount = stickerPack.getStickers().size();
        stickersCountText.setText(getResources().getQuantityString(
                R.plurals.sticker_count, stickerCount, stickerCount));

        // Create adapter and set to RecyclerView
        stickerPreviewAdapter = new StickerPreviewAdapter(this, stickerPack);
        stickersRecyclerView.setAdapter(stickerPreviewAdapter);

        // Check if pack is added to WhatsApp
        updateAddButtonStatus();

        showLoading(false);
    }

    /**
     * Check if the sticker pack is already added to WhatsApp and update UI accordingly.
     */
    private void updateAddButtonStatus() {
        if (stickerPack != null) {
            new WhitelistCheckTask(this).execute(stickerPack);
        }
    }

    /**
     * Update the UI based on whether the pack is already added to WhatsApp.
     */
    private void updateAddUI(boolean isWhitelisted) {
        if (isWhitelisted) {
            // Pack is already added to WhatsApp
            addToWhatsAppButton.setVisibility(View.GONE);
            alreadyAddedText.setVisibility(View.VISIBLE);
        } else {
            // Pack is not added to WhatsApp
            addToWhatsAppButton.setVisibility(View.VISIBLE);
            alreadyAddedText.setVisibility(View.GONE);
        }
    }

    /**
     * Add the sticker pack to WhatsApp.
     */
    private void addStickerPackToWhatsApp() {
        try {
            // Check if WhatsApp is installed
            if (!WhitelistCheck.isWhatsAppConsumerAppInstalled(StickerPackDetailsActivity.this) &&
                    !WhitelistCheck.isWhatsAppSmbAppInstalled(StickerPackDetailsActivity.this)) {
                Toast.makeText(this, R.string.whatsapp_not_installed, Toast.LENGTH_LONG).show();
                return;
            }

            // Create intent to add sticker pack
            Intent intent = new Intent();
            intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
            intent.putExtra("sticker_pack_id", stickerPack.identifier);
            intent.putExtra("sticker_pack_authority", getPackageName() + ".stickercontentprovider");
            intent.putExtra("sticker_pack_name", stickerPack.name);

            Log.d(TAG, "Sending intent to add sticker pack: " + stickerPack.identifier);

            // Start WhatsApp activity
            startActivityForResult(intent, 200);
        } catch (Exception e) {
            Log.e(TAG, "Error adding sticker pack to WhatsApp", e);
            Toast.makeText(this, R.string.add_pack_fail, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200) {
            if (resultCode == RESULT_CANCELED) {
                if (data != null) {
                    final String validationError = data.getStringExtra("validation_error");
                    if (validationError != null) {
                        Log.e(TAG, "Validation error: " + validationError);
                        Toast.makeText(this, validationError, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, R.string.sticker_pack_not_added, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, R.string.sticker_pack_not_added, Toast.LENGTH_LONG).show();
                }
            } else if (resultCode == RESULT_OK) {
                Toast.makeText(this, R.string.sticker_pack_added, Toast.LENGTH_LONG).show();
                // Refresh the whitelist status
                updateAddButtonStatus();
            }
        }
    }

    /**
     * Show or hide the loading indicator.
     */
    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        stickersRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    /**
     * AsyncTask to load a sticker pack in the background.
     */
    private static class LoadStickerPackTask extends AsyncTask<String, Void, StickerPack> {
        private final WeakReference<StickerPackDetailsActivity> activityRef;

        LoadStickerPackTask(StickerPackDetailsActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        protected StickerPack doInBackground(String... ids) {
            try {
                StickerPackDetailsActivity activity = activityRef.get();
                if (activity == null) {
                    return null;
                }

                String packId = ids[0];
                Log.d(TAG, "Loading sticker pack: " + packId);

                List<StickerPack> packs = StickerPackLoader.getStickerPacks(activity);

                // Find the matching pack
                for (StickerPack pack : packs) {
                    if (pack.identifier.equals(packId)) {
                        return pack;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading sticker pack", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(StickerPack stickerPack) {
            StickerPackDetailsActivity activity = activityRef.get();
            if (activity != null) {
                activity.stickerPack = stickerPack;
                activity.displayStickerPack();
            }
        }
    }

    /**
     * AsyncTask to check if a sticker pack is added to WhatsApp.
     */
    private static class WhitelistCheckTask extends AsyncTask<StickerPack, Void, Boolean> {
        private final WeakReference<StickerPackDetailsActivity> activityRef;

        WhitelistCheckTask(StickerPackDetailsActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        protected Boolean doInBackground(StickerPack... packs) {
            try {
                StickerPackDetailsActivity activity = activityRef.get();
                if (activity != null && packs.length > 0) {
                    return WhitelistCheck.isWhitelisted(activity, packs[0].identifier);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking whitelist status", e);
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean isWhitelisted) {
            StickerPackDetailsActivity activity = activityRef.get();
            if (activity != null) {
                activity.updateAddUI(isWhitelisted);
            }
        }
    }
}