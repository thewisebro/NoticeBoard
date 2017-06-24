package connections;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by thewisebro on 25/6/17.
 */

public abstract class AsynchronousPost {
    public abstract OkHttpClient getClient();
    public abstract void onFail(IOException e);
    public abstract void onSuccess(String responseBody, Headers responseHeaders);

    public void getResponse(String url, Map<String,String> headers, Map<String,String> params){
        Request.Builder requestBuilder = new Request.Builder();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();

        if (params==null) onFail(new IOException(""));

        FormBody.Builder bodyBuilder = new FormBody.Builder();
        for(Map.Entry<String,String> param : params.entrySet()){
            bodyBuilder.add(param.getKey(),param.getValue());
        }
        requestBuilder.post(bodyBuilder.build());

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
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                onFail(e);
            }

            @Override
            public void onResponse(Call call, final Response response) {
                //TODO: Raise proper exception
                if (!response.isSuccessful()) onFail(new IOException(""));

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            onSuccess(response.body().string(),response.headers());
                        } catch (IOException e) {
                            e.printStackTrace();
                            //TODO: Raise proper exception
                            onFail(new IOException(""));
                        }
                    }
                });

            }
        });
    }
}
