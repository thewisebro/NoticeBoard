package com.channeli.noticeboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
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
import com.squareup.leakcanary.LeakCanary;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import adapters.CustomDrawerListViewAdapter;
import adapters.CustomRecyclerViewAdapter;
import connections.AsynchronousGet;
import connections.SynchronousGet;
import objects.DrawerItem;
import objects.NoticeObject;
import objects.User;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import utilities.EndlessRecyclerViewScrollListener;
import utilities.Parsing;
import utilities.RoundImageView;
import utilities.SQLHelper;

public class Notices extends AppCompatActivity {

    public static final int TYPE_LIST = 1, TYPE_SEARCH = 2, TYPE_STARRED = 3, TYPE_READ = 4;
    public static final String NOTICE_OLD = "old", NOTICE_NEW = "new", NOTICE_ALL="All";

    public static final String HOST_URL="http://people.iitr.ernet.in/";
    public static final String NOTICES_URL = HOST_URL+"notices/";
    public static final String LOGIN_URL = HOST_URL+"login/";
    public static final String PEOPLE_SEARCH_URL = HOST_URL+"peoplesearch/";
//    public static final String UrlOfFCMRegistration = HOST_URL+"push_subscription_sync/";
    public static final String PHOTO_URL = HOST_URL+"photo/";
    public static final String PREFS_NAME = "MyPrefsFile";
    public static String NoticeType = "new", MainCategory = "All", Category="All";

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private User mUser;
    private CookieJar mCookieJar;
    private SQLHelper mSqlHelper;
    private ArrayList<DrawerItem> mDrawerItems=new ArrayList<>();
    private ArrayList<NoticeObject> mNoticeList;
    private ArrayList<NoticeObject> mStarredList;
    private ArrayList<Integer> mReadList;
    private CustomRecyclerViewAdapter mCustomRecyclerViewAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private RecyclerView mRecyclerView;
    private CustomDrawerListViewAdapter mCustomDrawerListViewAdapter;

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
        mSharedPreferences = getSharedPreferences(Notices.PREFS_NAME,0);
        mEditor = mSharedPreferences.edit();
        mUser = new User(mSharedPreferences.getString("username","14115019"),
                mSharedPreferences.getString("csrftoken",""), mSharedPreferences.getString("CHANNELI_SESSID","")
        );
        mNoticeList=new ArrayList<NoticeObject>();
        mStarredList=new ArrayList<NoticeObject>();
        mReadList=new ArrayList<Integer>();

