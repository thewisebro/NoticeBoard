package com.channeli.noticeboard;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ExpandableListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.firebase.messaging.FirebaseMessaging;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;
import com.squareup.leakcanary.LeakCanary;

import org.apache.http.client.methods.HttpGet;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import adapters.CustomDrawerListViewAdapter;
import adapters.CustomRecyclerViewAdapter;
import connections.AsynchronousGet;
import connections.ConnectTaskHttpGet;
import connections.FCMIDService;
import connections.ProfilePicTask;
import objects.DrawerItem;
import objects.NoticeObject;
import objects.User;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import utilities.EndlessRecyclerViewScrollListener;
import utilities.Parsing;
import utilities.RoundImageView;
import utilities.SQLHelper;


public class MainActivity extends AppCompatActivity {

    public static String UrlOfHost="http://people.iitr.ernet.in/";
    public static String UrlOfNotice = UrlOfHost+"notices/";
    public static String UrlOfLogin = UrlOfHost+"login/";
    public static String UrlOfPeopleSearch = UrlOfHost+"peoplesearch/";
    public static String UrlOfFCMRegistration = UrlOfHost+"push_subscription_sync/";
    private ActionBarDrawerToggle mDrawerToggle;
//    private Toolbar toolbar;
//    private NavigationView navigationView;
    public static String NoticeType = "new", MainCategory = "All", Category="All";
//    private User user;
    public BottomBar bottomBar;

//    CoordinatorLayout coordinatorLayout;
//    HttpGet httpGet;
    public static final String PREFS_NAME = "MyPrefsFile";
//    Parsing parsing;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private ArrayList<DrawerItem> mDrawerList;
    private DrawerLayout mDrawerLayout;
//    ProgressDialog mDialog=null;

