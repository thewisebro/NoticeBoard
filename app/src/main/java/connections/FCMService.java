package connections;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.channeli.noticeboard.MainActivity;
import com.channeli.noticeboard.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import objects.noticeNotification;
import utilities.SQLHelper;

public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private final int NOTIFICATION_ID=1803;
    private List<noticeNotification> notifications;
    private SQLHelper sqlHelper;
    private static int MAX_LINES=6;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        sqlHelper=new SQLHelper(this);

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

        noticeNotification noticeNotification=new noticeNotification(main_category,category,subject);
        notifications=new SQLHelper(this).getNotifications();
        sqlHelper.addNotification(noticeNotification);
        notifications.add(noticeNotification);      //this way to ensure one notification in case of exceptions/sql_errors

        //Calling method to send notification
        sendNotification(generateNotification());
    }

    private Notification generateNotification(){
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        //intent.putExtra("category",category);
        //intent.putExtra("main_category",main_category);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.InboxStyle inboxStyle=new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("New Notices : ");
        int i=0;
        for(;i<MAX_LINES && i<notifications.size();i++){
            noticeNotification notification=notifications.get(i);
            inboxStyle.addLine(notification.getCategory()+" : "+notification.getSubject());
        }
        if(i<notifications.size()){
            inboxStyle.setSummaryText("and "+(notifications.size()-i)+" more");
        }

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("Channel i NoticeBoard")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentText("New Notices have been uploaded!")
                .setStyle(inboxStyle)
                .setContentIntent(pendingIntent);

        return notificationBuilder.build();
    }
    private void sendNotification(Notification notification) {

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_ID, notification);
    }
}