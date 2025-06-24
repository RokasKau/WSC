package com.example.music2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;

public class MediaPlayerService extends Service {

    public static final String ACTION_PLAY = "com.example.music2.action.PLAY";
    public static final String ACTION_PAUSE = "com.example.music2.action.PAUSE";
    public static final String ACTION_STOP = "com.example.music2.action.STOP";
    public static final String ACTION_TOGGLE = "com.example.music2.action.TOGGLE";

    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_FILENAME = "filename";

    public static final String CHANNEL_ID = "MusicPlayerChannel";
    public static final String ACTION_SONG_COMPLETED = "com.example.music2.SONG_COMPLETED";

    private MediaPlayer mediaPlayer;
    private String currentPath = null;
    private boolean isPaused = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) return START_STICKY;

        String action = intent.getAction();

        switch (action) {
            case ACTION_PLAY: {
                String category = intent.getStringExtra(EXTRA_CATEGORY);
                String filename = intent.getStringExtra(EXTRA_FILENAME);

                if (category != null && filename != null) {
                    String newPath = category + "/" + filename;
                    if (!newPath.equals(currentPath)) {
                        currentPath = newPath;
                        resetMediaPlayer();
                        initMediaPlayerWithAsset(currentPath);
                    }
                }

                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                    isPaused = false;
                    startForeground(1, buildNotification(true));
                }
                break;
            }

            case ACTION_PAUSE: {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPaused = true;
                    startForeground(1, buildNotification(false));
                }
                break;
            }

            case ACTION_TOGGLE: {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        isPaused = true;
                        startForeground(1, buildNotification(false));
                    } else {
                        mediaPlayer.start();
                        isPaused = false;
                        startForeground(1, buildNotification(true));
                    }
                }
                break;
            }

            case ACTION_STOP: {
                stopForeground(true);
                resetMediaPlayer();
                stopSelf();
                break;
            }
        }

        return START_STICKY;
    }

    private void initMediaPlayerWithAsset(String assetPath) {
        try {
            AssetFileDescriptor afd = getAssets().openFd(assetPath);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            mediaPlayer.setLooping(false);
            mediaPlayer.prepare();

            mediaPlayer.setOnCompletionListener(mp -> {
                Intent completeIntent = new Intent(ACTION_SONG_COMPLETED);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(completeIntent);
            });

        } catch (IOException e) {
            e.printStackTrace();
            mediaPlayer = null;
        }
    }

    private void resetMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private Notification buildNotification(boolean isPlaying) {
        Intent toggleIntent = new Intent(this, MediaPlayerService.class).setAction(ACTION_TOGGLE);
        PendingIntent piToggle = PendingIntent.getService(this, 0, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, MediaPlayerService.class).setAction(ACTION_STOP);
        PendingIntent piStop = PendingIntent.getService(this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int icon = isPlaying ? R.drawable.ic_stop : R.drawable.start;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Music Player")
                .setContentText(isPlaying ? "Playing music..." : "Paused")
                .setSmallIcon(icon)
                .addAction(icon, isPlaying ? "Pause" : "Play", piToggle)
                .addAction(R.drawable.ic_stop, "Stop", piStop)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1))
                .setOnlyAlertOnce(true)
                .setOngoing(isPlaying)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID, "Music Player", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(chan);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetMediaPlayer();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
