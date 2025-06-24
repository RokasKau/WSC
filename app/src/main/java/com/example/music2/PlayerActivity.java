package com.example.music2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.music2.DatabaseHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class PlayerActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "PlayerActivity";

    private AspectRatioVideoView videoView;
    private View controlPanel, statsPanel, extraSoundPanel, rootView;
    private Runnable hideRunnable;
    private final Handler hideHandler = new Handler();
    private static final int HIDE_DELAY_MS = 8000;
    private Sensor lightSensor;
    private static final float DARKNESS_THRESHOLD = 10.0f;
    private static final int MOVEMENT_THRESHOLD = 1;
    private long lastMovementTime = System.currentTimeMillis();
    private static final long IDLE_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes
    private boolean isUserActive = true;

    private TextView levelText, xpText;
    private int level = 1, xp = 0;
    private final int XP_PER_LEVEL = 10;

    private final Handler xpHandler = new Handler();
    private final int XP_INTERVAL_MS = 60000;
    private DatabaseHelper dbHelper;
    private final Runnable xpRunnable = new Runnable() {
        @Override
        public void run() {
            if (isUserActive) {
                gainXp(1);
            } else {
                Log.d(TAG, "User inactive, XP gain paused.");
            }
            xpHandler.postDelayed(this, XP_INTERVAL_MS);
        }
    };

    private String categoryPrefix;
    private String[] assetSongFiles;
    private int currentSongIndex = 0;

    private SharedPreferences prefs;

    private boolean isRainPlaying = false;
    private boolean isCityPlaying = false;
    private boolean isPeoplePlaying = false;
    private AlertDialog sessionDialog;
    private MediaPlayer rainPlayer, cityPlayer, peoplePlayer;
    private AppCompatImageButton toggleButton;
    private boolean isPlaying = true; // 🔁 Track playback state

    // Sensor
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private final BroadcastReceiver songCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentSongIndex = (currentSongIndex + 1) % assetSongFiles.length;
            playSong(currentSongIndex);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_player);

        dbHelper = new DatabaseHelper(this); // Initialize DB helper

        // Sensor setup
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT); // Add this line
        }

        // Load last saved WorkSession from DB
        WorkSession savedSession = dbHelper.getLastWorkSession();
        if (savedSession != null) {
            // Directly show controls, no popup confirmation
            showWorkSessionControls(savedSession);
        }

        initCategoryPrefix();
        initUI();
        loadCategorySongs();
        startBackgroundVideo();
        loadUserProgress(); // Loads from database

        resetHideTimer();
        xpHandler.postDelayed(xpRunnable, XP_INTERVAL_MS);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(songCompleteReceiver, new IntentFilter(MediaPlayerService.ACTION_SONG_COMPLETED));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        // Register light sensor listener here
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double acceleration = Math.sqrt(x * x + y * y + z * z);

            if (acceleration > MOVEMENT_THRESHOLD) {
                lastMovementTime = System.currentTimeMillis();
                if (!isUserActive) {
                    isUserActive = true;
                    Log.d(TAG, "User active detected.");
                }
            }

            if (System.currentTimeMillis() - lastMovementTime > IDLE_TIMEOUT_MS) {
                if (isUserActive) {
                    isUserActive = false;
                    Log.d(TAG, "User inactive detected, stopping XP gain.");
                }
            }
        }
        else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            if (lux < DARKNESS_THRESHOLD) {
                setScreenBrightness(0.1f);
            } else {
                setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);  // Restore default brightness
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // no-op
    }

    private void setScreenBrightness(float brightness) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        if (layoutParams.screenBrightness != brightness) {
            layoutParams.screenBrightness = brightness;
            getWindow().setAttributes(layoutParams);
        }
    }


    private void showWorkSessionControls(WorkSession session) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.pop_up_session_control, null);
        builder.setView(dialogView);

        TextView sessionStatusText = dialogView.findViewById(R.id.sessionStatusText);
        Button pauseButton = dialogView.findViewById(R.id.pauseButton);
        Button clearButton = dialogView.findViewById(R.id.clearButton);
        Button startButton = dialogView.findViewById(R.id.startButton);

        final boolean[] isRunning = {false};
        final boolean[] isWorkPhase = {true};
        final CountDownTimer[] timer = {null};

        long workMillis = session.workMinutes * 60_000L;   // Convert minutes to milliseconds
        long breakMillis = session.breakMinutes * 60_000L;


        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        final AlertDialog sessionDialog = builder.create();

        sessionDialog.setCanceledOnTouchOutside(false);
        sessionDialog.setCancelable(false);

        sessionDialog.getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        );
        sessionDialog.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        Runnable startTimer = new Runnable() {
            @Override
            public void run() {
                long millisToCount = isWorkPhase[0] ? workMillis : breakMillis;
                String phaseText = isWorkPhase[0] ? "Working: " : "Break Time: ";

                if (millisToCount <= 0) {
                    sessionStatusText.setText(phaseText + "00:00:00");
                    return;
                }

                timer[0] = new CountDownTimer(millisToCount, 1000) {
                    public void onTick(long millisUntilFinished) {
                        String timeStr = timeFormat.format(new Date(millisUntilFinished));
                        sessionStatusText.setText(phaseText + timeStr);
                    }

                    public void onFinish() {
                        isWorkPhase[0] = !isWorkPhase[0];
                        run();
                    }
                }.start();

                isRunning[0] = true;
                startButton.setEnabled(false);
                pauseButton.setEnabled(true);
            }
        };

        pauseButton.setEnabled(false);

        startButton.setOnClickListener(v -> {
            if (!isRunning[0]) {
                startTimer.run();
            }
        });

        pauseButton.setOnClickListener(v -> {
            if (isRunning[0]) {
                timer[0].cancel();
                isRunning[0] = false;
                sessionStatusText.setText(sessionStatusText.getText() + " (Paused)");
                startButton.setEnabled(true);
                pauseButton.setEnabled(false);
            }
        });

        clearButton.setOnClickListener(v -> {
            if (timer[0] != null) {
                timer[0].cancel();
            }
            dbHelper.clearWorkSession();
            Log.d(TAG, "Session cleared");
            sessionStatusText.setText("Session cleared");

            sessionDialog.dismiss();
        });

        sessionStatusText.setText("Working: " + timeFormat.format(new Date(workMillis)));

        sessionDialog.show();
    }

    private void gainXp(int amount) {
        xp += amount;
        int xpNeededForNextLevel = XP_PER_LEVEL * level;

        while (xp >= xpNeededForNextLevel) {
            xp -= xpNeededForNextLevel;
            level++;
            xpNeededForNextLevel = XP_PER_LEVEL * level;
        }

        saveUserProgress();
        updateXpUi();
    }

    private void updateXpUi() {
        int xpNeededForNextLevel = XP_PER_LEVEL * level;
        levelText.setText("Level: " + level);
        xpText.setText("XP: " + xp + "/" + xpNeededForNextLevel);
    }

    private void loadUserProgress() {
        level = dbHelper.getUserLevel(categoryPrefix);
        xp = dbHelper.getUserExperience(categoryPrefix);
        updateXpUi();
    }

    private void saveUserProgress() {
        dbHelper.updateUserProgress(categoryPrefix, level, xp);
    }

    private void initCategoryPrefix() {
        String category = getIntent().getStringExtra("category");
        if (category == null) category = "default";
        categoryPrefix = category;
    }

    private void initUI() {
        levelText = findViewById(R.id.levelText);
        xpText = findViewById(R.id.xpText);
        rootView = findViewById(R.id.rootView);
        controlPanel = findViewById(R.id.controlPanel);
        statsPanel = findViewById(R.id.statsPanel);
        extraSoundPanel = findViewById(R.id.extraSoundPanel);
        videoView = findViewById(R.id.videoView);

        AppCompatImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        toggleButton = findViewById(R.id.stopButton);
        toggleButton.setOnClickListener(v -> togglePlayPause());

        AppCompatImageButton prevButton = findViewById(R.id.prevButton);
        prevButton.setOnClickListener(v -> {
            if (assetSongFiles == null || assetSongFiles.length == 0) return;
            currentSongIndex = (currentSongIndex - 1 + assetSongFiles.length) % assetSongFiles.length;
            playSong(currentSongIndex);
        });

        AppCompatImageButton nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(v -> {
            if (assetSongFiles == null || assetSongFiles.length == 0) return;
            currentSongIndex = (currentSongIndex + 1) % assetSongFiles.length;
            playSong(currentSongIndex);
        });

        AppCompatImageButton rainButton = findViewById(R.id.rainButton);
        rainButton.setAlpha(0.5f);
        rainButton.setOnClickListener(v -> toggleAmbientSound("rain", rainButton));

        AppCompatImageButton cityButton = findViewById(R.id.cityButton);
        cityButton.setAlpha(0.5f);
        cityButton.setOnClickListener(v -> toggleAmbientSound("city", cityButton));

        AppCompatImageButton peopleButton = findViewById(R.id.peopleButton);
        peopleButton.setAlpha(0.5f);
        peopleButton.setOnClickListener(v -> toggleAmbientSound("people", peopleButton));

        rootView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (controlPanel.getVisibility() == View.VISIBLE) {
                    hideHandler.removeCallbacks(hideRunnable);
                    hideUIWithAnimation();
                } else {
                    showUIWithAnimation();
                    resetHideTimer();
                }
            }
            return true;
        });

        hideRunnable = this::hideUIWithAnimation;
    }

    private void loadCategorySongs() {
        try {
            AssetManager assetManager = getAssets();
            assetSongFiles = assetManager.list(categoryPrefix);

            if (assetSongFiles == null || assetSongFiles.length == 0) {
                Log.e(TAG, "No music files found in assets/" + categoryPrefix);
                return;
            }
            currentSongIndex = new java.util.Random().nextInt(assetSongFiles.length);

            playSong(currentSongIndex);

        } catch (IOException e) {
            Log.e(TAG, "Error loading assets for category: " + categoryPrefix, e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while loading or playing song", e);
        }
    }


    private void playSong(int index) {
        String filename = assetSongFiles[index];
        Intent intent = new Intent(this, MediaPlayerService.class);
        intent.setAction(MediaPlayerService.ACTION_PLAY);
        intent.putExtra(MediaPlayerService.EXTRA_CATEGORY, categoryPrefix);
        intent.putExtra(MediaPlayerService.EXTRA_FILENAME, filename);
        startService(intent);
        toggleButton.setImageResource(R.drawable.ic_stop);
        isPlaying = true;
    }

    private void togglePlayPause() {
        Intent intent = new Intent(this, MediaPlayerService.class);
        intent.setAction(MediaPlayerService.ACTION_TOGGLE);
        startService(intent);

        if (isPlaying) {
            toggleButton.setImageResource(R.drawable.start);
        } else {
            toggleButton.setImageResource(R.drawable.ic_stop);
        }

        isPlaying = !isPlaying;
    }

    private void toggleAmbientSound(String type, AppCompatImageButton button) {
        try {
            switch (type) {
                case "rain":
                    if (isRainPlaying) {
                        stopPlayer(rainPlayer);
                        rainPlayer = null;
                        isRainPlaying = false;
                        button.setAlpha(0.5f);
                    } else {
                        rainPlayer = playAmbient("raining.mp3");
                        isRainPlaying = true;
                        button.setAlpha(1f);
                    }
                    break;
                case "city":
                    if (isCityPlaying) {
                        stopPlayer(cityPlayer);
                        cityPlayer = null;
                        isCityPlaying = false;
                        button.setAlpha(0.5f);
                    } else {
                        cityPlayer = playAmbient("cyty.mp3");
                        isCityPlaying = true;
                        button.setAlpha(1f);
                    }
                    break;
                case "people":
                    if (isPeoplePlaying) {
                        stopPlayer(peoplePlayer);
                        peoplePlayer = null;
                        isPeoplePlaying = false;
                        button.setAlpha(0.5f);
                    } else {
                        peoplePlayer = playAmbient("peop.mp3");
                        isPeoplePlaying = true;
                        button.setAlpha(1f);
                    }
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error toggling ambient sound: " + type, e);
        }
    }

    private MediaPlayer playAmbient(String filename) throws IOException {
        AssetFileDescriptor afd = getAssets().openFd("ambient/" + filename);
        MediaPlayer player = new MediaPlayer();
        player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        afd.close();
        player.setLooping(true);
        player.prepare();
        player.start();
        return player;
    }

    private void stopPlayer(MediaPlayer player) {
        if (player != null) {
            if (player.isPlaying()) player.stop();
            player.release();
        }
    }

    private void startBackgroundVideo() {
        int videoResId = getIntent().getIntExtra("video_res_id", -1);
        if (videoResId == -1) {
            Log.e(TAG, "No videoResId passed to PlayerActivity.");
            return;
        }

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + videoResId);
        videoView = findViewById(R.id.videoView); // Make sure this is initialized here if not already
        videoView.setVideoURI(videoUri);

        videoView.setOnPreparedListener(mp -> {
            int videoWidth = mp.getVideoWidth();
            int videoHeight = mp.getVideoHeight();

            // Ensure custom resizing takes effect
            if (videoView instanceof AspectRatioVideoView) {
                ((AspectRatioVideoView) videoView).setVideoSize(videoWidth, videoHeight);
            }

            mp.setLooping(true);
            videoView.start();
        });
    }


    private void resetHideTimer() {
        hideHandler.removeCallbacks(hideRunnable);
        hideHandler.postDelayed(hideRunnable, HIDE_DELAY_MS);
    }

    private void hideUIWithAnimation() {
        fadeOutAndHide(controlPanel);
        fadeOutAndHide(statsPanel);
        fadeOutAndHide(extraSoundPanel);
    }

    private void showUIWithAnimation() {
        fadeInIfHidden(controlPanel);
        fadeInIfHidden(statsPanel);
        fadeInIfHidden(extraSoundPanel);
    }

    private void fadeInIfHidden(View view) {
        if (view.getVisibility() != View.VISIBLE) {
            view.setAlpha(0f);
            view.setVisibility(View.VISIBLE);
            view.animate().alpha(1f).setDuration(500).start();
        }
    }

    private void fadeOutAndHide(View view) {
        view.animate().alpha(0f).setDuration(500)
                .withEndAction(() -> view.setVisibility(View.GONE)).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopPlayer(rainPlayer);
        stopPlayer(cityPlayer);
        stopPlayer(peoplePlayer);

        xpHandler.removeCallbacks(xpRunnable);

        if (videoView != null) {
            videoView.stopPlayback();
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(songCompleteReceiver);

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }}


