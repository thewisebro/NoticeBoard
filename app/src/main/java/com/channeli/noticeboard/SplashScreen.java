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

import com.google.firebase.messaging.FirebaseMessaging;
import com.squareup.leakcanary.LeakCanary;

import org.apache.http.client.methods.HttpGet;

import connections.SessIDGet;
import utilities.SQLHelper;

public class SplashScreen  extends Activity{
    private static int SPLASH_TIME_OUT = 1000;

//    SharedPreferences settings;
//    SharedPreferences.Editor editor;

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
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        final String channeli_sessid = settings.getString("CHANNELI_SESSID", "");
        final String csrftoken=settings.getString("csrftoken", "");
        if(!channeli_sessid.equals("")){
            if (isOnline()){
                new Thread(){
                    @Override
                    public void run(){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                HttpGet httpGet = new HttpGet(MainActivity.UrlOfLogin);
                                httpGet.addHeader("Cookie","csrftoken="+csrftoken);
                                httpGet.addHeader("Cookie", "CHANNELI_SESSID=" + channeli_sessid);
                                httpGet.addHeader("Accept", "application/xml");
                                httpGet.addHeader("Content-Type", "application/x-www-form-urlencoded");
                                try {
                                    Boolean valid=new SessIDGet().execute(httpGet).get();
                                    //True if logged in else False
                                    if (valid)
                                        goToMain();
                                    else
                                        goToLogin();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    goToMain();
                                }
                            }
                        });
                    }
                }.start();
            }
            else goToMain();
        }
        else goToLogin();
    }
    public void goToLogin(){
        SharedPreferences.Editor editor= getSharedPreferences(MainActivity.PREFS_NAME, 0).edit();
        editor.clear();
        editor.apply();
        new SQLHelper(this).clear();
        if (FirebaseMessaging.getInstance()!=null) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("Placement%20Office");
            FirebaseMessaging.getInstance().unsubscribeFromTopic("Authorities");
            FirebaseMessaging.getInstance().unsubscribeFromTopic("Departments");
        }
        Intent intent = new Intent(SplashScreen.this, Login.class);
        startActivity(intent);
        finish();
    }
    public void goToMain(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashScreen.this, Notices.class);
                if (getIntent().getBooleanExtra("notification",false))
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                else
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
    public boolean isOnline() {

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    public void onBackPressed(){
        super.onBackPressed();
        finish();
        //System.exit(0);
        //TODO close the app
    }
}
