package connections;

import android.content.SharedPreferences;

import com.channeli.noticeboard.MainActivity;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FCMIDService extends FirebaseInstanceIdService {

    private static final String TAG = "FCMIDService";
    private static final String FCM_APP_SERVER_URL =
            MainActivity.UrlOfNotice+"fcm_register/";

    @Override
    public void onTokenRefresh() {

        // TODO: Implement this method to send any registration to your app's servers.
        sendRegistrationToServer(getSharedPreferences(MainActivity.PREFS_NAME,0));
    }

    //Method to store the token on your server
    public void sendRegistrationToServer(SharedPreferences preferences) {

        SharedPreferences.Editor editor= preferences.edit();

        //Getting registration token
        String token = FirebaseInstanceId.getInstance().getToken();

        String csrftoken=preferences.getString("csrftoken", "");
        String CHANNELI_SESSID=preferences.getString("CHANNELI_SESSID","");
        //Check user logged in
        if (CHANNELI_SESSID!=""){

            //Subscribe to topics
            Set<String> subscriptions = preferences.getStringSet("subscriptions",null);
            if (subscriptions==null){
                subscriptions=new HashSet<String>();
                subscriptions.add("Placement Office");
                subscriptions.add("Authorities");
                subscriptions.add("Departments");
                editor.putStringSet("subscriptions", subscriptions);
            }
            for (String s: subscriptions){
                FirebaseMessaging.getInstance().subscribeToTopic(s.replace(" ","%20"));
            }
            //Code below is not necessary
            try {
                HttpPost httpPost=new HttpPost(FCM_APP_SERVER_URL);
                httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpPost.setHeader("Cookie", "csrftoken=" + csrftoken);
                httpPost.setHeader("Cookie", "CHANNELI_SESSID=" + CHANNELI_SESSID);
                httpPost.setHeader("CHANNELI_DEVICE", "android");
                httpPost.setHeader("X-CSRFToken=", csrftoken);
                List<NameValuePair> params=new ArrayList<>();
                params.add(new BasicNameValuePair("token",token));
                httpPost.setEntity(new UrlEncodedFormEntity(params));
                String res=new ConnectTaskHttpPost().execute(httpPost).get();
                if (res.contains("success") || res.contains("exists")) {
                    editor.putBoolean("FCM_isRegistered", true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            editor.apply();
        }
    }
}