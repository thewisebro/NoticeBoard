package com.channeli.noticeboard;

import android.annotation.TargetApi;
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

import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import adapters.CustomDrawerListViewAdapter;
import adapters.CustomRecyclerViewAdapter;
import connections.ConnectTaskHttpGet;
import connections.FCMIDService;
import connections.ProfilePicTask;
import objects.DrawerItem;
import objects.NoticeObject;
import objects.User;
import utilities.EndlessRecyclerViewScrollListener;
import utilities.Parsing;
import utilities.RoundImageView;
import utilities.SQLHelper;


public class MainActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    public static String UrlOfHost="http://people.iitr.ernet.in/";
    //public static String UrlOfHost="http://192.168.121.187:7000/";
    public static String UrlOfNotice = UrlOfHost+"notices/";
    public static String UrlOfLogin = UrlOfHost+"login/";
    public static String UrlOfPeopleSearch = UrlOfHost+"peoplesearch/";
    public static String UrlOfFCMRegistration = UrlOfHost+"push_subscription_sync/";
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar toolbar;
    private NavigationView navigationView;
    public static String NoticeType = "new", MainCategory = "All", Category="All";
    private User user;
    public BottomBar bottomBar;

    CoordinatorLayout coordinatorLayout;
    HttpGet httpGet;
    public static final String PREFS_NAME = "MyPrefsFile";
    Parsing parsing;
    SharedPreferences settings;
    SharedPreferences.Editor editor;
    ArrayList<DrawerItem> drawerList;
    ProgressDialog mDialog=null;

    private RecyclerView recyclerView;
    private CustomRecyclerViewAdapter customAdapter=null;
    private CustomDrawerListViewAdapter drawerAdapter=null;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ArrayList<NoticeObject> noticelist;
    private ArrayList<NoticeObject> starredList;
    private ArrayList<Integer> readList;
    private SQLHelper sqlHelper;
    private String csrftoken, CHANNELI_SESSID;
    private ExpandableListView listView;
    private EndlessRecyclerViewScrollListener scrollListener;

    String msg=null;
    boolean refreshScroll=false;  //denotes when the adapter is updated so as to control scroll listener calls
    boolean swiperefresh=false; //to update starred list and read list

    private void addToDB(ArrayList<NoticeObject> list){
        try {
            sqlHelper.addNoticesList(list);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    @TargetApi(21)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        //MainCategory=getIntent().getStringExtra("main_category");
        //Category=getIntent().getStringExtra("category");
        sqlHelper=new SQLHelper(this);
        sqlHelper.clearNotifications(); //remove all pending notifications
        settings = getSharedPreferences(PREFS_NAME, 0);
        editor=settings.edit();
        csrftoken = settings.getString("csrftoken","");
        CHANNELI_SESSID = settings.getString("CHANNELI_SESSID", "");
        if ("".equals(CHANNELI_SESSID)){
            cleanLogout();
            closeDialog();
            startActivity(new Intent(this,SplashScreen.class));
            finish();
        }
        if (!settings.getBoolean("FCM_isRegistered",false)){
            new FCMIDService().sendRegistrationToServer(settings);
        }
        parsing = new Parsing();
        user = new User(settings.getString("name",""), settings.getString("info",""),
                settings.getString("enrollment_no",""));
        navigationView= (NavigationView) findViewById(R.id.left_drawer);
        toolbar= (Toolbar) findViewById(R.id.toolbar);
        coordinatorLayout= (CoordinatorLayout) findViewById(R.id.main_content);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        recyclerView= (RecyclerView) swipeRefreshLayout.findViewById(R.id.list_view);
        bottomBar= (BottomBar) findViewById(R.id.bottom_bar);
        setSupportActionBar(toolbar);
        noticelist=new ArrayList<NoticeObject>();
        starredList=new ArrayList<NoticeObject>();
        readList=new ArrayList<Integer>();
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

        customAdapter=new CustomRecyclerViewAdapter(this,
                R.layout.list_itemview, noticelist,starredList,readList);

        setNavigationView();
        setBottomBar();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_close
        ){
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();
        //setTitle("All");

        recyclerView.setAdapter(customAdapter);
        WrapContentLinearLayoutManager layoutManager=new WrapContentLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setLayoutManager(layoutManager);
        scrollListener=new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, final int totalItemsCount, RecyclerView view) {
                if(totalItemsCount>0 && totalItemsCount<=noticelist.size() && (!MainCategory.equals("Starred")))
                    new Thread(){
                        @Override
                        public void run(){
                            if (isOnline()) {
                                int initialTab=bottomBar.getCurrentTabId();
                                if (totalItemsCount<=0 || totalItemsCount>noticelist.size())
                                    return;
                                httpGet = new HttpGet(MainActivity.UrlOfNotice +
                                        "list_notices/" + NoticeType + "/" + MainCategory.replace(" ","%20") +
                                        "/All/1/20/" + noticelist.get(totalItemsCount - 1).getId());
                                httpGet.setHeader("Cookie", "csrftoken=" + csrftoken);
                                httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                                httpGet.setHeader("Cookie", "CHANNELI_SESSID=" + CHANNELI_SESSID);
                                httpGet.setHeader("X-CSRFToken", csrftoken);
                                AsyncTask<HttpGet, Void, String> mTask = new ConnectTaskHttpGet().execute(httpGet);
                                String result = null;
                                try {
                                    result = mTask.get();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                //mTask.cancel(true);
                                if (initialTab!=bottomBar.getCurrentTabId()){
                                    scrollListener.resetState();
                                    return;
                                }
                                if (result != null && result!="") {
                                    ArrayList<NoticeObject> list = parsing.parseNotices(result,starredList,readList);
                                    if (list!= null) {
                                        addToDB(list);
                                        noticelist.addAll(list);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                recyclerView.getRecycledViewPool().clear();
                                                customAdapter.notifyDataSetChanged();
                                            }
                                        });
                                    }
                                    return;
                                }
                                showNetworkError();
                            }
                            scrollListener.resetState();
                        }
                    }.start();
            }
        };
        recyclerView.setOnScrollListener(scrollListener);
        //recyclerView.setOnScrollListener(new RecyclerViewScrollListener(layoutManager));
        //Scroll only when touched, not due to drawer clicks
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                refreshScroll = false;
                return false;
            }
        });

        swipeRefreshLayout.setColorSchemeColors(new int[]{getResources().getColor(R.color.colorAccentDark)
                ,getResources().getColor(R.color.colorPrimaryDark)});
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshListener());
    }
    @Override
    public void onPause() {
        super.onPause();
        closeDialog();
    }
    public void closeDialog(){
        try{
            if ((mDialog != null) && mDialog.isShowing()) {
                mDialog.dismiss();
            }
        }catch(Exception e){
        }finally {
            mDialog=null;
        }
    }

    void setNavigationView(){
        setHeader();
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
                        //msg = "Current Notices";
                        changeList();
                        //showMessage("Current Notices");
                        break;
                    case R.id.old_items:
                        NoticeType = "old";
                        //msg = "Expired Notices";
                        changeList();
                        //showMessage("Expired Notices");
                        break;
                }
            }
        });
    }
    private void setHeader(){
        View headerView=navigationView.findViewById(R.id.drawer_header);
        final RoundImageView view= (RoundImageView) headerView.findViewById(R.id.profile_picture);
        final TextView nameView= (TextView) headerView.findViewById(R.id.name);
        final TextView infoView= (TextView) headerView.findViewById(R.id.info);

        nameView.setText(user.getName());
        infoView.setText(user.getInfo());
        new Thread(){
            @Override
            public void run(){
                final Bitmap bitmap=sqlHelper.getProfilePic();
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
                                    sqlHelper.addProfilePic(bitm);
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
        Set<String> constants=settings.getStringSet("constants", null);
        if(constants!=null && constants.size()>0){
            List<String> listConstants=new ArrayList<>();
            for (String s: constants)
                listConstants.add(s);
            Collections.sort(listConstants);
            for (String s:listConstants) {
                Set<String> set=settings.getStringSet(s,null);
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
            httpGet.setHeader("Cookie","csrftoken="+settings.getString("csrftoken",""));
            httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpGet.setHeader("Cookie", "CHANNELI_SESSID=" + settings.getString("CHANNELI_SESSID", ""));
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
        listView= (ExpandableListView) navigationView.findViewById(R.id.drawer_menu);
        drawerList=getConstants();
        drawerList.add(new DrawerItem("Starred", null));
        drawerList.add(new DrawerItem("", null));
        drawerList.add(new DrawerItem("Notifications Settings", null));
        drawerList.add(new DrawerItem("Feedback", null));
        drawerList.add(new DrawerItem("Logout", null));

        drawerAdapter=new CustomDrawerListViewAdapter(drawerList,this){
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
        };
        listView.setAdapter(drawerAdapter);
    }
    public boolean checkDrawerColorChange(int pos){
        String gp_name=drawerList.get(pos).getName();
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
        switch (drawerList.get(groupPosition).getName()) {
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
                intent2.setData(Uri.parse("market://details?id=in.channeli.noticeboard"));
                startActivity(intent2);
                break;
            case "Logout": //Logout
                logout();
                break;
            default:
                MainCategory = drawerList.get(groupPosition).getName();
                if(childPosition<0)
                    Category="All";
                else
                    Category = drawerList.get(groupPosition).getCategories().get(childPosition);
                selectItem();
        }
    }
    void cleanLogout(){
        editor.clear();
        editor.apply();
        sqlHelper.clear();
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
                            httpGet.setHeader("Cookie", "csrftoken=" + settings.getString("csrftoken", ""));
                            httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                            httpGet.setHeader("Cookie", "CHANNELI_SESSID=" + settings.getString("CHANNELI_SESSID", ""));
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
        //noticelist.clear();
        recyclerView.stopScroll();
        Thread thread=new Thread(){
            @Override
            public void run(){

                if (swiperefresh || readList.size()==0)
                    getReadNotices();
                if (swiperefresh || starredList.size()==0)
                    getStarredNotices();

                swiperefresh=false;

                httpGet.setHeader("Cookie","csrftoken="+csrftoken);
                httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpGet.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
                httpGet.setHeader("X-CSRFToken", csrftoken);
                setContent();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            recyclerView.scrollToPosition(0);
                        }catch (Exception e){}

                        recyclerView.getRecycledViewPool().clear();
                        customAdapter.notifyDataSetChanged();
                        if (noticelist.size() > 0)
                            findViewById(R.id.no_notice).setVisibility(View.GONE);
                        else
                            findViewById(R.id.no_notice).setVisibility(View.VISIBLE);
                        scrollListener.resetState();
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDialog.show();
                }
            });
            String content = null;
            try {
                AsyncTask<HttpGet, Void, String> mTask = new ConnectTaskHttpGet().execute(httpGet);
                content = mTask.get();
                //mTask.cancel(true);
                //list=parsing.parseNotices(content,starredList,readList);
                if (MainCategory.equals("Starred")) {
                    list = parsing.parseStarredNotices(content,readList);
                    if (list!=null) {
                        starredList.clear();
                        starredList.addAll(list);
                    }
                }
                else
                    list=parsing.parseNotices(content,starredList,readList);

                if (list!=null) {
                    addToDB(list);
                    noticelist.clear();
                    noticelist.addAll(list);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if("All".equals(Category))
            list=sqlHelper.getNotices(MainCategory);
        else
            list=sqlHelper.getNotices(MainCategory,Category);
        if(list!=null) {
            noticelist.clear();
            noticelist.addAll(list);
        }
        showNetworkError();
    }
    private void getStarredNotices(){
        if (isOnline()){
            HttpGet httpget=new HttpGet(MainActivity.UrlOfNotice+"star_notice_list");
            try {
                httpget.setHeader("Cookie","csrftoken="+csrftoken);
                httpget.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpget.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
                httpget.setHeader("X-CSRFToken", csrftoken);

                AsyncTask<HttpGet, Void, String> mTask = new ConnectTaskHttpGet().execute(httpget);
                String content = mTask.get();
                //mTask.cancel(true);
                if (content != null) {
                    ArrayList<NoticeObject> list = new Parsing().parseStarredNotices(content);
                    if (list != null) {
                        //addToDB(list);
                        //addToDB(list);
                        starredList.clear();
                        starredList.addAll(list);
                        return;
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
                //showNetworkError();
            }
        }
        ArrayList<NoticeObject> list=sqlHelper.getNotices("Starred", "All");
        if (list!=null) {
            starredList.clear();
            starredList.addAll(list);
        }
    }
    private void getReadNotices(){
        if (isOnline()){
            HttpGet httpget=new HttpGet(MainActivity.UrlOfNotice+"read_notice_list/");
            try {
                httpget.setHeader("Cookie","csrftoken="+csrftoken);
                httpget.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpget.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
                httpget.setHeader("X-CSRFToken", csrftoken);

                AsyncTask<HttpGet, Void, String> mTask = new ConnectTaskHttpGet().execute(httpget);
                String content = mTask.get();
                //mTask.cancel(true);
                if (content != null) {
                    ArrayList<Integer> list = new Parsing().parseReadNotices(content);
                    if (list != null && list.size()>readList.size()) {
                        readList.clear();
                        readList.addAll(list);
                        return;
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
                //showNetworkError();
            }
        }
        ArrayList<Integer> list=sqlHelper.getReadNotices();
        if (list!=null && list.size()>readList.size()) {
            readList.clear();
            readList.addAll(list);
        }
    }

    private class SwipeRefreshListener implements SwipeRefreshLayout.OnRefreshListener{
        @Override
        public void onRefresh() {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(false);
                    swiperefresh=true;
                    //int groupPos=getGroupPosition();
                    //int childPos=getChildPosition(groupPos);
                    //drawerAdapter.groupPos=groupPos;
                    //drawerAdapter.childPos=childPos;
                    //drawerAdapter.notifyDataSetChanged();
                    changeList();
                }
            });
        }
    }
    private int getGroupPosition(){
        for (int pos=0;pos<drawerList.size();pos++)
            if (MainCategory.equals(drawerList.get(pos).getName()))
                return pos;
        return 0;
    }
    private int getChildPosition(int gp){
        if (Category.contains("All"))
            return -1;
        int size=drawerList.get(gp).getCategories().size();
        ArrayList<String> list=drawerList.get(gp).getCategories();
        for (int pos=0;pos<size;pos++){
            if (Category.equals(list.get(pos)))
                return pos;
        }
        return -1;
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
        Snackbar snackbar=Snackbar.make(coordinatorLayout,msg,Snackbar.LENGTH_SHORT);
        TextView tv= (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(getResources().getColor(R.color.colorPrimary));
        tv.setHeight((int) getResources().getDimension(R.dimen.bottomBarHeight));
        tv.setTypeface(null, Typeface.BOLD);
        snackbar.show();
    }
    public void showMessage(){
        Snackbar snackbar=Snackbar.make(coordinatorLayout,msg,Snackbar.LENGTH_SHORT);
        TextView tv= (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(getResources().getColor(R.color.colorPrimary));
        tv.setHeight((int) getResources().getDimension(R.dimen.bottomBarHeight));
        tv.setTypeface(null, Typeface.BOLD);
        snackbar.show();
        msg=null;
    }
    public void setTitle(String title){
        try {
            getSupportActionBar().setTitle(title.replaceAll("%20"," "));
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    public boolean isOnline() {

        Runtime runtime = Runtime.getRuntime();
        try {

            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);

        } catch (IOException e)          { e.printStackTrace(); }
        catch (InterruptedException e) { e.printStackTrace(); }

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
        else
            listView.getChildAt(0).performClick();
    }
}