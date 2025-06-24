package com.example.music2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Zadintuvo suveikimas
        Toast.makeText(context, "⏰ Alarm Triggered!", Toast.LENGTH_LONG).show();

        // Sustabdo muzikos leidima suveikus
        Intent stopService = new Intent(context, MediaPlayerService.class);
        stopService.setAction(MediaPlayerService.ACTION_STOP);
        context.startService(stopService);

        // Grazina vartotoja i pagrindini puslapi
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(mainIntent);
    }
}
