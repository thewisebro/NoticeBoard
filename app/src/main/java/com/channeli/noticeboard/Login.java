package com.channeli.noticeboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
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

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.squareup.leakcanary.LeakCanary;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import connections.SynchronousGet;
import connections.SynchronousPost;
import okhttp3.Cookie;
import okhttp3.OkHttpClient;

public class Login extends AppCompatActivity {
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private CoordinatorLayout mCoordinatorLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(getApplication());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.login);
        mSharedPreferences = getSharedPreferences(Constants.PREFS_NAME, 0);
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
    public void showMessage(final String msg){
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Snackbar snackbar=Snackbar.make(mCoordinatorLayout, msg, Snackbar.LENGTH_SHORT);
                TextView tv= (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                tv.setGravity(Gravity.CENTER);
                tv.setTextColor(getResources().getColor(R.color.colorPrimary));
                tv.setHeight((int) getResources().getDimension(R.dimen.bottomBarHeight));
                tv.setTypeface(null, Typeface.BOLD);
                snackbar.show();
            }
        });
    }

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
                            .connectTimeout(20, TimeUnit.SECONDS)
                            .readTimeout(30,TimeUnit.SECONDS)
                            .writeTimeout(30,TimeUnit.SECONDS)
                            .followRedirects(false)
                            .followSslRedirects(false)
                            .build();
                }
            }.getResponse(Constants.LOGIN_URL,null,null);

            Map<String,String> params = new HashMap<>();
            params.put("username",username);
            params.put("password",password);
            params.put("remember_me","on");
            params.put("csrfmiddlewaretoken", getCookiesList(cookiePersistor.loadAll()).get(Constants.CSRF_TOKEN));

            new SynchronousPost() {
                @Override
                public OkHttpClient setClient() {
                    return new OkHttpClient.Builder()
                            .cookieJar(cookieJar)
                            .connectTimeout(20, TimeUnit.SECONDS)
                            .readTimeout(30,TimeUnit.SECONDS)
                            .writeTimeout(30,TimeUnit.SECONDS)
                            .followRedirects(false)
                            .followSslRedirects(false)
                            .build();
                }
            }.getResponse(Constants.LOGIN_URL,null,params);

            if (!getCookiesList(cookiePersistor.loadAll()).containsKey(Constants.CHANNELI_SESSID)){
                showMessage("Wrong Credentials");
                return;
            }
            mEditor.putString("username",username);
            mEditor.putString(Constants.CSRF_TOKEN,getCookiesList(cookiePersistor.loadAll()).get(Constants.CSRF_TOKEN));
            mEditor.putString(Constants.CHANNELI_SESSID,getCookiesList(cookiePersistor.loadAll()).get(Constants.CHANNELI_SESSID));
            mEditor.apply();

            startActivity(new Intent(Login.this,Notices.class));
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Check Network Connection");
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
