package com.example.samplestickertestingapp.utils;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for adding custom stickers to existing sticker packs.
 */
public class StickerPackManager {
    private static final String TAG = "StickerPackManagerrrr";

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
     * @param callback Callback for operation completion
     */
    public static void addStickerToPack(Context context, CustomSticker customSticker, String packId, AddStickerCallback callback) {
        new AddStickerToPackTask(context, callback).execute(customSticker, packId);
    }

    /**
     * AsyncTask to add sticker to pack in background.
     */
    private static class AddStickerToPackTask extends AsyncTask<Object, Void, Boolean> {
        private final WeakReference<Context> contextRef;
        private final WeakReference<AddStickerCallback> callbackRef;
        private String packId;

        AddStickerToPackTask(Context context, AddStickerCallback callback) {
            this.contextRef = new WeakReference<>(context);
            this.callbackRef = new WeakReference<>(callback);
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
                List<StickerPack> packs = StickerPackLoader.getStickerPacks(context);
                StickerPack targetPack = null;

                for (StickerPack pack : packs) {
                    if (pack.identifier.equals(packId)) {
                        targetPack = pack;
                        break;
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

                if (!targetDir.exists()) {
                    if (!targetDir.mkdirs()) {
                        Log.e(TAG, "Failed to create target directory");
                        return false;
                    }
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

                // 5. Add the sticker to the pack
                List<Sticker> stickers = targetPack.getStickers();
                List<Sticker> newStickers = new ArrayList<>(stickers);
                newStickers.add(newSticker);
                targetPack.setStickers(newStickers);

                // 6. Update the pack's info in its directory
                updatePackInfo(context, targetPack);

                // 7. Make sure tray icon exists (use the first sticker if needed)
                ensureTrayIconExists(context, targetPack);

                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error adding sticker to pack", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            Context context = contextRef.get();
            if (context != null && success) {
                // Notify ContentProvider that data has changed
                notifyContentProviderChange(context, packId);

                // Send a broadcast to WhatsApp to update the sticker pack
                notifyWhatsAppOfUpdate(context, packId);
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

                stickerJson.put("accessibility_text", sticker.accessibilityText);

                stickersJson.put(stickerJson);
            }
            packJson.put("stickers", stickersJson);

            // Write to file
            try (FileWriter writer = new FileWriter(infoFile)) {
                writer.write(packJson.toString(2));
            }
        }

        /**
         * Ensure the tray icon file exists for a pack.
         */
        private void ensureTrayIconExists(Context context, StickerPack pack) throws IOException {
            File packDirectory = new File(context.getFilesDir(), pack.identifier);
            File trayFile = new File(packDirectory, pack.trayImageFile);

            if (!trayFile.exists() && !pack.getStickers().isEmpty()) {
                // Use first sticker as tray icon
                Sticker firstSticker = pack.getStickers().get(0);
                File firstStickerFile = new File(packDirectory, firstSticker.imageFileName);

                if (firstStickerFile.exists()) {
                    // Create a smaller version for tray icon
                    Bitmap original = BitmapFactory.decodeFile(firstStickerFile.getAbsolutePath());
                    if (original != null) {
                        Bitmap resized = Bitmap.createScaledBitmap(original, 96, 96, true);
                        try (OutputStream out = new FileOutputStream(trayFile)) {
                            resized.compress(Bitmap.CompressFormat.WEBP, 100, out);
                        }
                        resized.recycle();
                        original.recycle();
                    }
                }
            }
        }

        /**
         * Notify the ContentProvider that data has changed.
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

                Log.d(TAG, "Notified ContentProvider about changes to pack: " + packId);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying ContentProvider about changes", e);
            }
        }

        /**
         * Notify WhatsApp of sticker pack update.
         */
        private void notifyWhatsAppOfUpdate(Context context, String packId) {
            try {
                // Create intent for WhatsApp
                Intent intent = new Intent();
                intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
                intent.putExtra("sticker_pack_id", packId);
                intent.putExtra("sticker_pack_authority", BuildConfig.CONTENT_PROVIDER_AUTHORITY);

                // Make sure the intent reaches WhatsApp even if it's not running
                intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

                // Try to start the activity directly
                try {
                    context.startActivity(intent);
                    Log.d(TAG, "Started activity to enable sticker pack in WhatsApp");
                } catch (ActivityNotFoundException e) {
                    // If that fails, try broadcasting (older WhatsApp versions)
                    Log.d(TAG, "Activity not found, trying broadcast");
                    intent.setAction("com.whatsapp.intent.action.STICKER_PACK_RESULT");
                    context.sendBroadcast(intent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error notifying WhatsApp of update", e);
            }
        }
    }
}