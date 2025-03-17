package com.example.samplestickertestingapp.utils;


import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.samplestickertestingapp.BuildConfig;
import com.example.samplestickertestingapp.models.Sticker;
import com.example.samplestickertestingapp.models.StickerPack;
import com.example.samplestickertestingapp.providers.StickerContentProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for loading sticker packs from assets and files directory.
 */
public class StickerPackLoader {
    private static final String TAG = "StickerPackLoader";

    // Name of the JSON files
    private static final String CONTENT_FILE_NAME = "contents.json";
    private static final String PACK_INFO_FILE_NAME = "pack_info.json";

    // Keys in the JSON file
    private static final String KEY_STICKER_PACKS = "sticker_packs";
    private static final String KEY_IDENTIFIER = "identifier";
    private static final String KEY_NAME = "name";
    private static final String KEY_PUBLISHER = "publisher";
    private static final String KEY_TRAY_IMAGE_FILE = "tray_image_file";
    private static final String KEY_IMAGE_DATA_VERSION = "image_data_version";
    private static final String KEY_AVOID_CACHE = "avoid_cache";
    private static final String KEY_ANIMATED_STICKER_PACK = "animated_sticker_pack";
    private static final String KEY_PUBLISHER_EMAIL = "publisher_email";
    private static final String KEY_PUBLISHER_WEBSITE = "publisher_website";
    private static final String KEY_PRIVACY_POLICY_WEBSITE = "privacy_policy_website";
    private static final String KEY_LICENSE_AGREEMENT_WEBSITE = "license_agreement_website";
    private static final String KEY_STICKERS = "stickers";
    private static final String KEY_STICKER_FILE = "image_file";
    private static final String KEY_EMOJIS = "emojis";
    private static final String KEY_ACCESSIBILITY_TEXT = "accessibility_text";
    private static final String KEY_ANDROID_PLAY_STORE_LINK = "android_play_store_link";
    private static final String KEY_IOS_APP_STORE_LINK = "ios_app_store_link";

    // List of sticker packs (cached)
    private static List<StickerPack> stickerPackList;

    /**
     * Get all available sticker packs from both assets and app files directory.
     *
     * @param context Application context
     * @return List of sticker packs
     */
    public static List<StickerPack> getStickerPacks(Context context) throws JSONException, IOException {
        // Always reload to reflect any changes
        loadStickerPacks(context);
        return stickerPackList;
    }

