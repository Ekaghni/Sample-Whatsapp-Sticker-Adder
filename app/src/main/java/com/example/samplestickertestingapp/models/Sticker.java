package com.example.samplestickertestingapp.models;



import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class for a single sticker.
 * Contains information about the sticker image file, associated emojis, and accessibility text.
 * Implements Parcelable for easy passing between activities.
 */
public class Sticker implements Parcelable {
    // The name of the WebP image file for this sticker
    public final String imageFileName;

    // List of emojis associated with this sticker (1-3 emojis)
    public final List<String> emojis;

    // Accessibility text describing the sticker
    public final String accessibilityText;

    // Size of the sticker file in bytes
    private long size;

    /**
     * Constructor for creating a new Sticker
     *
     * @param imageFileName Name of the WebP image file
     * @param emojis List of emojis associated with this sticker
     * @param accessibilityText Text description for accessibility
     */
    public Sticker(String imageFileName, List<String> emojis, String accessibilityText) {
        this.imageFileName = imageFileName;
        this.emojis = emojis;
        this.accessibilityText = accessibilityText;
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

    // Parcelable implementation

    protected Sticker(Parcel in) {
        imageFileName = in.readString();
        emojis = new ArrayList<>();
        in.readStringList(emojis);
        accessibilityText = in.readString();
        size = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(imageFileName);
        dest.writeStringList(emojis);
        dest.writeString(accessibilityText);
        dest.writeLong(size);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Sticker> CREATOR = new Creator<Sticker>() {
        @Override
        public Sticker createFromParcel(Parcel in) {
            return new Sticker(in);
        }

        @Override
        public Sticker[] newArray(int size) {
            return new Sticker[size];
        }
    };
}
