package com.example.samplestickertestingapp.providers;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.samplestickertestingapp.BuildConfig;
import com.example.samplestickertestingapp.models.Sticker;
import com.example.samplestickertestingapp.models.StickerPack;
import com.example.samplestickertestingapp.utils.StickerPackLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Content Provider for sticker packs. This is the interface WhatsApp uses to access stickers.
 * Do not change the method signatures or identifiers as it would break compatibility.
 */
public class StickerContentProvider extends ContentProvider {
    private static final String TAG = "StickerContentProvider";

    // Column names used by WhatsApp to query sticker pack metadata
    public static final String STICKER_PACK_IDENTIFIER_IN_QUERY = "sticker_pack_identifier";
    public static final String STICKER_PACK_NAME_IN_QUERY = "sticker_pack_name";
    public static final String STICKER_PACK_PUBLISHER_IN_QUERY = "sticker_pack_publisher";
    public static final String STICKER_PACK_ICON_IN_QUERY = "sticker_pack_icon";
    public static final String ANDROID_APP_DOWNLOAD_LINK_IN_QUERY = "android_play_store_link";
    public static final String IOS_APP_DOWNLOAD_LINK_IN_QUERY = "ios_app_download_link";
    public static final String PUBLISHER_EMAIL = "sticker_pack_publisher_email";
    public static final String PUBLISHER_WEBSITE = "sticker_pack_publisher_website";
    public static final String PRIVACY_POLICY_WEBSITE = "sticker_pack_privacy_policy_website";
    public static final String LICENSE_AGREEMENT_WEBSITE = "sticker_pack_license_agreement_website";
    public static final String IMAGE_DATA_VERSION = "image_data_version";
    public static final String AVOID_CACHE = "whatsapp_will_not_cache_stickers";
    public static final String ANIMATED_STICKER_PACK = "animated_sticker_pack";

    // Column names used by WhatsApp to query sticker data
    public static final String STICKER_FILE_NAME_IN_QUERY = "sticker_file_name";
    public static final String STICKER_FILE_EMOJI_IN_QUERY = "sticker_emoji";
    public static final String STICKER_FILE_ACCESSIBILITY_TEXT_IN_QUERY = "sticker_accessibility_text";

    // Content provider paths
    public static final String METADATA = "metadata";
    public static final String STICKERS = "stickers";
    public static final String STICKERS_ASSET = "stickers_asset";

    // Authority URI that WhatsApp will use to query our provider
    public static final Uri AUTHORITY_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(BuildConfig.CONTENT_PROVIDER_AUTHORITY)
            .appendPath(METADATA)
            .build();

    // URI matcher codes
    private static final int METADATA_CODE = 1;
    private static final int METADATA_CODE_FOR_SINGLE_PACK = 2;
    private static final int STICKERS_CODE = 3;
    private static final int STICKERS_ASSET_CODE = 4;
    private static final int STICKER_PACK_TRAY_ICON_CODE = 5;

    // Cache for URI patterns - we'll map pack/sticker ids to codes
    private Map<String, Integer> uriCodeMap = new HashMap<>();
    private int nextUriCode = 10; // Start after our initial codes

    // URI matcher
    private UriMatcher MATCHER;

    // List of available sticker packs
    private List<StickerPack> stickerPackList;

    // Flag to track if an update is needed
    private boolean needUpdate = true;

    @Override
    public boolean onCreate() {
        final String authority = BuildConfig.CONTENT_PROVIDER_AUTHORITY;
        if (!authority.startsWith(Objects.requireNonNull(getContext()).getPackageName())) {
            throw new IllegalStateException("Content provider authority (" + authority +
                    ") should start with the app package name: " + getContext().getPackageName());
        }

        // Initialize URI matcher
        MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

        // Register basic URI patterns
        MATCHER.addURI(authority, METADATA, METADATA_CODE);
        MATCHER.addURI(authority, METADATA + "/*", METADATA_CODE_FOR_SINGLE_PACK);
        MATCHER.addURI(authority, STICKERS + "/*", STICKERS_CODE);

        // Initial load of sticker packs
        loadStickerPacks();

        return true;
    }

