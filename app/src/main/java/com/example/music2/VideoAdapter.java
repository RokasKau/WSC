package com.example.music2;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private final Context context;
    private final List<VideoItem> videoList;
    private final int userLevel;
    private final OnVideoSelectedListener listener;
    private int selectedPosition = -1;

    public interface OnVideoSelectedListener {
        void onVideoSelected(int resId);
    }

    public VideoAdapter(Context context, List<VideoItem> videoList, int userLevel, OnVideoSelectedListener listener) {
        this.context = context;
        this.videoList = videoList;
        this.userLevel = userLevel;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int size = (int) context.getResources().getDimension(R.dimen.gif_item_size);

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setLayoutParams(new RecyclerView.LayoutParams(size, size));

        ImageView thumbnail = new ImageView(context);
        FrameLayout.LayoutParams thumbnailParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        thumbnail.setLayoutParams(thumbnailParams);
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);

        View overlay = new View(context);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(Color.parseColor("#80000000"));
        overlay.setVisibility(View.GONE);

        TextView lockText = new TextView(context);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.CENTER;
        lockText.setLayoutParams(textParams);
        lockText.setTextColor(Color.WHITE);
        lockText.setTextSize(16);
        lockText.setText("Locked");
        lockText.setVisibility(View.GONE);

        frameLayout.addView(thumbnail);
        frameLayout.addView(overlay);
        frameLayout.addView(lockText);

        return new VideoViewHolder(frameLayout, thumbnail, overlay, lockText);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem video = videoList.get(position);
        boolean isUnlocked = userLevel >= video.getRequiredLevel();
        holder.bind(video, position == selectedPosition, isUnlocked);
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        View overlay;
        TextView lockText;

        public VideoViewHolder(@NonNull View itemView, ImageView thumbnail, View overlay, TextView lockText) {
            super(itemView);
            this.thumbnail = thumbnail;
            this.overlay = overlay;
            this.lockText = lockText;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    VideoItem video = videoList.get(position);
                    if (userLevel >= video.getRequiredLevel()) {
                        int previousPos = selectedPosition;
                        selectedPosition = position;
                        if (previousPos != -1) notifyItemChanged(previousPos);
                        notifyItemChanged(selectedPosition);
                        listener.onVideoSelected(video.getResId());
                    }
                }
            });
        }

        void bind(VideoItem video, boolean isSelected, boolean isUnlocked) {
            Uri videoUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + video.getResId());

            // Load a frame from video as thumbnail using Glide
            Glide.with(context)
                    .asBitmap()
                    .load(videoUri)
                    .apply(new RequestOptions()
                            .frame(1000000) // 1 second into the video
                            .centerCrop())
                    .into(thumbnail);

            if (!isUnlocked) {
                overlay.setVisibility(View.VISIBLE);
                lockText.setVisibility(View.VISIBLE);
                thumbnail.setAlpha(0.7f);
            } else {
                overlay.setVisibility(View.GONE);
                lockText.setVisibility(View.GONE);
                thumbnail.setAlpha(isSelected ? 0.6f : 1f);
            }
        }
    }
}