        //Set up Cookies for networking
        SetCookieCache cookieCache = new SetCookieCache();
        CookiePersistor cookiePersistor = new SharedPrefsCookiePersistor(getApplication());
        if (cookiePersistor.loadAll().isEmpty()){
            List<Cookie> cookieList = new ArrayList<Cookie>(2);
            cookieList.add(new Cookie.Builder().name("csrftoken").value(mUser.getCsrfToken()).domain(Notices.HOST_URL).build());
            cookieList.add(new Cookie.Builder().name("CHANNELI_SESSID").value(mUser.getChanneliSessid()).domain(Notices.HOST_URL).build());
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

        //TODO: GET user details, constants, notices
        fetchUserDetails();
        fetchConstants();
        fetchFirstTimeNotices(Notices.NOTICE_NEW, Notices.NOTICE_ALL, Notices.NOTICE_ALL, null, Notices.TYPE_LIST);
//        fetchNotices(NOTICE_NEW, NOTICE_ALL, NOTICE_ALL, 0, 20, "", TYPE_LIST);
    }
    private void fetchFirstTimeNotices(final String noticeType, final String category, final String mainCategory, final String query, final int type){
        new Thread(){
            @Override
            public void run(){
                Map<String,String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                headers.put("X-CSRFToken", mUser.getCsrfToken());
                try {
                    mReadList = Parsing.parseReadNotices(
                            (String) new SynchronousGet() {
                                @Override
                                public OkHttpClient setClient() {
                                    return new OkHttpClient.Builder()
                                            .cookieJar(mCookieJar)
                                            .build();
                                }
                            }.getResponse(Notices.NOTICES_URL + "read_notice_list",headers,null)
                                    .get("body")
                    );
                    mStarredList = Parsing.parseStarredNotices(
                            (String) new SynchronousGet() {
                                @Override
                                public OkHttpClient setClient() {
                                    return new OkHttpClient.Builder()
                                            .cookieJar(mCookieJar)
                                            .build();
                                }
                            }.getResponse(Notices.NOTICES_URL + "star_notice_list",headers,null)
                                    .get("body"),
                            mReadList
                    );
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (type == Notices.TYPE_LIST ) {
                                fetchNotices(noticeType, category, mainCategory, 0, 20, query, type);
                            } else {

                            }
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

    private void fetchNotices(String noticeType, String category, String mainCategory, int offset, int count, String query, int type){

        mRecyclerView.stopScroll();

        if (MainCategory.equals(Category)) {          //For All - All
            setTitle(MainCategory);
        }
        else {
            setTitle(MainCategory + " - " + Category);
        }
        mRecyclerView.stopScroll();

        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("X-CSRFToken", mUser.getCsrfToken());
        new AsynchronousGet() {
            @Override
            public OkHttpClient setClient() {
                return new OkHttpClient.Builder()
                        .cookieJar(mCookieJar)
//                        .followRedirects(false)
//                        .followSslRedirects(false)
                        .build();
            }

            @Override
            public void onSuccess(String responseBody, Headers responseHeaders, int responseCode) {
                try {
                    ArrayList<NoticeObject> list = Parsing.parseNotices(responseBody, mStarredList, mReadList);
                    mNoticeList.addAll(list);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            populateListView();
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(Exception e) {

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
                        .build();
            }

            @Override
            public void onSuccess(String responseBody, Headers responseHeaders, int responseCode) {
                try {
                    mDrawerItems.addAll(new Parsing().parseConstants(responseBody));
                    if (mDrawerItems.size() > 0) {
                        Set<String> constantSet = new HashSet<>(mDrawerItems.size());
                        for (int i = 0; i < mDrawerItems.size(); i++) {
                            Set<String> s = new HashSet<>(mDrawerItems.get(i).getCategories());
                            mEditor.putStringSet(mDrawerItems.get(i).getName(), s);
                            constantSet.add(mDrawerItems.get(i).getName());
                        }
                        mEditor.putStringSet("constants", constantSet);
                        mEditor.apply();
                    }
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            populateDrawerList();
                        }
                    });
                } catch (JSONException e){

                }
            }

            @Override
            public void onFail(Exception e) {

            }
        }.getResponse(Notices.NOTICES_URL + "get_constants/", headers, null);
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

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) findViewById(R.id.name)).setText(mUser.getName());
                            ((TextView) findViewById(R.id.info)).setText(mUser.getInfo());
                            fetchUserPhoto();
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    onFail(new JSONException(""));
                } catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(Exception e) {

            }

            @Override
            public OkHttpClient setClient() {
                return new OkHttpClient.Builder()
                        .cookieJar(mCookieJar)
                        .build();
            }
        }.getResponse(Notices.PEOPLE_SEARCH_URL + "return_details/?username=" + mUser.getUsername(), headers, null);
    }
    private void fetchUserPhoto(){
        RoundImageView imageView = (RoundImageView) findViewById(R.id.profile_picture);
        Glide.with(Notices.this).load(PHOTO_URL+mUser.getEnrollmentNo()).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                return false;
            }
            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                //Convert drawable to bitmap
                Bitmap bitmap = null;
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
        mRecyclerView = (RecyclerView) findViewById(R.id.list_view);
        mCustomRecyclerViewAdapter = new CustomRecyclerViewAdapter(Notices.this, R.layout.list_itemview, mNoticeList, mStarredList, mReadList);
        mRecyclerView.setAdapter(mCustomRecyclerViewAdapter);

        LinearLayoutManager layoutManager=new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setOnScrollListener(new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, final int totalItemsCount, RecyclerView view) {
                if(totalItemsCount>0 && totalItemsCount<=mNoticeList.size() && (!MainCategory.equals("Starred"))){
//                    fetchNotices();
                }
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

    private void populateListView(){
        mRecyclerView.getRecycledViewPool().clear();
        mCustomRecyclerViewAdapter.notifyDataSetChanged();

        if (mNoticeList.size() > 0) {
            findViewById(R.id.no_notice).setVisibility(View.GONE);
        }
        else {
            findViewById(R.id.no_notice).setVisibility(View.VISIBLE);
        }

        //mScrollListener.resetState()
    }
    private void populateDrawerList(){
        mDrawerItems.add(new DrawerItem("Starred", null));
        mDrawerItems.add(new DrawerItem("", null));
        mDrawerItems.add(new DrawerItem("Notifications Settings", null));
        mDrawerItems.add(new DrawerItem("Feedback", null));
        mDrawerItems.add(new DrawerItem("Logout", null));
        mCustomDrawerListViewAdapter.notifyDataSetChanged();
    }
/*    private void populateDrawerProfile(){

    }*/
    private void clickListener(int groupPosition, int childPosition){
        ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawers();
        switch (mDrawerItems.get(groupPosition).getName()) {
            case "Starred": //Starred
                MainCategory = "Starred";
                Category = "All";
//                selectItem();
                break;
            case "Notifications Settings":
                Intent intent1=new Intent(Notices.this,SubscriptionSettings.class);
                startActivity(intent1);
                break;
            case "Feedback": //Feedback
                Intent intent2 = new Intent(Intent.ACTION_VIEW);
                intent2.setData(Uri.parse("market://details?id=com.channeli.noticeboard"));
                startActivity(intent2);
                break;
            case "Logout": //Logout
//                logout();
                break;
            default:
                MainCategory = mDrawerItems.get(groupPosition).getName();
                if(childPosition<0)
                    Category="All";
                else
                    Category = mDrawerItems.get(groupPosition).getCategories().get(childPosition);
//                selectItem();
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
                return Notices.NOTICES_URL + TextUtils.join("/", new String[]{ "list_notices", noticeType.replaceAll(" ", "%20"), mainCategory.replaceAll(" ", "%20")
                        , category.replaceAll(" ", "%20"), String.valueOf(offset), String.valueOf(count), String.valueOf(nextId)});
            }
            case TYPE_SEARCH: {
                return NOTICES_URL + TextUtils.join("/", new String[]{ "search", noticeType.replaceAll(" ", "%20"), mainCategory.replaceAll(" ", "%20")
                        , category.replaceAll(" ", "%20"), "?=" + query });
            }
            case TYPE_STARRED: {
                return NOTICES_URL + "star_notice_list";
            }
            case TYPE_READ: {
                return NOTICES_URL + "read_notice_list";
            }
            default: return null;
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