    /**
     * Load all sticker packs from assets and files directory.
     *
     * @param context Application context
     * @throws JSONException if there's an error parsing the JSON file
     * @throws IOException if there's an error reading the files
     */
    private static void loadStickerPacks(Context context) throws JSONException, IOException {
        List<StickerPack> packs = new ArrayList<>();

        // Load packs from assets (if any)
        try {
            InputStream contentsInputStream = context.getAssets().open(CONTENT_FILE_NAME);
            JSONObject contentsJson = new JSONObject(inputStreamToString(contentsInputStream));

            if (contentsJson.has(KEY_STICKER_PACKS)) {
                // Parse sticker packs from JSON
                JSONArray jsonPacks = contentsJson.getJSONArray(KEY_STICKER_PACKS);
                for (int i = 0; i < jsonPacks.length(); i++) {
                    JSONObject jsonPack = jsonPacks.getJSONObject(i);
                    StickerPack pack = parseStickerPack(jsonPack);

                    // Add stickers to pack
                    List<Sticker> stickers = parseStickers(jsonPack.getJSONArray(KEY_STICKERS));
                    pack.setStickers(stickers);

                    // Set store links
                    if (contentsJson.has(KEY_ANDROID_PLAY_STORE_LINK)) {
                        pack.setAndroidPlayStoreLink(contentsJson.getString(KEY_ANDROID_PLAY_STORE_LINK));
                    }
                    if (contentsJson.has(KEY_IOS_APP_STORE_LINK)) {
                        pack.setIosAppStoreLink(contentsJson.getString(KEY_IOS_APP_STORE_LINK));
                    }

                    // Load file sizes for assets
                    loadStickerFileSizes(context, pack, true);

                    packs.add(pack);
                }
            }
        } catch (IOException e) {
            // It's okay if there's no contents.json in assets
            Log.d(TAG, "No contents.json found in assets or error reading: " + e.getMessage());
        }

        // Look for dynamically generated packs in files directory
        File[] directories = context.getFilesDir().listFiles(File::isDirectory);
        if (directories != null) {
            for (File directory : directories) {
                try {
                    // Check if this directory contains a sticker pack
                    File packInfoFile = new File(directory, PACK_INFO_FILE_NAME);
                    File trayIconFile = new File(directory, "tray_icon.webp");

                    // Directory must have pack_info.json and tray icon to be a valid sticker pack
                    if (packInfoFile.exists() && trayIconFile.exists()) {
                        StickerPack pack = loadStickerPackFromFiles(packInfoFile);
                        if (pack != null) {
                            // Load stickers
                            File[] stickerFiles = directory.listFiles(file ->
                                    file.isFile() && file.getName().endsWith(".webp") &&
                                            !file.getName().equals("tray_icon.webp"));

                            if (stickerFiles != null && stickerFiles.length >= 3) {
                                List<Sticker> stickers = new ArrayList<>();
                                for (File stickerFile : stickerFiles) {
                                    // Get sticker info from pack_info.json
                                    Sticker sticker = findStickerInfo(pack, stickerFile.getName());
                                    if (sticker != null) {
                                        sticker.setSize(stickerFile.length());
                                        stickers.add(sticker);
                                    }
                                }

                                // Set stickers and add pack to list
                                if (!stickers.isEmpty()) {
                                    pack.setStickers(stickers);
                                    packs.add(pack);
                                    Log.d(TAG, "Loaded sticker pack: " + pack.identifier + " with " + stickers.size() + " stickers");
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "Directory is not a valid sticker pack: " + directory.getName());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading sticker pack from directory: " + directory.getName(), e);
                }
            }
        }

        stickerPackList = packs;
        Log.d(TAG, "Loaded " + packs.size() + " sticker packs");
    }

    /**
     * Find sticker info in the pack's stickers.
     */
    private static Sticker findStickerInfo(StickerPack pack, String fileName) {
        // Check if we already have info for this sticker
        if (pack.getStickers() != null) {
            for (Sticker sticker : pack.getStickers()) {
                if (sticker.imageFileName.equals(fileName)) {
                    return sticker;
                }
            }
        }

        // If not found, create a default sticker
        List<String> defaultEmojis = new ArrayList<>();
        defaultEmojis.add("🎨");
        return new Sticker(fileName, defaultEmojis, "A colorful sticker");
    }

    /**
     * Load a sticker pack from a JSON file.
     *
     * @param packInfoFile File containing pack info
     * @return StickerPack object or null if invalid
     */
    private static StickerPack loadStickerPackFromFiles(File packInfoFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(packInfoFile))) {
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }

            JSONObject packJson = new JSONObject(jsonString.toString());

            // Create sticker pack with info from JSON
            StickerPack pack = new StickerPack(
                    packJson.getString(KEY_IDENTIFIER),
                    packJson.getString(KEY_NAME),
                    packJson.getString(KEY_PUBLISHER),
                    packJson.getString(KEY_TRAY_IMAGE_FILE),
                    packJson.optString(KEY_PUBLISHER_EMAIL, ""),
                    packJson.optString(KEY_PUBLISHER_WEBSITE, ""),
                    packJson.optString(KEY_PRIVACY_POLICY_WEBSITE, ""),
                    packJson.optString(KEY_LICENSE_AGREEMENT_WEBSITE, ""),
                    packJson.optString(KEY_IMAGE_DATA_VERSION, "1"),
                    packJson.optBoolean(KEY_AVOID_CACHE, false),
                    packJson.optBoolean(KEY_ANIMATED_STICKER_PACK, false)
            );

            // Parse stickers
            JSONArray stickersJson = packJson.getJSONArray(KEY_STICKERS);
            List<Sticker> stickers = new ArrayList<>();

            for (int i = 0; i < stickersJson.length(); i++) {
                JSONObject stickerJson = stickersJson.getJSONObject(i);

                String imageFile = stickerJson.getString(KEY_STICKER_FILE);

                // Parse emojis
                List<String> emojis = new ArrayList<>();
                JSONArray emojisJson = stickerJson.getJSONArray(KEY_EMOJIS);
                for (int j = 0; j < emojisJson.length(); j++) {
                    emojis.add(emojisJson.getString(j));
                }

                // Parse accessibility text
                String accessibilityText = stickerJson.optString(KEY_ACCESSIBILITY_TEXT, "");

                stickers.add(new Sticker(imageFile, emojis, accessibilityText));
            }

            pack.setStickers(stickers);
            return pack;

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error parsing pack info file: " + packInfoFile.getPath(), e);
            return null;
        }
    }

