package com.example.music2;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WorkActivity extends AppCompatActivity {

    private AspectRatioVideoView videoView;
    private RecyclerView videoRecyclerView;
    private VideoAdapter videoAdapter;
    private TextView levelTextView;
    private TextView workHoursLabel, breakMinutesLabel, sessionCountLabel;
    private SeekBar workHoursSeekBar, breakMinutesSeekBar, sessionCountSeekBar;

    private DatabaseHelper dbHelper;
    private int currentVideoRes = R.raw.work1video;
    private int currentPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        setContentView(R.layout.activity_work);

        videoView = findViewById(R.id.videoView);
        videoRecyclerView = findViewById(R.id.videoRecyclerView);
        levelTextView = findViewById(R.id.textView);

        dbHelper = new DatabaseHelper(this);
        dbHelper.getWritableDatabase();

        int userLevel = dbHelper.getUserLevel("work");

        List<VideoItem> videoList = new ArrayList<>();
        videoList.add(new VideoItem(R.raw.work1video, 1));
        videoList.add(new VideoItem(R.raw.work2video, 2));
        videoList.add(new VideoItem(R.raw.work3video, 3));

        videoRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        videoAdapter = new VideoAdapter(this, videoList, userLevel, videoResId -> {
            currentVideoRes = videoResId;
            playVideo(currentVideoRes);
        });

        videoRecyclerView.setAdapter(videoAdapter);

        playVideo(currentVideoRes);

        TextView setAlarmButton = findViewById(R.id.setAlarmButton);
        Button chooseSessionButton = findViewById(R.id.chooseSessionButton);
        chooseSessionButton.setOnClickListener(v -> showSessionDialog());
        setAlarmButton.setOnClickListener(v -> showTimePickerDialog());

        Button startButton = findViewById(R.id.start);
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(WorkActivity.this, PlayerActivity.class);
            intent.putExtra("category", "work");
            intent.putExtra("video_res_id", currentVideoRes);
            intent.putExtra("show_session_control", true);
            startActivity(intent);
        });

        AppCompatImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());
        updateProgressUI();
    }

    private void showSessionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_custom_session, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        TextView workLabel = view.findViewById(R.id.workHoursLabel);
        SeekBar workSeekBar = view.findViewById(R.id.workHoursSeekBar);
        TextView breakLabel = view.findViewById(R.id.breakMinutesLabel);
        SeekBar breakSeekBar = view.findViewById(R.id.breakMinutesSeekBar);
        TextView sessionLabel = view.findViewById(R.id.sessionCountLabel);
        SeekBar sessionSeekBar = view.findViewById(R.id.sessionCountSeekBar);
        Button saveButton = view.findViewById(R.id.saveSessionButton);

        workSeekBar.setMax(31);
        workSeekBar.setProgress(0);

        int initialWorkMinutes = (workSeekBar.getProgress() + 1) * 15;
        workLabel.setText(formatWorkDuration(initialWorkMinutes));

        breakLabel.setText("Break Minutes: " + Math.max(breakSeekBar.getProgress(), 1));
        sessionLabel.setText("Sessions: " + Math.max(sessionSeekBar.getProgress(), 1));

        workSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int totalMinutes = (progress + 1) * 15;
                workLabel.setText(formatWorkDuration(totalMinutes));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        breakSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int val = Math.max(progress, 1);
                breakLabel.setText("Break Minutes: " + val);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sessionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int val = Math.max(progress, 1);
                sessionLabel.setText("Sessions: " + val);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        saveButton.setOnClickListener(v -> {
            int workMinutes = (workSeekBar.getProgress() + 1) * 15;
            int breakMinutes = Math.max(breakSeekBar.getProgress(), 1);
            int sessionCount = Math.max(sessionSeekBar.getProgress(), 1);

            ContentValues cv = new ContentValues();
            cv.put("work_minutes", workMinutes);  // changed column name to 'work_minutes'
            cv.put("break_minutes", breakMinutes);
            cv.put("session_count", sessionCount);

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("work_session", null, null);
            db.insert("work_session", null, cv);
            db.close();

            dialog.dismiss();
        });

        dialog.show();
    }

    private String formatWorkDuration(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        if (hours > 0 && minutes > 0) {
            return "Work Duration: " + hours + "h " + minutes + "min";
        } else if (hours > 0) {
            return "Work Duration: " + hours + "h";
        } else {
            return "Work Duration: " + minutes + "min";
        }
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

        dbHelper.saveAlarm(hour, minute, true, "work");
    }

    private void playVideo(int videoResId) {
        if (videoView == null) return;

        videoView.stopPlayback();

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + videoResId);
        videoView.setVideoURI(videoUri);
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);

            if (currentPosition > 0) {
                videoView.seekTo(currentPosition);
            } else {
                videoView.seekTo(1);
            }

            videoView.postDelayed(videoView::start, 50);
        });

        videoView.setVisibility(AspectRatioVideoView.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null) {
            currentPosition = videoView.getCurrentPosition();
            if (videoView.isPlaying()) {
                videoView.pause();
            }
            videoView.suspend();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView != null) {
            playVideo(currentVideoRes);
        }
        updateProgressUI();
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

    private void updateProgressUI() {
        int userLevel = dbHelper.getUserLevel("work");
        int userExp = dbHelper.getUserExperience("work");
        int expNeeded = userLevel * 10;
        levelTextView.setText("Level " + userLevel);
    }
}
