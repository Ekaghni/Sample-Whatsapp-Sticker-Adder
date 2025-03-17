package com.example.samplestickertestingapp.adapters;



import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
 * Adapter for displaying stickers in a grid.
 * Uses standard ImageView instead of Fresco views.
 */
public class StickerPreviewAdapter extends RecyclerView.Adapter<StickerPreviewAdapter.ViewHolder> {
    private static final String TAG = "StickerPreviewAdapter";

    private final Context context;
    private final StickerPack stickerPack;
    private final List<Sticker> stickers;

    /**
     * Constructor for StickerPreviewAdapter.
     *
     * @param context Context
     * @param stickerPack StickerPack containing stickers to display
     */
    public StickerPreviewAdapter(Context context, StickerPack stickerPack) {
        this.context = context;
        this.stickerPack = stickerPack;
        this.stickers = stickerPack.getStickers();

        Log.d(TAG, "Created adapter with " + stickers.size() + " stickers");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sticker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sticker sticker = stickers.get(position);

        Log.d(TAG, "Binding sticker: " + sticker.imageFileName + " at position " + position);

        // Set sticker image (using standard ImageView)
        try {
            File stickerFile = new File(new File(context.getFilesDir(), stickerPack.identifier),
                    sticker.imageFileName);
            if (stickerFile.exists()) {
                holder.stickerImageView.setImageBitmap(BitmapFactory.decodeFile(stickerFile.getAbsolutePath()));
                Log.d(TAG, "Loaded sticker from file: " + stickerFile.getAbsolutePath());
            } else {
                // Try loading from assets
                try {
                    InputStream is = context.getAssets().open(stickerPack.identifier + "/" + sticker.imageFileName);
                    holder.stickerImageView.setImageBitmap(BitmapFactory.decodeStream(is));
                    is.close();
                    Log.d(TAG, "Loaded sticker from assets");
                } catch (Exception e) {
                    Log.e(TAG, "Error loading sticker from assets: " + e.getMessage());
                    holder.stickerImageView.setImageResource(R.drawable.ic_add);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading sticker: " + e.getMessage());
            holder.stickerImageView.setImageResource(R.drawable.ic_add);
        }

        // Set emojis text (optional)
        if (!sticker.emojis.isEmpty()) {
            StringBuilder emojiText = new StringBuilder();
            for (String emoji : sticker.emojis) {
                emojiText.append(emoji).append(" ");
            }
            holder.emojiTextView.setText(emojiText.toString().trim());
            holder.emojiTextView.setVisibility(View.VISIBLE);
        } else {
            holder.emojiTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return stickers.size();
    }

    /**
     * ViewHolder for sticker items.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView stickerImageView;
        final TextView emojiTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            stickerImageView = itemView.findViewById(R.id.sticker_image);
            emojiTextView = itemView.findViewById(R.id.sticker_emoji);
        }
    }
}
