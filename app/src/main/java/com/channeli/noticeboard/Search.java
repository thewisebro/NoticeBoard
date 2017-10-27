package com.channeli.noticeboard;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;
import com.squareup.leakcanary.LeakCanary;

import org.json.JSONException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import adapters.CustomRecyclerViewAdapter;
import connections.AsynchronousGet;
import connections.SynchronousGet;
import objects.NoticeObject;
import okhttp3.Cookie;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import utilities.Parsing;

public class Search extends AppCompatActivity {


    public static String NoticeType = Notices.NOTICE_NEW, MainCategory = Notices.NOTICE_ALL, Category = Notices.NOTICE_ALL;
    private RecyclerView mRecyclerView;
    private BottomBar mBottomBar;
    private List<NoticeObject> mNoticeList;
    private Set<NoticeObject> mStarredList;
    private Set<Integer> mReadList;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private PersistentCookieJar mCookieJar;
    private String mSessid;
    private String mCsrfToken;
    private String mQuery;
    private CustomRecyclerViewAdapter mCustomRecyclerViewAdapter;

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
        mSharedPreferences = getSharedPreferences(Notices.PREFS_NAME,0);
        mEditor = mSharedPreferences.edit();
        mNoticeList = new ArrayList<>();
        mStarredList = Notices.getStarredList();
        mReadList = Notices.getReadList();
        mSessid = mSharedPreferences.getString(Notices.CHANNELI_SESSID,"");
        mCsrfToken = mSharedPreferences.getString(Notices.CSRF_TOKEN,"");
        mQuery = getIntent().getStringExtra("query");

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

        //Set up Views
        setListView();
        setBottomBar();

