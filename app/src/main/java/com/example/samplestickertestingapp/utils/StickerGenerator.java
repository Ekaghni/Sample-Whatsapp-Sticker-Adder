package com.example.samplestickertestingapp.utils;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import com.example.samplestickertestingapp.models.Sticker;
import com.example.samplestickertestingapp.models.StickerPack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility class for generating colorful stickers programmatically.
 */
public class StickerGenerator {
    private static final String TAG = "StickerGenerator";

    // WhatsApp sticker requirements
    private static final int STICKER_SIZE = 512;
    private static final int TRAY_ICON_SIZE = 96;
    private static final int WHITE_BORDER_WIDTH = 8;
    private static final int SHAPE_PADDING = 40;

    // Maximum number of stickers in a pack (WhatsApp limit is 30)
    private static final int MAX_STICKERS_IN_PACK = 15;

    // Shape types
    public static final int SHAPE_CIRCLE = 0;
    public static final int SHAPE_SQUARE = 1;
    public static final int SHAPE_TRIANGLE = 2;
    public static final int SHAPE_STAR = 3;
    public static final int SHAPE_HEART = 4;

    private final Context context;
    private final Random random;

    public StickerGenerator(Context context) {
        this.context = context;
        this.random = new Random();
    }

    /**
     * Creates a complete sticker pack with randomly colored shapes.
     *
     * @param packId Unique identifier for the sticker pack
     * @param packName Display name for the sticker pack
     * @param packPublisher Publisher name
     * @return Created StickerPack object
     */
    public StickerPack createStickerPack(String packId, String packName, String packPublisher) {
        // Create directory for sticker pack if it doesn't exist
        File packDirectory = new File(context.getFilesDir(), packId);
        if (!packDirectory.exists()) {
            if (!packDirectory.mkdirs()) {
                Log.e(TAG, "Failed to create directory for sticker pack: " + packId);
                return null;
            }
        }

        // Create tray icon and save it
        String trayIconFileName = "tray_icon.webp";
        Bitmap trayIcon = createTrayIcon();
        if (!saveBitmapAsWebP(trayIcon, new File(packDirectory, trayIconFileName))) {
            Log.e(TAG, "Failed to save tray icon");
            return null;
        }

        // Create sticker pack object
        StickerPack stickerPack = new StickerPack(
                packId,
                packName,
                packPublisher,
                trayIconFileName,
                "",  // publisherEmail
                "",  // publisherWebsite
                "",  // privacyPolicyWebsite
                "",  // licenseAgreementWebsite
                "1", // imageDataVersion
                false, // avoidCache
                false  // animatedStickerPack
        );

        // Create stickers
        List<Sticker> stickers = createStickers(packDirectory);
        stickerPack.setStickers(stickers);

        // Save pack info to JSON file in the package directory
        savePackInfoToJson(packDirectory, stickerPack);

        return stickerPack;
    }

