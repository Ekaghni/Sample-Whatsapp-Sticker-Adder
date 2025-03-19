package com.example.samplestickertestingapp.adapters;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.samplestickertestingapp.R;
import com.example.samplestickertestingapp.models.CustomSticker;
import com.example.samplestickertestingapp.utils.FileUtils;

import java.io.File;
import java.util.List;

/**
 * Adapter for displaying saved custom stickers in a RecyclerView.
 */
public class SavedStickerAdapter extends RecyclerView.Adapter<SavedStickerAdapter.ViewHolder> {
    private static final String TAG = "SavedStickerAdapter";

    private final Context context;
    private final List<CustomSticker> customStickers;
    private final OnStickerClickListener listener;

    /**
     * Interface for handling sticker clicks.
     */
    public interface OnStickerClickListener {
        void onStickerAddToWhatsAppClick(CustomSticker sticker);
        void onStickerDeleteClick(CustomSticker sticker);
    }

    /**
     * Constructor for SavedStickerAdapter.
     *
     * @param context Context
     * @param customStickers List of custom stickers
     * @param listener Click listener
     */
    public SavedStickerAdapter(Context context, List<CustomSticker> customStickers, OnStickerClickListener listener) {
        this.context = context;
        this.customStickers = customStickers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_saved_sticker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomSticker sticker = customStickers.get(position);

        // Load sticker image
        try {
            File stickerFile = new File(FileUtils.getCustomStickersDirectory(context), sticker.getImageFileName());
            if (stickerFile.exists()) {
                holder.stickerImage.setImageBitmap(BitmapFactory.decodeFile(stickerFile.getAbsolutePath()));
            } else {
                holder.stickerImage.setImageResource(R.drawable.ic_add);
            }
        } catch (Exception e) {
            holder.stickerImage.setImageResource(R.drawable.ic_add);
        }

        // Set emojis text
        if (sticker.getEmojis() != null && !sticker.getEmojis().isEmpty()) {
            StringBuilder emojiText = new StringBuilder();
            for (String emoji : sticker.getEmojis()) {
                emojiText.append(emoji).append(" ");
            }
            holder.emojisText.setText(emojiText.toString().trim());
            holder.emojisText.setVisibility(View.VISIBLE);
        } else {
            holder.emojisText.setVisibility(View.GONE);
        }

        // Set click listeners
        holder.addToWhatsAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onStickerAddToWhatsAppClick(sticker);
                }
            }
        });

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onStickerDeleteClick(sticker);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return customStickers.size();
    }

    /**
     * ViewHolder for custom sticker items.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView stickerImage;
        final TextView emojisText;
        final ImageButton addToWhatsAppButton;
        final ImageButton deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            stickerImage = itemView.findViewById(R.id.sticker_image);
            emojisText = itemView.findViewById(R.id.sticker_emojis);
            addToWhatsAppButton = itemView.findViewById(R.id.btn_add_to_whatsapp);
            deleteButton = itemView.findViewById(R.id.btn_delete);
        }
    }
}