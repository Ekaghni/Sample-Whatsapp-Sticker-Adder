package com.example.samplestickertestingapp.models;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class for a sticker pack.
 * Contains information about the sticker pack and its stickers.
 * Implements Parcelable for easy passing between activities.
 */
public class StickerPack implements Parcelable {
    // Unique identifier for the sticker pack
    public final String identifier;

    // Display name of the sticker pack
    public final String name;

    // Publisher name
    public final String publisher;

    // Tray icon file name (96x96 WebP or PNG)
    public final String trayImageFile;

    // Contact email for the publisher
    public final String publisherEmail;

    // Publisher website URL
    public final String publisherWebsite;

    // Privacy policy URL
    public final String privacyPolicyWebsite;

    // License agreement URL
    public final String licenseAgreementWebsite;

    // Version identifier for sticker data
    public final String imageDataVersion;

    // Flag to indicate if stickers should not be cached by WhatsApp
    public final boolean avoidCache;

    // Flag to indicate if pack contains animated stickers
    public final boolean animatedStickerPack;

    // iOS App Store link
    public String iosAppStoreLink;

    // Android Play Store link
    public String androidPlayStoreLink;

    // List of stickers in this pack
    private List<Sticker> stickers;

    // Total size of all stickers in bytes
    private long totalSize;

    // Whether this pack is already added to WhatsApp
    private boolean isWhitelisted;

    /**
     * Constructor for a new sticker pack
     */
    public StickerPack(
            String identifier,
            String name,
            String publisher,
            String trayImageFile,
            String publisherEmail,
            String publisherWebsite,
            String privacyPolicyWebsite,
            String licenseAgreementWebsite,
            String imageDataVersion,
            boolean avoidCache,
            boolean animatedStickerPack) {
        this.identifier = identifier;
        this.name = name;
        this.publisher = publisher;
        this.trayImageFile = trayImageFile;
        this.publisherEmail = publisherEmail;
        this.publisherWebsite = publisherWebsite;
        this.privacyPolicyWebsite = privacyPolicyWebsite;
        this.licenseAgreementWebsite = licenseAgreementWebsite;
        this.imageDataVersion = imageDataVersion;
        this.avoidCache = avoidCache;
        this.animatedStickerPack = animatedStickerPack;
    }

    /**
     * Set the list of stickers for this pack and calculate total size
     *
     * @param stickers List of stickers
     */
    public void setStickers(List<Sticker> stickers) {
        this.stickers = stickers;
        this.totalSize = 0;
        for (Sticker sticker : stickers) {
            this.totalSize += sticker.getSize();
        }
    }

    /**
     * Get the list of stickers in this pack
     *
     * @return List of stickers
     */
    public List<Sticker> getStickers() {
        return stickers;
    }

    /**
     * Get the total size of all stickers in bytes
     *
     * @return Total size in bytes
     */
    public long getTotalSize() {
        return totalSize;
    }

    /**
     * Set whether this pack is added to WhatsApp
     *
     * @param isWhitelisted true if added to WhatsApp
     */
    public void setIsWhitelisted(boolean isWhitelisted) {
        this.isWhitelisted = isWhitelisted;
    }

    /**
     * Check if this pack is added to WhatsApp
     *
     * @return true if added to WhatsApp
     */
    public boolean getIsWhitelisted() {
        return isWhitelisted;
    }

    /**
     * Set the Android Play Store link
     *
     * @param androidPlayStoreLink Play Store URL
     */
    public void setAndroidPlayStoreLink(String androidPlayStoreLink) {
        this.androidPlayStoreLink = androidPlayStoreLink;
    }

    /**
     * Set the iOS App Store link
     *
     * @param iosAppStoreLink App Store URL
     */
    public void setIosAppStoreLink(String iosAppStoreLink) {
        this.iosAppStoreLink = iosAppStoreLink;
    }

    // Parcelable implementation

    protected StickerPack(Parcel in) {
        identifier = in.readString();
        name = in.readString();
        publisher = in.readString();
        trayImageFile = in.readString();
        publisherEmail = in.readString();
        publisherWebsite = in.readString();
        privacyPolicyWebsite = in.readString();
        licenseAgreementWebsite = in.readString();
        imageDataVersion = in.readString();
        avoidCache = in.readByte() != 0;
        animatedStickerPack = in.readByte() != 0;
        iosAppStoreLink = in.readString();
        androidPlayStoreLink = in.readString();
        stickers = in.createTypedArrayList(Sticker.CREATOR);
        totalSize = in.readLong();
        isWhitelisted = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(identifier);
        dest.writeString(name);
        dest.writeString(publisher);
        dest.writeString(trayImageFile);
        dest.writeString(publisherEmail);
        dest.writeString(publisherWebsite);
        dest.writeString(privacyPolicyWebsite);
        dest.writeString(licenseAgreementWebsite);
        dest.writeString(imageDataVersion);
        dest.writeByte((byte) (avoidCache ? 1 : 0));
        dest.writeByte((byte) (animatedStickerPack ? 1 : 0));
        dest.writeString(iosAppStoreLink);
        dest.writeString(androidPlayStoreLink);
        dest.writeTypedList(stickers);
        dest.writeLong(totalSize);
        dest.writeByte((byte) (isWhitelisted ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<StickerPack> CREATOR = new Creator<StickerPack>() {
        @Override
        public StickerPack createFromParcel(Parcel in) {
            return new StickerPack(in);
        }

        @Override
        public StickerPack[] newArray(int size) {
            return new StickerPack[size];
        }
    };
}
