package connections;

import android.os.AsyncTask;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.util.List;

/**
 * Created by Ankush on 26-12-2016.
 */
public class SessIDGet extends AsyncTask<HttpGet, Void, String> {

    @Override
    protected String doInBackground(HttpGet... params)  {
        String sessid="";
        HttpParams httpParams=new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
        HttpConnectionParams.setSoTimeout(httpParams,5000);
        DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
        try {
            CookieStore cookieStore = httpClient.getCookieStore();
            List<Cookie> cookies =  cookieStore.getCookies();
            for (Cookie cookie: cookies) {
                if (cookie.getName().equals("CHANNELI_SESSID")) {
                    sessid = cookie.getValue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sessid;
    }
}