    private RecyclerView mRecyclerView;
    private CustomRecyclerViewAdapter mCustomAdapter;
//    private CustomDrawerListViewAdapter drawerAdapter=null;
//    private SwipeRefreshLayout swipeRefreshLayout;
    private ArrayList<NoticeObject> mNoticeList;
    private ArrayList<NoticeObject> mStarredList;
    private ArrayList<Integer> mReadList;
    private SQLHelper mSqlHelper;
//    private String csrftoken, CHANNELI_SESSID;
//    private ExpandableListView listView;
    private EndlessRecyclerViewScrollListener mScrollListener;

//    String msg=null;
    public boolean refreshScroll=false;  //denotes when the adapter is updated so as to control scroll listener calls
    public boolean swiperefresh=false; //to update starred list and read list

/*    private void addToDB(ArrayList<NoticeObject> list){
        try {
            if (MainCategory.equals("Starred"))
                mSqlHelper.addNoticesList(list,"");
            else
                mSqlHelper.addNoticesList(list,NoticeType);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    @TargetApi(21)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(getApplication());
        setContentView(R.layout.main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        //Initialisations
        mSqlHelper=new SQLHelper(this);
        mSqlHelper.clearNotifications(); //remove all pending notifications
        mSharedPreferences = getSharedPreferences(PREFS_NAME, 0);
        mEditor = mSharedPreferences.edit();
//        csrftoken = settings.getString("csrftoken", "");
//        CHANNELI_SESSID = settings.getString("CHANNELI_SESSID", "");
        if ("".equals(mSharedPreferences.getString("CHANNELI_SESSID",""))){
            cleanLogout();
            closeDialog();
            startActivity(new Intent(this,SplashScreen.class));
            finish();
        }
        if (!mSharedPreferences.getBoolean("FCM_isRegistered",false)){
            new FCMIDService().sendRegistrationToServer(mSharedPreferences);
        }
//        parsing = new Parsing();
//        user = new User(settings.getString("name",""), settings.getString("info",""),
//                settings.getString("enrollment_no",""));
//        navigationView= (NavigationView) findViewById(R.id.left_drawer);
//        coordinatorLayout= (CoordinatorLayout) findViewById(R.id.main_content);
        bottomBar= (BottomBar) findViewById(R.id.bottom_bar);
        Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNoticeList=new ArrayList<NoticeObject>();
        mStarredList=new ArrayList<NoticeObject>();
        mReadList=new ArrayList<Integer>();

        mCustomAdapter = new CustomRecyclerViewAdapter(this,
                R.layout.list_itemview, mNoticeList, mStarredList, mReadList);

        setNavigationView();
        setBottomBar();

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

        mRecyclerView = (RecyclerView) findViewById(R.id.list_view);
        mRecyclerView.setAdapter(mCustomAdapter);
        WrapContentLinearLayoutManager layoutManager=new WrapContentLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);


        mScrollListener=new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, final int totalItemsCount, RecyclerView view) {
                if(totalItemsCount>0 && totalItemsCount<=mNoticeList.size() && (!MainCategory.equals("Starred")))
                    new Thread(){
                        @Override
                        public void run(){
                            if (isOnline()) {
                                int initialTab=bottomBar.getCurrentTabId();
                                if (totalItemsCount<=0 || totalItemsCount>mNoticeList.size()) return;

                                Map<String,String> headers = new HashMap<String, String>();
                                headers.put("Content-Type", "application/x-www-form-urlencoded");
                                headers.put("X-CSRFToken", csrftoken);
                                new AsynchronousGet() {
                                    @Override
                                    public OkHttpClient setClient() {
                                        return null;
                                    }

                                    @Override
                                    public void onSuccess(String responseBody, Headers responseHeaders, int responseCode) {

                                    }

                                    @Override
                                    public void onFail(Exception e) {

                                    }
                                }.getResponse(MainActivity.UrlOfNotice +
                                        "list_notices/" + NoticeType + "/" + MainCategory.replace(" ","%20") +
                                        "/All/1/20/" + mNoticeList.get(totalItemsCount - 1).getId(),
                                        headers, null
                                );

                                httpGet = new HttpGet(MainActivity.UrlOfNotice +
                                        "list_notices/" + NoticeType + "/" + MainCategory.replace(" ","%20") +
                                        "/All/1/20/" + mNoticeList.get(totalItemsCount - 1).getId());
                                httpGet.addHeader("Cookie", "csrftoken=" + csrftoken);
                                httpGet.addHeader("Content-Type", "application/x-www-form-urlencoded");
                                httpGet.addHeader("Cookie", "CHANNELI_SESSID=" + CHANNELI_SESSID);
                                httpGet.addHeader("X-CSRFToken", csrftoken);
                                AsyncTask<HttpGet, Void, String> mTask = new ConnectTaskHttpGet().execute(httpGet);
                                String result = null;
                                try {
                                    result = mTask.get();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                //mTask.cancel(true);
                                if (initialTab!=bottomBar.getCurrentTabId()){
                                    mScrollListener.resetState();
                                    return;
                                }
                                if (result != null && result!="") {
                                    ArrayList<NoticeObject> list = Parsing.parseNotices(result,mStarredList,mReadList);
                                    if (list!= null) {
                                        addToDB(list);
                                        mNoticeList.addAll(list);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mRecyclerView.getRecycledViewPool().clear();
                                                mCustomAdapter.notifyDataSetChanged();

                                            }
                                        });
                                    }
                                    return;
                                }
                                showNetworkError();
                            }
                            mScrollListener.resetState();
                        }
                    }.start();
            }
        };
        mRecyclerView.setOnScrollListener(mScrollListener);
        //Scroll only when touched, not due to drawer clicks
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                refreshScroll = false;
                return false;
            }
        });

        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        swipeRefreshLayout.setColorSchemeColors(new int[]{getResources().getColor(R.color.colorAccentDark)
                ,getResources().getColor(R.color.colorPrimaryDark)});
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshListener(swipeRefreshLayout));
    }
    @Override
    protected void onDestroy() {
        closeDialog();
        super.onDestroy();
    }
*//*    public void closeDialog(){
        try{
            if ((mDialog != null) && mDialog.isShowing()) {
                mDialog.dismiss();
            }
        }catch(Exception e){
        }finally {
            mDialog=null;
        }
    }*//*

    void setNavigationView(){
        addHeader();
        setDrawerMenu();
    }

    private void setBottomBar(){
        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(int itemId) {
                refreshScroll = true;
                switch (itemId) {
                    case R.id.new_items:
                        NoticeType = "new";
                        changeList();
                        break;
                    case R.id.old_items:
                        NoticeType = "old";
                        changeList();
                        break;
                }
            }
        });
    }
    private void addHeader(){
        View headerView = findViewById(R.id.drawer_header);
        final RoundImageView view= (RoundImageView) headerView.findViewById(R.id.profile_picture);
        final TextView nameView= (TextView) headerView.findViewById(R.id.name);
        final TextView infoView= (TextView) headerView.findViewById(R.id.info);

        nameView.setText(user.getName());
        infoView.setText(user.getInfo());
        new Thread(){
            @Override
            public void run(){
                final Bitmap bitmap = mSqlHelper.getProfilePic();
                if (bitmap==null){
                    String url="http://people.iitr.ernet.in/photo/"+user.getEnrollmentno();
                    HttpGet get=new HttpGet(url);
                    try {
                        final int dim=(int) getResources().getDimension(R.dimen.profile_pic_diameter);
                        final Bitmap bitm=new ProfilePicTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,get).get();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (bitm!=null && !(MainActivity.this.isFinishing())) {
                                    view.setImageBitmap(Bitmap.createScaledBitmap(bitm
                                            , dim, dim, false));
                                    view.setVisibility(View.VISIBLE);
                                    view.invalidate();
                                    mSqlHelper.addProfilePic(bitm);
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            view.setVisibility(View.VISIBLE);
                            view.setImageBitmap(bitmap);
                        }
                    });
                }
            }
        }.start();
    }
    ArrayList<DrawerItem> getConstants(){
        ArrayList<DrawerItem> list=new ArrayList<>();
        Set<String> constants=mSharedPreferences.getStringSet("constants", null);
        if(constants!=null && constants.size()>0){
            List<String> listConstants=new ArrayList<>();
            for (String s: constants)
                listConstants.add(s);
            Collections.sort(listConstants);
            for (String s:listConstants) {
                Set<String> set=mSharedPreferences.getStringSet(s,null);
                if (set==null)
                    list.add(new DrawerItem(s,null));
                else {
                    ArrayList list1=new ArrayList<String>(set);
                    Collections.sort(list1);
                    list.add(new DrawerItem(s, list1));
                }
            }
            return list;
        }
        if (isOnline()){
            httpGet = new HttpGet(UrlOfNotice+"get_constants/");
            httpGet.addHeader("Cookie", "csrftoken=" + settings.getString("csrftoken", ""));
            httpGet.addHeader("Content-Type", "application/x-www-form-urlencoded");
            httpGet.addHeader("Cookie", "CHANNELI_SESSID=" + settings.getString("CHANNELI_SESSID", ""));
            try {
                AsyncTask<HttpGet, Void, String> mTask = new ConnectTaskHttpGet().execute(httpGet);
                String constant = mTask.get(4000, TimeUnit.MILLISECONDS);
                //mTask.cancel(true);
                list.addAll(parsing.parseConstants(constant));
                if(list.size()>0){
                    Set<String> constantSet=new HashSet<>(list.size());
                    for(int i=0;i<list.size();i++) {
                        Set<String> s=new HashSet<>(list.get(i).getCategories());
                        editor.putStringSet(list.get(i).getName(),s);
                        constantSet.add(list.get(i).getName());
                    }
                    editor.putStringSet("constants", constantSet);
                    editor.apply();
                    return list;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }

        showNetworkError();
        return list;
    }

    void setDrawerMenu(){
        final ExpandableListView listView= (ExpandableListView) findViewById(R.id.drawer_menu);
        mDrawerList=getConstants();
        mDrawerList.add(new DrawerItem("Starred", null));
        mDrawerList.add(new DrawerItem("", null));
        mDrawerList.add(new DrawerItem("Notifications Settings", null));
        mDrawerList.add(new DrawerItem("Feedback", null));
        mDrawerList.add(new DrawerItem("Logout", null));

        listView.setAdapter(new CustomDrawerListViewAdapter(mDrawerList,this){
            int lastGroup=-1;

            @Override
            public void OnIndicatorClick(boolean isExpanded, int position) {
                if(isExpanded){
                    listView.collapseGroup(position);
                }else{
                    listView.expandGroup(position);
                    if(lastGroup!=position)
                        listView.collapseGroup(lastGroup);
                    lastGroup=position;
                }
            }
            @Override
            public void OnGroupItemClick(boolean isExpanded, int position) {
                if (isExpanded) {
                    listView.collapseGroup(position);
                }
                else {
                    if(lastGroup!=position)
                        listView.collapseGroup(lastGroup);
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
                //int index = listView.getFlatListPosition(ExpandableListView.getPackedPositionForChild(gp, cp));
                //listView.setItemChecked(index, true);
            }
        });
    }
    public boolean checkDrawerColorChange(int pos){
        String gp_name=mDrawerList.get(pos).getName();
        if (gp_name.contains("Notification"))
            return false;
        if (gp_name.contains("Feedback"))
            return false;
        if (gp_name.contains("Logout"))
            return false;
        return true;
    }
    private void clickListener(int groupPosition, int childPosition){
        mDrawerLayout.closeDrawers();
        switch (mDrawerList.get(groupPosition).getName()) {
            case "Starred": //Starred
                MainCategory = "Starred";
                Category = "All";
                selectItem();
                break;
            case "Notifications Settings":
                Intent intent1=new Intent(MainActivity.this,SubscriptionSettings.class);
                startActivity(intent1);
                break;
            case "Feedback": //Feedback
                Intent intent2 = new Intent(Intent.ACTION_VIEW);
                intent2.setData(Uri.parse("market://details?id=com.channeli.noticeboard"));
                startActivity(intent2);
                break;
            case "Logout": //Logout
                logout();
                break;
            default:
                MainCategory = mDrawerList.get(groupPosition).getName();
                if(childPosition<0)
                    Category="All";
                else
                    Category = mDrawerList.get(groupPosition).getCategories().get(childPosition);
                selectItem();
        }
    }
    void cleanLogout(){
        mEditor.clear();
        mEditor.apply();
        mSqlHelper.clear();
        if (FirebaseMessaging.getInstance()!=null) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("Placement%20Office");
            FirebaseMessaging.getInstance().unsubscribeFromTopic("Authorities");
            FirebaseMessaging.getInstance().unsubscribeFromTopic("Departments");
        }
    }
    void logout(){
        final AlertDialog.Builder dialog=new AlertDialog.Builder(this);
        dialog.setTitle("Logout");
        dialog.setMessage("Are you sure you want to Logout?");
        dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (isOnline()) {
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            httpGet = new HttpGet("http://people.iitr.ernet.in/logout/");
                            httpGet.addHeader("Cookie", "csrftoken=" + settings.getString("csrftoken", ""));
                            httpGet.addHeader("Content-Type", "application/x-www-form-urlencoded");
                            httpGet.addHeader("Cookie", "CHANNELI_SESSID=" + settings.getString("CHANNELI_SESSID", ""));
                            try {
                                new ConnectTaskHttpGet().execute(httpGet);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        cleanLogout();
                                        closeDialog();
                                        startActivity(new Intent(getApplicationContext(), SplashScreen.class));
                                        finish();
                                    }
                                });

                            } catch (Exception e) {
                                e.printStackTrace();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        closeDialog();
                                        showNetworkError();
                                    }
                                });
                            }
                        }
                    };
                    if (!isFinishing()) {
                        mDialog = ProgressDialog.show(MainActivity.this, null, "Logging out", true, false);
                        thread.start();
                    }
                } else
                    showNetworkError();
            }
        });
        dialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                closeDialog();
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.show();
            }
        }, 250);

    }

    private void selectItem() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                changeList();
            }
        };
        handler.postDelayed(runnable, 300);
    }
    private void changeList(){
        if (MainCategory.equals(Category))          //For All - All
            setTitle(MainCategory);
        else
            setTitle(MainCategory + " - " + Category);

        if (MainCategory.equals("Starred")) {
            bottomBar.setVisibility(View.GONE);
            httpGet=new HttpGet(MainActivity.UrlOfNotice+"star_notice_list");
        }
        else {
            bottomBar.setVisibility(View.VISIBLE);
            httpGet = new HttpGet(MainActivity.UrlOfNotice+"list_notices/" +NoticeType+"/"+MainCategory.replace(" ", "%20")
                    +"/"+Category.replace(" ", "%20")+"/0/20/0");
        }
        mRecyclerView.stopScroll();
        Thread thread=new Thread(){
            @Override
            public void run(){

                if (swiperefresh || mReadList.size()==0)
                    getReadNotices();
                if (swiperefresh || mStarredList.size()==0)
                    getStarredNotices();

                swiperefresh=false;

                httpGet.addHeader("Cookie", "csrftoken=" + csrftoken);
                httpGet.addHeader("Content-Type", "application/x-www-form-urlencoded");
                httpGet.addHeader("Cookie", "CHANNELI_SESSID=" + CHANNELI_SESSID);
                httpGet.addHeader("X-CSRFToken", csrftoken);
                setContent();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mRecyclerView.scrollToPosition(0);
                        }catch (Exception e){}

                        mRecyclerView.getRecycledViewPool().clear();
                        mCustomAdapter.notifyDataSetChanged();
                        if (mNoticeList.size() > 0)
                            findViewById(R.id.no_notice).setVisibility(View.GONE);
                        else
                            findViewById(R.id.no_notice).setVisibility(View.VISIBLE);
                        mScrollListener.resetState();
                        closeDialog();
                        if (msg != null)
                            showMessage();
                    }
                });

            }
        };
        if (!isFinishing()) {
            //mDialog = ProgressDialog.show(this, null, "Loading...", true, false);
            mDialog=new ProgressDialog(this);
            mDialog.setMessage("Loading...");
            mDialog.setIndeterminate(true);
            mDialog.setCancelable(false);
            if (isOnline())
                mDialog.show();
            thread.start();
        }
    }

    private void setContent(){
        ArrayList<NoticeObject> list=null;
        if (isOnline()){
            String content = null;
            try {
                AsyncTask<HttpGet, Void, String> mTask = new ConnectTaskHttpGet().execute(httpGet);
                content = mTask.get();
                if (MainCategory.equals("Starred")) {
                    list = parsing.parseStarredNotices(content,mReadList);
                    if (list!=null) {
                        mStarredList.clear();
                        mStarredList.addAll(list);
                    }
                }
                else
                    list=parsing.parseNotices(content,mStarredList,mReadList);

                if (list!=null) {
                    addToDB(list);
                    mNoticeList.clear();
                    mNoticeList.addAll(list);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if("All".equals(Category))
            list=mSqlHelper.getNotices(MainCategory,NoticeType);
        else
            list=mSqlHelper.getNotices(MainCategory,Category,NoticeType);
        if(list!=null) {
            mNoticeList.clear();
            mNoticeList.addAll(list);
        }
        showNetworkError();
    }
    private void getStarredNotices(){
        if (isOnline()){
            HttpGet httpget=new HttpGet(MainActivity.UrlOfNotice+"star_notice_list");
            try {
                httpget.addHeader("Cookie", "csrftoken=" + csrftoken);
                httpget.addHeader("Content-Type", "application/x-www-form-urlencoded");
                httpget.addHeader("Cookie", "CHANNELI_SESSID=" + CHANNELI_SESSID);
                httpget.addHeader("X-CSRFToken", csrftoken);

                AsyncTask<HttpGet, Void, String> mTask = new ConnectTaskHttpGet().execute(httpget);
                String content = mTask.get();
                //mTask.cancel(true);
                if (content != null) {
                    ArrayList<NoticeObject> list = new Parsing().parseStarredNotices(content);
                    if (list != null) {
                        mStarredList.clear();
                        mStarredList.addAll(list);
                        return;
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
                //showNetworkError();
            }
        }
        ArrayList<NoticeObject> list=mSqlHelper.getNotices("Starred", "All","");
        if (list!=null) {
            mStarredList.clear();
            mStarredList.addAll(list);
        }
    }
    private void getReadNotices(){
        if (isOnline()){
            HttpGet httpget=new HttpGet(MainActivity.UrlOfNotice+"read_notice_list/");
            try {
                httpget.addHeader("Cookie", "csrftoken=" + csrftoken);
                httpget.addHeader("Content-Type", "application/x-www-form-urlencoded");
                httpget.addHeader("Cookie", "CHANNELI_SESSID=" + CHANNELI_SESSID);
                httpget.addHeader("X-CSRFToken", csrftoken);

                AsyncTask<HttpGet, Void, String> mTask = new ConnectTaskHttpGet().execute(httpget);
                String content = mTask.get();
                //mTask.cancel(true);
                if (content != null) {
                    ArrayList<Integer> list = new Parsing().parseReadNotices(content);
                    if (list != null && list.size()>mReadList.size()) {
                        mReadList.clear();
                        mReadList.addAll(list);
                        return;
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
                //showNetworkError();
            }
        }
        ArrayList<Integer> list=mSqlHelper.getReadNotices();
        if (list!=null && list.size()>mReadList.size()) {
            mReadList.clear();
            mReadList.addAll(list);
        }
    }

    private class SwipeRefreshListener implements SwipeRefreshLayout.OnRefreshListener{
        private SwipeRefreshLayout mLayout;
        public SwipeRefreshListener(SwipeRefreshLayout layout){
            this.mLayout = layout;
        }
        public SwipeRefreshLayout getLayout(){
            return this.mLayout;
        }
        @Override
        public void onRefresh() {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    getLayout().setRefreshing(false);
                    swiperefresh=true;
                    changeList();
                }
            });
        }
    }
    public class WrapContentLinearLayoutManager extends LinearLayoutManager {
        public WrapContentLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (IndexOutOfBoundsException e) {
                //Log.e("probe", "met a IOOBE in RecyclerView");
            }
        }
    }

    public void showNetworkError(){
        showMessage("Check Network Connection");
    }
    public void showMessage(String msg){
        Snackbar snackbar=Snackbar.make((CoordinatorLayout) findViewById(R.id.main_content),msg,Snackbar.LENGTH_SHORT);
        TextView tv= (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(getResources().getColor(R.color.colorPrimary));
        tv.setHeight((int) getResources().getDimension(R.dimen.bottomBarHeight));
        tv.setTypeface(null, Typeface.BOLD);
        snackbar.show();
    }
*//*    public void showMessage(){
        Snackbar snackbar=Snackbar.make(coordinatorLayout,msg,Snackbar.LENGTH_SHORT);
        TextView tv= (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(getResources().getColor(R.color.colorPrimary));
        tv.setHeight((int) getResources().getDimension(R.dimen.bottomBarHeight));
        tv.setTypeface(null, Typeface.BOLD);
        snackbar.show();
        msg=null;
    }*//*
    public void setTitle(String title){
        try {
            getSupportActionBar().setTitle(title.replaceAll("%20", " "));
        }
        catch(Exception e){
            e.printStackTrace();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem searchMenuItem=menu.findItem(R.id.search);
        final SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, SearchActivity.class)));
        searchView.setIconified(false);
        //searchView.setSubmitButtonEnabled(true);
        searchView.setQueryHint("Search notices");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                //hideKeyboard();
                searchView.setVisibility(View.INVISIBLE);
                searchView.setVisibility(View.VISIBLE);
                searchMenuItem.collapseActionView();
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
                //hideKeyboard();
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
    public void hideKeyboard(){
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed(){
        if (MainCategory.contains("All")){
            AlertDialog.Builder dialog=new AlertDialog.Builder(this);
            dialog.setTitle("Exit");
            dialog.setMessage("Are you sure you want to exit?");
            dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    closeDialog();
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
            ExpandableListView listView= (ExpandableListView) findViewById(R.id.drawer_menu);
            listView.getChildAt(0).performClick();
        }
    }*/
}