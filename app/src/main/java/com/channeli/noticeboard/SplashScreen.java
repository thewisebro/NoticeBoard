package com.channeli.noticeboard;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.google.firebase.messaging.FirebaseMessaging;
import com.squareup.leakcanary.LeakCanary;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import java.util.ArrayList;
import java.util.List;

import connections.AsynchronousGet;
import connections.SessIDGet;
import connections.SynchronousGet;
import okhttp3.Cookie;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import utilities.SQLHelper;

public class SplashScreen  extends Activity{
    private static int SPLASH_TIME_OUT = 1000;
    private String mSessid;
    private String mCsrfToken;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private ClearableCookieJar mCookieJar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(getApplication());
        setContentView(R.layout.splash_screen);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mSharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        mEditor = mSharedPreferences.edit();
        mSessid = mSharedPreferences.getString(Notices.CHANNELI_SESSID, "");
        mCsrfToken = mSharedPreferences.getString(Notices.CSRF_TOKEN, "");


        SetCookieCache cookieCache = new SetCookieCache();
        SharedPrefsCookiePersistor cookiePersistor = new SharedPrefsCookiePersistor(getApplication());
        mCookieJar = new PersistentCookieJar( cookieCache, cookiePersistor);

        if (!mSessid.isEmpty()){
            if (cookiePersistor.loadAll().isEmpty()){
                List<Cookie> cookieList = new ArrayList<Cookie>(2);
                cookieList.add(new Cookie.Builder().name(Notices.CSRF_TOKEN).value(mCsrfToken).domain(Notices.HOST_URL).build());
                cookieList.add(new Cookie.Builder().name(Notices.CHANNELI_SESSID).value(mSessid).domain(Notices.HOST_URL).build());
                cookieCache.addAll(cookieList);
                cookiePersistor.saveAll(cookieList);
            }

            new AsynchronousGet() {
                @Override
                public OkHttpClient setClient() {
                    return new OkHttpClient.Builder()
                            .cookieJar(mCookieJar)
                            .followRedirects(false)
                            .followSslRedirects(false)
                            .build();
                }

                @Override
                public void onSuccess(String responseBody, Headers responseHeaders, int responseCode) {
                    //Go to main activity
                    if (responseCode >=300 && responseCode<400) {
                        Intent intent = new Intent(SplashScreen.this, Notices.class);
                        startActivity(intent);
                        finish();
                    } else{
                        onFail(new Exception(""));
                    }
                }

                @Override
                public void onFail(Exception e) {
                    //Go to login page
                    goToLogin();
                }
            }.getResponse(Notices.LOGIN_URL,null,null);
        } else{
            goToLogin();
        }
    }
    public void goToLogin(){
        mEditor.clear();
        mEditor.apply();
        new SQLHelper(getApplicationContext()).clear();
        mCookieJar.clear();
        mCookieJar.clearSession();
        if (FirebaseMessaging.getInstance()!=null) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("Placement%20Office");
            FirebaseMessaging.getInstance().unsubscribeFromTopic("Authorities");
            FirebaseMessaging.getInstance().unsubscribeFromTopic("Departments");
        }
        Intent intent = new Intent(SplashScreen.this, Login.class);
        startActivity(intent);
        finish();
    }

    public void onBackPressed(){
        super.onBackPressed();
        finish();
        //System.exit(0);
        //TODO close the app
    }
}