        //Fetch
//        fetchFirstTimeNotices(mQuery);
        fetchNotices(mQuery);
    }
    private void fetchFirstTimeNotices(String query){
        new Thread(){
            @Override
            public void run(){
                Map<String,String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                headers.put("X-CSRFToken", mCsrfToken);
                try {
                    mReadList = Parsing.parseReadNotices(
                            (String) new SynchronousGet() {
                                @Override
                                public OkHttpClient setClient() {
                                    return new OkHttpClient.Builder()
                                            .cookieJar(mCookieJar)
                                            .connectTimeout(10, TimeUnit.SECONDS)
                                            .readTimeout(30,TimeUnit.SECONDS)
                                            .writeTimeout(30,TimeUnit.SECONDS)
                                            .build();
                                }
                            }.getResponse(Notices.READ_NOTICES_URL,headers,null)
                                    .get("body")
                    );
                    mStarredList = Parsing.parseStarredNotices(
                            (String) new SynchronousGet() {
                                @Override
                                public OkHttpClient setClient() {
                                    return new OkHttpClient.Builder()
                                            .cookieJar(mCookieJar)
                                            .connectTimeout(10, TimeUnit.SECONDS)
                                            .readTimeout(30,TimeUnit.SECONDS)
                                            .writeTimeout(30,TimeUnit.SECONDS)
                                            .build();
                                }
                            }.getResponse(Notices.STARRED_NOTICES_URL,headers,null)
                                    .get("body"),
                            mReadList
                    );
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fetchNotices(mQuery);
                        }
                    });
                }catch (JSONException e){
                    e.printStackTrace();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void fetchNotices(String query){

        mRecyclerView.stopScroll();

        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("X-CSRFToken", mCsrfToken);

        //start loading
        final ProgressDialog pd =new ProgressDialog(Search.this);
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
//                        .followRedirects(false)
//                        .followSslRedirects(false)
                        .build();
            }

            @Override
            public void onSuccess(String responseBody, Headers responseHeaders, int responseCode) {
                try {
                    List<NoticeObject> list = Parsing.parseNotices(responseBody, mStarredList, mReadList);
                    mRecyclerView.smoothScrollToPosition(0);
                    mNoticeList.clear();
                    mNoticeList.addAll(list);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            populateListView();
                        }
                    });
                    //end loading
                    pd.dismiss();

                } catch (JSONException e) {
                    e.printStackTrace();
                    onFail(new Exception("Failed to parse notices. Please try again."));
                }
            }

            @Override
            public void onFail(Exception e) {

                //end loading
                pd.dismiss();
                showMessage(e.getMessage());
            }
        }.getResponse(getSearchUrl(query), headers, null);
    }
    private void setListView(){
        mRecyclerView = (RecyclerView) findViewById(R.id.search_list_view);
        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
        mCustomRecyclerViewAdapter = new CustomRecyclerViewAdapter(Search.this, R.layout.list_itemview, mNoticeList, mStarredList, mReadList);
        mRecyclerView.setAdapter(mCustomRecyclerViewAdapter);
    }

    private void setBottomBar(){
        mBottomBar = (BottomBar) findViewById(R.id.bottom_bar);
        mBottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(int itemId) {
                switch (itemId) {
                    case R.id.new_items:
                        NoticeType = Notices.NOTICE_NEW;
                        break;
                    case R.id.old_items:
                        NoticeType = Notices.NOTICE_OLD;
                        break;
                }
                fetchNotices(mQuery);
            }
        });
    }
    private void populateListView(){

        mRecyclerView.getRecycledViewPool().clear();
        mCustomRecyclerViewAdapter.notifyDataSetChanged();

        if (mNoticeList.size() > 0) {
            findViewById(R.id.no_notice).setVisibility(View.GONE);
        }
        else {
            findViewById(R.id.no_notice).setVisibility(View.VISIBLE);
        }
        mRecyclerView.setEnabled(true);
    }
    public String getSearchUrl(String query) {
        return Notices.NOTICES_URL + TextUtils.join("/", new String[]{"search", NoticeType.replaceAll(" ", "%20"), MainCategory.replaceAll(" ", "%20")
                , Category.replaceAll(" ", "%20"), "?q=" + query});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem searchMenuItem=menu.findItem(R.id.search);
        final SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        if (null != searchView )
        {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconified(false);
            searchView.clearFocus();
            searchView.setQueryHint("Search notices");
        }
        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener()
        {
            public boolean onQueryTextChange(String newText)
            {
                return true;
            }
            public boolean onQueryTextSubmit(String newText)
            {
                mQuery = newText;
                mQuery = mQuery.replaceAll(" ","%20");
                fetchNotices(mQuery);
                searchMenuItem.collapseActionView();
                searchView.setVisibility(View.INVISIBLE);
                searchView.setVisibility(View.VISIBLE);
                return true;
            }
        };
        searchView.setOnQueryTextListener(queryTextListener);
        MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                searchView.requestFocus();
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
                        toggleSoftInput(InputMethodManager.SHOW_FORCED,
                                InputMethodManager.HIDE_IMPLICIT_ONLY);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchView.clearFocus();
                searchView.setQuery("", false);
                searchView.setIconified(false);
                searchView.setVisibility(View.INVISIBLE);
                searchView.setVisibility(View.VISIBLE);
                return true;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchView.setQuery("", false);
                return true;
            }
        });
        int searchPlateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        View searchPlate = searchView.findViewById(searchPlateId);
        if (searchPlate!=null) {
            searchPlate.setBackground(getResources().getDrawable(R.drawable.normal));
            int searchTextId = searchPlate.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
            TextView searchText = (TextView) searchPlate.findViewById(searchTextId);
            if (searchText!=null) {
                searchText.setTextColor(Color.BLACK);
                searchText.setHintTextColor(Color.DKGRAY);
                try {
                    Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
                    mCursorDrawableRes.setAccessible(true);
                    mCursorDrawableRes.set(searchText, 0); //This sets the cursor resource ID to 0 or @null which will make it visible on white background
                } catch (Exception e) {}
            }
        }
        return true;
    }

/*    @Override
    protected void onNewIntent(Intent intent){
        handleIntent(intent);
    }

    private void handleIntent(Intent intent){

        if(Intent.ACTION_SEARCH.equals(intent.getAction())){
            mQuery = intent.getStringExtra(SearchManager.QUERY);
            mQuery = mQuery.replaceAll(" ","%20");
        }
    }*/

    public void showMessage(final String msg){
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                CoordinatorLayout coordinatorLayout= findViewById(R.id.main_content);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
