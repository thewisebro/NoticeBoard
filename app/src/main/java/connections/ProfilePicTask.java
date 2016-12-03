package connections;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.InputStream;


public class ProfilePicTask extends AsyncTask<HttpGet, Void, Bitmap> {
    @Override
    protected Bitmap doInBackground(HttpGet... httpPosts) {
// TODO Auto-generated method stub
        InputStream isr = null;
        Bitmap result=null;
        try{
            HttpParams httpParams=new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams,20000);
            HttpConnectionParams.setSoTimeout(httpParams,20000);
            HttpClient httpClient = new DefaultHttpClient(httpParams);
            HttpResponse response = httpClient.execute(httpPosts[0]);
            HttpEntity entity = response.getEntity();
            isr = entity.getContent();
            result= BitmapFactory.decodeStream(isr);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }
}
