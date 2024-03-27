package com.example.mlseriesdemonstrator.helpers.vision.drowsiness;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.mlseriesdemonstrator.R;

import java.util.Locale;

public class DrowsinessDetectionService extends Service {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final String CHANNEL_ID = "DrowsinessDetectionServiceChannel";
    private static final long UPDATE_INTERVAL_MS = 1000; // Atualiza a notificação a cada segundo

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = createNotification("Inicializando...");
        startForeground(1, notification);

        // Iniciar a atualização da notificação
        handler.post(updateNotificationRunnable);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateNotificationRunnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateNotification() {
        FaceDrowsiness faceDrowsiness = FaceDrowsiness.getInstance();

        String notificationText = String.format(Locale.US,
                "Left Eye: %.2f, Right Eye: %.2f, Avg Perclos: %.2f%%",
                faceDrowsiness.getLeftEyeOpenProb(),
                faceDrowsiness.getRightEyeOpenProb(),
                faceDrowsiness.getAveragePerclos());

        Notification notification = createNotification(notificationText);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

    private final Runnable updateNotificationRunnable = new Runnable() {
        @Override
        public void run() {
            updateNotification();
            handler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    private Notification createNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Detecção de Sonolência")
                .setContentText(text)
                .setSmallIcon(R.drawable.baseline_time_to_leave_black_18) // Substitua pelo ícone desejado.
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Drowsiness Detection Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
