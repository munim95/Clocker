package com.rigid.clocker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationsManager {
    //todo persistent notification so user can keep track of time for a particular goal
    //  expand the notif to view the next goal
    //  notif should show the current goal name, time left and next goal.
    //  clicking on the notif should bring up the app
    private NotificationManagerCompat notificationManagerCompat;

    private final String CHANNEL_ID="com.rigid.clocker";
    private final int NOTIFICATION_ID = 1000;
    private Context context;

    public NotificationsManager(Context context){
        notificationManagerCompat = NotificationManagerCompat.from(context);
        this.context=context;
    }
    public void updateNotification(String... strings){
        //show current sector (if any) and time left
        notificationManagerCompat.notify(NOTIFICATION_ID,createBuilder(context,strings));
    }
    private Notification createBuilder(Context context, String... strings){
        createNotificationChannelForO(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(strings[0])
                .setContentText(strings[1])
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

       return builder.build();
    }
    private void createNotificationChannelForO(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Clocker";
            String description = "Clocker Notification";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
