package com.example.samplestickertestingapp.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.example.samplestickertestingapp.BuildConfig;
import com.example.samplestickertestingapp.models.CustomSticker;
import com.example.samplestickertestingapp.models.Sticker;
import com.example.samplestickertestingapp.models.StickerPack;
import com.example.samplestickertestingapp.providers.StickerContentProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for adding custom stickers to existing sticker packs.
 */
public class StickerPackManager {
    private static final String TAG = "StickerPackManager";

    /**
     * Interface for callback when adding sticker to pack completes
     */
    public interface AddStickerCallback {
        void onStickerAdded(boolean success);
    }

    /**
     * Add a custom sticker to an existing sticker pack.
     *
     * @param context Application context
     * @param customSticker The custom sticker to add
     * @param packId The ID of the sticker pack to add to
     * @param notifyWhatsApp Whether to notify WhatsApp about the update
     * @param callback Callback for operation completion
     */
    public static void addStickerToPack(Context context, CustomSticker customSticker, String packId,
                                        boolean notifyWhatsApp, AddStickerCallback callback) {
        new AddStickerToPackTask(context, callback, notifyWhatsApp).execute(customSticker, packId);
    }

    /**
     * Add a custom sticker to an existing sticker pack.
     *
     * @param context Application context
     * @param customSticker The custom sticker to add
     * @param packId The ID of the sticker pack to add to
     * @param callback Callback for operation completion
     */
    public static void addStickerToPack(Context context, CustomSticker customSticker, String packId,
                                        AddStickerCallback callback) {
        // Default to notifying WhatsApp for backward compatibility
        addStickerToPack(context, customSticker, packId, true, callback);
    }

    /**
     * AsyncTask to add sticker to pack in background.
     */
    private static class AddStickerToPackTask extends AsyncTask<Object, Void, Boolean> {
        private final WeakReference<Context> contextRef;
        private final WeakReference<AddStickerCallback> callbackRef;
        private final boolean notifyWhatsApp;
        private String packId;

