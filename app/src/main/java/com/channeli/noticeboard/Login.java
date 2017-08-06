package com.channeli.noticeboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.squareup.leakcanary.LeakCanary;

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
import java.net.CookieManager;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import connections.AsynchronousGet;
import connections.ConnectTaskHttpGet;
import connections.CookiesHttpGet;
import connections.CookiesHttpPost;
import connections.SynchronousGet;
import connections.SynchronousPost;
import objects.DrawerItem;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import utilities.Parsing;
import utilities.SQLHelper;


public class Login extends AppCompatActivity {
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private CoordinatorLayout mCoordinatorLayout;
    private SQLHelper mSqlHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(getApplication());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.login);
        mSqlHelper=new SQLHelper(this);
        mSharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        mEditor = mSharedPreferences.edit();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        mCoordinatorLayout= (CoordinatorLayout) findViewById(R.id.login_container);
        mCoordinatorLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setViews();
                mCoordinatorLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

    }
    public void setViews(){
        findViewById(R.id.clear_focus).requestFocus();

        TextView link= (TextView) findViewById(R.id.link);
        link.setMovementMethod(LinkMovementMethod.getInstance());
        TextView img_love= (TextView) findViewById(R.id.made_with_love);
        String text = "Made with <font color=#F50057>"+String.valueOf(Character.toChars(0x2764))+"</font> by IMG";
        img_love.setText(Html.fromHtml(text));

        final EditText Username=(EditText) findViewById(R.id.username);
        final EditText Password=(EditText) findViewById(R.id.password);
        final Button button= (Button) findViewById(R.id.submit);
        final View overButton= findViewById(R.id.overSubmit);
        overButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMessage("Enter Login Credentials");
            }
        });
        Username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!Username.getText().toString().trim().isEmpty() && !Password.getText().toString().trim().isEmpty()) {
                    button.setEnabled(true);
                    overButton.setVisibility(View.GONE);
                } else {
                    button.setEnabled(false);
                    overButton.setVisibility(View.VISIBLE);
                }

            }
        });
        Password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!Username.getText().toString().trim().isEmpty() && !Password.getText().toString().trim().isEmpty()) {
                    button.setEnabled(true);
                    overButton.setVisibility(View.GONE);
                } else {
                    button.setEnabled(false);
                    overButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }
    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }
    public void showMessage(String msg){
        Snackbar snackbar=Snackbar.make(mCoordinatorLayout, msg, Snackbar.LENGTH_SHORT);
        TextView tv= (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(getResources().getColor(R.color.colorPrimary));
        tv.setHeight((int) getResources().getDimension(R.dimen.bottomBarHeight));
        tv.setTypeface(null, Typeface.BOLD);
        snackbar.show();
    }

 /*   public void getConstants() {

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cookie",
                "csrftoken=" + mSharedPreferences.getString("csrftoken", "") +
                        ";CHANNELI_SESSID=" + mSharedPreferences.getString("CHANNELI_SESSID", "")
        );
        headers.put("Content", "application/x-www-form-urlencoded");
        headers.put("Accept", "application/xml");

        try {
            Map<String, Object> response = new SynchronousGet() {
                @Override
                public OkHttpClient setClient() {
                    return new OkHttpClient();
                }
            }.getResponse(MainActivity.UrlOfNotice + "get_constants/", headers, null);

            if (response.get("status") == "fail") throw new IOException("");

            String constants = (String) response.get("body");
            ArrayList<DrawerItem> list = new Parsing().parseConstants(constants);
            if (list.size() > 0) {
                Set<String> constantSet = new HashSet<>(list.size());
                for (int i = 0; i < list.size(); i++) {
                    Set<String> s = new HashSet<>(list.get(i).getCategories());
                    mEditor.putStringSet(list.get(i).getName(), s);
                    constantSet.add(list.get(i).getName());
                }
                mEditor.putStringSet("constants", constantSet);
                mEditor.apply();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/


/*
    private void peopleLogin(String username, String password){

        httpGet=new HttpGet(MainActivity.UrlOfLogin);
        try {
            csrfToken= new CookiesHttpGet().execute(httpGet).get();
            if (csrfToken==null || csrfToken==""){
                showMessage("Check Network Connection!");
                return;
            }
            params.add(new BasicNameValuePair("username", username));
            params.add(new BasicNameValuePair("password",password));
            params.add(new BasicNameValuePair("csrfmiddlewaretoken", csrfToken));
            params.add(new BasicNameValuePair("remember_me","on"));

            httpPost=new HttpPost(MainActivity.UrlOfLogin);
            httpPost.addHeader("Cookie","csrftoken="+csrfToken);
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.addHeader("Accept", "application/xml");
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
                httpGet.addHeader("Cookie","csrftoken="+cookies.get("csrftoken"));
                httpGet.addHeader("Cookie", "CHANNELI_SESSID=" + cookies.get("CHANNELI_SESSID"));
                httpGet.addHeader("Accept", "application/xml");
                httpGet.addHeader("Content-Type", "application/x-www-form-urlencoded");

                mTask=new ConnectTaskHttpGet().execute(httpGet);
                JSONObject result = new JSONObject(mTask.get());
                mTask.cancel(true);
                String msg=result.getString("msg");
                if(msg.equals("YES")){
                    editor.putString("name", result.getString("_name"));
                    editor.putString("info", result.getString("info"));
                    editor.putString("enrollment_no", result.getString("enrollment_no"));
                    editor.putString("csrftoken",cookies.get("csrftoken"));
                    editor.putString("CHANNELI_SESSID", cookies.get("CHANNELI_SESSID"));
                    //editor.putLong("expiry_date",getExpiryDate());
                    editor.apply();
                    getConstants();
                    Intent intent = new Intent(this,MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
                else {
                    if(!isConnected()){
                        showMessage("Sorry! Could not connect. Check the internet connection!");
                    }
                    else if(result.getString("msg").contains("NO")){
                        showMessage("Wrong username or password!");
                    }
                    else{
                        showMessage("Sorry! Could not login! Try again later!");
                    }
                }
            }
            else if (cookies.containsKey("csrftoken"))
                showMessage("Wrong Credentials");
            else
                showMessage("Check Network Connection!");

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Check Network Connection!");
        }
    }
*/
    private Map<String,String> getCookiesList(List<Cookie> cookieList){
        Map<String,String> cookiesMap = new HashMap<>();
        for (Cookie cookie : cookieList){
            cookiesMap.put(cookie.name(),cookie.value());
        }
        return cookiesMap;
    }
    private void peopleLogin(String username, String password){
        try {
            SetCookieCache cookieCache = new SetCookieCache();
            SharedPrefsCookiePersistor cookiePersistor = new SharedPrefsCookiePersistor(getApplication());
            final ClearableCookieJar cookieJar = new PersistentCookieJar(
                    cookieCache,
                    cookiePersistor
            );
            cookieJar.clear();
            new SynchronousGet() {
                @Override
                public OkHttpClient setClient() {
                    return new OkHttpClient.Builder()
                            .cookieJar(cookieJar)
                            .followRedirects(false)
                            .followSslRedirects(false)
                            .build();
                }
            }.getResponse(MainActivity.UrlOfLogin,null,null);

            Map<String,String> params = new HashMap<>();
            params.put("username",username);
            params.put("password",password);
            params.put("remember_me","on");
            params.put("csrfmiddlewaretoken", getCookiesList(cookiePersistor.loadAll()).get(Notices.CSRF_TOKEN));

            new SynchronousPost() {
                @Override
                public OkHttpClient setClient() {
                    return new OkHttpClient.Builder()
                            .cookieJar(cookieJar)
                            .followRedirects(false)
                            .followSslRedirects(false)
                            .build();
                }
            }.getResponse(MainActivity.UrlOfLogin,null,params);

            if (!getCookiesList(cookiePersistor.loadAll()).containsKey(Notices.CHANNELI_SESSID)){
                throw new IOException("");
            }
            mEditor.putString("username",username);
            mEditor.putString(Notices.CSRF_TOKEN,getCookiesList(cookiePersistor.loadAll()).get(Notices.CSRF_TOKEN));
            mEditor.putString(Notices.CHANNELI_SESSID,getCookiesList(cookiePersistor.loadAll()).get(Notices.CHANNELI_SESSID));
            mEditor.apply();

            new AsynchronousGet() {
                @Override
                public void onSuccess(String responseBody, Headers responseHeaders, int responseCode) {
                    try {
                        JSONObject response = new JSONObject(responseBody);
                        if (response.getString("msg")!="YES") onFail(new JSONException(""));

                        mEditor.putString("name", response.getString("_name"));
                        mEditor.putString("info", response.getString("info"));
                        mEditor.putString("enrollment_no", response.getString("enrollment_no"));
                        mEditor.apply();

                        startActivity(new Intent(Login.this,Notices.class));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        onFail(new JSONException(""));
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFail(Exception e) {

                }

                @Override
                public OkHttpClient setClient() {
                    return new OkHttpClient.Builder()
                            .cookieJar(cookieJar)
                            .build();
                }
            }.getResponse(MainActivity.UrlOfPeopleSearch+"return_details/?username="+username,null,null);

            startActivity(new Intent(Login.this,MainActivity.class));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void login(View view) {
        hideKeyboard();
        final EditText Username=(EditText) findViewById(R.id.username);
        final EditText Password=(EditText) findViewById(R.id.password);
        final Button button= (Button) findViewById(R.id.submit);
        final View overButton= findViewById(R.id.overSubmit);
        button.setText("LOGGING IN");
        button.setClickable(false);
        overButton.setClickable(false);
        new Thread(){
            @Override
            public void run(){
                if (isConnected()){
                    String usernameText= Username.getText().toString().trim().replace("@iitr.ac.in","");
                    String passwordText= Password.getText().toString().trim();
                    if (usernameText.matches("")){
                        showMessage("Enter username");
                    }
                    else if (passwordText.matches("")){
                        showMessage("Enter password");
                    }
                    else {
                        peopleLogin(usernameText, passwordText);
                    }
                }
                else {
                    showMessage("Check network connection!");
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        button.setText("LOGIN");
                        button.setClickable(true);
                        overButton.setClickable(true);
                    }
                });
            }
        }.start();
    }
    public void hideKeyboard(){
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
