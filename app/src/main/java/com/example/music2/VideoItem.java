package com.example.music2;

public class VideoItem {
    private final int videoResId;
    private final int requiredLevel;

    public VideoItem(int videoResId, int requiredLevel) {
        this.videoResId = videoResId;
        this.requiredLevel = requiredLevel;
    }

    public int getResId() {
        return videoResId;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }
}

