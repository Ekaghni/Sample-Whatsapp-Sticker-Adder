package com.example.samplestickertestingapp.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom ImageView for background removal using brush strokes.
 * Supports erasing and redrawing with opacity control and undo/redo functionality.
 */
public class BrushImageView extends AppCompatImageView {
    private static final String TAG = "BrushImageView";

    // Constants
    private static final int DEFAULT_BRUSH_SIZE = 30;
    private static final int DEFAULT_OPACITY = 255; // Full opacity
    private static final int MAX_STEPS = 20; // Maximum undo/redo steps

    // Drawing tools
    private Paint brushPaint;
    private Path brushPath;
    private Canvas canvasBuffer;
    private Bitmap bufferBitmap;
    private Bitmap originalBitmap;
    private Bitmap resultBitmap;

    // State tracking
    private float lastTouchX;
    private float lastTouchY;
    private boolean isErasing = true; // Default to eraser mode
    private int brushSize = DEFAULT_BRUSH_SIZE;
    private int opacity = DEFAULT_OPACITY;

    // Undo/redo history
    private List<Bitmap> undoSteps;
    private List<Bitmap> redoSteps;

    public BrushImageView(@NonNull Context context) {
        super(context);
        init();
    }

    public BrushImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BrushImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize paint
        brushPaint = new Paint();
        brushPaint.setAntiAlias(true);
        brushPaint.setStyle(Paint.Style.STROKE);
        brushPaint.setStrokeJoin(Paint.Join.ROUND);
        brushPaint.setStrokeCap(Paint.Cap.ROUND);
        brushPaint.setStrokeWidth(brushSize);

        // Initialize path
        brushPath = new Path();

