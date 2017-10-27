package com.channeli.noticeboard;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.squareup.leakcanary.LeakCanary;

import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import connections.AsynchronousGet;
import objects.NoticeInfo;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import utilities.Parsing;
import utilities.SQLHelper;

public class Notice extends AppCompatActivity {
    private NoticeInfo mNoticeInfo;
    private String mSessid;
    private String mCsrfToken;
    private CookieJar mCookieJar;
    private SQLHelper mSqlHelper;
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(getApplication());
        setContentView(R.layout.notice);

        Toolbar toolbar= findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences sharedPreferences = getSharedPreferences(Notices.PREFS_NAME,0);
        mSessid = sharedPreferences.getString(Notices.CHANNELI_SESSID,"");
        mCsrfToken = sharedPreferences.getString(Notices.CSRF_TOKEN,"");
        mSqlHelper = new SQLHelper(this);

        //Set up Cookies for networking
        SetCookieCache cookieCache = new SetCookieCache();
        CookiePersistor cookiePersistor = new SharedPrefsCookiePersistor(getApplication());
        if (cookiePersistor.loadAll().isEmpty()){
            List<Cookie> cookieList = new ArrayList<Cookie>(2);
            cookieList.add(new Cookie.Builder().name(Notices.CSRF_TOKEN).value(mCsrfToken).domain(Notices.HOST_URL).build());
            cookieList.add(new Cookie.Builder().name(Notices.CHANNELI_SESSID).value(mSessid).domain(Notices.HOST_URL).build());
            cookieCache.addAll(cookieList);
            cookiePersistor.saveAll(cookieList);
        }
        mCookieJar = new PersistentCookieJar(cookieCache,cookiePersistor);


        fetchNotice(getIntent().getIntExtra("id",0));
    }
    void fetchNotice(int noticeId){
        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("X-CSRFToken", mCsrfToken);

        //start loading
        final ProgressDialog pd =new ProgressDialog(Notice.this);
        pd.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        pd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pd.show();
        pd.setContentView(R.layout.progress);

        new AsynchronousGet() {
            @Override
            public OkHttpClient setClient() {
                return new OkHttpClient.Builder()
                        .cookieJar(mCookieJar)
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(30,TimeUnit.SECONDS)
                        .writeTimeout(30,TimeUnit.SECONDS)
                        .build();
            }

            @Override
            public void onSuccess(String responseBody, Headers responseHeaders, int responseCode) {
                try {
                    mNoticeInfo = Parsing.parseNoticeInfo(responseBody);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            setHeaderViews();
                            setContent();


                        }
                    });
                    //end loading
                    pd.dismiss();
                } catch (JSONException e) {
                    e.printStackTrace();
                    onFail(new Exception("Failed to parse notice. Please try again."));
                }
            }

            @Override
            public void onFail(Exception e) {
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        //end loading
                        pd.dismiss();
                    }
                });
                showMessage(e.getMessage());
            }
        }.getResponse(Notices.NOTICE_URL + noticeId, headers, null);
    }
    public void setHeaderViews(){
        TextView subject = findViewById(R.id.notice_subject);
        TextView category = findViewById(R.id.notice_category);
        TextView date = findViewById(R.id.notice_date);
        subject.setText(mNoticeInfo.getSubject());
        category.setText(mNoticeInfo.getCategory());
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("hh:mm a dd-MMM-yyyy");
        try {
            Date idate = inputDateFormat.parse(mNoticeInfo.getDatetime_modified());
            String odate = outputDateFormat.format(idate);
            date.setText(odate);

        } catch (ParseException e) {
            e.printStackTrace();
            date.setText(mNoticeInfo.getDatetime_modified());
        }
    }
    public void setContent(){
        String result=mNoticeInfo.getContent();
        StringBuffer stringBuffer;

        if(result.contains("<img")  || result.contains("href")) {
            ArrayList<Integer> count = new ArrayList<>();
            stringBuffer = new StringBuffer(result);
            String add= "http://people.iitr.ernet.in";

            for(int index = result.indexOf("/media");
                index >= 0;
                index = result.indexOf("/media",index + 1)) {
                count.add(index);

            }
            for(int index = result.indexOf("/notices/userfiles");
                index >= 0;
                index = result.indexOf("/notices",index + 1)) {
                count.add(index);
            }

            int prev = 0;
            for(int i=0; i< count.size(); i++){
                stringBuffer = stringBuffer.insert(prev + count.get(i), add);
                prev = (i+1)*add.length();
            }
            result = stringBuffer.toString();
        }
        if(result.contains("</a>") && result.contains("<img")){
            stringBuffer = new StringBuffer(result);
            String add = "Download Attachment";
            int index = result.indexOf("<a href");
            int startIndex=index;
            while(result.charAt(startIndex) != '>'){
                startIndex++;
            }

            startIndex++;

            int endIndex = result.indexOf("</a>");
            //String toBeReplaced = result.substring(startIndex,endIndex);
            stringBuffer.replace(startIndex,endIndex, add);
            result = stringBuffer.toString();
        }
        //Log.e("notice",result);
        WebView webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setSupportZoom(true);
        try {
            webView.loadData(result, "text/html", "utf-8");
        }catch(Exception e){
            showMessage("Unable to fetch notice! Please try again");
        }
    }
    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        Notice.this.recreate();
    }

    public void showMessage(final String msg){
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                CoordinatorLayout coordinatorLayout= findViewById(R.id.notice_coordinator_layout);
                Snackbar snackbar=Snackbar.make(coordinatorLayout, msg, Snackbar.LENGTH_SHORT);
                TextView tv= snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                tv.setGravity(Gravity.CENTER);
                tv.setTextColor(getResources().getColor(R.color.colorPrimary));
                tv.setHeight((int) getResources().getDimension(R.dimen.bottomBarHeight));
                tv.setTypeface(null, Typeface.BOLD);
                snackbar.show();
            }
        });
    }

   /* @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notice, menu);
        return true;
    }*/
        @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            /*case R.id.archive:
                archiveNotice();
                return true;*/
        }

        return super.onOptionsItemSelected(item);
    }
}
