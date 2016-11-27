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

public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        JSONObject data=new JSONObject(remoteMessage.getData());
        //Calling method to generate notification
        Notification notification=generateNotification(data);

        //Calling method to send notification
        sendNotification(notification);
    }

    private Notification generateNotification(JSONObject data){
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
        intent.putExtra("category",category);
        intent.putExtra("main_category",main_category);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("Channel i NoticeBoard")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);
        String text="";
        if (subject!="")
            notificationBuilder.setSubText("Subject : "+subject);
            //notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText("Subject : "+subject));
        if (main_category.contains("All"))
            notificationBuilder.setContentText("New Notice");
        else{
            if (main_category.contains("Placement"))
                notificationBuilder.setContentText("New Notice from "+main_category);
            else if(main_category.contains("Department"))
                notificationBuilder.setContentText("New Notice from "+main_category+" : "+category+" Department");
            else
                notificationBuilder.setContentText("New Notice from "+main_category+" : "+category);
        }
        return notificationBuilder.build();
    }
    private void sendNotification(Notification notification) {

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notification);
    }
}