package com.example.samplestickertestingapp.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class for a custom sticker created by the user.
 * Contains information about the sticker image file, associated emojis, and creation date.
 */
public class CustomSticker implements Parcelable {
    // The name of the WebP image file for this sticker
    private final String imageFileName;

    // List of emojis associated with this sticker
    private final List<String> emojis;

    // Accessibility text describing the sticker
    private final String accessibilityText;

    // Creation timestamp
    private final long creationTimestamp;

    // Size of the sticker file in bytes
    private long size;

    // Source type (image or video)
    private final int sourceType;

    // Constants for source type
    public static final int SOURCE_TYPE_IMAGE = 1;
    public static final int SOURCE_TYPE_VIDEO = 2;

    /**
     * Constructor for creating a new custom sticker
     *
     * @param imageFileName Name of the WebP image file
     * @param emojis List of emojis associated with this sticker
     * @param accessibilityText Text description for accessibility
     * @param sourceType Type of source (image or video)
     */
    public CustomSticker(String imageFileName, List<String> emojis, String accessibilityText, int sourceType) {
        this.imageFileName = imageFileName;
        this.emojis = emojis;
        this.accessibilityText = accessibilityText;
        this.creationTimestamp = System.currentTimeMillis();
        this.sourceType = sourceType;
    }

    /**
     * Set the size of the sticker file in bytes
     *
     * @param size Size in bytes
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Get the size of the sticker file in bytes
     *
     * @return Size in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * Get the image file name
     *
     * @return File name
     */
    public String getImageFileName() {
        return imageFileName;
    }

    /**
     * Get the emojis list
     *
     * @return List of emojis
     */
    public List<String> getEmojis() {
        return emojis;
    }

    /**
     * Get the accessibility text
     *
     * @return Accessibility text
     */
    public String getAccessibilityText() {
        return accessibilityText;
    }

    /**
     * Get the creation timestamp
     *
     * @return Creation timestamp in milliseconds
     */
    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    /**
     * Get the source type
     *
     * @return Source type (image or video)
     */
    public int getSourceType() {
        return sourceType;
    }

    /**
     * Convert to standard sticker
     *
     * @return Sticker object
     */
    public Sticker toSticker() {
        Sticker sticker = new Sticker(imageFileName, emojis, accessibilityText);
        sticker.setSize(size);
        return sticker;
    }

    // Parcelable implementation
    protected CustomSticker(Parcel in) {
        imageFileName = in.readString();
        emojis = new ArrayList<>();
        in.readStringList(emojis);
        accessibilityText = in.readString();
        creationTimestamp = in.readLong();
        size = in.readLong();
        sourceType = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(imageFileName);
        dest.writeStringList(emojis);
        dest.writeString(accessibilityText);
        dest.writeLong(creationTimestamp);
        dest.writeLong(size);
        dest.writeInt(sourceType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CustomSticker> CREATOR = new Creator<CustomSticker>() {
        @Override
        public CustomSticker createFromParcel(Parcel in) {
            return new CustomSticker(in);
        }

        @Override
        public CustomSticker[] newArray(int size) {
            return new CustomSticker[size];
        }
    };
}