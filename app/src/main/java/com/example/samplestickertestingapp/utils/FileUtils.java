package com.example.samplestickertestingapp.utils;

import android.content.Context;
import android.util.Log;

import com.example.samplestickertestingapp.models.CustomSticker;
import com.example.samplestickertestingapp.models.StickerPack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for file operations related to custom stickers.
 */
public class FileUtils {
    private static final String TAG = "FileUtils";

    // Directory names
    private static final String CUSTOM_STICKERS_DIR = "custom_stickers";
    private static final String CUSTOM_STICKERS_INFO_FILE = "custom_stickers_info.json";

    /**
     * Get the directory for custom stickers.
     *
     * @param context Application context
     * @return File object pointing to the custom stickers directory
     */
    public static File getCustomStickersDirectory(Context context) {
        File directory = new File(context.getFilesDir(), CUSTOM_STICKERS_DIR);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                Log.e(TAG, "Failed to create custom stickers directory");
                return null;
            }
        }
        return directory;
    }

    /**
     * Generate a unique file name for a custom sticker.
     *
     * @param prefix Prefix for the file name (e.g., "image" or "video")
     * @return Unique file name with .webp extension
     */
    public static String generateStickerFileName(String prefix) {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return prefix + "_" + uniqueId + ".webp";
    }

    /**
     * Save custom sticker metadata to JSON.
     *
     * @param context Application context
     * @param stickers List of custom stickers
     * @return true if saved successfully, false otherwise
     */
    public static boolean saveCustomStickersInfo(Context context, List<CustomSticker> stickers) {
        try {
            JSONArray stickersArray = new JSONArray();

            for (CustomSticker sticker : stickers) {
                JSONObject stickerJson = new JSONObject();
                stickerJson.put("imageFileName", sticker.getImageFileName());

                JSONArray emojisArray = new JSONArray();
                for (String emoji : sticker.getEmojis()) {
                    emojisArray.put(emoji);
                }

                stickerJson.put("emojis", emojisArray);
                stickerJson.put("accessibilityText", sticker.getAccessibilityText());
                stickerJson.put("creationTimestamp", sticker.getCreationTimestamp());
                stickerJson.put("size", sticker.getSize());
                stickerJson.put("sourceType", sticker.getSourceType());

                stickersArray.put(stickerJson);
            }

            File infoFile = new File(getCustomStickersDirectory(context), CUSTOM_STICKERS_INFO_FILE);
            try (FileWriter writer = new FileWriter(infoFile)) {
                writer.write(stickersArray.toString());
            }

            return true;
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Error saving custom stickers info", e);
            return false;
        }
    }

    /**
     * Load custom stickers from JSON.
     *
     * @param context Application context
     * @return List of custom stickers
     */
    public static List<CustomSticker> loadCustomStickers(Context context) {
        List<CustomSticker> stickers = new ArrayList<>();

        try {
            File infoFile = new File(getCustomStickersDirectory(context), CUSTOM_STICKERS_INFO_FILE);
            if (!infoFile.exists()) {
                return stickers;
            }

            StringBuilder jsonString = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(infoFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonString.append(line);
                }
            }

            JSONArray stickersArray = new JSONArray(jsonString.toString());
            for (int i = 0; i < stickersArray.length(); i++) {
                JSONObject stickerJson = stickersArray.getJSONObject(i);

                String imageFileName = stickerJson.getString("imageFileName");

                List<String> emojis = new ArrayList<>();
                JSONArray emojisArray = stickerJson.getJSONArray("emojis");
                for (int j = 0; j < emojisArray.length(); j++) {
                    emojis.add(emojisArray.getString(j));
                }

                String accessibilityText = stickerJson.getString("accessibilityText");
                int sourceType = stickerJson.getInt("sourceType");

                CustomSticker sticker = new CustomSticker(imageFileName, emojis, accessibilityText, sourceType);
                sticker.setSize(stickerJson.getLong("size"));

                // Check if the sticker file exists
                File stickerFile = new File(getCustomStickersDirectory(context), imageFileName);
                if (stickerFile.exists()) {
                    stickers.add(sticker);
                }
            }
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Error loading custom stickers", e);
        }

        return stickers;
    }

    /**
     * Delete a custom sticker file and update the JSON info.
     *
     * @param context Application context
     * @param sticker Custom sticker to delete
     * @return true if deleted successfully, false otherwise
     */
    public static boolean deleteCustomSticker(Context context, CustomSticker sticker) {
        try {
            // Delete the sticker file
            File stickerFile = new File(getCustomStickersDirectory(context), sticker.getImageFileName());
            if (stickerFile.exists() && !stickerFile.delete()) {
                Log.e(TAG, "Failed to delete sticker file: " + stickerFile.getAbsolutePath());
                return false;
            }

            // Update the stickers info
            List<CustomSticker> stickers = loadCustomStickers(context);
            stickers.removeIf(s -> s.getImageFileName().equals(sticker.getImageFileName()));
            return saveCustomStickersInfo(context, stickers);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting custom sticker", e);
            return false;
        }
    }

    /**
     * Create a new sticker pack for custom stickers.
     *
     * @param context Application context
     * @param packName Pack name
     * @param publisher Publisher name
     * @return Created StickerPack or null if failed
     */
    public static StickerPack createCustomStickerPack(Context context, String packName, String publisher) {
        String packId = "custom_" + UUID.randomUUID().toString().substring(0, 8);

        // Create directory for sticker pack
        File packDirectory = new File(context.getFilesDir(), packId);
        if (!packDirectory.exists() && !packDirectory.mkdirs()) {
            Log.e(TAG, "Failed to create directory for custom sticker pack: " + packId);
            return null;
        }

        // Create tray icon (use the first sticker as tray icon for now)
        String trayIconFileName = "tray_icon.webp";

        StickerPack stickerPack = new StickerPack(
                packId,
                packName,
                publisher,
                trayIconFileName,
                "",  // publisherEmail
                "",  // publisherWebsite
                "",  // privacyPolicyWebsite
                "",  // licenseAgreementWebsite
                "1", // imageDataVersion
                false, // avoidCache
                false  // animatedStickerPack
        );

        return stickerPack;
    }
}