package com.example.samplestickertestingapp.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class for image processing related to stickers.
 */
public class ImageUtils {
    private static final String TAG = "ImageUtils";

    // WhatsApp sticker requirements
    public static final int STICKER_SIZE = 512;
    public static final int STICKER_QUALITY = 90;

    /**
     * Decode a Bitmap from a Uri.
     *
     * @param context Application context
     * @param uri Image Uri
     * @return Decoded Bitmap or null if error
     */
    public static Bitmap decodeBitmapFromUri(Context context, Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            InputStream inputStream = resolver.openInputStream(uri);
            if (inputStream == null) {
                return null;
            }

            // Decode the input stream to a bitmap
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            return bitmap;
        } catch (IOException e) {
            Log.e(TAG, "Error decoding bitmap from uri", e);
            return null;
        }
    }

    /**
     * Resize a bitmap to fit within sticker dimensions and maintain aspect ratio.
     *
     * @param source Source bitmap
     * @return Resized bitmap to fit within sticker dimensions
     */
    public static Bitmap resizeBitmap(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();

        float scaleFactor = Math.min(
                (float) STICKER_SIZE / width,
                (float) STICKER_SIZE / height
        );

        Matrix matrix = new Matrix();
        matrix.postScale(scaleFactor, scaleFactor);

        return Bitmap.createBitmap(source, 0, 0, width, height, matrix, true);
    }

    /**
     * Center a bitmap within a square of specified size (for stickers).
     *
     * @param source Source bitmap
     * @return Bitmap centered within a square canvas of STICKER_SIZE
     */
    public static Bitmap centerBitmap(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();

        // Create a square bitmap with transparent background
        Bitmap result = Bitmap.createBitmap(STICKER_SIZE, STICKER_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        // Calculate position to center the image
        float left = (STICKER_SIZE - width) / 2f;
        float top = (STICKER_SIZE - height) / 2f;

        // Draw the source bitmap onto the square canvas
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawBitmap(source, left, top, paint);

        return result;
    }

    /**
     * Save a bitmap as a WebP file.
     *
     * @param bitmap Bitmap to save
     * @param outputFile File to save to
     * @return true if saved successfully, false otherwise
     */
    public static boolean saveBitmapAsWebP(Bitmap bitmap, File outputFile) {
        try (OutputStream out = new FileOutputStream(outputFile)) {
            // Use lossless compression for best quality
            bitmap.compress(Bitmap.CompressFormat.WEBP, STICKER_QUALITY, out);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error saving WebP file: " + outputFile.getName(), e);
            return false;
        }
    }

    /**
     * Process and save a bitmap as a sticker.
     * Applies necessary transformations to meet WhatsApp sticker requirements.
     *
     * @param context Application context
     * @param bitmap Source bitmap
     * @param fileName Filename to save the sticker as
     * @return File object pointing to the saved sticker file or null if error
     */
    public static File saveAsStickerFile(Context context, Bitmap bitmap, String fileName) {
        try {
            // Ensure the directory exists
            File directory = FileUtils.getCustomStickersDirectory(context);
            if (directory == null) {
                return null;
            }

            // Process bitmap
            Bitmap resized = resizeBitmap(bitmap);
            Bitmap centered = centerBitmap(resized);
            if (resized != bitmap) {
                resized.recycle(); // Free memory if this is a new bitmap
            }

            // Save as WebP
            File outputFile = new File(directory, fileName);
            if (saveBitmapAsWebP(centered, outputFile)) {
                centered.recycle();
                return outputFile;
            } else {
                centered.recycle();
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving sticker file", e);
            return null;
        }
    }
}