    /**
     * Save sticker pack info to a JSON file in the package directory.
     * This helps with loading the pack later.
     */
    private void savePackInfoToJson(File directory, StickerPack pack) {
        try {
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

                if (sticker.accessibilityText != null) {
                    stickerJson.put("accessibility_text", sticker.accessibilityText);
                }

                stickersJson.put(stickerJson);
            }
            packJson.put("stickers", stickersJson);

            // Write to file
            File infoFile = new File(directory, "pack_info.json");
            try (FileWriter writer = new FileWriter(infoFile)) {
                writer.write(packJson.toString(2));
            }

            Log.d(TAG, "Saved pack info to " + infoFile.getAbsolutePath());
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Error saving pack info JSON", e);
        }
    }

    /**
     * Creates a set of stickers with various shapes and colors.
     *
     * @param directory Directory to save sticker files in
     * @return List of created Sticker objects
     */
    private List<Sticker> createStickers(File directory) {
        List<Sticker> stickers = new ArrayList<>();

        // Generate stickers with different shapes
        for (int i = 0; i < MAX_STICKERS_IN_PACK; i++) {
            // Cycle through different shapes
            int shapeType = i % 5;

            // Create sticker filename
            String filename = "sticker_" + (i + 1) + ".webp";

            // Create bitmap for this shape
            Bitmap stickerBitmap = createColorSticker(shapeType);

            // Save bitmap to file
            File outputFile = new File(directory, filename);
            if (saveBitmapAsWebP(stickerBitmap, outputFile)) {
                // Create sticker object with appropriate emojis
                Sticker sticker = new Sticker(
                        filename,
                        getEmojisForShape(shapeType),
                        getAccessibilityTextForShape(shapeType)
                );
                sticker.setSize(outputFile.length());
                stickers.add(sticker);
            } else {
                Log.e(TAG, "Failed to save sticker: " + filename);
            }

            // Recycle bitmap to free memory
            stickerBitmap.recycle();
        }

        return stickers;
    }

    /**
     * Creates a tray icon for the sticker pack.
     *
     * @return Bitmap of the tray icon
     */
    private Bitmap createTrayIcon() {
        Bitmap bitmap = Bitmap.createBitmap(TRAY_ICON_SIZE, TRAY_ICON_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Transparent background
        canvas.drawColor(Color.TRANSPARENT);

        // Draw a colorful pattern
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Draw a rainbow gradient background
        int[] colors = {
                Color.RED, Color.parseColor("#FF7F00"), Color.YELLOW,
                Color.GREEN, Color.BLUE, Color.parseColor("#4B0082"), Color.parseColor("#9400D3")
        };

        for (int i = 0; i < 7; i++) {
            paint.setColor(colors[i]);
            float left = TRAY_ICON_SIZE * i / 7f;
            float right = TRAY_ICON_SIZE * (i + 1) / 7f;
            canvas.drawRect(left, 0, right, TRAY_ICON_SIZE, paint);
        }

        // Add a white circular border
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        canvas.drawCircle(TRAY_ICON_SIZE / 2f, TRAY_ICON_SIZE / 2f,
                TRAY_ICON_SIZE / 2f - 4, paint);

        return bitmap;
    }

    /**
     * Creates a colored sticker with the specified shape.
     *
     * @param shapeType Type of shape to create
     * @return Bitmap of the sticker
     */
    private Bitmap createColorSticker(int shapeType) {
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

        // Generate random colors
        int mainColor = getRandomBrightColor();
        fillPaint.setColor(mainColor);

        // Draw the shape
        switch (shapeType) {
            case SHAPE_CIRCLE:
                drawCircle(canvas, fillPaint, strokePaint);
                break;
            case SHAPE_SQUARE:
                drawSquare(canvas, fillPaint, strokePaint);
                break;
            case SHAPE_TRIANGLE:
                drawTriangle(canvas, fillPaint, strokePaint);
                break;
            case SHAPE_STAR:
                drawStar(canvas, fillPaint, strokePaint);
                break;
            case SHAPE_HEART:
                drawHeart(canvas, fillPaint, strokePaint);
                break;
        }

        return bitmap;
    }

    // Draw methods for different shapes

    private void drawCircle(Canvas canvas, Paint fillPaint, Paint strokePaint) {
        float radius = (STICKER_SIZE / 2f) - SHAPE_PADDING;
        canvas.drawCircle(STICKER_SIZE / 2f, STICKER_SIZE / 2f, radius, fillPaint);
        canvas.drawCircle(STICKER_SIZE / 2f, STICKER_SIZE / 2f, radius, strokePaint);
    }

    private void drawSquare(Canvas canvas, Paint fillPaint, Paint strokePaint) {
        float left = SHAPE_PADDING;
        float top = SHAPE_PADDING;
        float right = STICKER_SIZE - SHAPE_PADDING;
        float bottom = STICKER_SIZE - SHAPE_PADDING;

        canvas.drawRect(left, top, right, bottom, fillPaint);
        canvas.drawRect(left, top, right, bottom, strokePaint);
    }

    private void drawTriangle(Canvas canvas, Paint fillPaint, Paint strokePaint) {
        Path path = new Path();
        path.moveTo(STICKER_SIZE / 2f, SHAPE_PADDING);
        path.lineTo(SHAPE_PADDING, STICKER_SIZE - SHAPE_PADDING);
        path.lineTo(STICKER_SIZE - SHAPE_PADDING, STICKER_SIZE - SHAPE_PADDING);
        path.close();

        canvas.drawPath(path, fillPaint);
        canvas.drawPath(path, strokePaint);
    }

    private void drawStar(Canvas canvas, Paint fillPaint, Paint strokePaint) {
        Path path = new Path();
        float centerX = STICKER_SIZE / 2f;
        float centerY = STICKER_SIZE / 2f;
        float outerRadius = (STICKER_SIZE / 2f) - SHAPE_PADDING;
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

    private void drawHeart(Canvas canvas, Paint fillPaint, Paint strokePaint) {
        Path path = new Path();
        float centerX = STICKER_SIZE / 2f;
        float centerY = STICKER_SIZE / 2f;
        float size = (STICKER_SIZE / 2f) - SHAPE_PADDING;

        // Move to bottom center of heart
        path.moveTo(centerX, centerY + size * 0.3f);

        // Left curve
        path.cubicTo(
                centerX - size, centerY,
                centerX - size, centerY - size * 0.7f,
                centerX, centerY - size * 0.3f
        );

        // Right curve
        path.cubicTo(
                centerX + size, centerY - size * 0.7f,
                centerX + size, centerY,
                centerX, centerY + size * 0.3f
        );

        path.close();

        canvas.drawPath(path, fillPaint);
        canvas.drawPath(path, strokePaint);
    }

    // Helper methods

    /**
     * Generates a random bright color.
     *
     * @return Random bright color as an ARGB int
     */
    private int getRandomBrightColor() {
        // Use HSV color model to ensure vibrant colors
        float hue = random.nextFloat() * 360f; // 0-360 degrees on color wheel
        float saturation = 0.8f + random.nextFloat() * 0.2f; // 0.8-1.0 for vibrant colors
        float value = 0.8f + random.nextFloat() * 0.2f; // 0.8-1.0 for bright colors

        // Convert HSV to RGB color
        float[] hsv = new float[]{hue, saturation, value};
        return Color.HSVToColor(hsv);
    }

    /**
     * Returns appropriate emoji tags for a shape.
     *
     * @param shapeType Type of shape
     * @return List of emojis relevant to the shape
     */
    private List<String> getEmojisForShape(int shapeType) {
        List<String> emojis = new ArrayList<>();

        switch (shapeType) {
            case SHAPE_CIRCLE:
                emojis.add("🔵");
                emojis.add("⚪");
                emojis.add("🔴");
                break;
            case SHAPE_SQUARE:
                emojis.add("🟥");
                emojis.add("🟩");
                emojis.add("🟦");
                break;
            case SHAPE_TRIANGLE:
                emojis.add("🔺");
                emojis.add("📐");
                emojis.add("⚠️");
                break;
            case SHAPE_STAR:
                emojis.add("⭐");
                emojis.add("✨");
                emojis.add("🌟");
                break;
            case SHAPE_HEART:
                emojis.add("❤️");
                emojis.add("💖");
                emojis.add("💓");
                break;
        }

        return emojis;
    }

    /**
     * Returns accessibility text for a shape.
     *
     * @param shapeType Type of shape
     * @return Accessibility description text
     */
    private String getAccessibilityTextForShape(int shapeType) {
        switch (shapeType) {
            case SHAPE_CIRCLE:
                return "A colorful circle sticker";
            case SHAPE_SQUARE:
                return "A colorful square sticker";
            case SHAPE_TRIANGLE:
                return "A colorful triangle sticker";
            case SHAPE_STAR:
                return "A colorful star sticker";
            case SHAPE_HEART:
                return "A colorful heart sticker";
            default:
                return "A colorful shape sticker";
        }
    }

    /**
     * Saves a bitmap as a WebP file.
     *
     * @param bitmap Bitmap to save
     * @param outputFile File to save to
     * @return true if saved successfully, false otherwise
     */
    private boolean saveBitmapAsWebP(Bitmap bitmap, File outputFile) {
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            // Use lossless compression for best quality
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, out);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error saving WebP file: " + outputFile.getName(), e);
            return false;
        }
    }
}
