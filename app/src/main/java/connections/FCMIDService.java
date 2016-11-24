package connections;

import android.content.SharedPreferences;

import com.channeli.noticeboard.MainActivity;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

public class FCMIDService extends FirebaseInstanceIdService {

    private static final String TAG = "FCMIDService";

    @Override
    public void onTokenRefresh() {

        // TODO: Implement this method to send any registration to your app's servers.
        sendRegistrationToServer(getSharedPreferences(MainActivity.PREFS_NAME,0));
    }

    public void sendRegistrationToServer(SharedPreferences preferences) {

        //Getting registration token
        String token = FirebaseInstanceId.getInstance().getToken();

        //You can implement this method to store the token on your server 
        //Not required for current project

        String csrftoken=preferences.getString("csrftoken", "");
        String CHANNELI_SESSID=preferences.getString("CHANNELI_SESSID","");
        //Check user logged in
        if (CHANNELI_SESSID!=""){
            try {
                HttpPost httpPost=new HttpPost(Config.GCM_APP_SERVER_URL);
                httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpPost.setHeader("Cookie", "csrftoken=" + csrftoken);
                httpPost.setHeader("Cookie", "CHANNELI_SESSID=" + CHANNELI_SESSID);
                httpPost.setHeader("CHANNELI_DEVICE", "android");
                httpPost.setHeader("X-CSRFToken=", csrftoken);
                List<NameValuePair> params=new ArrayList<>();
                params.add(new BasicNameValuePair("auth",Config.FCM_SERVER_KEY));
                params.add(new BasicNameValuePair("endpoint","https://fcm.googleapis.com/fcm/send"));
                params.add(new BasicNameValuePair("p256dh", token));
                httpPost.setEntity(new UrlEncodedFormEntity(params));
                String res=new ConnectTaskHttpPost().execute(httpPost).get();
                if (res.contains("Done") || res.contains("Exists")) {
                    preferences.edit().putBoolean("FCM_isRegistered", true);
                    preferences.edit().apply();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}