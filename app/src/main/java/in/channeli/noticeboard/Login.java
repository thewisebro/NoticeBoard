package in.channeli.noticeboard;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
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
import java.util.HashMap;
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
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
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

    private void peopleLogin(String username, String password){

        httpGet=new HttpGet(MainActivity.UrlOfLogin);
        try {
            csrfToken= new CookiesHttpGet().execute(httpGet).get();
            params.add(new BasicNameValuePair("username", username));
            params.add(new BasicNameValuePair("password",password));
            params.add(new BasicNameValuePair("csrfmiddlewaretoken", csrfToken));
            params.add(new BasicNameValuePair("remember_me","on"));

            httpPost=new HttpPost(MainActivity.UrlOfLogin);
            httpPost.setHeader("Cookie","csrftoken="+csrfToken);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setHeader("Accept", "application/xml");
            httpPost.setEntity(new UrlEncodedFormEntity(params));

            HashMap<String, String> cookies =new CookiesHttpPost().execute(httpPost).get();
            BasicClientCookie csrf_cookie = new BasicClientCookie("csrftoken", cookies.get("csrftoken"));
            DefaultHttpClient httpClient = new DefaultHttpClient();
            org.apache.http.client.CookieStore cookieStore = httpClient.getCookieStore();
            cookieStore.addCookie(csrf_cookie);
// Create local HTTP context - to store cookies
            HttpContext localContext = new BasicHttpContext();
// Bind custom cookie store to the local context
            localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            if(cookies.containsKey("CHANNELI_SESSID") ){
                httpGet = new HttpGet(MainActivity.UrlOfPeopleSearch+"return_details/?username="+username);
                httpGet.setHeader("Cookie","csrftoken="+cookies.get("csrftoken"));
                httpGet.setHeader("Cookie", "CHANNELI_SESSID=" + cookies.get("CHANNELI_SESSID"));
                httpGet.setHeader("Accept", "application/xml");
                httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                JSONObject result = new JSONObject(new ConnectTaskHttpGet().execute(httpGet).get());
                String msg=result.getString("msg");
                if(msg.equals("YES")){
                    editor.putString("name", result.getString("_name"));
                    editor.putString("info", result.getString("info"));
                    editor.putString("enrollment_no", result.getString("enrollment_no"));
                    editor.putString("csrftoken",cookies.get("csrftoken"));
                    editor.putString("CHANNELI_SESSID",cookies.get("CHANNELI_SESSID"));
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
            else if (cookies.containsKey("csrftoken"))
                Toast.makeText(getApplicationContext(),"Wrong Credentials",Toast.LENGTH_SHORT).show();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void login(View view) {
        if (isConnected()){
            EditText Username=(EditText) findViewById(R.id.username);
            EditText Password=(EditText) findViewById(R.id.password);
            final String usernameText= Username.getText().toString();
            final String passwordText= Password.getText().toString();
            if (usernameText.matches("")){
                Toast.makeText(getApplicationContext(),"Enter username", Toast.LENGTH_SHORT).show();
            }
            else if (passwordText.matches("")){
                Toast.makeText(getApplicationContext(),"Enter password", Toast.LENGTH_SHORT).show();
            }
            else {
                final ProgressDialog progressDialog= ProgressDialog.show(this,"Signing In","Please Wait...",true,false);
                Thread thread=new Thread(){
                    @Override
                    public void run(){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                peopleLogin(usernameText,passwordText);
                                progressDialog.dismiss();
                            }
                        });
                    }
                };
                thread.start();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "Check network connection!", Toast.LENGTH_LONG).show();
        }
    }
}
