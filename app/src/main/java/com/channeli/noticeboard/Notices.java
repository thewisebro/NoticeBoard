package com.channeli.noticeboard;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.google.firebase.messaging.FirebaseMessaging;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;
import com.squareup.leakcanary.LeakCanary;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import adapters.CustomDrawerListViewAdapter;
import adapters.CustomRecyclerViewAdapter;
import connections.AsynchronousGet;
import connections.SynchronousGet;
import objects.DrawerItem;
import objects.NoticeObject;
import objects.User;
import okhttp3.Cookie;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import utilities.EndlessRecyclerViewScrollListener;
import utilities.Parsing;
import utilities.RoundImageView;
import utilities.SQLHelper;

public class Notices extends AppCompatActivity {

    public static final int TYPE_LIST = 1, TYPE_SEARCH = 2, TYPE_STARRED = 3, TYPE_READ = 4, TYPE_ARCHIVED=5;
    public static final String NOTICE_OLD = "old", NOTICE_NEW = "new", NOTICE_ALL="All";
    public static final int BATCH_SIZE = 20;
    public static String NoticeType = NOTICE_NEW, MainCategory = NOTICE_ALL, Category=NOTICE_ALL;
    public static int CurrentType = TYPE_LIST;

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private User mUser;
    private PersistentCookieJar mCookieJar;
    private SQLHelper mSqlHelper;
    private List<DrawerItem> mDrawerItems;
    private static List<NoticeObject> mNoticeList;
    private static Set<NoticeObject> mStarredList;
    private static Set<Integer> mReadList;
    private CustomRecyclerViewAdapter mCustomRecyclerViewAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private RecyclerView mRecyclerView;
    private CustomDrawerListViewAdapter mCustomDrawerListViewAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private BottomBar mBottomBar;
    private EndlessRecyclerViewScrollListener mScrollListener;
    private ProgressDialog mDialog;

