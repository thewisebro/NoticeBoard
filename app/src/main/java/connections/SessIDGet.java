package connections;

import android.os.AsyncTask;

import com.channeli.noticeboard.MainActivity;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * Created by Ankush on 26-12-2016.
 */
public class SessIDGet extends AsyncTask<HttpGet, Void, Boolean> {

    @Override
    protected Boolean doInBackground(HttpGet... params)  {
        boolean valid=true;
        HttpParams httpParams=new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
        HttpConnectionParams.setSoTimeout(httpParams, 5000);
        HttpClientParams.setRedirecting(httpParams,false);
        DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
        try {
            HttpResponse response=httpClient.execute(params[0]);
            if (response.getStatusLine().getStatusCode()== 302){
                if (response.containsHeader("Location")) {
                    if (MainActivity.UrlOfHost.contains(response.getFirstHeader("Location").getValue().trim()))
                        valid = true;
                    else
                        valid = false;
                }
                else
                    valid=false;
            }
            else
                valid=false;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return valid;
    }
}
