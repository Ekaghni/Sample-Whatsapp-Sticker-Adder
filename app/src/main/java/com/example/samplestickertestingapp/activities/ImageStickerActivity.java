package com.example.samplestickertestingapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.samplestickertestingapp.R;
import com.example.samplestickertestingapp.utils.ImageUtils;

/**
 * Activity for selecting and cropping an image for sticker creation.
 */
public class ImageStickerActivity extends AppCompatActivity {
    private static final String TAG = "ImageStickerActivity";

    // Request codes
    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_IMAGE_CROP = 1002;

    // Views
    private Button selectImageButton;
    private Button nextButton;
    private ImageView previewImageView;
    private ProgressBar progressBar;

    // Image data
    private Uri selectedImageUri;
    private Bitmap selectedImageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_sticker);

        // Initialize views
        selectImageButton = findViewById(R.id.btn_select_image);
        nextButton = findViewById(R.id.btn_next);
        previewImageView = findViewById(R.id.image_preview);
        progressBar = findViewById(R.id.progress_bar);

        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.image_to_sticker);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Set click listeners
        selectImageButton.setOnClickListener(v -> openImagePicker());
        nextButton.setOnClickListener(v -> proceedToBackgroundRemoval());

        // Initially disable next button
        nextButton.setEnabled(false);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Open image picker gallery intent.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image)), REQUEST_IMAGE_PICK);
    }

    /**
     * Proceed to background removal activity.
     */
    private void proceedToBackgroundRemoval() {
        if (selectedImageBitmap == null) {
            Toast.makeText(this, R.string.please_select_image, Toast.LENGTH_SHORT).show();
            return;
        }

        // Start background removal activity
        Intent intent = new Intent(this, BackgroundRemovalActivity.class);
        if (selectedImageUri != null) {
            intent.putExtra("image_uri", selectedImageUri.toString());
        }
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_PICK) {
                handleImagePickResult(data);
            } else if (requestCode == REQUEST_IMAGE_CROP) {
                handleImageCropResult(data);
            }
        }
    }

    /**
     * Handle result from image picker.
     */
    private void handleImagePickResult(Intent data) {
        try {
            // Get selected image URI
            selectedImageUri = data.getData();
            if (selectedImageUri == null) {
                Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading
            progressBar.setVisibility(View.VISIBLE);

            // Load the image into bitmap
            selectedImageBitmap = ImageUtils.decodeBitmapFromUri(this, selectedImageUri);

            if (selectedImageBitmap != null) {
                // Display preview
                previewImageView.setImageBitmap(selectedImageBitmap);
                previewImageView.setVisibility(View.VISIBLE);

                // Enable next button
                nextButton.setEnabled(true);
            } else {
                Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
            }

            // Hide loading
            progressBar.setVisibility(View.GONE);
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle result from image cropper.
     */
    private void handleImageCropResult(Intent data) {
        // If we implement a custom cropper in the future,
        // this is where we would handle the result
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up bitmap to prevent memory leaks
        if (selectedImageBitmap != null && !selectedImageBitmap.isRecycled()) {
            selectedImageBitmap.recycle();
            selectedImageBitmap = null;
        }
    }
}