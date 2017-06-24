package connections;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by thewisebro on 24/6/17.
 */

public abstract class SynchronousGet {
//    private final OkHttpClient mClient=new OkHttpClient();

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

        Response response = getClient().newCall(requestBuilder.build()).execute();
        if (!response.isSuccessful()) throw new IOException("");

        responseMap.put("headers",response.headers());
        responseMap.put("body",response.body().string());
        responseMap.put("status","success");

        return responseMap;
    }
}
