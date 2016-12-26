package com.channeli.noticeboard;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import org.apache.http.client.methods.HttpGet;

import java.util.concurrent.TimeUnit;

import connections.SessIDGet;

public class SplashScreen  extends Activity{
    private static int SPLASH_TIME_OUT = 1000;
    public String CHANNELI_SESSID, csrftoken;

    SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        //if(Build.VERSION.SDK_INT >= 21){
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //}
        settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        CHANNELI_SESSID = settings.getString("CHANNELI_SESSID", "");
        csrftoken=settings.getString("csrftoken","");

        long expiry_date=settings.getLong("expiry_date",0);
        if(!CHANNELI_SESSID.equals("") && System.currentTimeMillis()<expiry_date){
            if (isOnline()){
                new Thread(){
                    @Override
                    public void run(){
                        HttpGet httpGet = new HttpGet(MainActivity.UrlOfLogin);
                        //httpGet.setHeader("Cookie","CHANNELI_SESSID="+csrftoken);
                        httpGet.setHeader("Cookie", "CHANNELI_SESSID=" + CHANNELI_SESSID);
                        httpGet.setHeader("Accept", "application/xml");
                        httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");

                        try {
                            String sessID=new SessIDGet().execute(httpGet).get(5000, TimeUnit.MILLISECONDS);
                            //If logged in no 'set-cookie' is received, thus sessID should be empty if logged in
                            if ("".equals(sessID)){
                                goToMain();
                            }
                            else
                                goToLogin();
                        } catch (Exception e) {
                            e.printStackTrace();
                            goToMain();
                        }
                    }
                }.start();
            }
            else
                goToMain();
        }
        else
            goToLogin();
    }
    public void goToLogin(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashScreen.this, Login.class);
                startActivity(intent);
                finish();
            }
        });
    }
    public void goToMain(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(SplashScreen.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                }, SPLASH_TIME_OUT);
            }
        });
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