        AddStickerToPackTask(Context context, AddStickerCallback callback, boolean notifyWhatsApp) {
            this.contextRef = new WeakReference<>(context);
            this.callbackRef = new WeakReference<>(callback);
            this.notifyWhatsApp = notifyWhatsApp;
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            Context context = contextRef.get();
            if (context == null || params.length < 2) {
                return false;
            }

            try {
                CustomSticker customSticker = (CustomSticker) params[0];
                packId = (String) params[1];

                // 1. Get the sticker pack
                StickerPack targetPack = null;

                try {
                    List<StickerPack> packs = StickerPackLoader.getStickerPacks(context);

                    for (StickerPack pack : packs) {
                        if (pack.identifier.equals(packId)) {
                            targetPack = pack;
                            break;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading sticker packs: " + e.getMessage());
                }

                if (targetPack == null) {
                    Log.d(TAG, "Pack not found in loader, trying to load directly: " + packId);

                    // Try to load the pack directly from its directory
                    File packDirectory = new File(context.getFilesDir(), packId);
                    File packInfoFile = new File(packDirectory, "pack_info.json");

                    if (packInfoFile.exists()) {
                        try {
                            // Read the pack_info.json file
                            StringBuilder jsonString = new StringBuilder();
                            try (FileReader reader = new FileReader(packInfoFile)) {
                                int read;
                                char[] buffer = new char[1024];
                                while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                                    jsonString.append(buffer, 0, read);
                                }
                            }

                            JSONObject packJson = new JSONObject(jsonString.toString());

                            // Create StickerPack from JSON
                            targetPack = new StickerPack(
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

                            // Parse existing stickers if any
                            List<Sticker> stickers = new ArrayList<>();
                            if (packJson.has("stickers")) {
                                JSONArray stickersJson = packJson.getJSONArray("stickers");

                                for (int i = 0; i < stickersJson.length(); i++) {
                                    JSONObject stickerJson = stickersJson.getJSONObject(i);

                                    String imageFile = stickerJson.getString("image_file");

                                    // Parse emojis
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

                            targetPack.setStickers(stickers);
                            Log.d(TAG, "Successfully loaded pack directly: " + packId + " with " + stickers.size() + " stickers");
                        } catch (JSONException | IOException e) {
                            Log.e(TAG, "Error loading pack info: " + e.getMessage());
                        }
                    }
                }

                if (targetPack == null) {
                    Log.e(TAG, "Target pack not found: " + packId);
                    return false;
                }

                // 2. Create a new sticker based on the custom sticker
                String newStickerFileName = "sticker_" + System.currentTimeMillis() + ".webp";

                // 3. Copy the custom sticker file to the sticker pack directory
                File sourceFile = new File(FileUtils.getCustomStickersDirectory(context), customSticker.getImageFileName());
                File targetDir = new File(context.getFilesDir(), packId);
                File targetFile = new File(targetDir, newStickerFileName);

                // Create directory if it doesn't exist
                if (!targetDir.exists() && !targetDir.mkdirs()) {
                    Log.e(TAG, "Failed to create directory: " + targetDir.getAbsolutePath());
                    return false;
                }

                // Copy the file
                copyFile(sourceFile, targetFile);

                // 4. Create a new sticker object
                Sticker newSticker = new Sticker(
                        newStickerFileName,
                        customSticker.getEmojis(),
                        customSticker.getAccessibilityText()
                );
                newSticker.setSize(targetFile.length());

                // 5. Add the sticker to the BEGINNING of the pack (reverse order)
                List<Sticker> stickers = targetPack.getStickers();
                List<Sticker> newStickers = new ArrayList<>();

                // Add the user's sticker first
                newStickers.add(newSticker);

                // Then add all existing stickers
                newStickers.addAll(stickers);

                targetPack.setStickers(newStickers);

                // 6. Update the pack's info in its directory
                updatePackInfo(context, targetPack);

                // 7. Make the user's sticker the tray icon
                updateTrayIcon(context, targetPack, sourceFile);

                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error adding sticker to pack: " + e.getMessage(), e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            Context context = contextRef.get();
            if (context != null && success) {
                // Always notify ContentProvider about changes
                notifyContentProviderChange(context, packId);
            }

            AddStickerCallback callback = callbackRef.get();
            if (callback != null) {
                callback.onStickerAdded(success);
            }
        }

        /**
         * Copy a file from source to target.
         */
        private void copyFile(File sourceFile, File destFile) throws IOException {
            if (!sourceFile.exists()) {
                return;
            }

            try (FileChannel source = new FileInputStream(sourceFile).getChannel();
                 FileChannel destination = new FileOutputStream(destFile).getChannel()) {
                destination.transferFrom(source, 0, source.size());
            }
        }

        /**
         * Update the tray icon to use the user's custom sticker
         */
        private void updateTrayIcon(Context context, StickerPack pack, File customStickerFile) {
            try {
                File packDirectory = new File(context.getFilesDir(), pack.identifier);
                File trayIconFile = new File(packDirectory, pack.trayImageFile);

                // Create a 96x96 version of the custom sticker for the tray icon
                Bitmap originalBitmap = BitmapFactory.decodeFile(customStickerFile.getAbsolutePath());
                if (originalBitmap != null) {
                    // Scale the bitmap to 96x96 (WhatsApp tray icon size)
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 96, 96, true);
                    originalBitmap.recycle();

                    // Save the scaled bitmap as the tray icon
                    try (FileOutputStream out = new FileOutputStream(trayIconFile)) {
                        scaledBitmap.compress(Bitmap.CompressFormat.WEBP, 100, out);
                    }
                    scaledBitmap.recycle();

                    Log.d(TAG, "Updated tray icon for pack: " + pack.identifier);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating tray icon: " + e.getMessage());
            }
        }

        /**
         * Update the pack info JSON for a pack.
         */
        private void updatePackInfo(Context context, StickerPack pack) throws JSONException, IOException {
            File packDirectory = new File(context.getFilesDir(), pack.identifier);
            File infoFile = new File(packDirectory, "pack_info.json");

            JSONObject packJson = new JSONObject();
            packJson.put("identifier", pack.identifier);
            packJson.put("name", pack.name);
            packJson.put("publisher", pack.publisher);
            packJson.put("tray_image_file", pack.trayImageFile);
            packJson.put("image_data_version", pack.imageDataVersion);
            packJson.put("avoid_cache", pack.avoidCache);
            packJson.put("animated_sticker_pack", pack.animatedStickerPack);

            // Add optional fields
            if (pack.publisherEmail != null && !pack.publisherEmail.isEmpty()) {
                packJson.put("publisher_email", pack.publisherEmail);
            }
            if (pack.publisherWebsite != null && !pack.publisherWebsite.isEmpty()) {
                packJson.put("publisher_website", pack.publisherWebsite);
            }
            if (pack.privacyPolicyWebsite != null && !pack.privacyPolicyWebsite.isEmpty()) {
                packJson.put("privacy_policy_website", pack.privacyPolicyWebsite);
            }
            if (pack.licenseAgreementWebsite != null && !pack.licenseAgreementWebsite.isEmpty()) {
                packJson.put("license_agreement_website", pack.licenseAgreementWebsite);
            }

            // Add stickers
            JSONArray stickersJson = new JSONArray();
            for (Sticker sticker : pack.getStickers()) {
                JSONObject stickerJson = new JSONObject();
                stickerJson.put("image_file", sticker.imageFileName);

                JSONArray emojisJson = new JSONArray();
                for (String emoji : sticker.emojis) {
                    emojisJson.put(emoji);
                }
                stickerJson.put("emojis", emojisJson);

                if (sticker.accessibilityText != null && !sticker.accessibilityText.isEmpty()) {
                    stickerJson.put("accessibility_text", sticker.accessibilityText);
                }

                stickersJson.put(stickerJson);
            }
            packJson.put("stickers", stickersJson);

            // Write to file
            try (FileWriter writer = new FileWriter(infoFile)) {
                writer.write(packJson.toString(2));
            }

            Log.d(TAG, "Updated pack info for: " + pack.identifier + " with " + pack.getStickers().size() + " stickers");
        }

        /**
         * Notify the ContentProvider that data has changed.
         * This is critical for WhatsApp to detect the changes.
         */
        private void notifyContentProviderChange(Context context, String packId) {
            try {
                // Get the authority of our content provider
                String authority = BuildConfig.CONTENT_PROVIDER_AUTHORITY;
                ContentResolver resolver = context.getContentResolver();

                // Notify changes to all relevant URIs
                Uri packUri = Uri.parse("content://" + authority + "/" +
                        StickerContentProvider.METADATA + "/" + packId);
                Uri stickersUri = Uri.parse("content://" + authority + "/" +
                        StickerContentProvider.STICKERS + "/" + packId);

                // Notify the content provider about the changes
                resolver.notifyChange(packUri, null);
                resolver.notifyChange(stickersUri, null);

                // Also notify the main metadata URI
                Uri metadataUri = Uri.parse("content://" + authority + "/" +
                        StickerContentProvider.METADATA);
                resolver.notifyChange(metadataUri, null);

                Log.d(TAG, "Successfully notified ContentProvider about changes to pack: " + packId);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying ContentProvider about changes", e);
            }
        }
    }
}