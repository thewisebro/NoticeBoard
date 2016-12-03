package com.channeli.noticeboard;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import android.widget.TextView;

import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;

import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import adapters.CustomRecyclerViewAdapter;
import connections.ConnectTaskHttpGet;
import objects.NoticeObject;
import utilities.Parsing;
import utilities.SQLHelper;

public class SearchActivity extends AppCompatActivity {
    String query;
    String searchUrl;
    Parsing parsing;
    ArrayList<NoticeObject> noticelist;
    ArrayList<Integer> readList;
    ArrayList<NoticeObject> starredList;
    CustomRecyclerViewAdapter adapter;
    String noticetype;
    String[] type = {"Current","Expired"};
    RecyclerView recyclerView;
    HttpGet httpGet;
    String csrftoken;
    String CHANNELI_SESSID;
    BottomBar bottomBar;
    CoordinatorLayout coordinatorLayout;
    String msg=null;
    SQLHelper sqlHelper;
    ProgressDialog progressDialog=null;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        hideKeyboard();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        parsing = new Parsing();
        sqlHelper=new SQLHelper(this);
        noticetype = "new";
        searchUrl = MainActivity.UrlOfNotice+"search/"+noticetype+"/All/All/?q=";
        setTitle("Searched Results");
        handleIntent(getIntent());
        recyclerView = (RecyclerView) findViewById(R.id.search_list_view);
        coordinatorLayout= (CoordinatorLayout) findViewById(R.id.main_content);
        bottomBar= (BottomBar) findViewById(R.id.bottom_bar);
        noticelist=new ArrayList<NoticeObject>();
        starredList=new ArrayList<>();
        readList=new ArrayList<>();

        SharedPreferences preferences=getSharedPreferences(MainActivity.PREFS_NAME, 0);
        csrftoken=preferences.getString("csrftoken","");
        CHANNELI_SESSID=preferences.getString("CHANNELI_SESSID","");

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        adapter=new CustomRecyclerViewAdapter(this,R.layout.list_itemview,noticelist,starredList,readList);
        recyclerView.setAdapter(adapter);
        setBottomBar();
    }
    @Override
    public void onPause() {
        super.onPause();
        if ((progressDialog != null) && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog=null;
    }
    private void setBottomBar(){
        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(int itemId) {
                switch (itemId) {
                    case R.id.new_items:
                        noticetype="new";
                        searchUrl = MainActivity.UrlOfNotice+"search/"+noticetype+"/All/All/?q=";
                        setNoticelist(searchUrl + query);
                        break;
                    case R.id.old_items:
                        noticetype="old";
                        searchUrl = MainActivity.UrlOfNotice+"search/"+noticetype+"/All/All/?q=";
                        setNoticelist(searchUrl + query);
                        break;
                }
            }
        });
    }

    public void showNetworkError(){
        showMessage("Check Newtwork Connection");
    }
    public void showMessage(String msg){
        CoordinatorLayout coordinatorLayout= (CoordinatorLayout) findViewById(R.id.main_content);
        Snackbar snackbar=Snackbar.make(coordinatorLayout,msg,Snackbar.LENGTH_SHORT);
        TextView tv= (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(getResources().getColor(R.color.colorPrimary));
        tv.setHeight((int) getResources().getDimension(R.dimen.bottomBarHeight));
        tv.setTypeface(null, Typeface.BOLD);
        snackbar.show();
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
            //searchView.setSubmitButtonEnabled(true);
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
                query = newText;
                query = query.replaceAll(" ","%20");
                onTextSubmit();
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
    public void hideKeyboard(){
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onNewIntent(Intent intent){
        handleIntent(intent);
    }

    private void handleIntent(Intent intent){

        if(Intent.ACTION_SEARCH.equals(intent.getAction())){
            query = intent.getStringExtra(SearchManager.QUERY);
            query = query.replaceAll(" ","%20");
        }
    }

    private void onTextSubmit(){
        String url = MainActivity.UrlOfNotice+"search/"+noticetype+"/All/All/?q="+query;
        Log.e("url sent for searching",url);
        setNoticelist(url);
    }

    private void setNoticelist(final String url){
        if (isOnline()){
            recyclerView.stopScroll();
            Thread thread=new Thread(){
                @Override
                public void run(){

                    if (readList.size()==0)
                        getReadNotices();
                    if (starredList.size()==0)
                        getStarredNotices();

                    final ArrayList<NoticeObject> list=getSearchedNotices(url);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(list!=null) {
                                try {
                                    recyclerView.scrollToPosition(0);
                                }catch (Exception e){}

                                recyclerView.getRecycledViewPool().clear();
                                noticelist.clear();
                                noticelist.addAll(list);
                                adapter.notifyDataSetChanged();
                                setTitle("Searched : " + query.replaceAll("%20", " "));
                                if (noticelist.size()>0)
                                    findViewById(R.id.no_notice).setVisibility(View.GONE);
                                else
                                    findViewById(R.id.no_notice).setVisibility(View.VISIBLE);

                                if ((progressDialog != null) && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                            }
                            else
                                showNetworkError();

                        }
                    });
                }
            };
            if (!isFinishing()){
                progressDialog=ProgressDialog.show(this,null,"Fetching Results...",true,false);
                thread.start();
            }
        }
        else
            showNetworkError();
    }

    private ArrayList<NoticeObject> getSearchedNotices(String url){
        if (isOnline()){
            httpGet=new HttpGet(url);
            httpGet.setHeader("Cookie","csrftoken="+csrftoken);
            httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpGet.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
            httpGet.setHeader("X-CSRFToken",csrftoken);

            AsyncTask<HttpGet, Void, String> task=new ConnectTaskHttpGet().execute(httpGet);
            try {
                String result=task.get();
                //task.cancel(true);
                return parsing.parseSearchNotices(result,starredList,readList);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            //task.cancel(true);
        }
        showNetworkError();
        return null;
    }
    private void getStarredNotices(){
        if (isOnline()){
            httpGet=new HttpGet(MainActivity.UrlOfNotice+"star_notice_list");
            try {
                httpGet.setHeader("Cookie","csrftoken="+csrftoken);
                httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpGet.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
                httpGet.setHeader("X-CSRFToken", csrftoken);

                AsyncTask<HttpGet, Void, String> task = new ConnectTaskHttpGet().execute(httpGet);
                String content = task.get();
                //task.cancel(true);
                if (content != null) {
                    ArrayList<NoticeObject> list = new Parsing().parseStarredNotices(content);
                    if (list != null) {
                        //sqlHelper.addNoticesList(list);
                        starredList.addAll(list);
                        return;
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
                showNetworkError();
            }
        }
        ArrayList<NoticeObject> list=sqlHelper.getNotices("Starred", "All");
        if (list!=null)
            starredList.addAll(list);
    }
    private void getReadNotices(){
        if (isOnline()){
            httpGet=new HttpGet(MainActivity.UrlOfNotice+"read_notice_list/");
            try {
                httpGet.setHeader("Cookie","csrftoken="+csrftoken);
                httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpGet.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
                httpGet.setHeader("X-CSRFToken", csrftoken);

                AsyncTask<HttpGet, Void, String> task = new ConnectTaskHttpGet().execute(httpGet);
                String content = task.get();
                //task.cancel(true);
                if (content != null) {
                    ArrayList<Integer> list = new Parsing().parseReadNotices(content);
                    if (list != null) {
                        readList.addAll(list);
                        return;
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
                showNetworkError();
            }
        }
        ArrayList<Integer> list=sqlHelper.getReadNotices();
        if (list!=null)
            readList.addAll(list);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
