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

import java.util.List;

import objects.noticeNotification;
import utilities.SQLHelper;

public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String APP_NAME = "Channel i NoticeBoard";
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
        notifications=sqlHelper.getNotifications();
        sqlHelper.addNotification(noticeNotification);
        notifications.add(noticeNotification);      //this way to ensure one notification in case of exceptions/sql_errors

        //Calling method to send notification
        sendNotification(generateNotification());
    }

    private Notification generateNotification(){
        Intent intent = new Intent(this, SplashScreen.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("notification",true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

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
                //.setContentTitle("Channel i NoticeBoard")
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSound(defaultSoundUri)
                //.setContentText("New Notices have been uploaded!")
                .setNumber(notifications.size())
                //.setStyle(inboxStyle)
                .setColor(getResources().getColor(R.color.colorAccent))
                .setShowWhen(true)
                .setTicker("New Notice!")
                .setLargeIcon(((BitmapDrawable)getResources().getDrawable(R.drawable.logo)).getBitmap())
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);
        if (notifications.size()>1){
            notificationBuilder.setContentTitle("Channel i NoticeBoard");
            notificationBuilder.setContentText("New Notices have been uploaded!");
            notificationBuilder.setStyle(inboxStyle);
        }
        else{
            notificationBuilder.setContentTitle("New Notice : ");
            notificationBuilder.setContentText(notifications.get(0).getCategory()+" : "+notifications.get(0).getSubject());
        }
        return notificationBuilder.build();
    }
    private void sendNotification(Notification notification) {

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(APP_NAME,NOTIFICATION_ID, notification);
    }
}