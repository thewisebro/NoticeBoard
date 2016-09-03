package connections;

import android.os.AsyncTask;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.List;

/*
Created by manohar on 26/11/15.
 */
public class CookiesHttpGet extends AsyncTask<HttpGet, Void, String>{

    @Override
    protected String doInBackground(HttpGet... params) {
        String csrfToken="";
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            httpClient.execute(params[0]);
            CookieStore cookieStore = httpClient.getCookieStore();
            List<Cookie> cookies =  cookieStore.getCookies();
            for (Cookie cookie: cookies) {
                if (cookie.getName().equals("csrftoken")) {
                    csrfToken = cookie.getValue();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return csrfToken;
    }
}
