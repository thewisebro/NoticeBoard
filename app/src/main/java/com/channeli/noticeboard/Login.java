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
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import connections.ConnectTaskHttpGet;
import connections.CookiesHttpGet;
import connections.CookiesHttpPost;
import objects.DrawerItem;
import utilities.Parsing;
import utilities.SQLHelper;


public class Login extends AppCompatActivity {
    List<NameValuePair> params=new ArrayList<NameValuePair>();
    HttpPost httpPost;
    HttpGet httpGet;
    String csrfToken;
    SharedPreferences settings;
    SharedPreferences.Editor editor;
    CoordinatorLayout coordinatorLayout;
    SQLHelper sqlHelper;
    EditText Username;
    EditText Password;
    Button button;
    View overButton;
    Boolean flag1=false,flag2=false;
    AsyncTask<HttpGet, Void, String> mTask=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.login);
        sqlHelper=new SQLHelper(this);
        settings=getSharedPreferences(MainActivity.PREFS_NAME, 0);
        editor=settings.edit();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        coordinatorLayout= (CoordinatorLayout) findViewById(R.id.login_container);
        coordinatorLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setViews();
                coordinatorLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

    }
    public void setViews(){
        findViewById(R.id.clear_focus).requestFocus();
        Username=(EditText) findViewById(R.id.username);
        Password=(EditText) findViewById(R.id.password);
        button= (Button) findViewById(R.id.submit);
        overButton= findViewById(R.id.overSubmit);
        overButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMessage("Enter Login Credentials");
            }
        });
        TextView link= (TextView) findViewById(R.id.link);
        link.setMovementMethod(LinkMovementMethod.getInstance());
        TextView img_love= (TextView) findViewById(R.id.made_with_love);
        String text = "Made with <font color=#F50057>"+String.valueOf(Character.toChars(0x2764))+"</font> by IMG";
        img_love.setText(Html.fromHtml(text));
        Username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (Username.getText().toString().trim().isEmpty())
                    flag1 = false;
                else
                    flag1 = true;
                if (flag1 && flag2) {
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
                if (Password.getText().toString().trim().isEmpty())
                    flag2 = false;
                else
                    flag2 = true;
                if (flag1 && flag2) {
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
        Snackbar snackbar=Snackbar.make(coordinatorLayout, msg, Snackbar.LENGTH_SHORT);
        TextView tv= (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(getResources().getColor(R.color.colorPrimary));
        tv.setHeight((int) getResources().getDimension(R.dimen.bottomBarHeight));
        tv.setTypeface(null, Typeface.BOLD);
        snackbar.show();
    }

    public void getConstants(){
        String constants=null;
        httpGet = new HttpGet(MainActivity.UrlOfNotice+"get_constants/");
        httpGet.addHeader("Cookie", "csrftoken=" + settings.getString("csrftoken", ""));
        httpGet.addHeader("Content-Type", "application/x-www-form-urlencoded");
        httpGet.addHeader("Cookie", "CHANNELI_SESSID=" + settings.getString("CHANNELI_SESSID", ""));
        try {
            mTask = new ConnectTaskHttpGet().execute(httpGet);
            constants = mTask.get(4000, TimeUnit.MILLISECONDS);
            mTask.cancel(true);
            ArrayList<DrawerItem> list=new Parsing().parseConstants(constants);
            if(list.size()>0){
                Set<String> constantSet=new HashSet<>(list.size());
                for(int i=0;i<list.size();i++) {
                    Set<String> s=new HashSet<>(list.get(i).getCategories());
                    editor.putStringSet(list.get(i).getName(),s);
                    constantSet.add(list.get(i).getName());
                }
                editor.putStringSet("constants", constantSet);
                editor.apply();
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            e.printStackTrace();
        }
    }

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

    public void login(View view) {
        hideKeyboard();
        button.setText("LOGGING IN");
        button.setClickable(false);
        overButton.setClickable(false);
        new Thread(){
            @Override
            public void run(){
                if (isConnected()){

                    final String usernameText= Username.getText().toString();
                    final String passwordText= Password.getText().toString();
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
/*
    public long getExpiryDate(){
        Calendar c=Calendar.getInstance();
        c.add(Calendar.DATE,15);
        Date date=c.getTime();
        return date.getTime();
    }
*/
    public void hideKeyboard(){
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
