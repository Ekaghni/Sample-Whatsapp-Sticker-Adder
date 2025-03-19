package com.example.samplestickertestingapp;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestLoggingListener;

import java.util.HashSet;
import java.util.Set;

/**
 * Main Application class for the Color Sticker app.
 * Initializes Fresco for image handling and other app-wide configurations.
 */
public class ColorStickerApp extends Application {
    private static final String TAG = "ColorStickerApp";

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();

        // Initialize Fresco with debug logging
        Set<RequestListener> requestListeners = new HashSet<>();
        requestListeners.add(new RequestLoggingListener());

        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setRequestListeners(requestListeners)
                .setDownsampleEnabled(true)
                .build();

        // Initialize Fresco BEFORE any UI is created
        Fresco.initialize(this, config);

        Log.d(TAG, "ColorStickerApp initialized with Fresco");
    }

    /**
     * Get application context statically
     * @return Application context
     */
    public static Context getAppContext() {
        return appContext;
    }
}