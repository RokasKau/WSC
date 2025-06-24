package com.example.music2;

import java.util.ArrayList;
import java.util.List;

public class VideoRepository {

    public static List<VideoItem> getChillVideos() {
        List<VideoItem> videos = new ArrayList<>();
        videos.add(new VideoItem(R.raw.chill1video, 1));
        videos.add(new VideoItem(R.raw.chill2video, 2));
        videos.add(new VideoItem(R.raw.chill3video, 3));
        return videos;
    }

    public static List<VideoItem> getSleepVideos() {
        List<VideoItem> videos = new ArrayList<>();
        videos.add(new VideoItem(R.raw.sleep1video, 1));
        videos.add(new VideoItem(R.raw.sleep2video, 2));
        videos.add(new VideoItem(R.raw.sleep3video, 3));
        return videos;
    }

    public static List<VideoItem> getWorkVideos() {
        List<VideoItem> videos = new ArrayList<>();
        videos.add(new VideoItem(R.raw.work1video, 1));
        videos.add(new VideoItem(R.raw.work2video, 2));
        videos.add(new VideoItem(R.raw.work3video, 3));
        return videos;
    }
}