    /**
     * Register URIs for a specific pack and its stickers.
     */
    private void registerPackUris(String authority, StickerPack pack) {
        String packId = pack.identifier;
        Log.d(TAG, "Registering URIs for pack: " + packId);

        // Register URI for tray icon - first check if we already have a code
        String trayKey = packId + "/" + pack.trayImageFile;
        if (!uriCodeMap.containsKey(trayKey)) {
            int code = nextUriCode++;
            uriCodeMap.put(trayKey, code);
            MATCHER.addURI(authority,
                    STICKERS_ASSET + "/" + trayKey,
                    code);
            Log.d(TAG, "Registered tray icon URI: " + STICKERS_ASSET + "/" + trayKey + " with code " + code);
        }

        // Register URI for each sticker
        for (Sticker sticker : pack.getStickers()) {
            String stickerKey = packId + "/" + sticker.imageFileName;
            if (!uriCodeMap.containsKey(stickerKey)) {
                int code = nextUriCode++;
                uriCodeMap.put(stickerKey, code);
                MATCHER.addURI(authority,
                        STICKERS_ASSET + "/" + stickerKey,
                        code);
                Log.d(TAG, "Registered sticker URI: " + STICKERS_ASSET + "/" + stickerKey + " with code " + code);
            }
        }
    }

    /**
     * Force reload of sticker packs. Called when content changes.
     */
    private synchronized void loadStickerPacks() {
        try {
            // Clear existing packs
            if (stickerPackList != null) {
                stickerPackList.clear();
            } else {
                stickerPackList = new ArrayList<>();
            }

            // Load from StickerPackLoader first
            try {
                List<StickerPack> loaderPacks = StickerPackLoader.getStickerPacks(getContext());
                stickerPackList.addAll(loaderPacks);
                Log.d(TAG, "Loaded " + loaderPacks.size() + " packs from StickerPackLoader");
            } catch (Exception e) {
                Log.e(TAG, "Error loading from StickerPackLoader: " + e.getMessage());
            }

            // Then scan directories directly to catch any new packs or updates
            scanDirectoriesForStickerPacks();

            // Register URIs for all packs
            if (getContext() != null) {
                String authority = BuildConfig.CONTENT_PROVIDER_AUTHORITY;
                for (StickerPack pack : stickerPackList) {
                    registerPackUris(authority, pack);
                }
            }

            // Reset the update flag
            needUpdate = false;

            Log.d(TAG, "Total loaded packs: " + stickerPackList.size());
        } catch (Exception e) {
            Log.e(TAG, "Error loading sticker packs", e);
            if (stickerPackList == null) {
                stickerPackList = new ArrayList<>();
            }
        }
    }

    /**
     * Scan directories for sticker packs that might not be loaded by the StickerPackLoader.
     */
    private void scanDirectoriesForStickerPacks() {
        Context context = getContext();
        if (context == null) return;

        File[] directories = context.getFilesDir().listFiles(File::isDirectory);
        if (directories == null) return;

        for (File directory : directories) {
            String dirName = directory.getName();

            // Skip if not a custom sticker pack directory
            if (!dirName.startsWith("custom_") && !dirName.startsWith("colorstickers_")) continue;

            // Check for pack_info.json
            File packInfoFile = new File(directory, "pack_info.json");
            if (!packInfoFile.exists()) continue;

            boolean alreadyLoaded = false;
            int existingPackIndex = -1;

            // Check if we already have this pack
            for (int i = 0; i < stickerPackList.size(); i++) {
                StickerPack pack = stickerPackList.get(i);
                if (pack.identifier.equals(dirName)) {
                    alreadyLoaded = true;
                    existingPackIndex = i;
                    break;
                }
            }

            try {
                // Read the info file
                StringBuilder jsonString = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(packInfoFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonString.append(line);
                    }
                }

                JSONObject packJson = new JSONObject(jsonString.toString());

                // Create the pack
                StickerPack pack = new StickerPack(
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

                        // Parse emojis
                        List<String> emojis = new ArrayList<>();
                        if (stickerJson.has("emojis")) {
                            JSONArray emojisJson = stickerJson.getJSONArray("emojis");
                            for (int j = 0; j < emojisJson.length(); j++) {
                                emojis.add(emojisJson.getString(j));
                            }
                        } else {
                            // Default emoji
                            emojis.add("🎨");
                        }

                        String accessibilityText = stickerJson.optString("accessibility_text", "");

                        Sticker sticker = new Sticker(imageFile, emojis, accessibilityText);
                        File stickerFile = new File(directory, imageFile);
                        if (stickerFile.exists()) {
                            sticker.setSize(stickerFile.length());
                            stickers.add(sticker);
                        }
                    }
                }

                // Also check for sticker files directly (may not be listed in JSON yet)
                File[] stickerFiles = directory.listFiles(file ->
                        file.isFile() && file.getName().endsWith(".webp") &&
                                !file.getName().equals(pack.trayImageFile));

                if (stickerFiles != null) {
                    for (File file : stickerFiles) {
                        // Check if sticker already added
                        boolean alreadyAdded = false;
                        for (Sticker sticker : stickers) {
                            if (sticker.imageFileName.equals(file.getName())) {
                                alreadyAdded = true;
                                break;
                            }
                        }

                        if (!alreadyAdded) {
                            // Create a default sticker entry
                            List<String> defaultEmojis = new ArrayList<>();
                            defaultEmojis.add("🎨");
                            Sticker sticker = new Sticker(
                                    file.getName(),
                                    defaultEmojis,
                                    "Custom sticker"
                            );
                            sticker.setSize(file.length());
                            stickers.add(sticker);
                        }
                    }
                }

                // Only add if we have at least 1 sticker (WhatsApp requires 3 but we're more lenient)
                if (stickers.size() >= 1) {
                    pack.setStickers(stickers);

                    if (alreadyLoaded && existingPackIndex >= 0) {
                        // Update existing pack with new data
                        stickerPackList.set(existingPackIndex, pack);
                        Log.d(TAG, "Updated existing pack: " + pack.identifier +
                                " with " + stickers.size() + " stickers");
                    } else {
                        // Add as new pack
                        stickerPackList.add(pack);
                        Log.d(TAG, "Added pack from directory scan: " + pack.identifier +
                                " with " + stickers.size() + " stickers");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading pack from directory: " + directory.getName(), e);
            }
        }
    }

