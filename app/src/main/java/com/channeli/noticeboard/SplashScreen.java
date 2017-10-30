package com.channeli.noticeboard;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.google.firebase.messaging.FirebaseMessaging;
import com.squareup.leakcanary.LeakCanary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import connections.AsynchronousGet;
import connections.FCMIDService;
import okhttp3.Cookie;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import utilities.SQLHelper;

public class SplashScreen  extends Activity{
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

        mSharedPreferences = getSharedPreferences(Constants.PREFS_NAME, 0);
        mEditor = mSharedPreferences.edit();
        mSessid = mSharedPreferences.getString(Constants.CHANNELI_SESSID, "");
        mCsrfToken = mSharedPreferences.getString(Constants.CSRF_TOKEN, "");


        SetCookieCache cookieCache = new SetCookieCache();
        SharedPrefsCookiePersistor cookiePersistor = new SharedPrefsCookiePersistor(getApplication());
        mCookieJar = new PersistentCookieJar( cookieCache, cookiePersistor);

        new FCMIDService().sendRegistrationToServer(this);
        if (!mSessid.isEmpty()){
            if (cookiePersistor.loadAll().isEmpty()){
                List<Cookie> cookieList = new ArrayList<Cookie>(2);
                cookieList.add(new Cookie.Builder().name(Constants.CSRF_TOKEN).value(mCsrfToken).domain(Constants.DOMAIN_URL).build());
                cookieList.add(new Cookie.Builder().name(Constants.CHANNELI_SESSID).value(mSessid).domain(Constants.DOMAIN_URL).build());
                cookieCache.addAll(cookieList);
                cookiePersistor.saveAll(cookieList);
            }

            new AsynchronousGet() {
                @Override
                public OkHttpClient setClient() {
                    return new OkHttpClient.Builder()
                            .cookieJar(mCookieJar)
                            .connectTimeout(20, TimeUnit.SECONDS)
                            .readTimeout(30,TimeUnit.SECONDS)
                            .writeTimeout(30,TimeUnit.SECONDS)
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
                        onFail(new Exception("Login Fail"));
                    }
                }

                @Override
                public void onFail(Exception e) {
                    //Go to login page
                    goToLogin();
                }
            }.getResponse(Constants.LOGIN_URL,null,null);
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