    /**
     * Parse a sticker pack from a JSON object.
     *
     * @param jsonObject JSON object containing sticker pack data
     * @return StickerPack object
     * @throws JSONException if there's an error parsing the JSON
     */
    private static StickerPack parseStickerPack(JSONObject jsonObject) throws JSONException {
        String identifier = jsonObject.getString(KEY_IDENTIFIER);
        String name = jsonObject.getString(KEY_NAME);
        String publisher = jsonObject.getString(KEY_PUBLISHER);
        String trayImageFile = jsonObject.getString(KEY_TRAY_IMAGE_FILE);

        // Optional fields
        String publisherEmail = jsonObject.optString(KEY_PUBLISHER_EMAIL, "");
        String publisherWebsite = jsonObject.optString(KEY_PUBLISHER_WEBSITE, "");
        String privacyPolicyWebsite = jsonObject.optString(KEY_PRIVACY_POLICY_WEBSITE, "");
        String licenseAgreementWebsite = jsonObject.optString(KEY_LICENSE_AGREEMENT_WEBSITE, "");
        String imageDataVersion = jsonObject.optString(KEY_IMAGE_DATA_VERSION, "1");
        boolean avoidCache = jsonObject.optBoolean(KEY_AVOID_CACHE, false);
        boolean animatedStickerPack = jsonObject.optBoolean(KEY_ANIMATED_STICKER_PACK, false);

        return new StickerPack(
                identifier, name, publisher, trayImageFile,
                publisherEmail, publisherWebsite, privacyPolicyWebsite, licenseAgreementWebsite,
                imageDataVersion, avoidCache, animatedStickerPack
        );
    }

    /**
     * Parse stickers from a JSON array.
     *
     * @param jsonArray JSON array containing sticker data
     * @return List of Sticker objects
     * @throws JSONException if there's an error parsing the JSON
     */
    private static List<Sticker> parseStickers(JSONArray jsonArray) throws JSONException {
        List<Sticker> stickers = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonSticker = jsonArray.getJSONObject(i);
            String imageFile = jsonSticker.getString(KEY_STICKER_FILE);

            // Parse emojis
            List<String> emojis = new ArrayList<>();
            if (jsonSticker.has(KEY_EMOJIS)) {
                JSONArray jsonEmojis = jsonSticker.getJSONArray(KEY_EMOJIS);
                for (int j = 0; j < jsonEmojis.length(); j++) {
                    emojis.add(jsonEmojis.getString(j));
                }
            } else {
                // Default emoji if none provided
                emojis.add("🎨");
            }

            // Parse accessibility text
            String accessibilityText = jsonSticker.optString(KEY_ACCESSIBILITY_TEXT, "");

            stickers.add(new Sticker(imageFile, emojis, accessibilityText));
        }

        return stickers;
    }

    /**
     * Load the file sizes for all stickers in a pack.
     *
     * @param context Application context
     * @param stickerPack Sticker pack to load file sizes for
     * @param fromAssets Whether to load from assets or files directory
     * @throws IOException if there's an error reading the files
     */
    private static void loadStickerFileSizes(Context context, StickerPack stickerPack, boolean fromAssets)
            throws IOException {
        // Load tray icon size
        if (fromAssets) {
            String trayIconPath = stickerPack.identifier + "/" + stickerPack.trayImageFile;
            byte[] trayIconBytes = getBytes(context, trayIconPath, fromAssets);

            // Load sticker sizes
            for (Sticker sticker : stickerPack.getStickers()) {
                String path = stickerPack.identifier + "/" + sticker.imageFileName;
                byte[] bytes = getBytes(context, path, fromAssets);
                sticker.setSize(bytes.length);
            }
        } else {
            // Files are already loaded with their size when creating the sticker objects
        }
    }

    /**
     * Load bytes from a file, either from assets or files directory.
     *
     * @param context Application context
     * @param path Path to the file
     * @param fromAssets Whether to load from assets or files directory
     * @return Byte array containing the file contents
     * @throws IOException if there's an error reading the file
     */
    private static byte[] getBytes(Context context, String path, boolean fromAssets) throws IOException {
        InputStream inputStream;

        if (fromAssets) {
            inputStream = context.getAssets().open(path);
        } else {
            String[] pathParts = path.split("/");
            if (pathParts.length != 2) {
                throw new IOException("Invalid path: " + path);
            }

            File file = new File(new File(context.getFilesDir(), pathParts[0]), pathParts[1]);
            inputStream = new FileInputStream(file);
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int bytesRead;
        byte[] data = new byte[1024];

        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }

        inputStream.close();
        return buffer.toByteArray();
    }

    /**
     * Convert an InputStream to a String.
     */
    private static String inputStreamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }

    /**
     * Build a content URI for a sticker file.
     *
     * @param identifier Sticker pack identifier
     * @param stickerName Sticker file name
     * @return URI for accessing the sticker through the ContentProvider
     */
    public static Uri getStickerAssetUri(String identifier, String stickerName) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(BuildConfig.CONTENT_PROVIDER_AUTHORITY)
                .appendPath(StickerContentProvider.STICKERS_ASSET)
                .appendPath(identifier)
                .appendPath(stickerName)
                .build();
    }
}
