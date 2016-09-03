package in.channeli.noticeboard;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import connections.ConnectTaskHttpGet;
import connections.CookiesHttpGet;
import connections.CookiesHttpPost;


public class Login extends Activity {
    List<NameValuePair> params=new ArrayList<NameValuePair>();
    HttpPost httpPost;
    HttpGet httpGet;
    String csrfToken;
    SharedPreferences settings;
    SharedPreferences.Editor editor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        settings=getSharedPreferences(MainActivity.PREFS_NAME, 0);
        editor=settings.edit();
        setContentView(R.layout.login_page);
    }
    public boolean isConnected(){
        Runtime runtime = Runtime.getRuntime();
        try {

            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);

        } catch (IOException e)          { e.printStackTrace(); }
        catch (InterruptedException e) { e.printStackTrace(); }

        return false;
    }

    public void login(View view) throws ExecutionException, InterruptedException, UnsupportedEncodingException, JSONException {
        if (isConnected()){
            EditText Username=(EditText) findViewById(R.id.username);
            EditText Password=(EditText) findViewById(R.id.password);
            String usernameText= Username.getText().toString();
            String passwordText= Password.getText().toString();
            if (usernameText.matches("")){
                Toast.makeText(getApplicationContext(),"Enter username", Toast.LENGTH_SHORT).show();
            }
            else if (passwordText.matches("")){
                Toast.makeText(getApplicationContext(),"Enter password", Toast.LENGTH_SHORT).show();
            }
            else {

                httpGet=new HttpGet(MainActivity.UrlOfLogin);
                csrfToken= new CookiesHttpGet().execute(httpGet).get();

                params.add(new BasicNameValuePair("username", usernameText));
                params.add(new BasicNameValuePair("password",passwordText));
                params.add(new BasicNameValuePair("csrfmiddlewaretoken", csrfToken));
                params.add(new BasicNameValuePair("remember_me","on"));

                httpPost=new HttpPost(MainActivity.UrlOfLogin);
                httpPost.setHeader("Cookie","csrftoken="+csrfToken);
                //httpPost.setHeader("Cookie", "CHANNELI_DEVICE=android");
                httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpPost.setHeader("Accept", "application/xml");
                httpPost.setEntity(new UrlEncodedFormEntity(params));

                String cookies[] =new CookiesHttpPost().execute(httpPost).get();
                BasicClientCookie csrf_cookie = new BasicClientCookie("csrftoken", cookies[0]);
                DefaultHttpClient httpClient = new DefaultHttpClient();
                org.apache.http.client.CookieStore cookieStore = httpClient.getCookieStore();
                cookieStore.addCookie(csrf_cookie);
// Create local HTTP context - to store cookies
                HttpContext localContext = new BasicHttpContext();
// Bind custom cookie store to the local context
                localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

                if(cookies[1]!=null && cookies[1]!=""){
                    httpGet = new HttpGet(MainActivity.UrlOfPeopleSearch+"return_details/?username="+usernameText);
                    httpGet.setHeader("Cookie","csrftoken="+cookies[0]);
                    httpGet.setHeader("Cookie", "CHANNELI_SESSID=" + cookies[1]);
                    httpGet.setHeader("Accept", "application/xml");
                    httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                    JSONObject result = new JSONObject(new ConnectTaskHttpGet().execute(httpGet).get());
                    String msg=result.getString("msg");
                    if(msg.equals("YES")){
                        editor.putString("name", result.getString("_name"));
                        editor.putString("info", result.getString("info"));
                        editor.putString("enrollment_no", result.getString("enrollment_no"));
                        editor.putString("csrftoken",cookies[0]);
                        editor.putString("CHANNELI_SESSID",cookies[1]);
                        editor.commit();
                        editor.apply();
                        Intent intent = new Intent(this,MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else {
                        if(!isConnected()){
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "Sorry! Could not connect. Check the internet connection!", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        else if(result.getString("msg").contains("NO")){
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "Wrong username or password!", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        else{
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "Sorry! Could not login! Try again later!", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                }
                else if (cookies[0]!=null)
                    Toast.makeText(getApplicationContext(),"Wrong Credentials",Toast.LENGTH_SHORT).show();

            }
        }
        else {
            Toast.makeText(getApplicationContext(), "Check network connection!", Toast.LENGTH_LONG).show();
        }
    }
}
