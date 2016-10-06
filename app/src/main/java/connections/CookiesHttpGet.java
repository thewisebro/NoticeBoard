package connections;

import android.os.AsyncTask;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.util.List;


public class CookiesHttpGet extends AsyncTask<HttpGet, Void, String>{

    @Override
    protected String doInBackground(HttpGet... params) {
        String csrfToken="";
        HttpParams httpParams=new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
        HttpConnectionParams.setSoTimeout(httpParams,5000);
        DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
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