    private List<StickerPack> getStickerPackList() {
        // Check if we need to refresh the sticker pack data
        if (needUpdate || stickerPackList == null) {
            loadStickerPacks();
        }
        return stickerPackList;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        final int code = MATCHER.match(uri);
        Log.d(TAG, "Query URI: " + uri + ", code: " + code);

        // Always reload on metadata queries to ensure fresh data
        if (code == METADATA_CODE || code == METADATA_CODE_FOR_SINGLE_PACK) {
            needUpdate = true;
        }

        // Get the updated sticker pack list
        List<StickerPack> packs = getStickerPackList();

        switch (code) {
            case METADATA_CODE:
                return getPackForAllStickerPacks(uri);
            case METADATA_CODE_FOR_SINGLE_PACK:
                return getCursorForSingleStickerPack(uri);
            case STICKERS_CODE:
                return getStickersForAStickerPack(uri);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        final int matchCode = MATCHER.match(uri);
        Log.d(TAG, "Open asset file URI: " + uri + ", code: " + matchCode);

        // Special handling for sticker assets
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 3) {
            throw new IllegalArgumentException("Invalid URI path segments: " + uri);
        }

        final String fileName = pathSegments.get(pathSegments.size() - 1);
        final String identifier = pathSegments.get(pathSegments.size() - 2);

        Log.d(TAG, "Fetching asset: " + identifier + "/" + fileName);

        // Try to open the file
        try {
            // Check app's files directory for dynamically generated stickers
            File stickerFile = new File(new File(getContext().getFilesDir(), identifier), fileName);

            if (stickerFile.exists() && stickerFile.isFile()) {
                // File exists in app's files directory
                Log.d(TAG, "Found file in app directory: " + stickerFile.getAbsolutePath());
                ParcelFileDescriptor pfd = ParcelFileDescriptor.open(
                        stickerFile, ParcelFileDescriptor.MODE_READ_ONLY);
                return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
            } else {
                // File doesn't exist in app's files directory, try assets
                Log.d(TAG, "File not found in app directory, trying assets");
                return getContext().getAssets().openFd(identifier + "/" + fileName);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error opening sticker file: " + uri, e);
            throw new FileNotFoundException("Error opening sticker file: " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int matchCode = MATCHER.match(uri);
        switch (matchCode) {
            case METADATA_CODE:
                return "vnd.android.cursor.dir/vnd." + BuildConfig.CONTENT_PROVIDER_AUTHORITY + "." + METADATA;
            case METADATA_CODE_FOR_SINGLE_PACK:
                return "vnd.android.cursor.item/vnd." + BuildConfig.CONTENT_PROVIDER_AUTHORITY + "." + METADATA;
            case STICKERS_CODE:
                return "vnd.android.cursor.dir/vnd." + BuildConfig.CONTENT_PROVIDER_AUTHORITY + "." + STICKERS;
            default:
                if (matchCode != UriMatcher.NO_MATCH) {
                    // This is likely a sticker asset URI
                    List<String> pathSegments = uri.getPathSegments();
                    if (pathSegments.size() == 3 &&
                            pathSegments.get(0).equals(STICKERS_ASSET) &&
                            pathSegments.get(2).endsWith(".webp")) {
                        return "image/webp";
                    }
                }
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    private Cursor getPackForAllStickerPacks(@NonNull Uri uri) {
        return getStickerPackInfo(uri, getStickerPackList());
    }

    private Cursor getCursorForSingleStickerPack(@NonNull Uri uri) {
        final String identifier = uri.getLastPathSegment();
        Log.d(TAG, "Searching for pack: " + identifier);

        for (StickerPack stickerPack : getStickerPackList()) {
            if (identifier.equals(stickerPack.identifier)) {
                Log.d(TAG, "Found pack: " + stickerPack.identifier + " with " +
                        stickerPack.getStickers().size() + " stickers");
                return getStickerPackInfo(uri, Collections.singletonList(stickerPack));
            }
        }

        Log.e(TAG, "Pack not found: " + identifier);
        return getStickerPackInfo(uri, new ArrayList<>());
    }

    @NonNull
    private Cursor getStickerPackInfo(@NonNull Uri uri, @NonNull List<StickerPack> stickerPackList) {
        MatrixCursor cursor = new MatrixCursor(
                new String[]{
                        STICKER_PACK_IDENTIFIER_IN_QUERY,
                        STICKER_PACK_NAME_IN_QUERY,
                        STICKER_PACK_PUBLISHER_IN_QUERY,
                        STICKER_PACK_ICON_IN_QUERY,
                        ANDROID_APP_DOWNLOAD_LINK_IN_QUERY,
                        IOS_APP_DOWNLOAD_LINK_IN_QUERY,
                        PUBLISHER_EMAIL,
                        PUBLISHER_WEBSITE,
                        PRIVACY_POLICY_WEBSITE,
                        LICENSE_AGREEMENT_WEBSITE,
                        IMAGE_DATA_VERSION,
                        AVOID_CACHE,
                        ANIMATED_STICKER_PACK,
                });

        for (StickerPack stickerPack : stickerPackList) {
            MatrixCursor.RowBuilder builder = cursor.newRow();
            builder.add(stickerPack.identifier);
            builder.add(stickerPack.name);
            builder.add(stickerPack.publisher);
            builder.add(stickerPack.trayImageFile);
            builder.add(stickerPack.androidPlayStoreLink);
            builder.add(stickerPack.iosAppStoreLink);
            builder.add(stickerPack.publisherEmail);
            builder.add(stickerPack.publisherWebsite);
            builder.add(stickerPack.privacyPolicyWebsite);
            builder.add(stickerPack.licenseAgreementWebsite);
            builder.add(stickerPack.imageDataVersion);
            builder.add(stickerPack.avoidCache ? 1 : 0);
            builder.add(stickerPack.animatedStickerPack ? 1 : 0);
        }

        cursor.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), uri);
        return cursor;
    }

    @NonNull
    private Cursor getStickersForAStickerPack(@NonNull Uri uri) {
        final String identifier = uri.getLastPathSegment();
        Log.d(TAG, "Getting stickers for pack: " + identifier);

        MatrixCursor cursor = new MatrixCursor(new String[]{STICKER_FILE_NAME_IN_QUERY, STICKER_FILE_EMOJI_IN_QUERY, STICKER_FILE_ACCESSIBILITY_TEXT_IN_QUERY});
        for (StickerPack stickerPack : getStickerPackList()) {
            if (identifier.equals(stickerPack.identifier)) {
                Log.d(TAG, "Found pack, adding " + stickerPack.getStickers().size() + " stickers to cursor");
                for (Sticker sticker : stickerPack.getStickers()) {
                    cursor.addRow(new Object[]{
                            sticker.imageFileName,
                            TextUtils.join(",", sticker.emojis),
                            sticker.accessibilityText
                    });
                }
                break;
            }
        }

        cursor.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }
}