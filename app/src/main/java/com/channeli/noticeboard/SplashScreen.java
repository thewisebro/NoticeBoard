package com.channeli.noticeboard;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

/*
 Created by manohar on 12/8/15.
 */

public class SplashScreen  extends Activity{
    private static int SPLASH_TIME_OUT = 2000;
    public String msg="YES", CHANNELI_SESSID;

    SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        //if(Build.VERSION.SDK_INT >= 21){
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //}
        settings = getSharedPreferences(MainActivity.PREFS_NAME,0);
        CHANNELI_SESSID = settings.getString("CHANNELI_SESSID","");

        if(!CHANNELI_SESSID.equals("")){

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(SplashScreen.this, MainActivity.class);
                        //intent.putExtra("category","All");
                        //intent.putExtra("main_category","All");
                        startActivity(intent);
                        finish();
                    }
                }, SPLASH_TIME_OUT);
        }
        else {
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
        }
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
