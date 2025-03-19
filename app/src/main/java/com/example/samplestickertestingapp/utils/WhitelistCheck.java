package com.example.samplestickertestingapp.utils;



import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.example.samplestickertestingapp.BuildConfig;


/**
 * Utility class to check if a sticker pack has been added to WhatsApp.
 * This is used to display different UI based on whether a pack is already added.
 */
public class WhitelistCheck {
    private static final String TAG = "WhitelistCheck";

    // Package names for WhatsApp apps
    public static final String CONSUMER_WHATSAPP_PACKAGE_NAME = "com.whatsapp";
    public static final String SMB_WHATSAPP_PACKAGE_NAME = "com.whatsapp.w4b";

    // Content provider path for checking whitelist status
    private static final String CONTENT_PROVIDER = ".provider.sticker_whitelist_check";
    private static final String QUERY_PATH = "is_whitelisted";
    private static final String QUERY_RESULT_COLUMN_NAME = "result";

    // Query parameters
    private static final String AUTHORITY_QUERY_PARAM = "authority";
    private static final String IDENTIFIER_QUERY_PARAM = "identifier";

    // Your app's content provider authority
    private static final String STICKER_APP_AUTHORITY = BuildConfig.CONTENT_PROVIDER_AUTHORITY;

    /**
     * Checks if a sticker pack is added to any WhatsApp app.
     *
     * @param context Application context
     * @param identifier The sticker pack identifier to check
     * @return true if the pack is added to any WhatsApp app
     */
    public static boolean isWhitelisted(Context context, String identifier) {
        try {
            Log.d("WhitelistCheck", "Checking if pack is whitelisted: " + identifier);

            // If WhatsApp isn't installed, return false
            if (!isWhatsAppConsumerAppInstalled(context) && !isWhatsAppSmbAppInstalled(context)) {
                Log.d("WhitelistCheck", "WhatsApp isn't installed");
                return false;
            }

            // Check consumer WhatsApp
            boolean consumerResult = false;
            if (isWhatsAppConsumerAppInstalled(context)) {
                consumerResult = isStickerPackWhitelistedInWhatsAppConsumer(context, identifier);
                Log.d("WhitelistCheck", "Consumer WhatsApp result: " + consumerResult);
            }

            // Check business WhatsApp
            boolean smbResult = false;
            if (isWhatsAppSmbAppInstalled(context)) {
                smbResult = isStickerPackWhitelistedInWhatsAppSmb(context, identifier);
                Log.d("WhitelistCheck", "Business WhatsApp result: " + smbResult);
            }

            boolean result = consumerResult || smbResult;
            Log.d("WhitelistCheck", "Final result for " + identifier + ": " + result);
            return result;
        } catch (Exception e) {
            Log.e("WhitelistCheck", "Error checking whitelist status: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if a sticker pack is added to consumer WhatsApp.
     *
     * @param context Application context
     * @param identifier The sticker pack identifier to check
     * @return true if the pack is added to consumer WhatsApp
     */
    public static boolean isStickerPackWhitelistedInWhatsAppConsumer(Context context, String identifier) {
        return isWhitelistedFromProvider(context, identifier, CONSUMER_WHATSAPP_PACKAGE_NAME);
    }

    /**
     * Checks if a sticker pack is added to WhatsApp Business.
     *
     * @param context Application context
     * @param identifier The sticker pack identifier to check
     * @return true if the pack is added to WhatsApp Business
     */
    public static boolean isStickerPackWhitelistedInWhatsAppSmb(Context context, String identifier) {
        return isWhitelistedFromProvider(context, identifier, SMB_WHATSAPP_PACKAGE_NAME);
    }

    /**
     * Internal method to query a specific WhatsApp app's content provider.
     *
     * @param context Application context
     * @param identifier The sticker pack identifier to check
     * @param whatsappPackageName The WhatsApp package name to check
     * @return true if the pack is added to that WhatsApp app
     */
    private static boolean isWhitelistedFromProvider(Context context, String identifier, String whatsappPackageName) {
        final PackageManager packageManager = context.getPackageManager();
        if (isPackageInstalled(whatsappPackageName, packageManager)) {
            final String whatsappProviderAuthority = whatsappPackageName + CONTENT_PROVIDER;
            final Uri queryUri = new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(whatsappProviderAuthority)
                    .appendPath(QUERY_PATH)
                    .appendQueryParameter(AUTHORITY_QUERY_PARAM, STICKER_APP_AUTHORITY)
                    .appendQueryParameter(IDENTIFIER_QUERY_PARAM, identifier)
                    .build();

            Log.d("WhitelistCheck", "Querying WhatsApp with URI: " + queryUri);

            try (final Cursor cursor = context.getContentResolver().query(queryUri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    final int columnIndex = cursor.getColumnIndex(QUERY_RESULT_COLUMN_NAME);
                    if (columnIndex != -1) {
                        final int whiteListResult = cursor.getInt(columnIndex);
                        Log.d("WhitelistCheck", "WhatsApp returned result: " + whiteListResult + " for package: " + whatsappPackageName);
                        return whiteListResult == 1;
                    } else {
                        Log.e("WhitelistCheck", "Column not found in cursor: " + QUERY_RESULT_COLUMN_NAME);
                    }
                } else {
                    Log.d("WhitelistCheck", "Cursor is null or empty for package: " + whatsappPackageName);
                }
            } catch (Exception e) {
                Log.e("WhitelistCheck", "Error querying WhatsApp (" + whatsappPackageName + "): " + e.getMessage(), e);
            }
        } else {
            Log.d("WhitelistCheck", "Package not installed: " + whatsappPackageName);
        }

        // If we get here, something went wrong or the pack isn't whitelisted
        return false;
    }

    /**
     * Checks if the consumer WhatsApp app is installed.
     *
     * @param context Application context
     * @return true if consumer WhatsApp is installed
     */
    public static boolean isWhatsAppConsumerAppInstalled(Context context) {
        return isPackageInstalled(CONSUMER_WHATSAPP_PACKAGE_NAME, context.getPackageManager());
    }

    /**
     * Checks if the WhatsApp Business app is installed.
     *
     * @param context Application context
     * @return true if WhatsApp Business is installed
     */
    public static boolean isWhatsAppSmbAppInstalled(Context context) {
        return isPackageInstalled(SMB_WHATSAPP_PACKAGE_NAME, context.getPackageManager());
    }

    /**
     * Checks if a package is installed and enabled.
     *
     * @param packageName Package name to check
     * @param packageManager Package manager instance
     * @return true if the package is installed and enabled
     */
    public static boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            final ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return applicationInfo != null && applicationInfo.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
