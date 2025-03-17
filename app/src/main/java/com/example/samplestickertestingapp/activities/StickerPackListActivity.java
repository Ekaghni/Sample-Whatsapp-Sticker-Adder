package com.example.samplestickertestingapp.activities;



import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.samplestickertestingapp.R;
import com.example.samplestickertestingapp.adapters.StickerPackAdapter;
import com.example.samplestickertestingapp.models.StickerPack;
import com.example.samplestickertestingapp.utils.StickerPackLoader;
import com.example.samplestickertestingapp.utils.WhitelistCheck;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity to display the list of available sticker packs.
 */
public class StickerPackListActivity extends AppCompatActivity implements StickerPackAdapter.OnStickerPackClickListener {
    private static final String TAG = "StickerPackListActivity";

    private RecyclerView recyclerView;
    private StickerPackAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;

    private List<StickerPack> stickerPacks = new ArrayList<>();
    private LoadStickersTask loadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_pack_list);

        // Set up the app bar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_activity_sticker_pack_list);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        recyclerView = findViewById(R.id.recycler_sticker_packs);
        progressBar = findViewById(R.id.progress_bar);
        emptyView = findViewById(R.id.empty_view);

        // Set up the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Create and set the adapter
        adapter = new StickerPackAdapter(this, stickerPacks, this);
        recyclerView.setAdapter(adapter);

        // Load sticker packs
        loadStickerPacks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh whitelist status
        if (adapter != null) {
            new WhitelistCheckTask(this).execute(stickerPacks.toArray(new StickerPack[0]));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadTask != null) {
            loadTask.cancel(true);
            loadTask = null;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Load all available sticker packs.
     */
    private void loadStickerPacks() {
        showLoading(true);
        loadTask = new LoadStickersTask(this);
        loadTask.execute();
    }

    /**
     * Show or hide the loading indicator.
     *
     * @param isLoading Whether the content is loading
     */
    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    /**
     * Update the UI with the loaded sticker packs.
     *
     * @param packs List of sticker packs
     */
    private void updateStickerPacks(List<StickerPack> packs) {
        showLoading(false);

        stickerPacks.clear();
        if (packs != null) {
            stickerPacks.addAll(packs);
        }

        adapter.notifyDataSetChanged();

        // Show empty view if no packs are available
        emptyView.setVisibility(stickerPacks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * Handle click on a sticker pack.
     *
     * @param stickerPack The clicked sticker pack
     */
    @Override
    public void onStickerPackClick(StickerPack stickerPack) {
        // Open the sticker pack details activity
        Intent intent = new Intent(this, StickerPackDetailsActivity.class);
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_ID, stickerPack.identifier);
        startActivity(intent);
    }

    /**
     * Handle click on the "Add to WhatsApp" button.
     *
     * @param stickerPack The sticker pack to add
     */
    @Override
    public void onAddButtonClick(StickerPack stickerPack) {
        try {
            // Create intent to add sticker pack to WhatsApp
            Intent intent = new Intent();
            intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
            intent.putExtra("sticker_pack_id", stickerPack.identifier);
            intent.putExtra("sticker_pack_authority", getPackageName() + ".stickercontentprovider");
            intent.putExtra("sticker_pack_name", stickerPack.name);

            // Check if WhatsApp is installed
            if (!WhitelistCheck.isWhatsAppConsumerAppInstalled(StickerPackListActivity.this) &&
                    !WhitelistCheck.isWhatsAppSmbAppInstalled(StickerPackListActivity.this)) {
                Toast.makeText(this, R.string.whatsapp_not_installed, Toast.LENGTH_LONG).show();
                return;
            }

            // Start the WhatsApp activity
            startActivityForResult(intent, 200);
        } catch (Exception e) {
            Log.e(TAG, "Error adding sticker pack to WhatsApp", e);
            Toast.makeText(this, R.string.add_pack_fail, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200) {
            if (resultCode == RESULT_CANCELED) {
                if (data != null) {
                    final String validationError = data.getStringExtra("validation_error");
                    if (validationError != null) {
                        Log.e(TAG, "Validation error: " + validationError);
                        Toast.makeText(this, validationError, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, R.string.sticker_pack_not_added, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, R.string.sticker_pack_not_added, Toast.LENGTH_LONG).show();
                }
            } else if (resultCode == RESULT_OK) {
                Toast.makeText(this, R.string.sticker_pack_added, Toast.LENGTH_LONG).show();
                // Refresh the whitelist status
                new WhitelistCheckTask(this).execute(stickerPacks.toArray(new StickerPack[0]));
            }
        }
    }

    /**
     * AsyncTask to load sticker packs in the background.
     */
    private static class LoadStickersTask extends AsyncTask<Void, Void, List<StickerPack>> {
        private final WeakReference<StickerPackListActivity> activityRef;

        LoadStickersTask(StickerPackListActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        protected List<StickerPack> doInBackground(Void... voids) {
            try {
                StickerPackListActivity activity = activityRef.get();
                if (activity != null) {
                    return StickerPackLoader.getStickerPacks(activity);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading sticker packs", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<StickerPack> stickerPacks) {
            StickerPackListActivity activity = activityRef.get();
            if (activity != null) {
                activity.updateStickerPacks(stickerPacks);
                // Check whitelist status for all packs
                if (stickerPacks != null && !stickerPacks.isEmpty()) {
                    new WhitelistCheckTask(activity).execute(stickerPacks.toArray(new StickerPack[0]));
                }
            }
        }
    }

    /**
     * AsyncTask to check whitelist status of sticker packs.
     */
    private static class WhitelistCheckTask extends AsyncTask<StickerPack, Void, Void> {
        private final WeakReference<StickerPackListActivity> activityRef;

        WhitelistCheckTask(StickerPackListActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(StickerPack... stickerPacks) {
            StickerPackListActivity activity = activityRef.get();
            if (activity == null) {
                return null;
            }

            for (StickerPack pack : stickerPacks) {
                pack.setIsWhitelisted(WhitelistCheck.isWhitelisted(activity, pack.identifier));
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            StickerPackListActivity activity = activityRef.get();
            if (activity != null && activity.adapter != null) {
                activity.adapter.notifyDataSetChanged();
            }
        }
    }
}