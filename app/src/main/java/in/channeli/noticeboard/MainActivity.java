package in.channeli.noticeboard;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import adapters.CustomDrawerListAdapter;
import connections.ConnectTaskHttpGet;
import objects.Category;
import objects.User;
import utilities.Parsing;


public class MainActivity extends ActionBarActivity {

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    public static String UrlOfHost="http://192.168.121.187:7000/";
    public static String UrlOfNotice = UrlOfHost+"notices/";
    public static String UrlOfLogin = UrlOfHost+"login/";
    public static String UrlOfPeopleSearch = UrlOfHost+"peoplesearch/";
    private ActionBarDrawerToggle mDrawerToggle;
    public static String NoticeType = "new", MainCategory = "All";

    HttpGet httpGet;
    public static final String PREFS_NAME = "MyPrefsFile";
    ArrayList<Category> categories;
    Parsing parsing;
    SharedPreferences settings;
    SharedPreferences.Editor editor;

    @Override
    @TargetApi(21)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = getSharedPreferences(PREFS_NAME, 0);
        editor=settings.edit();

        httpGet = new HttpGet(UrlOfNotice+"get_constants/");
        httpGet.setHeader("Cookie","csrftoken="+settings.getString("csrftoken",""));
        httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
        httpGet.setHeader("Cookie","CHANNELI_SESSID="+settings.getString("CHANNELI_SESSID",""));
        //ArrayList<Category> listCategory=new ArrayList<>();
        String constants = settings.getString("constants","");
        if (isOnline()){
            AsyncTask<HttpGet, Void, String> mTask;
            try {

                mTask = new ConnectTaskHttpGet().execute(httpGet);
                constants = mTask.get(4000, TimeUnit.MILLISECONDS);
                mTask.cancel(true);
                if(constants!=null && constants!="") {
                    editor.putString("constants", constants);
                    editor.commit();
                    editor.apply();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }

        getSupportActionBar().setIcon(R.drawable.ic_drawer);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.rgb(30, 136, 229)));
        parsing = new Parsing();

        categories = new ArrayList<>();
        categories.add(new Category(true));
        categories.add(new Category());
        categories.add(new Category("space"));
        categories.addAll(parsing.parse_constants(constants));
        categories.add(new Category("space"));
        categories.add(new Category("Feedback"));
        categories.add(new Category("Logout"));
        categories.add(new Category("space"));

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setBackgroundColor(Color.WHITE);
        changingFragment("All");
        setTitle("All Current");
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

        User user = new User(settings.getString("name",""), settings.getString("info",""),
                settings.getString("enrollment_no",""));
        mDrawerList.setAdapter(new CustomDrawerListAdapter(this,
                R.layout.drawerlist_itemview, categories,user));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        if(Build.VERSION.SDK_INT >= 21){
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.statusbarcolor));
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    void changingFragment(String category){
        Fragment fragment = new DrawerClickFragment();
        Bundle args = new Bundle();
        args.putString("category",category);
        args.putString("noticetype",NoticeType);
        fragment.setArguments(args);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, SearchResultsActivity.class)));
        searchView.setIconified(false);
        searchView.setSubmitButtonEnabled(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(isOnline()) {
                if (categories.get(position).main_category.contains("Logout")) {
                    httpGet = new HttpGet("http://people.iitr.ernet.in/logout/");
                    httpGet.setHeader("Cookie","csrftoken="+settings.getString("csrftoken",""));
                    httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                    httpGet.setHeader("Cookie","CHANNELI_SESSID="+settings.getString("CHANNELI_SESSID",""));
                    try{
                        new ConnectTaskHttpGet().execute(httpGet);
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "logged out successfully", Toast.LENGTH_SHORT);
                        toast.show();
                        editor.putString("CHANNELI_SESSID","");
                        editor.putString("csrftoken","");
                        editor.commit();
                        finish();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                else if(categories.get(position).main_category.contains("Feedback")){
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://details?id=in.channeli.noticeboard"));
                    startActivity(intent);
                }
                else if (!categories.get(position).main_category.equals("space") &&
                        !categories.get(position).main_category.equals("null"))
                    selectItem(position);
            }
            else{
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Sorry! Could not connect. Check the internet connection!", Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    private void selectItem(final int position) {
        mDrawerLayout.closeDrawer(mDrawerList);
        mDrawerList.setItemChecked(position, true);

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                    MainCategory = categories.get(position).main_category;
                    changingFragment(MainCategory);
                    if (NoticeType.equals("new"))
                        setTitle(categories.get(position).main_category + " "
                                + "Current");
                    else
                        setTitle(categories.get(position).main_category + " "
                                + "Expired");
            }
        };
        handler.postDelayed(runnable, 250);
    }

    public void setTitle(String title){
        try {
            getSupportActionBar().setTitle(title);
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
    public void onBackPressed(){
        super.onBackPressed();
    }
}