package connections;

import android.content.SharedPreferences;

import com.channeli.noticeboard.Constants;
import com.channeli.noticeboard.Notices;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashSet;
import java.util.Set;

public class FCMIDService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {

        // TODO: Implement this method to send any registration to your app's servers.
        sendRegistrationToServer(getSharedPreferences(Constants.PREFS_NAME,0));
    }

    //Method to store the token on your server
    public void sendRegistrationToServer(SharedPreferences preferences) {

        SharedPreferences.Editor editor= preferences.edit();

        //Getting registration token
        String token = FirebaseInstanceId.getInstance().getToken();

        String csrftoken=preferences.getString("csrftoken", "");
        String channeli_sessid=preferences.getString("CHANNELI_SESSID","");
        //Check user logged in
        if (channeli_sessid!=""){

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
        }
    }
}