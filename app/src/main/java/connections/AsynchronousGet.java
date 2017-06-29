package connections;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Headers;

/**
 * Created by thewisebro on 24/6/17.
 */

public abstract class AsynchronousGet {

    private OkHttpClient mClient;
    private boolean mCompleted;

    public abstract OkHttpClient setClient();
    public abstract void onSuccess(String responseBody, Headers responseHeaders, int responseCode);
    public abstract void onFail(Exception e);

    public OkHttpClient getClient(){ return this.mClient; }
    public boolean isCompleted(){ return this.mCompleted; }
    public void setCompletion(boolean flag){ this.mCompleted = flag; }

    public AsynchronousGet(){
        this.mCompleted = true;
        this.mClient = setClient();
        if (mClient == null) this.mClient = new OkHttpClient();
    }

    public void getResponse(String url, Map<String,String> headers, Map<String,String> params){
        setCompletion(false);
        Request.Builder requestBuilder = new Request.Builder();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        //Add get parameters to url
        if (params != null) {
            for (Map.Entry<String, String> param : params.entrySet()) {
                urlBuilder.addQueryParameter(param.getKey(), param.getValue());
            }
        }
        //Provide url to requestBuilder
        requestBuilder.url(urlBuilder.build());

        //Add Headers
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.addHeader(header.getKey(), header.getValue());
            }
        }

        getClient().newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                e.printStackTrace();
                setCompletion(true);
                onFail(e);
            }

            @Override
            public void onResponse(Call call, final Response response) {
                //TODO: Raise proper exception
                if (!response.isSuccessful() && !response.isRedirect()) onFail(new IOException(""));
                setCompletion(true);

                try {
                    onSuccess(response.body().string(),response.headers(),response.code());
                } catch (IOException e) {
                    e.printStackTrace();
                    //TODO: Raise proper exception
                    onFail(new IOException(""));
                }
            }
        });
    }
}
