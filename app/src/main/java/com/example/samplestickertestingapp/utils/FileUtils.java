package com.example.samplestickertestingapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.Log;

import com.example.samplestickertestingapp.models.CustomSticker;
import com.example.samplestickertestingapp.models.Sticker;
import com.example.samplestickertestingapp.models.StickerPack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stickers.removeIf(s -> s.getImageFileName().equals(sticker.getImageFileName()));
            }
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
    // In FileUtils.java, modify the createCustomStickerPack method:

    // In FileUtils.java, modify the createCustomStickerPack method:

    public static StickerPack createCustomStickerPack(Context context, String packName, String publisher) {
        String packId = "custom_" + UUID.randomUUID().toString().substring(0, 8);

        // Create directory for sticker pack
        File packDirectory = new File(context.getFilesDir(), packId);
        if (!packDirectory.exists() && !packDirectory.mkdirs()) {
            Log.e(TAG, "Failed to create directory for custom sticker pack: " + packId);
            return null;
        }

        // Create tray icon (a simple colored square for now)
        String trayIconFileName = "tray_icon.webp";
        try {
            // Create a simple colored tray icon
            Bitmap trayIcon = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(trayIcon);

            // Use a green color similar to WhatsApp
            Paint paint = new Paint();
            paint.setColor(Color.rgb(37, 211, 102));
            canvas.drawRect(0, 0, 96, 96, paint);

            // Add a white border
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            canvas.drawRect(4, 4, 92, 92, paint);

            // Save the tray icon to file
            File trayIconFile = new File(packDirectory, trayIconFileName);
            try (FileOutputStream out = new FileOutputStream(trayIconFile)) {
                trayIcon.compress(Bitmap.CompressFormat.WEBP, 100, out);
            }
            trayIcon.recycle();
        } catch (IOException e) {
            Log.e(TAG, "Error creating tray icon: " + e.getMessage());
            return null;
        }

        // Create the StickerPack object
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

        // Create default stickers using StickerGenerator
        List<Sticker> defaultStickers = createDefaultStickers(context, packDirectory);
        stickerPack.setStickers(defaultStickers);

        // Save pack info to JSON file
        try {
            JSONObject packJson = new JSONObject();
            packJson.put("identifier", stickerPack.identifier);
            packJson.put("name", stickerPack.name);
            packJson.put("publisher", stickerPack.publisher);
            packJson.put("tray_image_file", stickerPack.trayImageFile);
            packJson.put("image_data_version", stickerPack.imageDataVersion);
            packJson.put("avoid_cache", stickerPack.avoidCache);
            packJson.put("animated_sticker_pack", stickerPack.animatedStickerPack);

            // Add default stickers to JSON
            JSONArray stickersArray = new JSONArray();
            for (Sticker sticker : defaultStickers) {
                JSONObject stickerJson = new JSONObject();
                stickerJson.put("image_file", sticker.imageFileName);

                JSONArray emojisArray = new JSONArray();
                for (String emoji : sticker.emojis) {
                    emojisArray.put(emoji);
                }
                stickerJson.put("emojis", emojisArray);
                stickerJson.put("accessibility_text", sticker.accessibilityText);

                stickersArray.put(stickerJson);
            }
            packJson.put("stickers", stickersArray);

            // Write the JSON to pack_info.json file
            File packInfoFile = new File(packDirectory, "pack_info.json");
            try (FileWriter writer = new FileWriter(packInfoFile)) {
                writer.write(packJson.toString(2));
            }

            Log.d(TAG, "Created sticker pack: " + packId + " with " + defaultStickers.size() +
                    " default stickers, info file and tray icon");
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Error saving pack info: " + e.getMessage());
            return null;
        }

        return stickerPack;
    }

    /**
     * Create default shape stickers for a new sticker pack
     *
     * @param context Application context
     * @param packDirectory Directory to save sticker files
     * @return List of created stickers
     */
    private static List<Sticker> createDefaultStickers(Context context, File packDirectory) {
        List<Sticker> stickers = new ArrayList<>();

        // Define the shapes we'll create (circle, square, star)
        int[] shapeTypes = {
                StickerGenerator.SHAPE_CIRCLE,
                StickerGenerator.SHAPE_SQUARE,
                StickerGenerator.SHAPE_STAR
        };

        // Create a StickerGenerator instance
        StickerGenerator generator = new StickerGenerator(context);
        Random random = new Random();

        // Create 3 stickers with different shapes
        for (int i = 0; i < 3; i++) {
            // Create sticker filename
            String filename = "default_sticker_" + (i + 1) + ".webp";

            try {
                // Create bitmap for this shape (use similar code as in StickerGenerator class)
                Bitmap stickerBitmap = createShapeSticker(shapeTypes[i], random);

                // Save bitmap to file
                File outputFile = new File(packDirectory, filename);
                try (FileOutputStream out = new FileOutputStream(outputFile)) {
                    stickerBitmap.compress(Bitmap.CompressFormat.WEBP, 100, out);
                }

                // Create sticker object with appropriate emojis (using helper methods from StickerGenerator)
                List<String> emojis = getEmojisForShape(shapeTypes[i]);
                String accessibilityText = getAccessibilityTextForShape(shapeTypes[i]);

                Sticker sticker = new Sticker(
                        filename,
                        emojis,
                        accessibilityText
                );
                sticker.setSize(outputFile.length());
                stickers.add(sticker);

                // Recycle bitmap to free memory
                stickerBitmap.recycle();
            } catch (Exception e) {
                Log.e(TAG, "Error creating default sticker: " + e.getMessage());
            }
        }

        return stickers;
    }

    /**
     * Create a colored sticker with the specified shape.
     */
    private static Bitmap createShapeSticker(int shapeType, Random random) {
        // Constants for sticker creation
        final int STICKER_SIZE = 512;
        final int WHITE_BORDER_WIDTH = 8;
        final int SHAPE_PADDING = 40;

        Bitmap bitmap = Bitmap.createBitmap(STICKER_SIZE, STICKER_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Transparent background
        canvas.drawColor(Color.TRANSPARENT);

        // Create paints
        Paint fillPaint = new Paint();
        fillPaint.setAntiAlias(true);
        fillPaint.setStyle(Paint.Style.FILL);

        Paint strokePaint = new Paint();
        strokePaint.setAntiAlias(true);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(WHITE_BORDER_WIDTH);
        strokePaint.setColor(Color.WHITE);

        // Generate random bright color
        fillPaint.setColor(getRandomBrightColor(random));

        // Draw the shape
        switch (shapeType) {
            case StickerGenerator.SHAPE_CIRCLE:
                drawCircle(canvas, fillPaint, strokePaint, STICKER_SIZE, SHAPE_PADDING);
                break;
            case StickerGenerator.SHAPE_SQUARE:
                drawSquare(canvas, fillPaint, strokePaint, STICKER_SIZE, SHAPE_PADDING);
                break;
            case StickerGenerator.SHAPE_TRIANGLE:
                drawTriangle(canvas, fillPaint, strokePaint, STICKER_SIZE, SHAPE_PADDING);
                break;
            case StickerGenerator.SHAPE_STAR:
                drawStar(canvas, fillPaint, strokePaint, STICKER_SIZE, SHAPE_PADDING);
                break;
            case StickerGenerator.SHAPE_HEART:
                drawHeart(canvas, fillPaint, strokePaint, STICKER_SIZE, SHAPE_PADDING);
                break;
        }

        return bitmap;
    }

    // Helper methods for drawing shapes
    private static void drawCircle(Canvas canvas, Paint fillPaint, Paint strokePaint,
                                   int size, int padding) {
        float radius = (size / 2f) - padding;
        canvas.drawCircle(size / 2f, size / 2f, radius, fillPaint);
        canvas.drawCircle(size / 2f, size / 2f, radius, strokePaint);
    }

    private static void drawSquare(Canvas canvas, Paint fillPaint, Paint strokePaint,
                                   int size, int padding) {
        float left = padding;
        float top = padding;
        float right = size - padding;
        float bottom = size - padding;

        canvas.drawRect(left, top, right, bottom, fillPaint);
        canvas.drawRect(left, top, right, bottom, strokePaint);
    }

    private static void drawTriangle(Canvas canvas, Paint fillPaint, Paint strokePaint,
                                     int size, int padding) {
        Path path = new Path();
        path.moveTo(size / 2f, padding);
        path.lineTo(padding, size - padding);
        path.lineTo(size - padding, size - padding);
        path.close();

        canvas.drawPath(path, fillPaint);
        canvas.drawPath(path, strokePaint);
    }

    private static void drawStar(Canvas canvas, Paint fillPaint, Paint strokePaint,
                                 int size, int padding) {
        Path path = new Path();
        float centerX = size / 2f;
        float centerY = size / 2f;
        float outerRadius = (size / 2f) - padding;
        float innerRadius = outerRadius * 0.4f;

        for (int i = 0; i < 10; i++) {
            float radius = (i % 2 == 0) ? outerRadius : innerRadius;
            double angle = Math.PI * i / 5;
            float x = (float) (centerX + radius * Math.sin(angle));
            float y = (float) (centerY - radius * Math.cos(angle));

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();

        canvas.drawPath(path, fillPaint);
        canvas.drawPath(path, strokePaint);
    }

    private static void drawHeart(Canvas canvas, Paint fillPaint, Paint strokePaint,
                                  int size, int padding) {
        Path path = new Path();
        float centerX = size / 2f;
        float centerY = size / 2f;
        float heartSize = (size / 2f) - padding;

        // Move to bottom center of heart
        path.moveTo(centerX, centerY + heartSize * 0.3f);

        // Left curve
        path.cubicTo(
                centerX - heartSize, centerY,
                centerX - heartSize, centerY - heartSize * 0.7f,
                centerX, centerY - heartSize * 0.3f
        );

        // Right curve
        path.cubicTo(
                centerX + heartSize, centerY - heartSize * 0.7f,
                centerX + heartSize, centerY,
                centerX, centerY + heartSize * 0.3f
        );

        path.close();

        canvas.drawPath(path, fillPaint);
        canvas.drawPath(path, strokePaint);
    }

    // Helper method to generate a random bright color
    private static int getRandomBrightColor(Random random) {
        // Use HSV color model to ensure vibrant colors
        float hue = random.nextFloat() * 360f; // 0-360 degrees on color wheel
        float saturation = 0.8f + random.nextFloat() * 0.2f; // 0.8-1.0 for vibrant colors
        float value = 0.8f + random.nextFloat() * 0.2f; // 0.8-1.0 for bright colors

        // Convert HSV to RGB color
        float[] hsv = new float[]{hue, saturation, value};
        return Color.HSVToColor(hsv);
    }

    // Helper methods to get emojis and accessibility text for shapes
    private static List<String> getEmojisForShape(int shapeType) {
        List<String> emojis = new ArrayList<>();

        switch (shapeType) {
            case StickerGenerator.SHAPE_CIRCLE:
                emojis.add("🔵");
                emojis.add("⚪");
                emojis.add("🔴");
                break;
            case StickerGenerator.SHAPE_SQUARE:
                emojis.add("🟥");
                emojis.add("🟩");
                emojis.add("🟦");
                break;
            case StickerGenerator.SHAPE_TRIANGLE:
                emojis.add("🔺");
                emojis.add("📐");
                emojis.add("⚠️");
                break;
            case StickerGenerator.SHAPE_STAR:
                emojis.add("⭐");
                emojis.add("✨");
                emojis.add("🌟");
                break;
            case StickerGenerator.SHAPE_HEART:
                emojis.add("❤️");
                emojis.add("💖");
                emojis.add("💓");
                break;
        }

        return emojis;
    }

    private static String getAccessibilityTextForShape(int shapeType) {
        switch (shapeType) {
            case StickerGenerator.SHAPE_CIRCLE:
                return "A colorful circle sticker";
            case StickerGenerator.SHAPE_SQUARE:
                return "A colorful square sticker";
            case StickerGenerator.SHAPE_TRIANGLE:
                return "A colorful triangle sticker";
            case StickerGenerator.SHAPE_STAR:
                return "A colorful star sticker";
            case StickerGenerator.SHAPE_HEART:
                return "A colorful heart sticker";
            default:
                return "A colorful shape sticker";
        }
    }
}