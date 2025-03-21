package com.example.samplestickertestingapp.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.samplestickertestingapp.BuildConfig;
import com.example.samplestickertestingapp.R;
import com.example.samplestickertestingapp.models.CustomSticker;
import com.example.samplestickertestingapp.models.Sticker;
import com.example.samplestickertestingapp.models.StickerPack;
import com.example.samplestickertestingapp.providers.StickerContentProvider;
import com.example.samplestickertestingapp.utils.FileUtils;
import com.example.samplestickertestingapp.utils.ImageUtils;
import com.example.samplestickertestingapp.utils.StickerPackLoader;
import com.example.samplestickertestingapp.utils.StickerPackManager;
import com.example.samplestickertestingapp.utils.WhitelistCheck;
import com.example.samplestickertestingapp.views.BrushImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Activity for removing background from an image using brush/eraser.
 */
public class BackgroundRemovalActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "BackgroundRemovallll";

    // Views
    private BrushImageView brushImageView;
    private ImageButton eraserButton;
    private ImageButton brushButton;
    private ImageButton undoButton;
    private ImageButton redoButton;
    private Button saveButton;
    private SeekBar opacitySeekBar;
    private SeekBar brushSizeSeekBar;
    private TextView brushSizeText;
    private TextView opacityText;

    // Image data
    private Uri imageUri;
    private CustomSticker savedSticker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_removal);

        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.remove_background);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        brushImageView = findViewById(R.id.brush_image_view);
        eraserButton = findViewById(R.id.btn_eraser);
        brushButton = findViewById(R.id.btn_brush);
        undoButton = findViewById(R.id.btn_undo);
        redoButton = findViewById(R.id.btn_redo);
        saveButton = findViewById(R.id.btn_save);
        opacitySeekBar = findViewById(R.id.seekbar_opacity);
        brushSizeSeekBar = findViewById(R.id.seekbar_brush_size);
        brushSizeText = findViewById(R.id.text_brush_size);
        opacityText = findViewById(R.id.text_opacity);

        // Set click listeners
        eraserButton.setOnClickListener(this);
        brushButton.setOnClickListener(this);
        undoButton.setOnClickListener(this);
        redoButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);

        // Set up seekbars
        setupSeekBars();

        // Load image
        if (getIntent() != null && getIntent().hasExtra("image_uri")) {
            String uriString = getIntent().getStringExtra("image_uri");
            imageUri = Uri.parse(uriString);
            loadImage();
        }

        // Default to eraser mode
        updateToolSelection(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Set up opacity and brush size seekbars.
     */
    private void setupSeekBars() {
        // Opacity seekbar (0-255)
        opacitySeekBar.setMax(255);
        opacitySeekBar.setProgress(255); // Default full opacity
        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int opacity = progress;
                brushImageView.setBrushOpacity(opacity);
                opacityText.setText(String.format("%d%%", (opacity * 100) / 255));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Brush size seekbar (5-100)
        brushSizeSeekBar.setMax(95);
        brushSizeSeekBar.setProgress(25); // Default size 30
        brushSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int size = progress + 5; // Minimum size 5
                brushImageView.setBrushSize(size);
                brushSizeText.setText(String.format("%d", size));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    /**
     * Load the image into the BrushImageView.
     */
    private void loadImage() {
        try {
            // Decode bitmap from uri
            Bitmap bitmap = ImageUtils.decodeBitmapFromUri(this, imageUri);
            if (bitmap != null) {
                // Set bitmap to the brush image view
                brushImageView.setSourceBitmap(bitmap);
            } else {
                Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Update UI to show which tool is currently selected.
     *
     * @param erasing true if eraser is selected, false if brush is selected
     */
    private void updateToolSelection(boolean erasing) {
        brushImageView.setErasing(erasing);

        eraserButton.setSelected(erasing);
        brushButton.setSelected(!erasing);

        // Visual feedback for selection
        eraserButton.setAlpha(erasing ? 1.0f : 0.5f);
        brushButton.setAlpha(erasing ? 0.5f : 1.0f);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_eraser) {
            updateToolSelection(true);
        } else if (id == R.id.btn_brush) {
            updateToolSelection(false);
        } else if (id == R.id.btn_undo) {
            if (!brushImageView.undo()) {
                // No more undo steps
                Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.btn_redo) {
            if (!brushImageView.redo()) {
                // No more redo steps
                Toast.makeText(this, "Nothing to redo", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.btn_save) {
            saveSticker();
        }
    }

    /**
     * Save the processed image as a sticker.
     */
    private void saveSticker() {
        // Get result bitmap
        Bitmap resultBitmap = brushImageView.getResultBitmap();
        if (resultBitmap == null) {
            Toast.makeText(this, R.string.sticker_save_error, Toast.LENGTH_SHORT).show();
            return;
        }

        // Show saving dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.creating_sticker));
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Save sticker in background
        new SaveStickerTask(this, progressDialog).execute(resultBitmap);
    }

    /**
     * Show dialog to add sticker to WhatsApp.
     *
     * @param customSticker Saved custom sticker
     */
    private void showAddToWhatsAppDialog(final CustomSticker customSticker) {
        this.savedSticker = customSticker;

        // Check if WhatsApp is installed
        boolean whatsAppInstalled = WhitelistCheck.isWhatsAppConsumerAppInstalled(this) ||
                WhitelistCheck.isWhatsAppSmbAppInstalled(this);

        if (!whatsAppInstalled) {
            Toast.makeText(this, R.string.whatsapp_not_installed, Toast.LENGTH_LONG).show();
            return;
        }

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_sticker_to_whatsapp);
        builder.setMessage("Would you like to add this sticker to WhatsApp?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showPackSelectionDialog(customSticker);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        builder.show();
    }

    private void showPackSelectionDialog(final CustomSticker customSticker) {
        try {
            // Get existing sticker packs
            final List<StickerPack> packs = StickerPackLoader.getStickerPacks(this);

            // Refresh whitelist status for all packs
            for (StickerPack pack : packs) {
                boolean isWhitelisted = WhitelistCheck.isWhitelisted(this, pack.identifier);
                pack.setIsWhitelisted(isWhitelisted);
                Log.d(TAG, "Pack: " + pack.name + ", ID: " + pack.identifier + ", Whitelisted: " + isWhitelisted);
            }

            final String[] packNames = new String[packs.size() + 1];
            final String[] packIds = new String[packs.size() + 1];
            final boolean[] packWhitelisted = new boolean[packs.size() + 1];

            // Add existing packs
            for (int i = 0; i < packs.size(); i++) {
                StickerPack pack = packs.get(i);
                packNames[i] = pack.name + (pack.getIsWhitelisted() ? " (Already in WhatsApp)" : "");
                packIds[i] = pack.identifier;
                packWhitelisted[i] = pack.getIsWhitelisted();
            }

            // Add option to create new pack
            packNames[packs.size()] = getString(R.string.create_new_pack);
            packIds[packs.size()] = "new";
            packWhitelisted[packs.size()] = false;

            // Create dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.select_pack);
            builder.setItems(packNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (packIds[which].equals("new")) {
                        // Create new pack
                        showCreateNewPackDialog(customSticker);
                    } else {
                        final String selectedPackId = packIds[which];
                        final boolean isAlreadyInWhatsApp = packWhitelisted[which];

                        if (isAlreadyInWhatsApp) {
                            // Pack is already in WhatsApp - just add sticker silently
                            addStickerToExistingPack(customSticker, selectedPackId);
                        } else {
                            // Pack is not in WhatsApp - show confirmation dialog
                            new AlertDialog.Builder(BackgroundRemovalActivity.this)
                                    .setTitle("Add to WhatsApp")
                                    .setMessage("This pack is not yet added to WhatsApp. Adding your sticker will also add the pack to WhatsApp.")
                                    .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            addStickerToNewPack(customSticker, selectedPackId);
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        }
                    }
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing pack selection dialog: " + e.getMessage(), e);
            Toast.makeText(this, R.string.sticker_save_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void addStickerToExistingPack(CustomSticker customSticker, String packId) {
        Log.d(TAG, "Starting to add sticker to existing pack: " + packId);

        // Show progress dialog
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Adding sticker to existing pack...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new AsyncTask<Void, Void, Boolean>() {
            private String errorMessage = "";

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    Log.d(TAG, "Background task started for adding sticker to: " + packId);

                    // 1. Get source sticker file
                    File sourceFile = new File(FileUtils.getCustomStickersDirectory(BackgroundRemovalActivity.this),
                            customSticker.getImageFileName());

                    if (!sourceFile.exists()) {
                        errorMessage = "Source sticker file not found";
                        Log.e(TAG, errorMessage + ": " + sourceFile.getAbsolutePath());
                        return false;
                    }

                    Log.d(TAG, "Source file exists: " + sourceFile.getAbsolutePath() + " Size: " + sourceFile.length() + " bytes");

                    // 2. Create a new sticker filename
                    String newStickerFileName = "sticker_" + System.currentTimeMillis() + ".webp";
                    Log.d(TAG, "New sticker filename: " + newStickerFileName);

                    // 3. Get target pack directory
                    File packDirectory = new File(getFilesDir(), packId);
                    if (!packDirectory.exists()) {
                        boolean created = packDirectory.mkdirs();
                        if (!created) {
                            errorMessage = "Failed to create pack directory";
                            Log.e(TAG, errorMessage + ": " + packDirectory.getAbsolutePath());
                            return false;
                        }
                    }

                    Log.d(TAG, "Pack directory exists: " + packDirectory.getAbsolutePath());

                    // 4. Copy sticker file to pack directory
                    File targetFile = new File(packDirectory, newStickerFileName);
                    try {
                        FileInputStream in = new FileInputStream(sourceFile);
                        FileOutputStream out = new FileOutputStream(targetFile);
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        in.close();
                        out.close();

                        if (!targetFile.exists() || targetFile.length() == 0) {
                            errorMessage = "Failed to copy sticker file";
                            Log.e(TAG, errorMessage);
                            return false;
                        }

                        Log.d(TAG, "Sticker file copied to: " + targetFile.getAbsolutePath() +
                                " Size: " + targetFile.length() + " bytes");
                    } catch (IOException e) {
                        errorMessage = "Error copying file: " + e.getMessage();
                        Log.e(TAG, errorMessage, e);
                        return false;
                    }

                    // 5. Find and load pack_info.json
                    File packInfoFile = new File(packDirectory, "pack_info.json");
                    if (!packInfoFile.exists()) {
                        errorMessage = "Pack info file not found";
                        Log.e(TAG, errorMessage + ": " + packInfoFile.getAbsolutePath());
                        return false;
                    }

                    Log.d(TAG, "Found pack info file: " + packInfoFile.getAbsolutePath());

                    // Load and parse the JSON
                    String jsonContent = "";
                    try {
                        StringBuilder jsonString = new StringBuilder();
                        BufferedReader reader = new BufferedReader(new FileReader(packInfoFile));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            jsonString.append(line);
                        }
                        reader.close();
                        jsonContent = jsonString.toString();
                        Log.d(TAG, "Read pack info JSON: " + (jsonContent.length() > 100 ?
                                jsonContent.substring(0, 100) + "..." : jsonContent));
                    } catch (IOException e) {
                        errorMessage = "Error reading pack info: " + e.getMessage();
                        Log.e(TAG, errorMessage, e);
                        return false;
                    }

                    JSONObject packJson;
                    try {
                        packJson = new JSONObject(jsonContent);
                    } catch (JSONException e) {
                        errorMessage = "Error parsing pack JSON: " + e.getMessage();
                        Log.e(TAG, errorMessage, e);
                        return false;
                    }

                    // 6. Add the new sticker to the stickers array
                    try {
                        // Create sticker entry
                        JSONObject stickerJson = new JSONObject();
                        stickerJson.put("image_file", newStickerFileName);

                        // Add emojis
                        JSONArray emojisArray = new JSONArray();
                        for (String emoji : customSticker.getEmojis()) {
                            emojisArray.put(emoji);
                        }
                        stickerJson.put("emojis", emojisArray);

                        // Add accessibility text
                        if (customSticker.getAccessibilityText() != null && !customSticker.getAccessibilityText().isEmpty()) {
                            stickerJson.put("accessibility_text", customSticker.getAccessibilityText());
                        }

                        // Get existing stickers or create a new array
                        JSONArray stickersArray;
                        if (packJson.has("stickers")) {
                            stickersArray = packJson.getJSONArray("stickers");
                        } else {
                            stickersArray = new JSONArray();
                        }

                        Log.d(TAG, "Original stickers array length: " + stickersArray.length());

                        // Create a new array with our sticker at the beginning
                        JSONArray newStickersArray = new JSONArray();

                        // Put the new sticker at the beginning
                        newStickersArray.put(stickerJson);

                        // Add all existing stickers
                        for (int i = 0; i < stickersArray.length(); i++) {
                            newStickersArray.put(stickersArray.getJSONObject(i));
                        }

                        // Replace the stickers array in the pack JSON
                        packJson.put("stickers", newStickersArray);

                        Log.d(TAG, "New stickers array length: " + newStickersArray.length());
                        Log.d(TAG, "Updated pack JSON: " +
                                (packJson.toString().length() > 100 ?
                                        packJson.toString().substring(0, 100) + "..." :
                                        packJson.toString()));

                        // 7. Update the version to ensure WhatsApp sees the change
                        if (packJson.has("image_data_version")) {
                            String version = packJson.getString("image_data_version");
                            try {
                                int versionNum = Integer.parseInt(version);
                                packJson.put("image_data_version", String.valueOf(versionNum + 1));
                                Log.d(TAG, "Updated image_data_version from " + version + " to " + (versionNum + 1));
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Couldn't parse version number, using incremented string");
                                packJson.put("image_data_version", version + ".1");
                            }
                        }

                        // 8. Write the updated JSON back to the file
                        try {
                            FileWriter writer = new FileWriter(packInfoFile);
                            writer.write(packJson.toString(2)); // Pretty print with 2-space indentation
                            writer.close();
                            Log.d(TAG, "Successfully wrote updated pack info to file");
                        } catch (IOException e) {
                            errorMessage = "Error writing pack info: " + e.getMessage();
                            Log.e(TAG, errorMessage, e);
                            return false;
                        }

                    } catch (JSONException e) {
                        errorMessage = "Error updating JSON: " + e.getMessage();
                        Log.e(TAG, errorMessage, e);
                        return false;
                    }

                    // 9. Notify the ContentProvider about the changes
                    try {
                        String authority = BuildConfig.CONTENT_PROVIDER_AUTHORITY;
                        ContentResolver resolver = getContentResolver();

                        Uri metadataUri = Uri.parse("content://" + authority + "/" + StickerContentProvider.METADATA);
                        Uri packUri = Uri.parse("content://" + authority + "/" +
                                StickerContentProvider.METADATA + "/" + packId);
                        Uri stickersUri = Uri.parse("content://" + authority + "/" +
                                StickerContentProvider.STICKERS + "/" + packId);

                        Log.d(TAG, "Notifying ContentResolver of changes on URIs:");
                        Log.d(TAG, "  - " + metadataUri);
                        Log.d(TAG, "  - " + packUri);
                        Log.d(TAG, "  - " + stickersUri);

                        resolver.notifyChange(metadataUri, null);
                        resolver.notifyChange(packUri, null);
                        resolver.notifyChange(stickersUri, null);

                        // Also notify any asset URIs related to this pack
                        Uri assetsUri = Uri.parse("content://" + authority + "/" +
                                StickerContentProvider.STICKERS_ASSET + "/" + packId);
                        Log.d(TAG, "  - " + assetsUri);
                        resolver.notifyChange(assetsUri, null);

                        Log.d(TAG, "Successfully notified ContentProvider of changes");
                    } catch (Exception e) {
                        Log.e(TAG, "Error notifying ContentProvider: " + e.getMessage(), e);
                        // Continue anyway, this is not critical
                    }

                    Log.d(TAG, "Sticker successfully added to pack: " + packId);
                    return true;

                } catch (Exception e) {
                    errorMessage = "Unexpected error: " + e.getMessage();
                    Log.e(TAG, errorMessage, e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                Log.d(TAG, "Task completed with result: " + success);

                // Safely dismiss progress dialog
                try {
                    if (progressDialog != null && progressDialog.isShowing() && !isFinishing()) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error dismissing progress dialog: " + e.getMessage());
                }

                if (isFinishing()) {
                    Log.d(TAG, "Activity is finishing, skipping result handling");
                    return;
                }

                if (success) {
                    Toast.makeText(BackgroundRemovalActivity.this,
                            "Sticker added to pack successfully!",
                            Toast.LENGTH_LONG).show();

                    // Show a dialog with options to open WhatsApp or just finish
                    new AlertDialog.Builder(BackgroundRemovalActivity.this)
                            .setTitle("Sticker Added")
                            .setMessage("Your sticker has been added to the pack. You may need to restart WhatsApp to see it.")
                            .setPositiveButton("Open WhatsApp", (dialog, which) -> {
                                try {
                                    Intent intent = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
                                    if (intent == null) {
                                        intent = getPackageManager().getLaunchIntentForPackage("com.whatsapp.w4b");
                                    }

                                    if (intent != null) {
                                        Log.d(TAG, "Launching WhatsApp");
                                        startActivity(intent);
                                    } else {
                                        Log.w(TAG, "No WhatsApp app found to launch");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error launching WhatsApp: " + e.getMessage(), e);
                                }
                                finish();
                            })
                            .setNegativeButton("Done", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                } else {
                    String message = errorMessage.isEmpty() ?
                            "Failed to add sticker to pack" :
                            "Error: " + errorMessage;

                    Toast.makeText(BackgroundRemovalActivity.this, message, Toast.LENGTH_LONG).show();

                    new AlertDialog.Builder(BackgroundRemovalActivity.this)
                            .setTitle("Error Adding Sticker")
                            .setMessage(message)
                            .setPositiveButton("OK", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                }
            }
        }.execute();
    }

    /**
     * Remove a sticker pack from WhatsApp and then re-add it.
     *
     * @param packId The ID of the pack to remove and re-add
     * @param progressDialog The progress dialog to update and dismiss when done
     */
    private void removeAndReAddPack(String packId, ProgressDialog progressDialog) {
        progressDialog.setMessage("Updating WhatsApp sticker pack...");

        // Try to get the pack info
        StickerPack packToUpdate = null;
        try {
            List<StickerPack> packs = StickerPackLoader.getStickerPacks(this);
            for (StickerPack pack : packs) {
                if (pack.identifier.equals(packId)) {
                    packToUpdate = pack;
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding pack to update: " + e.getMessage(), e);
        }

        if (packToUpdate == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error: Could not find sticker pack to update", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Now add the pack to WhatsApp (this will replace the existing one)
        try {
            // Create intent to add sticker pack
            Intent intent = new Intent();
            intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
            intent.putExtra("sticker_pack_id", packToUpdate.identifier);
            intent.putExtra("sticker_pack_authority", BuildConfig.CONTENT_PROVIDER_AUTHORITY);
            intent.putExtra("sticker_pack_name", packToUpdate.name);
            intent.putExtra("sticker_pack_publisher", packToUpdate.publisher);

            // Make sure WhatsApp gets this intent
            intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

            // Keep a reference to the progress dialog for the callback
            final ProgressDialog finalDialog = progressDialog;

            // Start the WhatsApp activity
            // Start the WhatsApp activity for result so we can dismiss the dialog
            startActivityForResult(intent, 201); // Use a different request code

            // Set up a handler to dismiss the dialog after a timeout if activity result doesn't come back
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (finalDialog.isShowing()) {
                        finalDialog.dismiss();
                        Toast.makeText(BackgroundRemovalActivity.this,
                                "Sticker added to pack and WhatsApp updated!",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            }, 5000); // 5 second timeout
        } catch (Exception e) {
            Log.e(TAG, "Error updating WhatsApp pack: " + e.getMessage(), e);
            progressDialog.dismiss();
            Toast.makeText(this, "Sticker added to pack, but couldn't update WhatsApp. " +
                    "Try reopening WhatsApp to see your new sticker.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void addStickerToNewPack(CustomSticker customSticker, String packId) {
        // Show progress dialog
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Adding sticker to pack...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Add sticker to pack AND notify WhatsApp
        StickerPackManager.addStickerToPack(this, customSticker, packId, true,
                new StickerPackManager.AddStickerCallback() {
                    @Override
                    public void onStickerAdded(boolean success) {
                        progressDialog.dismiss();

                        if (success) {
                            // For new packs, show success and add to WhatsApp
                            Toast.makeText(BackgroundRemovalActivity.this,
                                    R.string.sticker_added_to_pack,
                                    Toast.LENGTH_LONG).show();
                            addStickerToWhatsApp(packId);
                        } else {
                            Toast.makeText(BackgroundRemovalActivity.this,
                                    "Failed to add sticker to pack",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                });
    }

    /**
     * Show dialog to create new sticker pack.
     *
     * @param customSticker Saved custom sticker
     */
    private void showCreateNewPackDialog(final CustomSticker customSticker) {
        // Create dialog with pack name input
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.create_new_pack);

        // Set up the input
        final EditText input = new EditText(this);
        input.setHint(R.string.enter_pack_name);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String packName = input.getText().toString().trim();
                if (packName.isEmpty()) {
                    packName = "My Custom Stickers";
                }
                createPackAndAddSticker(customSticker, packName);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Add sticker to existing pack.
     *
     * @param customSticker Saved custom sticker
     * @param packId Pack identifier
     */
    private void addStickerToPack(CustomSticker customSticker, String packId) {
        // Show progress dialog
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Adding sticker to pack...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Add sticker to pack
        StickerPackManager.addStickerToPack(this, customSticker, packId,
                new StickerPackManager.AddStickerCallback() {
                    @Override
                    public void onStickerAdded(boolean success) {
                        progressDialog.dismiss();

                        if (success) {
                            // Check if we're adding to a new pack or existing pack
                            boolean isNewPack = false;
                            try {
                                List<StickerPack> packs = StickerPackLoader.getStickerPacks(BackgroundRemovalActivity.this);
                                for (StickerPack pack : packs) {
                                    if (pack.identifier.equals(packId)) {
                                        // If this pack is whitelisted, it's an existing pack
                                        isNewPack = !pack.getIsWhitelisted();
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error checking pack status: " + e.getMessage());
                            }

                            if (isNewPack) {
                                // For new packs, we need to add them to WhatsApp
                                Toast.makeText(BackgroundRemovalActivity.this,
                                        R.string.sticker_added_to_pack,
                                        Toast.LENGTH_LONG).show();
                                addStickerToWhatsApp(packId);
                            } else {
                                // For existing packs, just show success message and finish
                                Toast.makeText(BackgroundRemovalActivity.this,
                                        "Sticker added to existing pack successfully!",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            }
                        } else {
                            Toast.makeText(BackgroundRemovalActivity.this,
                                    "Failed to add sticker to pack",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                });
    }

    /**
     * Add the sticker pack to WhatsApp
     * @param packId The pack identifier to add
     */
    /**
     * Add the sticker pack to WhatsApp
     * @param packId The pack identifier to add
     */
    private void addStickerToWhatsApp(String packId) {
        try {
            // Show progress dialog
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Adding sticker to WhatsApp...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Get the actual pack info to make sure we have all details
            StickerPack packToAdd = null;
            try {
                // First try regular loader
                List<StickerPack> packs = StickerPackLoader.getStickerPacks(this);
                for (StickerPack pack : packs) {
                    if (pack.identifier.equals(packId)) {
                        packToAdd = pack;
                        break;
                    }
                }

                // If not found, try loading directly from file
                if (packToAdd == null) {
                    Log.d(TAG, "Pack not found in loader, trying direct file loading");
                    File packDirectory = new File(getFilesDir(), packId);
                    File packInfoFile = new File(packDirectory, "pack_info.json");

                    if (packInfoFile.exists()) {
                        // Load JSON from file
                        StringBuilder jsonString = new StringBuilder();
                        try (BufferedReader reader = new BufferedReader(new FileReader(packInfoFile))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                jsonString.append(line);
                            }
                        }

                        JSONObject packJson = new JSONObject(jsonString.toString());

                        // Create StickerPack from JSON
                        packToAdd = new StickerPack(
                                packJson.getString("identifier"),
                                packJson.getString("name"),
                                packJson.getString("publisher"),
                                packJson.getString("tray_image_file"),
                                packJson.optString("publisher_email", ""),
                                packJson.optString("publisher_website", ""),
                                packJson.optString("privacy_policy_website", ""),
                                packJson.optString("license_agreement_website", ""),
                                packJson.optString("image_data_version", "1"),
                                packJson.optBoolean("avoid_cache", false),
                                packJson.optBoolean("animated_sticker_pack", false)
                        );

                        // Load stickers
                        List<Sticker> stickers = new ArrayList<>();
                        if (packJson.has("stickers")) {
                            JSONArray stickersJson = packJson.getJSONArray("stickers");

                            for (int i = 0; i < stickersJson.length(); i++) {
                                JSONObject stickerJson = stickersJson.getJSONObject(i);

                                String imageFile = stickerJson.getString("image_file");

                                List<String> emojis = new ArrayList<>();
                                if (stickerJson.has("emojis")) {
                                    JSONArray emojisJson = stickerJson.getJSONArray("emojis");
                                    for (int j = 0; j < emojisJson.length(); j++) {
                                        emojis.add(emojisJson.getString(j));
                                    }
                                }

                                String accessibilityText = stickerJson.optString("accessibility_text", "");

                                Sticker sticker = new Sticker(imageFile, emojis, accessibilityText);
                                File stickerFile = new File(packDirectory, imageFile);
                                if (stickerFile.exists()) {
                                    sticker.setSize(stickerFile.length());
                                    stickers.add(sticker);
                                }
                            }
                        }

                        packToAdd.setStickers(stickers);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error finding pack", e);
            }

            if (packToAdd == null) {
                progressDialog.dismiss();
                Toast.makeText(this, "Sticker pack not found", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            Log.d(TAG, "Adding pack to WhatsApp: " + packToAdd.identifier + " with " +
                    packToAdd.getStickers().size() + " stickers");

            // Add sticker to WhatsApp with all required fields
            Intent intent = new Intent();
            intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
            intent.putExtra("sticker_pack_id", packToAdd.identifier);
            intent.putExtra("sticker_pack_authority", BuildConfig.CONTENT_PROVIDER_AUTHORITY);
            intent.putExtra("sticker_pack_name", packToAdd.name);

            // Additional fields that might help
            intent.putExtra("sticker_pack_publisher", packToAdd.publisher);

            // This is important - we need to broadcast the changes to WhatsApp
            intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

            try {
                // First try to start the activity to add the pack
                startActivityForResult(intent, 200);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Activity not found, trying broadcast: " + e.getMessage());
                // If that fails, try sending a broadcast (older WhatsApp versions)
                intent.setAction("com.whatsapp.intent.action.STICKER_PACK_RESULT");
                sendBroadcast(intent);

                Toast.makeText(this, "Sticker pack added to WhatsApp", Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
                finish();
            }

            progressDialog.dismiss();
        } catch (Exception e) {
            Log.e(TAG, "Error adding sticker pack to WhatsApp", e);
            Toast.makeText(this, R.string.add_pack_fail, Toast.LENGTH_LONG).show();
            finish();
        }
    }
    /**
     * Create a new pack and add the sticker to it.
     *
     * @param customSticker Saved custom sticker
     * @param packName Pack name
     */
    private void createPackAndAddSticker(CustomSticker customSticker, String packName) {
        // Show progress dialog
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating sticker pack...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            // Create new sticker pack
            StickerPack stickerPack = FileUtils.createCustomStickerPack(
                    this,
                    packName,
                    "Created with " + getString(R.string.app_name)
            );

            if (stickerPack != null) {
                // Pack created successfully - now add sticker to it
                progressDialog.dismiss();

                // Explicitly notify ContentProvider about the new pack
                ContentResolver resolver = getContentResolver();
                Uri metadataUri = Uri.parse("content://" + BuildConfig.CONTENT_PROVIDER_AUTHORITY + "/" +
                        StickerContentProvider.METADATA);
                resolver.notifyChange(metadataUri, null);

                // Force a delay to let ContentProvider register the new pack
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Now add the sticker to the new pack
                        addStickerToNewPack(customSticker, stickerPack.identifier);
                    }
                }, 500); // Small delay for ContentProvider to update
            } else {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to create sticker pack", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating pack: " + e.getMessage(), e);
            progressDialog.dismiss();
            Toast.makeText(this, "Failed to create sticker pack: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200) {
            // Original add to WhatsApp request
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
            }
            finish();
        } else if (requestCode == 201) {
            // Remove and re-add request
            if (resultCode == RESULT_CANCELED) {
                if (data != null) {
                    final String validationError = data.getStringExtra("validation_error");
                    if (validationError != null) {
                        Log.e(TAG, "Validation error in re-add: " + validationError);
                        Toast.makeText(this, validationError, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "WhatsApp pack update canceled.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "WhatsApp pack update canceled.", Toast.LENGTH_LONG).show();
                }
            } else if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Sticker added and WhatsApp pack updated!", Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }

    /**
     * AsyncTask to save sticker in background.
     */
    private static class SaveStickerTask extends AsyncTask<Bitmap, Void, CustomSticker> {
        private final WeakReference<BackgroundRemovalActivity> activityRef;
        private final WeakReference<ProgressDialog> dialogRef;

        SaveStickerTask(BackgroundRemovalActivity activity, ProgressDialog dialog) {
            this.activityRef = new WeakReference<>(activity);
            this.dialogRef = new WeakReference<>(dialog);
        }

        @Override
        protected CustomSticker doInBackground(Bitmap... bitmaps) {
            BackgroundRemovalActivity activity = activityRef.get();
            if (activity == null || bitmaps.length == 0 || bitmaps[0] == null) {
                return null;
            }

            try {
                // Generate file name
                String fileName = FileUtils.generateStickerFileName("image");

                // Save bitmap as WebP
                File outputFile = ImageUtils.saveAsStickerFile(activity, bitmaps[0], fileName);
                if (outputFile == null) {
                    return null;
                }

                // Create custom sticker object
                List<String> emojis = new ArrayList<>(Arrays.asList("😀", "🎨", "🖼️"));
                CustomSticker sticker = new CustomSticker(
                        fileName,
                        emojis,
                        "Custom sticker from image",
                        CustomSticker.SOURCE_TYPE_IMAGE
                );
                sticker.setSize(outputFile.length());

                // Add to custom stickers list
                List<CustomSticker> stickers = FileUtils.loadCustomStickers(activity);
                stickers.add(sticker);
                FileUtils.saveCustomStickersInfo(activity, stickers);

                return sticker;
            } catch (Exception e) {
                Log.e(TAG, "Error saving sticker", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(CustomSticker sticker) {
            // Dismiss progress dialog
            ProgressDialog dialog = dialogRef.get();
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }

            BackgroundRemovalActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }

            if (sticker != null) {
                Toast.makeText(activity, activity.getString(R.string.sticker_saved), Toast.LENGTH_SHORT).show();
                activity.showAddToWhatsAppDialog(sticker);
            } else {
                Toast.makeText(activity, activity.getString(R.string.sticker_save_error), Toast.LENGTH_SHORT).show();
                activity.finish();
            }
        }
    }
}