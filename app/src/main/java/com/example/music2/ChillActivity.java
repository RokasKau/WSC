package com.example.music2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ChillActivity extends AppCompatActivity {

    private AspectRatioVideoView videoView;
    private RecyclerView videoRecyclerView;
    private VideoAdapter videoAdapter;
    private ProgressBar progressBar;
    private TextView levelTextView;
    private DatabaseHelper dbHelper;

    private int currentVideoRes = R.raw.chill1video;
    private int currentPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        setContentView(R.layout.activity_chill);

        videoView = findViewById(R.id.videoView);
        videoRecyclerView = findViewById(R.id.videoRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        levelTextView = findViewById(R.id.textView);
        AppCompatImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        dbHelper = new DatabaseHelper(this);
        dbHelper.getWritableDatabase();

        int userLevel = dbHelper.getUserLevel("chill");

        List<VideoItem> videoList = new ArrayList<>();
        videoList.add(new VideoItem(R.raw.chill1video, 1));
        videoList.add(new VideoItem(R.raw.chill2video, 2));
        videoList.add(new VideoItem(R.raw.chill3video, 3));

        videoRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        videoAdapter = new VideoAdapter(this, videoList, userLevel, videoResId -> {
            currentVideoRes = videoResId;
            playVideo(currentVideoRes);
        });
        videoRecyclerView.setAdapter(videoAdapter);

        // Default video
        playVideo(currentVideoRes);

        TextView setAlarmButton = findViewById(R.id.setAlarm);
        setAlarmButton.setOnClickListener(v -> showTimePickerDialog());

        Button startButton = findViewById(R.id.start);
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(ChillActivity.this, PlayerActivity.class);
            intent.putExtra("category", "chill");
            intent.putExtra("video_res_id", currentVideoRes);
            startActivity(intent);
        });

        updateProgressUI();
    }

    private void playVideo(int videoResId) {
        if (videoView == null) return;

        videoView.stopPlayback();

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + videoResId);
        videoView.setVideoURI(videoUri);
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            videoView.seekTo(currentPosition > 0 ? currentPosition : 1);
            videoView.postDelayed(videoView::start, 50);
        });

        videoView.setVisibility(android.view.View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null) {
            currentPosition = videoView.getCurrentPosition();
            if (videoView.isPlaying()) videoView.pause();
            videoView.suspend();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView != null) playVideo(currentVideoRes);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
            videoView.suspend();
        }
        if (dbHelper != null) dbHelper.close();
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> setAlarm(selectedHour, selectedMinute),
                hour, minute, true
        );
        timePickerDialog.show();
    }

    private void setAlarm(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1);
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags);

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }

        // Save alarm to database
        dbHelper.saveAlarm(hour, minute, true, "chill");
    }


    private void addExperience(int minutesListened) {
        int currentExp = dbHelper.getUserExperience("chill");
        int currentLevel = dbHelper.getUserLevel("chill");

        currentExp += minutesListened;
        int expNeeded = currentLevel * 10;

        while (currentExp >= expNeeded) {
            currentExp -= expNeeded;
            currentLevel++;
            expNeeded = currentLevel * 10;
        }

        dbHelper.updateUserProgress("chill", currentLevel, currentExp);
        updateProgressUI();
    }

    private void updateProgressUI() {
        int userLevel = dbHelper.getUserLevel("chill");
        int userExp = dbHelper.getUserExperience("chill");
        int expNeeded = userLevel * 10;

        progressBar.setMax(expNeeded);
        progressBar.setProgress(userExp);
        levelTextView.setText("Level " + userLevel);
    }
}
