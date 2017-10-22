package connections;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.channeli.noticeboard.R;
import com.channeli.noticeboard.SplashScreen;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

import objects.NoticeNotification;
import utilities.SQLHelper;

public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String APP_NAME = "Channel i NoticeBoard";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        JSONObject data=new JSONObject(remoteMessage.getData());
        String category="All";
        String main_category="All";
        String subject="";
        try {
            category=data.getString("category");
            main_category=data.getString("main_category");
            subject=data.getString("subject");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        NoticeNotification noticeNotification=new NoticeNotification(main_category,category,subject);
        //Calling method to send notification
        sendNotification(generateNotification(noticeNotification));
    }

    private Notification generateNotification(NoticeNotification noticeNotification){
        Intent intent = new Intent(this, SplashScreen.class);
        intent.putExtra("notification",true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.logo)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSound(defaultSoundUri)
                .setColor(getResources().getColor(R.color.colorAccent))
                .setShowWhen(true)
                .setTicker("New Notice!")
                .setLargeIcon(((BitmapDrawable)getResources().getDrawable(R.drawable.logo)).getBitmap())
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setContentTitle("New Notice : ")
                .setContentText(noticeNotification.getCategory()+" : "+noticeNotification.getSubject())
                .build();
    }
    private void sendNotification(Notification notification) {

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(APP_NAME, (int) (System.currentTimeMillis()%Integer.MAX_VALUE), notification);
    }
}