package com.example.samplestickertestingapp.activities;



import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.samplestickertestingapp.R;

/**
 * Enhanced Main Activity with options for:
 * 1. Image to Sticker
 * 2. Video to Sticker
 * 3. My Work (saved stickers)
 * 4. Original shape stickers functionality
 */
public class EnhancedMainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1002;

    private CardView cardImageToSticker;
    private CardView cardVideoToSticker;
    private CardView cardMyWork;
    private CardView cardShapeStickers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enhanced_main);

        // Initialize views
        cardImageToSticker = findViewById(R.id.card_image_to_sticker);
        cardVideoToSticker = findViewById(R.id.card_video_to_sticker);
        cardMyWork = findViewById(R.id.card_my_work);
        cardShapeStickers = findViewById(R.id.card_shape_stickers);

        // Set click listeners
        cardImageToSticker.setOnClickListener(this);
        cardVideoToSticker.setOnClickListener(this);
        cardMyWork.setOnClickListener(this);
        cardShapeStickers.setOnClickListener(this);

        // Request necessary permissions
        checkAndRequestPermissions();
    }

    /**
     * Check and request necessary permissions for the app.
     */
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                            PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE || requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.card_image_to_sticker) {
            // Check permissions before opening image sticker activity
            if (checkStoragePermission()) {
                startActivity(new Intent(this, ImageStickerActivity.class));
            }
        } else if (id == R.id.card_video_to_sticker) {
            // Video to sticker functionality (to be implemented in future)
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.card_my_work) {
            // Open saved stickers activity
            startActivity(new Intent(this, SavedStickersActivity.class));
        } else if (id == R.id.card_shape_stickers) {
            // Open original shape stickers functionality
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    /**
     * Check storage permission and request if not granted
     * @return true if permission is granted, false otherwise
     */
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                            PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        STORAGE_PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }
}