    public static Set<NoticeObject> getStarredList(){ return mStarredList; }
    public static Set<Integer> getReadList(){ return mReadList; }
    public static List<NoticeObject> getNoticeList(){ return mNoticeList; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(getApplication());
        setContentView(R.layout.main);

        //Initialize instance variables
        mSqlHelper = new SQLHelper(getApplicationContext());
        mSharedPreferences = getSharedPreferences(Constants.PREFS_NAME,0);
        mEditor = mSharedPreferences.edit();
        mUser = new User(
                mSharedPreferences.getString("username",""),
                mSharedPreferences.getString(Constants.CSRF_TOKEN,""),
                mSharedPreferences.getString(Constants.CHANNELI_SESSID,"")
        );
        mDrawerItems=new ArrayList<>();
        mNoticeList=new ArrayList<>();
        mStarredList=new LinkedHashSet<>();
        mReadList=new HashSet<>();

        //Set up progress dialog box
        mDialog = new ProgressDialog(Notices.this);
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDialog.setMessage("Loading");
        //Set up Cookies for networking
        SetCookieCache cookieCache = new SetCookieCache();
        CookiePersistor cookiePersistor = new SharedPrefsCookiePersistor(getApplication());
        if (cookiePersistor.loadAll().isEmpty()){
            List<Cookie> cookieList = new ArrayList<Cookie>(2);
            cookieList.add(new Cookie.Builder().name(Constants.CSRF_TOKEN).value(mUser.getCsrfToken()).domain(Constants.DOMAIN_URL).build());
            cookieList.add(new Cookie.Builder().name(Constants.CHANNELI_SESSID).value(mUser.getChanneliSessid()).domain(Constants.DOMAIN_URL).build());
            cookieCache.addAll(cookieList);
            cookiePersistor.saveAll(cookieList);
        }
        mCookieJar = new PersistentCookieJar(cookieCache,cookiePersistor);

        //Set up Toolbar
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        //TODO: Set up views
        setDrawerLayout();
        setDrawerList();
        setListView();
        setBottomBar();

        //TODO: GET user details, constants, notices
        fetchUserDetails();
        fetchConstants();
        fetchFirstTimeNotices(Notices.NOTICE_NEW, Notices.NOTICE_ALL, Notices.NOTICE_ALL, null, Notices.TYPE_LIST);
    }
    private void fetchFirstTimeNotices(final String noticeType, final String category, final String mainCategory, final String query, final int type){
        new Thread(){
            @Override
            public void run(){
                showDialog();
                Map<String,String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                headers.put("X-CSRFToken", mUser.getCsrfToken());
                try {
                    Set<Integer> readList = Parsing.parseReadNotices(
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
                            }.getResponse(Constants.READ_NOTICES_URL,headers,null)
                                    .get("body")
                    );
                    mReadList.clear();
                    mReadList.addAll(readList);
                    readList.clear();

                    Set<NoticeObject> starredList = Parsing.parseStarredNotices(
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
                            }.getResponse(Constants.STARRED_NOTICES_URL,headers,null)
                                    .get("body"),
                            mReadList
                    );
                    mStarredList.clear();
                    mStarredList.addAll(starredList);
                    starredList.clear();

                    closeDialog();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fetchNotices(noticeType, category, mainCategory, 0, BATCH_SIZE, query, type);
                        }
                    });
                }catch (JSONException e){
                    e.printStackTrace();
                    showMessage("Failed to parse. Please try again.");
                    closeDialog();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            populateListView();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    showMessage("Check Network Connection");
                    closeDialog();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            populateListView();
                        }
                    });
                }
            }
        }.start();
    }

    private void fetchNotices(String noticeType, String category, String mainCategory, final int offset, int count, String query, int type){


        mRecyclerView.stopScroll();

        if (mainCategory.equals(category)) {          //For All - All
            setTitle(mainCategory);
        }
        else {
            setTitle(mainCategory + " - " + category);
        }

        if (type == TYPE_STARRED){
            mRecyclerView.smoothScrollToPosition(0);
            mNoticeList.clear();
            mNoticeList.addAll(mStarredList);
            populateListView();
            return;
        }

        if(type == TYPE_ARCHIVED){
            mRecyclerView.smoothScrollToPosition(0);
            mNoticeList.clear();
            mNoticeList.addAll(mSqlHelper.getNotices());
            populateListView();
            return;
        }

        showDialog();
        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("X-CSRFToken", mUser.getCsrfToken());

        new AsynchronousGet() {
            @Override
            public OkHttpClient setClient() {
                return new OkHttpClient.Builder()
                        .cookieJar(mCookieJar)
                        .connectTimeout(20, TimeUnit.SECONDS)
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
                    if (offset==0){
                        mRecyclerView.smoothScrollToPosition(0);
                        mNoticeList.clear();
                    }
                    mNoticeList.addAll(list);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            populateListView();
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    showMessage("Failed to parse notices. Please try again.");
                }
                closeDialog();
            }

            @Override
            public void onFail(Exception e) {
                closeDialog();
                showMessage(e.getMessage());
            }
        }.getResponse(generateUrl(noticeType,category,mainCategory,offset,count,0,query,type), headers, null);
    }
    private void fetchConstants(){
        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("X-CSRFToken", mUser.getCsrfToken());
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
                    mDrawerItems.addAll(Parsing.parseConstants(responseBody));
                    if (mDrawerItems.size() > 0) {
                        Set<String> constantSet = new HashSet<String>();
                        for (DrawerItem item : mDrawerItems) {
                            Set<String> s=new HashSet<String>(item.getCategories());
                            mEditor.putStringSet(item.getName(),s);
                            constantSet.add(item.getName());
                        }
                        mEditor.putStringSet("constants", constantSet);
                        mEditor.apply();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            populateDrawerList();
                        }
                    });
                } catch (JSONException e){
                    onFail(e);
                }
            }

            @Override
            public void onFail(Exception e) {
                Set<String> constants=mSharedPreferences.getStringSet("constants", new HashSet<String>());
                List<String> listConstants = new ArrayList<String>(constants);
                Collections.sort(listConstants);
                for (String s : listConstants) {
                    List<String> list = new ArrayList<String>(mSharedPreferences.getStringSet(s,new HashSet<String>()));
                    Collections.sort(list);
                    mDrawerItems.add(new DrawerItem(s,list));
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        populateDrawerList();
                    }
                });
            }
        }.getResponse(Constants.NOTICES_URL + "get_constants/", headers, null);
    }
    private void fetchUserDetails(){
        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("X-CSRFToken", mUser.getCsrfToken());
        new AsynchronousGet() {
            @Override
            public void onSuccess(String responseBody, Headers responseHeaders, int responseCode) {
                try {
                    JSONObject response = new JSONObject(responseBody);
                    if (response.getString("msg")!="YES") onFail(new JSONException(""));

                    mUser.setName(response.getString("_name"));
                    mUser.setInfo(response.getString("info"));
                    mUser.setEnrollmentNo(response.getString("enrollment_no"));

                    mEditor.putString("name", mUser.getName());
                    mEditor.putString("info", mUser.getInfo());
                    mEditor.putString("enrollment_no", mUser.getEnrollmentNo());
                    mEditor.apply();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) findViewById(R.id.name)).setText(mUser.getName());
                            ((TextView) findViewById(R.id.info)).setText(mUser.getInfo());
                            fetchUserPhoto();
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    onFail(e);
                }
            }

            @Override
            public void onFail(Exception e) {
//                showMessage(e.getMessage());

            }

            @Override
            public OkHttpClient setClient() {
                return new OkHttpClient.Builder()
                        .cookieJar(mCookieJar)
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(30,TimeUnit.SECONDS)
                        .writeTimeout(30,TimeUnit.SECONDS)
                        .build();
            }
        }.getResponse(Constants.PEOPLE_SEARCH_URL + "return_details/?username=" + mUser.getUsername(), headers, null);
    }
    private void fetchUserPhoto(){
        RoundImageView imageView = findViewById(R.id.profile_picture);
        Glide.with(Notices.this).load(Constants.PHOTO_URL+mUser.getEnrollmentNo()).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                return false;
            }
            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                //Convert drawable to bitmap
                Bitmap bitmap;
                if (resource instanceof BitmapDrawable) {
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) resource;
                    if(bitmapDrawable.getBitmap() != null) {
                        mSqlHelper.addProfilePic(bitmapDrawable.getBitmap());
                        return false;
                    }
                }

                if(resource.getIntrinsicWidth() <= 0 || resource.getIntrinsicHeight() <= 0) {
                    bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
                } else {
                    bitmap = Bitmap.createBitmap(resource.getIntrinsicWidth(), resource.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                }

                Canvas canvas = new Canvas(bitmap);
                resource.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                resource.draw(canvas);
                mSqlHelper.addProfilePic(bitmap);
                return false;
            }
        }).into(imageView);
    }

    private void setListView(){
        mRecyclerView = findViewById(R.id.list_view);
        mCustomRecyclerViewAdapter = new CustomRecyclerViewAdapter(Notices.this, R.layout.list_itemview, mNoticeList, mStarredList, mReadList);
        mRecyclerView.setAdapter(mCustomRecyclerViewAdapter);

        LinearLayoutManager layoutManager=new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mScrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, final int totalItemsCount, RecyclerView view) {
                if(totalItemsCount>0 && totalItemsCount<=mNoticeList.size() && CurrentType==TYPE_LIST){
                    fetchNotices( NoticeType, Category, MainCategory, totalItemsCount, totalItemsCount + BATCH_SIZE, null, TYPE_LIST);
                }
            }
        };
        mRecyclerView.setOnScrollListener(mScrollListener);
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccentDark)
                ,getResources().getColor(R.color.colorPrimaryDark));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
