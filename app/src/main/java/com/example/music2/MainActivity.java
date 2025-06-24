package com.example.music2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        // Deklaruojami mygtukai
        Button workButton = findViewById(R.id.working);
        Button chillButton = findViewById(R.id.chilling);
        Button sleepButton = findViewById(R.id.sleeping);

        // Naudojam AspectRatioVideoView, kad pritaikytu vaizdo medziaga, prie vaizdo lango.
        AspectRatioVideoView video1 = findViewById(R.id.videoView1);
        AspectRatioVideoView video2 = findViewById(R.id.videoView2);
        AspectRatioVideoView video3 = findViewById(R.id.videoView3);

        // Uzkraunama ir paleidziama vaizdo medziaga
        setVideo(video1, R.raw.sleep1video);
        setVideo(video2, R.raw.chill1video);
        setVideo(video3, R.raw.work1video);

        SQLiteDatabase db = new DatabaseHelper(this).getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        while (c.moveToNext()) {
            Log.d("DB_TABLE", "Found table: " + c.getString(0));
        }
        c.close();

        // Animuojami mygtukai
        animateButton(sleepButton);
        animateButton(chillButton);
        animateButton(workButton);
        setButtonTouchAnimation(sleepButton);
        setButtonTouchAnimation(chillButton);
        setButtonTouchAnimation(workButton);

        sleepButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SleepActivity.class));
        });

        workButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, WorkActivity.class));
        });

        chillButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ChillActivity.class));
        });
    }

    // Paleidziama ir pritaikoma vaizdo medziaga per AspectRatioVideoView
    private void setVideo(AspectRatioVideoView videoView, int videoResId) {
        String path = "android.resource://" + getPackageName() + "/" + videoResId;
        videoView.setVideoURI(Uri.parse(path));
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            videoView.start();
        });
    }

    // Mygtuku animacija
    private void animateButton(View button) {
        button.setAlpha(0f);
        button.setTranslationY(-300f);
        button.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800);
    }
    private void setButtonTouchAnimation(View button) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }
}
