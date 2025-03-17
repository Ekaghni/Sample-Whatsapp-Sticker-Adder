package com.example.samplestickertestingapp.activities;



import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.example.samplestickertestingapp.R;
import com.example.samplestickertestingapp.models.StickerPack;
import com.example.samplestickertestingapp.utils.StickerGenerator;
import com.example.samplestickertestingapp.utils.WhitelistCheck;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main activity for the Color Stickers app.
 * Provides UI to generate and view sticker packs.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private Button generateButton;
    private Button viewStickerPacksButton;
    private ProgressBar progressBar;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        generateButton = findViewById(R.id.btn_generate_stickers);
        viewStickerPacksButton = findViewById(R.id.btn_view_stickers);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);

        // Set button click listeners
        generateButton.setOnClickListener(v -> checkPermissionsAndGenerateStickers());
        viewStickerPacksButton.setOnClickListener(v -> openStickerPackList());

        // Check if WhatsApp is installed
        updateWhatsAppStatus();
    }

    /**
     * Update the status text based on WhatsApp installation.
     */
    private void updateWhatsAppStatus() {
        boolean isWhatsAppConsumerInstalled = WhitelistCheck.isWhatsAppConsumerAppInstalled(MainActivity.this);
        boolean isWhatsAppBusinessInstalled = WhitelistCheck.isWhatsAppSmbAppInstalled(MainActivity.this);

        if (!isWhatsAppConsumerInstalled && !isWhatsAppBusinessInstalled) {
            statusText.setText(R.string.whatsapp_not_installed);
            statusText.setVisibility(View.VISIBLE);
        } else {
            statusText.setVisibility(View.GONE);
        }
    }

    /**
     * Check for necessary permissions and generate stickers if granted.
     */
    private void checkPermissionsAndGenerateStickers() {
        // For Android 6.0+ we need to request storage permissions at runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                // Permission already granted, proceed
                generateStickers();
            }
        } else {
            // Permission is automatically granted on older Android versions
            generateStickers();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with generating stickers
                generateStickers();
            } else {
                // Permission denied
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Generate a new sticker pack with randomly colored shapes.
     */
    private void generateStickers() {
        // Show progress and disable buttons
        progressBar.setVisibility(View.VISIBLE);
        generateButton.setEnabled(false);
        viewStickerPacksButton.setEnabled(false);

        // Generate a unique ID for the sticker pack
        String packId = "colorstickers_" + UUID.randomUUID().toString().substring(0, 8);

        // Execute the sticker generation in a background task
        new GenerateStickersTask(this).execute(packId);
    }

    /**
     * Open the StickerPackListActivity to view all available sticker packs.
     */
    private void openStickerPackList() {
        Intent intent = new Intent(this, StickerPackListActivity.class);
        startActivity(intent);
    }

    /**
     * AsyncTask to generate stickers in the background to avoid freezing the UI.
     */
    private static class GenerateStickersTask extends AsyncTask<String, Void, StickerPack> {
        private final WeakReference<MainActivity> activityRef;

        GenerateStickersTask(MainActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        protected StickerPack doInBackground(String... params) {
            MainActivity activity = activityRef.get();
            if (activity == null) {
                return null;
            }

            try {
                String packId = params[0];
                StickerGenerator generator = new StickerGenerator(activity);

                // Generate a new sticker pack
                return generator.createStickerPack(
                        packId,
                        activity.getString(R.string.sticker_pack_name),
                        activity.getString(R.string.sticker_pack_publisher)
                );
            } catch (Exception e) {
                Log.e(TAG, "Error generating stickers", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(StickerPack stickerPack) {
            MainActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }

            // Hide progress bar
            activity.progressBar.setVisibility(View.GONE);

            // Enable buttons
            activity.generateButton.setEnabled(true);
            activity.viewStickerPacksButton.setEnabled(true);

            if (stickerPack != null) {
                Toast.makeText(activity,
                        activity.getString(R.string.stickers_generated_success),
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(activity,
                        activity.getString(R.string.stickers_generated_error),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}