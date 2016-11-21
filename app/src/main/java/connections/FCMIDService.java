package connections;

import android.content.SharedPreferences;
import android.util.Log;

import com.channeli.noticeboard.MainActivity;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

public class FCMIDService extends FirebaseInstanceIdService {

    private static final String TAG = "FCMIDService";

    @Override
    public void onTokenRefresh() {

        //Getting registration token
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        //Displaying token on logcat 
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        // TODO: Implement this method to send any registration to your app's servers.
        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String token) {
        //You can implement this method to store the token on your server 
        //Not required for current project

        SharedPreferences preferences=getSharedPreferences(MainActivity.PREFS_NAME,0);
        String csrftoken=preferences.getString("csrftoken", "");
        String CHANNELI_SESSID=preferences.getString("CHANNELI_SESSID","");

        try {
            HttpPost httpPost=new HttpPost(MainActivity.UrlOfNotice);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setHeader("Cookie", "csrftoken=" + csrftoken);
            httpPost.setHeader("Cookie", "CHANNELI_SESSID=" + CHANNELI_SESSID);
            httpPost.setHeader("CHANNELI_DEVICE", "android");
            httpPost.setHeader("X-CSRFToken=", csrftoken);
            httpPost.setEntity(new StringEntity(token));
            new ConnectTaskHttpPost().execute(httpPost);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}