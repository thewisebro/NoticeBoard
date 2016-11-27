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

import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;

import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
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
import connections.ProfilePicService;
import objects.DrawerItem;
import objects.NoticeObject;
import objects.User;
import utilities.DownloadResultReceiver;
import utilities.EndlessRecyclerViewScrollListener;
import utilities.Parsing;
import utilities.RoundImageView;
import utilities.SQLHelper;


public class MainActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    public static String UrlOfHost="http://192.168.121.187:7000/";
    //public static String UrlOfHost="http://people.iitr.ernet.in/";
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

    private RecyclerView recyclerView;
    private CustomRecyclerViewAdapter customAdapter=null;
    private CustomDrawerListViewAdapter drawerAdapter=null;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ArrayList<NoticeObject> noticelist;
    private AsyncTask<HttpGet, Void, String> mTask;
    private SQLHelper sqlHelper;
    private String csrftoken, CHANNELI_SESSID;
    private ExpandableListView listView;
    private EndlessRecyclerViewScrollListener scrollListener;

    String msg=null;
    boolean refreshScroll=false;  //denotes when the adapter is updated so as to control scroll listener calls

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
        MainCategory=getIntent().getStringExtra("main_category");
        Category=getIntent().getStringExtra("category");
        settings = getSharedPreferences(PREFS_NAME, 0);
        if (!settings.getBoolean("FCM_isRegistered",false)){
            new FCMIDService().sendRegistrationToServer(settings);
        }
        editor=settings.edit();
        parsing = new Parsing();
        user = new User(settings.getString("name",""), settings.getString("info",""),
                settings.getString("enrollment_no",""));
        sqlHelper=new SQLHelper(this);
        navigationView= (NavigationView) findViewById(R.id.left_drawer);
        toolbar= (Toolbar) findViewById(R.id.toolbar);
        coordinatorLayout= (CoordinatorLayout) findViewById(R.id.main_content);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        recyclerView= (RecyclerView) swipeRefreshLayout.findViewById(R.id.list_view);
        bottomBar= (BottomBar) findViewById(R.id.bottom_bar);
        setSupportActionBar(toolbar);
        noticelist=new ArrayList<NoticeObject>();
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        csrftoken = settings.getString("csrftoken","");
        CHANNELI_SESSID = settings.getString("CHANNELI_SESSID","");
        customAdapter=new CustomRecyclerViewAdapter(this,
                R.layout.list_itemview, noticelist);

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
        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        scrollListener=new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, final int totalItemsCount, RecyclerView view) {
                if(totalItemsCount>0)
                    new Thread(){
                        @Override
                        public void run(){
                            if (isOnline()) {
                                httpGet = new HttpGet(MainActivity.UrlOfNotice +
                                        "list_notices/" + NoticeType + "/" + MainCategory +
                                        "/All/1/20/" + noticelist.get(totalItemsCount - 1).getId());
                                httpGet.setHeader("Cookie", "csrftoken=" + csrftoken);
                                httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                                httpGet.setHeader("Cookie", "CHANNELI_SESSID=" + CHANNELI_SESSID);
                                httpGet.setHeader("X-CSRFToken", csrftoken);
                                mTask = new ConnectTaskHttpGet().execute(httpGet);
                                String result = null;
                                try {
                                    result = mTask.get();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                mTask.cancel(true);
                                if (result != null && result!="") {
                                    final String parseString=result;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            int curSize = customAdapter.getItemCount();
                                            ArrayList<NoticeObject> list = parsing.parseNotices(parseString);
                                            addToDB(list);
                                            noticelist.addAll(list);
                                            customAdapter.notifyItemRangeInserted(curSize, list.size());
                                        }
                                    });
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

        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshListener());
    }

    void setNavigationView(){
        setHeader();
        setDrawerMenu();
    }

    private void setBottomBar(){
        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(int itemId) {
                refreshScroll=true;
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
        Bitmap bitmap=sqlHelper.getProfilePic();
        if(bitmap==null){
            try{
                String url="http://people.iitr.ernet.in/photo/"+user.getEnrollmentno();
                DownloadResultReceiver resultReceiver = new DownloadResultReceiver(new Handler());
                resultReceiver.setReceiver(new DownloadResultReceiver.Receiver() {
                    @Override
                    public void onReceiveResult(int resultCode, Bundle resultData) {
                        try{
                            Bitmap bitmap = resultData.getParcelable("imagebitmap");
                            view.setImageBitmap(bitmap);
                            sqlHelper.addProfilePic(bitmap);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                });
                Intent intent = new Intent(Intent.ACTION_SYNC, null, getApplicationContext(),
                        ProfilePicService.class);
                intent.putExtra("receiver", resultReceiver);
                intent.putExtra("imageurl", url);
                getApplicationContext().startService(intent);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            view.setImageBitmap(bitmap);
        }
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
            AsyncTask<HttpGet, Void, String> mTask;
            try {
                mTask = new ConnectTaskHttpGet().execute(httpGet);
                String constant = mTask.get(4000, TimeUnit.MILLISECONDS);
                mTask.cancel(true);
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
        drawerList.add(new DrawerItem("Notifications Settings",null));
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
                    this.groupPos=position;
                    this.childPos=-1;
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
    void logout(){
        final AlertDialog.Builder dialog=new AlertDialog.Builder(this);
        dialog.setTitle("Logout");
        dialog.setMessage("Are you sure you want to Logout?");
        dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (isOnline()){
                    final ProgressDialog progressDialog=ProgressDialog.show(MainActivity.this,null,"Logging out",true,false);
                    new Thread(){
                        @Override
                        public void run(){
                            httpGet = new HttpGet("http://people.iitr.ernet.in/logout/");
                            httpGet.setHeader("Cookie", "csrftoken=" + settings.getString("csrftoken", ""));
                            httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                            httpGet.setHeader("Cookie", "CHANNELI_SESSID=" + settings.getString("CHANNELI_SESSID", ""));
                            try {
                                new ConnectTaskHttpGet().execute(httpGet);
                                editor.clear();
                                editor.apply();
                                editor.commit();
                                sqlHelper.clear();
                                startActivity(new Intent(getApplicationContext(), SplashScreen.class));
                                progressDialog.dismiss();
                                finish();
                            } catch (Exception e) {
                                e.printStackTrace();
                                showNetworkError();
                            }
                            progressDialog.dismiss();
                        }
                    }.start();
                }
                else
                    showNetworkError();
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
        noticelist.clear();

        final ProgressDialog pd=ProgressDialog.show(this,null,"Loading...",true,false);

        new Thread(){
            @Override
        public void run(){
                if (isOnline()){
                    httpGet.setHeader("Cookie","csrftoken="+csrftoken);
                    httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                    httpGet.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
                    httpGet.setHeader("X-CSRFToken", csrftoken);
                    setContent();
                }
                else{
                    ArrayList<NoticeObject> list=null;
                    if("All".equals(Category))
                        list=sqlHelper.getNotices(MainCategory);
                    else
                        list=sqlHelper.getNotices(MainCategory,Category);
                    if(list!=null)
                        noticelist.addAll(list);
                    showNetworkError();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.scrollToPosition(0);
                        customAdapter.notifyDataSetChanged();
                        scrollListener.resetState();
                        pd.dismiss();
                        if (msg!=null)
                            showMessage();
                    }
                });

            }
        }.start();
    }

    private void setContent(){
        String content = null;
        try {
            mTask = new ConnectTaskHttpGet().execute(httpGet);
            content = mTask.get();
            mTask.cancel(true);
            parsing = new Parsing();
            ArrayList<NoticeObject> list = parsing.parseNotices(content);
            addToDB(list);
            noticelist.addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
            showNetworkError();
        }
    }

    private class SwipeRefreshListener implements SwipeRefreshLayout.OnRefreshListener{
        @Override
        public void onRefresh() {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(false);
                    changeList();
                }
            });
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
        final SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, SearchActivity.class)));
        searchView.setIconified(false);
        searchView.setSubmitButtonEnabled(true);
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
                hideKeyboard();
                return true;
            }
        });
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