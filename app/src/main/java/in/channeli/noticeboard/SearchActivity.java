package in.channeli.noticeboard;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.SearchView;
import android.widget.Spinner;

import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import adapters.CustomRecyclerViewAdapter;
import adapters.CustomSpinnerAdapter;
import connections.ConnectTaskHttpGet;
import objects.NoticeObject;
import utilities.Parsing;

public class SearchActivity extends ActionBarActivity {
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

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        parsing = new Parsing();
        noticetype = "new";
        setTitle("Searched Results");
        handleIntent(getIntent());
        recyclerView = (RecyclerView) findViewById(R.id.search_list_view);
        noticelist=new ArrayList<NoticeObject>();

        SharedPreferences preferences=getSharedPreferences(MainActivity.PREFS_NAME, 0);
        csrftoken=preferences.getString("csrftoken","");
        CHANNELI_SESSID=preferences.getString("CHANNELI_SESSID","");

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.rgb(30, 136, 229)));
        if(Build.VERSION.SDK_INT >= 21){
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.statusbarcolor));
        }

        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        adapter=new CustomRecyclerViewAdapter(getApplicationContext(),R.layout.list_itemview,noticelist);
        recyclerView.setAdapter(adapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_search, menu);

        Spinner spinner = (Spinner) menu.findItem(R.id.toggle).getActionView();
        if(null!= spinner) {
            CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(this,
                    R.layout.spinner_item, type);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position != 0) {
                        noticetype = "old";
                        searchUrl = MainActivity.UrlOfNotice+"search/"+noticetype+"/All/All/?q=";
                    }
                    else {
                        noticetype = "new";
                        searchUrl = MainActivity.UrlOfNotice+"search/"+noticetype+"/All/All/?q=";
                    }
                    setNoticelist(searchUrl+query);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        if (null != searchView )
        {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);
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
        return super.onCreateOptionsMenu(menu);
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

    private void setNoticelist(String url){
        ProgressDialog dialog=new ProgressDialog(this);
        dialog.setTitle("Loading");
        dialog.setMessage("");
        dialog.setCancelable(false);
        dialog.show();
        ArrayList<NoticeObject> list=getSearchedNotices(url);
        if(list!=null) {
            int size=noticelist.size();
            noticelist.clear();
            adapter.notifyItemRangeRemoved(0, size);
            noticelist.addAll(list);
            adapter.notifyItemRangeInserted(0, list.size());
            adapter.notifyDataSetChanged();
        }
        dialog.dismiss();
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
