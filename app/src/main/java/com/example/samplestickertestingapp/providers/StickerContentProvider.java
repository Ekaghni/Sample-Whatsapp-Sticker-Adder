package com.example.samplestickertestingapp.providers;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    // URI matcher
    private UriMatcher MATCHER;

    // List of available sticker packs
    private List<StickerPack> stickerPackList;

    @Override
    public boolean onCreate() {
        final String authority = BuildConfig.CONTENT_PROVIDER_AUTHORITY;
        if (!authority.startsWith(Objects.requireNonNull(getContext()).getPackageName())) {
            throw new IllegalStateException("Content provider authority (" + authority +
                    ") should start with the app package name: " + getContext().getPackageName());
        }

        // Initialize URI matcher
        initializeUriMatcher(authority);

        // Initial load of sticker packs
        loadStickerPacks();

        return true;
    }

    /**
     * Initialize the URI matcher with all necessary patterns
     */
    private void initializeUriMatcher(String authority) {
        // Create a new matcher
        MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

        // Register basic URI patterns
        MATCHER.addURI(authority, METADATA, METADATA_CODE);
        MATCHER.addURI(authority, METADATA + "/*", METADATA_CODE_FOR_SINGLE_PACK);
        MATCHER.addURI(authority, STICKERS + "/*", STICKERS_CODE);

        // Register URI patterns for all stickers
        registerStickerUris(authority);
    }

    /**
     * Register URIs for all stickers in all packs.
     */
    private void registerStickerUris(String authority) {
        // Register URIs for each sticker
        for (StickerPack stickerPack : getStickerPackList()) {
            String packId = stickerPack.identifier;

            // Register URI for tray icon
            MATCHER.addURI(authority,
                    STICKERS_ASSET + "/" + packId + "/" + stickerPack.trayImageFile,
                    STICKER_PACK_TRAY_ICON_CODE);

            // Register URI for each sticker
            for (Sticker sticker : stickerPack.getStickers()) {
                MATCHER.addURI(authority,
                        STICKERS_ASSET + "/" + packId + "/" + sticker.imageFileName,
                        STICKERS_ASSET_CODE);
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
            }

            // Load fresh from storage
            stickerPackList = StickerPackLoader.getStickerPacks(getContext());

            // Re-initialize URI matcher with new sticker packs
            if (getContext() != null) {
                initializeUriMatcher(BuildConfig.CONTENT_PROVIDER_AUTHORITY);
            }

            Log.d(TAG, "Loaded " + stickerPackList.size() + " sticker packs");
        } catch (Exception e) {
            Log.e(TAG, "Error loading sticker packs", e);
            stickerPackList = new ArrayList<>();
        }
    }

    private List<StickerPack> getStickerPackList() {
        if (stickerPackList == null) {
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

        // Reload sticker packs to ensure fresh data
        loadStickerPacks();

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

        if (matchCode == STICKERS_ASSET_CODE || matchCode == STICKER_PACK_TRAY_ICON_CODE) {
            return getImageAsset(uri);
        }

        return null;
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
            case STICKERS_ASSET_CODE:
                return "image/webp";
            case STICKER_PACK_TRAY_ICON_CODE:
                return "image/webp";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    private Cursor getPackForAllStickerPacks(@NonNull Uri uri) {
        return getStickerPackInfo(uri, getStickerPackList());
    }

    private Cursor getCursorForSingleStickerPack(@NonNull Uri uri) {
        final String identifier = uri.getLastPathSegment();

        for (StickerPack stickerPack : getStickerPackList()) {
            if (identifier.equals(stickerPack.identifier)) {
                return getStickerPackInfo(uri, Collections.singletonList(stickerPack));
            }
        }

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
        MatrixCursor cursor = new MatrixCursor(new String[]{
                STICKER_FILE_NAME_IN_QUERY,
                STICKER_FILE_EMOJI_IN_QUERY,
                STICKER_FILE_ACCESSIBILITY_TEXT_IN_QUERY
        });

        for (StickerPack stickerPack : getStickerPackList()) {
            if (identifier.equals(stickerPack.identifier)) {
                for (Sticker sticker : stickerPack.getStickers()) {
                    cursor.addRow(new Object[]{
                            sticker.imageFileName,
                            TextUtils.join(",", sticker.emojis),
                            sticker.accessibilityText
                    });
                }
            }
        }

        cursor.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), uri);
        return cursor;
    }

    private AssetFileDescriptor getImageAsset(Uri uri) throws FileNotFoundException {
        final List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 3) {
            throw new IllegalArgumentException("Invalid URI path segments: " + uri);
        }

        final String fileName = pathSegments.get(pathSegments.size() - 1);
        final String identifier = pathSegments.get(pathSegments.size() - 2);

        Log.d(TAG, "Fetching asset: " + identifier + "/" + fileName);

        // Check that requested file is actually in the known sticker packs
        boolean fileFound = false;
        for (StickerPack stickerPack : getStickerPackList()) {
            if (identifier.equals(stickerPack.identifier)) {
                if (fileName.equals(stickerPack.trayImageFile)) {
                    fileFound = true;
                    break;
                }

                for (Sticker sticker : stickerPack.getStickers()) {
                    if (fileName.equals(sticker.imageFileName)) {
                        fileFound = true;
                        break;
                    }
                }

                if (fileFound) {
                    break;
                }
            }
        }

        if (!fileFound) {
            Log.e(TAG, "Sticker file not found in registered packs: " + uri);
            throw new FileNotFoundException("Sticker file not found: " + uri);
        }

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