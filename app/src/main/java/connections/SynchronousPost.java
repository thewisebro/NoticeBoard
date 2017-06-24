package connections;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

/**
 * Created by thewisebro on 24/6/17.
 */

public abstract class SynchronousPost {
    public abstract OkHttpClient getClient();

    public Map getResponse(String url, Map<String,String> headers, Map<String,String> params) throws IOException {
        Map<String,Object> responseMap = new HashMap<String,Object>();
        responseMap.put("status","fail");

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

        //Add Request Body
        if (params==null) throw new IOException("");

        FormBody.Builder bodyBuilder = new FormBody.Builder();
        for(Map.Entry<String,String> param : params.entrySet()){
            bodyBuilder.add(param.getKey(),param.getValue());
        }
        requestBuilder.post(bodyBuilder.build());

        Response response = getClient().newCall(requestBuilder.build()).execute();
        if (!response.isSuccessful()) throw new IOException("");

        responseMap.put("body",response.body().string());
        responseMap.put("headees",response.headers());
        responseMap.put("status","success");

        return responseMap;
    }
}