//                mRecyclerView.setEnabled(false);
                mSwipeRefreshLayout.setRefreshing(false);
                fetchFirstTimeNotices(NoticeType,Category, MainCategory,null,CurrentType);
            }
        });
    }
    private void setDrawerList(){
        final ExpandableListView drawerListView= (ExpandableListView) findViewById(R.id.drawer_menu);
        mCustomDrawerListViewAdapter = new CustomDrawerListViewAdapter(mDrawerItems,this){
            int lastGroup=-1;

            @Override
            public void OnIndicatorClick(boolean isExpanded, int position) {
                if(isExpanded) {
                    drawerListView.collapseGroup(position);
                } else {
                    drawerListView.expandGroup(position);
                    if(lastGroup!=position) drawerListView.collapseGroup(lastGroup);
                    lastGroup=position;
                }
            }
            @Override
            public void OnGroupItemClick(boolean isExpanded, int position) {
                if (isExpanded) {
                    drawerListView.collapseGroup(position);
                }
                else {
                    if(lastGroup!=position) {
                        drawerListView.collapseGroup(lastGroup);
                    }
                    lastGroup=position;
                    clickListener(position, -1);
                    if (checkDrawerColorChange(position)) {
                        this.groupPos = position;
                        this.childPos = -1;
                    }
                    notifyDataSetChanged();
                }
            }
            @Override
            public void OnChildItemClick(int gp, int cp){
                clickListener(gp, cp);
                this.groupPos=gp;
                this.childPos=cp;
                notifyDataSetChanged();
            }
        };

        drawerListView.setAdapter(mCustomDrawerListViewAdapter);
    }

    private void setDrawerLayout(){
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.string.drawer_open,
                R.string.drawer_close
        ) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        drawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();
    }
    private void setBottomBar(){
        mBottomBar = (BottomBar) findViewById(R.id.bottom_bar);
        final boolean[] bottomBarSetUp = {true};
        mBottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(int itemId) {
//                refreshScroll = true;
                if(bottomBarSetUp[0]){
                    bottomBarSetUp[0] =false;
                    return;
                }
                switch (itemId) {
                    case R.id.new_items:
                        NoticeType = NOTICE_NEW;
                        fetchNotices(NoticeType,Category,MainCategory,0,BATCH_SIZE,null,TYPE_LIST);
                        break;
                    case R.id.old_items:
                        NoticeType = NOTICE_OLD;
                        fetchNotices(NoticeType,Category,MainCategory,0,BATCH_SIZE,null,TYPE_LIST);
                        break;
                }
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
//        mRecyclerView.setEnabled(true);
        mScrollListener.resetState();
    }
    private void populateDrawerList(){
        mDrawerItems.add(new DrawerItem("Starred", new ArrayList<String>()));
//        mDrawerItems.add(new DrawerItem("Archived", null));
        mDrawerItems.add(new DrawerItem("", new ArrayList<String>()));
        mDrawerItems.add(new DrawerItem("Notifications Settings", new ArrayList<String>()));
        mDrawerItems.add(new DrawerItem("Feedback", new ArrayList<String>()));
        mDrawerItems.add(new DrawerItem("Logout", new ArrayList<String>()));
        mCustomDrawerListViewAdapter.notifyDataSetChanged();
    }
    /*    private void populateDrawerProfile(){

        }*/
    private void clickListener(int groupPosition, int childPosition){
        ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawers();

        switch (mDrawerItems.get(groupPosition).getName()) {
            case "Starred": //Starred
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MainCategory = "Starred";
                        Category = NOTICE_ALL;
                        mNoticeList.clear();
                        CurrentType = TYPE_STARRED;
                        mBottomBar.setVisibility(View.GONE);
                        fetchNotices(null,Category,MainCategory,0,0,null,TYPE_STARRED);
                    }
                },250);
                break;
            case "Archived" : //Archived Notices
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MainCategory = "Archived";
                        Category = NOTICE_ALL;
                        mNoticeList.clear();
                        CurrentType = TYPE_ARCHIVED;
                        mBottomBar.setVisibility(View.GONE);
                        fetchNotices(null,Category,MainCategory,0,0,null,TYPE_ARCHIVED);
                    }
                },250);
                break;
            case "Notifications Settings":
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(Notices.this,SubscriptionSettings.class));
                    }
                },250);
                break;
            case "Feedback": //Feedback
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("market://details?id=com.channeli.noticeboard"));
                        startActivity(intent);
                    }
                },250);
                break;
            case "Logout": //Logout
                logout();
                break;
            default:
                mNoticeList.clear();
                MainCategory = mDrawerItems.get(groupPosition).getName();
                if(childPosition<0) {
                    Category = NOTICE_ALL;
                }
                else {
                    Category = mDrawerItems.get(groupPosition).getCategories().get(childPosition);
                }
                CurrentType = TYPE_LIST;
                mBottomBar.setVisibility(View.VISIBLE);
                fetchNotices(NoticeType,Category,MainCategory,0,BATCH_SIZE,null,TYPE_LIST);
        }
    }
    public boolean checkDrawerColorChange(int pos){
        String gp_name=mDrawerItems.get(pos).getName();
        if (gp_name.contains("Notification"))
            return false;
        if (gp_name.contains("Feedback"))
            return false;
        if (gp_name.contains("Logout"))
            return false;
        return true;
    }
    private String generateUrl(String noticeType, String category, String mainCategory, Integer offset, Integer count, int nextId,String query, int type){
        switch (type){
            case TYPE_LIST: {
                return Constants.NOTICES_URL + TextUtils.join("/", new String[]{ "list_notices", noticeType.replaceAll(" ", "%20"), mainCategory.replaceAll(" ", "%20")
                        , category.replaceAll(" ", "%20"), String.valueOf(offset), String.valueOf(count), String.valueOf(nextId)});
            }
            case TYPE_SEARCH: {
                return Constants.NOTICES_URL + TextUtils.join("/", new String[]{ "search", noticeType.replaceAll(" ", "%20"), mainCategory.replaceAll(" ", "%20")
                        , category.replaceAll(" ", "%20"), "?=" + query });
            }
            case TYPE_STARRED: {
                return Constants.NOTICES_URL + "star_notice_list";
            }
            case TYPE_READ: {
                return Constants.NOTICES_URL + "read_notice_list";
            }
            default: return null;
        }
    }

    //TODO: Proper logout
    public void logout(){
        final AlertDialog.Builder dialog=new AlertDialog.Builder(this);
        dialog.setTitle("Logout");
        dialog.setMessage("Are you sure you want to Logout?");
        dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mEditor.clear();
                mEditor.apply();
                mSqlHelper.clear();
                mCookieJar.clear();
                mCookieJar.clearSession();
                if (FirebaseMessaging.getInstance()!=null) {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("Placement%20Office");
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("Authorities");
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("Departments");
                }

                Map<String,String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                headers.put("X-CSRFToken", mUser.getCsrfToken());
                new AsynchronousGet() {
                    @Override
                    public void onSuccess(String responseBody, Headers responseHeaders, int responseCode) {
                    }

                    @Override
                    public void onFail(Exception e) {
//                showMessage(e.getMessage());

                    }

                    @Override
                    public OkHttpClient setClient() {
                        return new OkHttpClient.Builder()
                                .cookieJar(mCookieJar)
                                .connectTimeout(10, TimeUnit.SECONDS)
                                .readTimeout(30,TimeUnit.SECONDS)
                                .writeTimeout(30,TimeUnit.SECONDS)
                                .build();
                    }
                }.getResponse(Constants.LOGOUT_URL, headers, null);

                dialog.dismiss();
                startActivity(new Intent(Notices.this,Login.class));
                finish();
            }
        });
        dialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.show();
            }
        }, 250);
    }

    private void showDialog(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDialog.show();
            }
        });
    }
    private void closeDialog(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDialog.dismiss();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem searchMenuItem=menu.findItem(R.id.search);
        final SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, Search.class)));
        searchView.setIconified(false);
        //searchView.setSubmitButtonEnabled(true);
        searchView.setQueryHint("Search notices");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                searchView.setVisibility(View.INVISIBLE);
                searchView.setVisibility(View.VISIBLE);
                searchMenuItem.collapseActionView();
                Intent intent = new Intent(Notices.this,Search.class);
                intent.putExtra("query",query);
                startActivity(intent);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.clearFocus();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public void showMessage(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CoordinatorLayout coordinatorLayout= (CoordinatorLayout) findViewById(R.id.main_content);
                Snackbar snackbar=Snackbar.make(coordinatorLayout, msg, Snackbar.LENGTH_SHORT);
                TextView tv= (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                tv.setGravity(Gravity.CENTER);
                tv.setTextColor(getResources().getColor(R.color.colorPrimary));
                tv.setHeight((int) getResources().getDimension(R.dimen.bottomBarHeight));
                tv.setTypeface(null, Typeface.BOLD);
                snackbar.show();
            }
        });
    }
    @Override
    public void onBackPressed(){
        if (MainCategory.contains(NOTICE_ALL)){
            AlertDialog.Builder dialog=new AlertDialog.Builder(this);
            dialog.setTitle("Exit");
            dialog.setMessage("Are you sure you want to exit?");
            dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            dialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
        else {
            ExpandableListView listView= findViewById(R.id.drawer_menu);
            listView.getChildAt(0).performClick();
        }
    }
}

