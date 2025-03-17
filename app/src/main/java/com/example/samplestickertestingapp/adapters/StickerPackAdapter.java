package com.example.samplestickertestingapp.adapters;



import android.content.Context;
import android.graphics.BitmapFactory;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.samplestickertestingapp.R;
import com.example.samplestickertestingapp.models.Sticker;
import com.example.samplestickertestingapp.models.StickerPack;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * Adapter for displaying a list of sticker packs in a RecyclerView.
 * Simplified to use standard ImageView instead of Fresco views.
 */
public class StickerPackAdapter extends RecyclerView.Adapter<StickerPackAdapter.ViewHolder> {
    private static final String TAG = "StickerPackAdapter";
    private final Context context;
    private final List<StickerPack> stickerPacks;
    private final OnStickerPackClickListener listener;

    // Maximum number of stickers to preview in list
    private static final int MAX_PREVIEW_STICKERS = 3;

    /**
     * Interface for handling sticker pack clicks.
     */
    public interface OnStickerPackClickListener {
        void onStickerPackClick(StickerPack stickerPack);
        void onAddButtonClick(StickerPack stickerPack);
    }

    /**
     * Constructor for StickerPackAdapter.
     *
     * @param context Context
     * @param stickerPacks List of sticker packs to display
     * @param listener Click listener
     */
    public StickerPackAdapter(Context context, List<StickerPack> stickerPacks, OnStickerPackClickListener listener) {
        this.context = context;
        this.stickerPacks = stickerPacks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sticker_pack, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StickerPack pack = stickerPacks.get(position);

        Log.d(TAG, "Binding sticker pack: " + pack.identifier + " - " + pack.name);

        // Set pack name and publisher
        holder.packNameText.setText(pack.name);
        holder.publisherText.setText(pack.publisher);

        // Set pack size
        String packSize = Formatter.formatShortFileSize(context, pack.getTotalSize());
        holder.packSizeText.setText(packSize);

        // Set sticker count
        int stickerCount = pack.getStickers().size();
        holder.stickerCountText.setText(context.getResources().getQuantityString(
                R.plurals.sticker_count, stickerCount, stickerCount));

        // Set tray image (simplified using standard drawables)
        try {
            File trayFile = new File(new File(context.getFilesDir(), pack.identifier), pack.trayImageFile);
            if (trayFile.exists()) {
                holder.trayImageView.setImageBitmap(BitmapFactory.decodeFile(trayFile.getAbsolutePath()));
            } else {
                // Try loading from assets
                try {
                    InputStream is = context.getAssets().open(pack.identifier + "/" + pack.trayImageFile);
                    holder.trayImageView.setImageBitmap(BitmapFactory.decodeStream(is));
                    is.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error loading tray from assets: " + e.getMessage());
                    holder.trayImageView.setImageResource(R.drawable.ic_add);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading tray image: " + e.getMessage());
            holder.trayImageView.setImageResource(R.drawable.ic_add);
        }

        // Clear existing preview stickers
        holder.stickerPreviewLayout.removeAllViews();

        // Add preview stickers
        List<Sticker> stickers = pack.getStickers();
        int previewCount = Math.min(stickers.size(), MAX_PREVIEW_STICKERS);

        Log.d(TAG, "Adding " + previewCount + " stickers to preview");

        for (int i = 0; i < previewCount; i++) {
            // Inflate the entire layout for preview item
            View previewView = LayoutInflater.from(context)
                    .inflate(R.layout.item_sticker_preview, holder.stickerPreviewLayout, false);

            // Find the ImageView inside the inflated layout
            ImageView imageView = previewView.findViewById(R.id.sticker_image);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    context.getResources().getDimensionPixelSize(R.dimen.sticker_preview_size),
                    context.getResources().getDimensionPixelSize(R.dimen.sticker_preview_size));

            if (i > 0) {
                layoutParams.leftMargin = context.getResources().getDimensionPixelSize(R.dimen.sticker_preview_margin);
            }

            previewView.setLayoutParams(layoutParams);

            // Load sticker image (simplified)
            try {
                File stickerFile = new File(new File(context.getFilesDir(), pack.identifier),
                        stickers.get(i).imageFileName);
                if (stickerFile.exists()) {
                    imageView.setImageBitmap(BitmapFactory.decodeFile(stickerFile.getAbsolutePath()));
                } else {
                    // Try loading from assets
                    try {
                        InputStream is = context.getAssets().open(pack.identifier + "/" + stickers.get(i).imageFileName);
                        imageView.setImageBitmap(BitmapFactory.decodeStream(is));
                        is.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading sticker from assets: " + e.getMessage());
                        imageView.setImageResource(R.drawable.ic_add);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading sticker: " + e.getMessage());
                imageView.setImageResource(R.drawable.ic_add);
            }

            holder.stickerPreviewLayout.addView(previewView);
        }

        // Set add button state based on whitelist status
        updateAddButton(holder, pack);

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStickerPackClick(pack);
            }
        });

        holder.addButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddButtonClick(pack);
            }
        });
    }

    /**
     * Update the add button appearance based on whether the pack is already added to WhatsApp.
     *
     * @param holder ViewHolder
     * @param pack StickerPack to check
     */
    private void updateAddButton(ViewHolder holder, StickerPack pack) {
        if (pack.getIsWhitelisted()) {
            // Pack is already added to WhatsApp
            holder.addButton.setImageResource(R.drawable.ic_added);
            holder.addButton.setEnabled(false);
            holder.addedText.setVisibility(View.VISIBLE);
        } else {
            // Pack is not added to WhatsApp
            holder.addButton.setImageResource(R.drawable.ic_add);
            holder.addButton.setEnabled(true);
            holder.addedText.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return stickerPacks.size();
    }

    /**
     * ViewHolder for sticker pack items.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView trayImageView;
        final TextView packNameText;
        final TextView publisherText;
        final TextView packSizeText;
        final TextView stickerCountText;
        final LinearLayout stickerPreviewLayout;
        final ImageView addButton;
        final TextView addedText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            trayImageView = itemView.findViewById(R.id.sticker_pack_tray_image);
            packNameText = itemView.findViewById(R.id.sticker_pack_name);
            publisherText = itemView.findViewById(R.id.sticker_pack_publisher);
            packSizeText = itemView.findViewById(R.id.sticker_pack_size);
            stickerCountText = itemView.findViewById(R.id.sticker_pack_sticker_count);
            stickerPreviewLayout = itemView.findViewById(R.id.sticker_pack_preview);
            addButton = itemView.findViewById(R.id.sticker_pack_add_button);
            addedText = itemView.findViewById(R.id.sticker_pack_added_text);
        }
    }
}
