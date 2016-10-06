package in.channeli.noticeboard;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnMenuTabSelectedListener;

import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import adapters.CustomRecyclerViewAdapter;
import connections.ConnectTaskHttpGet;
import objects.NoticeObject;
import utilities.Parsing;

public class SearchActivity extends AppCompatActivity {
    String query;
    String searchUrl;
    Parsing parsing;
    ArrayList<NoticeObject> noticelist;
    CustomRecyclerViewAdapter adapter;
    String noticetype;
    String[] type = {"Current","Expired"};
    RecyclerView recyclerView;
    HttpGet httpGet;
    String csrftoken;
    String CHANNELI_SESSID;
    AsyncTask<HttpGet, Void, String> task;
    BottomBar bottomBar;
    CoordinatorLayout coordinatorLayout;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        parsing = new Parsing();
        noticetype = "new";
        searchUrl = MainActivity.UrlOfNotice+"search/"+noticetype+"/All/All/?q=";
        setTitle("Searched Results");
        handleIntent(getIntent());
        recyclerView = (RecyclerView) findViewById(R.id.search_list_view);
        coordinatorLayout= (CoordinatorLayout) findViewById(R.id.search_view);
        bottomBar= BottomBar.attach(this,savedInstanceState);
        setBottomBar();
        noticelist=new ArrayList<NoticeObject>();

        SharedPreferences preferences=getSharedPreferences(MainActivity.PREFS_NAME, 0);
        csrftoken=preferences.getString("csrftoken","");
        CHANNELI_SESSID=preferences.getString("CHANNELI_SESSID","");

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        adapter=new CustomRecyclerViewAdapter(getApplicationContext(),R.layout.list_itemview,noticelist);
        recyclerView.setAdapter(adapter);
        setNoticelist(searchUrl+query);
    }
    private void setBottomBar(){
        bottomBar.setItemsFromMenu(R.menu.menu_bottom_bar, new OnMenuTabSelectedListener() {
            @Override
            public void onMenuItemSelected(int itemId) {
                switch (itemId) {
                    case R.id.new_items:
                        noticetype="new";
                        searchUrl = MainActivity.UrlOfNotice+"search/"+noticetype+"/All/All/?q=";
                        setNoticelist(searchUrl+query);
                        Snackbar.make(coordinatorLayout, "Current Notices", Snackbar.LENGTH_SHORT).show();
                        break;
                    case R.id.old_items:
                        noticetype="old";
                        searchUrl = MainActivity.UrlOfNotice+"search/"+noticetype+"/All/All/?q=";
                        setNoticelist(searchUrl + query);
                        Snackbar.make(coordinatorLayout, "Expired Notices", Snackbar.LENGTH_SHORT).show();
                        break;
                }
            }
        });
        bottomBar.setActiveTabColor(getResources().getColor(R.color.bottomBarActive));
        bottomBar.useDarkTheme(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        if (null != searchView )
        {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconified(false);
            searchView.setSubmitButtonEnabled(true);
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
                return true;
            }
        };
        searchView.setOnQueryTextListener(queryTextListener);
        return true;
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
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setMessage("Loading...");
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.show();
        Thread thread= new Thread(){
            @Override
            public void run(){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<NoticeObject> list=getSearchedNotices(url);
                        if(list!=null) {
                            int size=noticelist.size();
                            noticelist.clear();
                            noticelist.addAll(list);
                            adapter.notifyDataSetChanged();
                        }
                        pd.dismiss();
                    }
                });
            }
        };
        thread.start();
    }

    private ArrayList<NoticeObject> getSearchedNotices(String url){
        if (isOnline()){
            httpGet=new HttpGet(url);
            httpGet.setHeader("Cookie","csrftoken="+csrftoken);
            httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpGet.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
            httpGet.setHeader("X-CSRFToken",csrftoken);

            task=new ConnectTaskHttpGet().execute(httpGet);
            try {
                String result=task.get();
                return parsing.parseSearchNotices(result);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return null;
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
