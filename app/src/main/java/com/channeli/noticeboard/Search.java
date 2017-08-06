package com.channeli.noticeboard;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.roughike.bottombar.BottomBar;
import com.squareup.leakcanary.LeakCanary;

import java.util.ArrayList;
import java.util.List;

import objects.NoticeObject;
import okhttp3.Cookie;
import utilities.SQLHelper;

public class Search extends AppCompatActivity {

    public String NoticeType="new";
    private SQLHelper mSqlHelper;
    private RecyclerView mRecyclerView;
    private BottomBar mBottomBar;
    private ArrayList<NoticeObject> mNoticeList;
    private ArrayList<NoticeObject> mStarredList;
    private ArrayList<Integer> mReadList;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private PersistentCookieJar mCookieJar;
    private String mSessid;
    private String mCsrfToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(getApplication());

        //Hide Keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        //Initialise variables
//        mRecyclerView = (RecyclerView) findViewById(R.id.search_list_view);
        mSqlHelper = new SQLHelper(getApplicationContext());
        mSharedPreferences = getSharedPreferences(Notices.PREFS_NAME,0);
        mEditor = mSharedPreferences.edit();
        mNoticeList = new ArrayList<>();
        mStarredList = new ArrayList<>();
        mReadList = new ArrayList<>();
        mSessid = mSharedPreferences.getString(Notices.CHANNELI_SESSID,"");
        mCsrfToken = mSharedPreferences.getString(Notices.CSRF_TOKEN,"");

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

        //Set up Toolbar
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setTitle("Search Results");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }
    public String getSearchUrl(String query){
        return Notices.NOTICES_URL+"search/"+NoticeType+"/All/All/?q="+query;
    }
}