        // Initialize undo/redo lists
        undoSteps = new ArrayList<>();
        redoSteps = new ArrayList<>();
    }

    /**
     * Set the source bitmap for editing.
     *
     * @param bitmap Bitmap to edit
     */
    public void setSourceBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Null bitmap provided to BrushImageView");
            return;
        }

        // Store original bitmap
        originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // Create buffer bitmap to draw on
        bufferBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        canvasBuffer = new Canvas(bufferBitmap);
        canvasBuffer.drawBitmap(originalBitmap, 0, 0, null);

        // Clear history
        clearHistory();
        // Save initial state
        saveState();

        // Set image
        updateImageView();
    }

    /**
     * Update the displayed image from the buffer.
     */
    private void updateImageView() {
        resultBitmap = Bitmap.createBitmap(bufferBitmap.getWidth(), bufferBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);

        // Draw checkerboard pattern to show transparency
        Paint checkerPaint = new Paint();
        checkerPaint.setColor(Color.LTGRAY);
        for (int x = 0; x < resultBitmap.getWidth(); x += 20) {
            for (int y = 0; y < resultBitmap.getHeight(); y += 20) {
                if ((x / 20 + y / 20) % 2 == 0) {
                    canvas.drawRect(x, y, x + 20, y + 20, checkerPaint);
                }
            }
        }

        // Draw the actual image on top
        canvas.drawBitmap(bufferBitmap, 0, 0, null);
        setImageBitmap(resultBitmap);
    }

    /**
     * Set brush size.
     *
     * @param size Brush size in pixels
     */
    public void setBrushSize(int size) {
        this.brushSize = size;
        brushPaint.setStrokeWidth(size);
    }

    /**
     * Set brush opacity.
     *
     * @param opacity Opacity value (0-255)
     */
    public void setBrushOpacity(int opacity) {
        this.opacity = opacity;
        updateBrushMode();
    }

    /**
     * Set brush mode to eraser or brush.
     *
     * @param erasing true for eraser mode, false for brush mode
     */
    public void setErasing(boolean erasing) {
        isErasing = erasing;
        updateBrushMode();
    }

    /**
     * Update brush paint based on current mode and opacity.
     */
    private void updateBrushMode() {
        brushPaint.setAlpha(opacity);

        if (isErasing) {
            // Eraser mode - use CLEAR mode for transparency
            brushPaint.setColor(Color.TRANSPARENT);
            brushPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        } else {
            // Brush mode - restore original pixels
            // This isn't ideal for actual pixel restoration, but serves as a visual indicator
            brushPaint.setXfermode(null);
            brushPaint.setColor(Color.RED); // Visual indicator for redraw mode
        }
    }

    /**
     * Save current state for undo history.
     */
    private void saveState() {
        // Add current state to undo history
        Bitmap copy = bufferBitmap.copy(Bitmap.Config.ARGB_8888, true);
        undoSteps.add(copy);

        // Limit history size
        if (undoSteps.size() > MAX_STEPS) {
            Bitmap removed = undoSteps.remove(0);
            removed.recycle();
        }

        // Clear redo history
        clearRedoHistory();
    }

    /**
     * Clear undo/redo history.
     */
    private void clearHistory() {
        // Recycle bitmaps to free memory
        for (Bitmap bitmap : undoSteps) {
            bitmap.recycle();
        }
        undoSteps.clear();

        clearRedoHistory();
    }

    /**
     * Clear redo history.
     */
    private void clearRedoHistory() {
        // Recycle bitmaps to free memory
        for (Bitmap bitmap : redoSteps) {
            bitmap.recycle();
        }
        redoSteps.clear();
    }

    /**
     * Undo last action.
     *
     * @return true if undo was successful, false otherwise
     */
    public boolean undo() {
        if (undoSteps.size() <= 1) {
            // Cannot undo if there's only initial state or less
            return false;
        }

        // Move current state to redo history
        Bitmap current = undoSteps.remove(undoSteps.size() - 1);
        redoSteps.add(current);

        // Restore previous state
        Bitmap previous = undoSteps.get(undoSteps.size() - 1);
        bufferBitmap = previous.copy(Bitmap.Config.ARGB_8888, true);
        canvasBuffer = new Canvas(bufferBitmap);

        // Update display
        updateImageView();
        return true;
    }

    /**
     * Redo last undone action.
     *
     * @return true if redo was successful, false otherwise
     */
    public boolean redo() {
        if (redoSteps.isEmpty()) {
            // Nothing to redo
            return false;
        }

        // Get last redo state
        Bitmap redoState = redoSteps.remove(redoSteps.size() - 1);

        // Save current state to undo history
        undoSteps.add(redoState);

        // Restore redo state
        bufferBitmap = redoState.copy(Bitmap.Config.ARGB_8888, true);
        canvasBuffer = new Canvas(bufferBitmap);

        // Update display
        updateImageView();
        return true;
    }

    /**
     * Get the resulting bitmap after editing.
     *
     * @return The edited bitmap with transparent background
     */
    public Bitmap getResultBitmap() {
        return bufferBitmap;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        // Convert touch coordinates to bitmap coordinates
        Matrix inverse = new Matrix();
        getImageMatrix().invert(inverse);
        float[] touchPoint = {x, y};
        inverse.mapPoints(touchPoint);
        x = touchPoint[0];
        y = touchPoint[1];

        // Handle touch events
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleTouchStart(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                handleTouchMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
                handleTouchEnd();
                break;
            default:
                return false;
        }

        // Force redraw
        invalidate();
        return true;
    }

    /**
     * Handle touch start event.
     */
    private void handleTouchStart(float x, float y) {
        // Start new path
        brushPath.reset();
        brushPath.moveTo(x, y);
        lastTouchX = x;
        lastTouchY = y;
    }

    /**
     * Handle touch move event.
     */
    private void handleTouchMove(float x, float y) {
        // Add point to path
        float dx = Math.abs(x - lastTouchX);
        float dy = Math.abs(y - lastTouchY);

        if (dx >= 4 || dy >= 4) {
            // Only add significant movements to the path
            brushPath.quadTo(lastTouchX, lastTouchY, (x + lastTouchX) / 2, (y + lastTouchY) / 2);
            lastTouchX = x;
            lastTouchY = y;

            // Draw path to buffer
            canvasBuffer.drawPath(brushPath, brushPaint);

            // Reset path for next segment
            brushPath.reset();
            brushPath.moveTo(lastTouchX, lastTouchY);

            // Update display
            updateImageView();
        }
    }

    /**
     * Handle touch end event.
     */
    private void handleTouchEnd() {
        // Complete the path
        canvasBuffer.drawPath(brushPath, brushPaint);
        brushPath.reset();

        // Save state for undo
        saveState();

        // Update display
        updateImageView();
    }

    /**
     * Clean up resources when the view is detached.
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Recycle bitmaps to prevent memory leaks
        if (originalBitmap != null) {
            originalBitmap.recycle();
            originalBitmap = null;
        }

        if (bufferBitmap != null) {
            bufferBitmap.recycle();
            bufferBitmap = null;
        }

        if (resultBitmap != null) {
            resultBitmap.recycle();
            resultBitmap = null;
        }

        clearHistory();
    }
}