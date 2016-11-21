package connections;

import android.os.AsyncTask;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class CookiesHttpPost extends AsyncTask<HttpPost, Void, HashMap<String,String>>{
    HashMap<String,String> cookie_list;
    @Override
    protected HashMap<String,String> doInBackground(HttpPost[] params) {
        HttpParams httpParams=new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
        HttpConnectionParams.setSoTimeout(httpParams,10000);
        DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
        cookie_list = new HashMap<String,String>();
        try {
            httpClient.execute(params[0]);
            CookieStore cookieStore = httpClient.getCookieStore();
            List<Cookie> cookies =  cookieStore.getCookies();
            for (Cookie cookie: cookies) {
                /*if (cookie.getName().equals("CHANNELI_SESSID")){
                    cookie_list[1] = cookie.getValue();

                }
                if (cookie.getName().equals("csrftoken")) {
                    cookie_list[0] = cookie.getValue();
                }*/
                cookie_list.put(cookie.getName(),cookie.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return cookie_list;
    }
}
