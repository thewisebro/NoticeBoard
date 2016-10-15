package in.channeli.noticeboard;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;

import org.apache.http.client.methods.HttpGet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import adapters.CustomRecyclerViewAdapter;
import connections.ConnectTaskHttpGet;
import connections.Connections;
import connections.ProfilePicService;
import objects.DrawerItem;
import objects.NoticeObject;
import objects.User;
import utilities.DownloadResultReceiver;
import utilities.Parsing;
import utilities.RoundImageView;
import utilities.SQLHelper;


public class MainActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    public static String UrlOfHost="http://192.168.121.187:7000/";
    public static String UrlOfNotice = UrlOfHost+"notices/";
    public static String UrlOfLogin = UrlOfHost+"login/";
    public static String UrlOfPeopleSearch = UrlOfHost+"peoplesearch/";
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar toolbar;
    private NavigationView navigationView;
    public static String NoticeType = "new", MainCategory = "All";
    private User user;
    public BottomBar bottomBar;

    CoordinatorLayout coordinatorLayout;
    HttpGet httpGet;
    public static final String PREFS_NAME = "MyPrefsFile";
    Parsing parsing;
    SharedPreferences settings;
    SharedPreferences.Editor editor;
    ArrayList<DrawerItem> group1;
    ArrayList<DrawerItem> group2;

    private RecyclerView recyclerView;
    private CustomRecyclerViewAdapter customAdapter=null;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Connections con;
    private ArrayList<NoticeObject> noticelist;
    private AsyncTask<HttpGet, Void, String> mTask;
    private SQLHelper sqlHelper;
    private String csrftoken, CHANNELI_SESSID;

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
        navigationView= (NavigationView) findViewById(R.id.left_drawer);
        toolbar= (Toolbar) findViewById(R.id.toolbar);
        coordinatorLayout= (CoordinatorLayout) findViewById(R.id.main_content);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        recyclerView= (RecyclerView) swipeRefreshLayout.findViewById(R.id.list_view);
        bottomBar= (BottomBar) findViewById(R.id.bottom_bar);
        setSupportActionBar(toolbar);
        settings = getSharedPreferences(PREFS_NAME, 0);
        editor=settings.edit();
        parsing = new Parsing();
        user = new User(settings.getString("name",""), settings.getString("info",""),
                settings.getString("enrollment_no",""));
        sqlHelper=new SQLHelper(this);
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
        setTitle("All");

        recyclerView.setAdapter(customAdapter);
        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setOnScrollListener(new RecyclerViewScrollListener(layoutManager));

        swipeRefreshLayout.setColorSchemeColors(
                Color.RED, Color.BLUE, Color.BLACK);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshListener());


    }

    void setNavigationView(){
        View headerView= LayoutInflater.from(this).inflate(R.layout.navigation_profile,null);
        navigationView.addHeaderView(headerView);
        setHeader(headerView);
        setMenu();
        //navigationView.setCheckedItem();
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                //Checking if the item is in checked state or not, if not make it in checked state
                Menu menu = navigationView.getMenu();
                for (int i = 0; i < menu.size(); i++)
                    menu.getItem(i).setChecked(false);
                menuItem.setChecked(true);

                //Closing drawer on item click
                mDrawerLayout.closeDrawers();
                if (menuItem.getGroupId() == R.id.group1) {
                    MainCategory = group1.get(menuItem.getItemId()).getName();
                    selectItem();
                } else {
                    switch (menuItem.getItemId()) {
                        case 0: //Starred
                            MainCategory = "Starred";
                            selectItem();
                            return true;
                        case 1: //Feedback
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse("market://details?id=in.channeli.noticeboard"));
                            startActivity(intent);
                            return true;
                        case 2: //Logout
                            logout();
                            return true;
                        default:
                            return true;
                    }
                }
                return true;
            }
        });
    }
    private void setBottomBar(){
        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(int itemId) {
                switch (itemId) {
                    case R.id.new_items:
                        NoticeType = "new";
                        //changingFragment();
                        changeList();
                        Snackbar.make(coordinatorLayout, "Current Notices", Snackbar.LENGTH_SHORT).show();
                        break;
                    case R.id.old_items:
                        NoticeType = "old";
                        //changingFragment();
                        changeList();
                        Snackbar.make(coordinatorLayout, "Expired Notices", Snackbar.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }
    private void setHeader(View headerView){
        final RoundImageView view= (RoundImageView) headerView.findViewById(R.id.profile_picture);
        final TextView nameView= (TextView) headerView.findViewById(R.id.name);
        final TextView infoView= (TextView) headerView.findViewById(R.id.info);

        nameView.setText(user.getName());
        infoView.setText(user.getInfo());
        final String bitmapString64=settings.getString("profilePic","");
        if(bitmapString64==""){
            try{
                String url="http://people.iitr.ernet.in/photo/"+user.getEnrollmentno();
                DownloadResultReceiver resultReceiver = new DownloadResultReceiver(new Handler());
                resultReceiver.setReceiver(new DownloadResultReceiver.Receiver() {
                    @Override
                    public void onReceiveResult(int resultCode, Bundle resultData) {
                        try{
                            Bitmap bitmap = resultData.getParcelable("imagebitmap");
                            view.setImageBitmap(bitmap);
                            ByteArrayOutputStream stream= new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);
                            String bitmapString64= Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);
                            editor.putString("profilePic", bitmapString64);
                            editor.commit();
                            editor.apply();
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
            byte[] bitmapByte= Base64.decode(bitmapString64,Base64.DEFAULT);
            Bitmap bitmap= BitmapFactory.decodeByteArray(bitmapByte, 0, bitmapByte.length);
            view.setImageBitmap(bitmap);
        }
    }
    ArrayList<DrawerItem> getConstants(){
        ArrayList<DrawerItem> list=new ArrayList<>();
        if (isOnline()){
            String constants=null;
            httpGet = new HttpGet(UrlOfNotice+"get_constants/");
            httpGet.setHeader("Cookie","csrftoken="+settings.getString("csrftoken",""));
            httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpGet.setHeader("Cookie","CHANNELI_SESSID="+settings.getString("CHANNELI_SESSID",""));
            AsyncTask<HttpGet, Void, String> mTask;
            try {
                mTask = new ConnectTaskHttpGet().execute(httpGet);
                constants = mTask.get(4000, TimeUnit.MILLISECONDS);
                mTask.cancel(true);
                list.addAll(parsing.parseConstants(constants));
                Set<String> constantSet=new HashSet<>(list.size());
                for(int i=0;i<list.size();i++)
                    constantSet.add(list.get(i).getName());
                editor.putStringSet("constants",constantSet);
                editor.apply();
                editor.commit();
                return list;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }
        if(settings.contains("constants")){
            Set<String> constants=settings.getStringSet("constants", null);
            for (String s: constants)
                list.add(new DrawerItem(s));
        }
        return list;
    }
    void setMenu(){
        final Menu menu=navigationView.getMenu();
        group1=getConstants();
        group2=new ArrayList<>();
        group2.add(new DrawerItem("Starred"));
        group2.add(new DrawerItem("Feedback"));
        group2.add(new DrawerItem("Logout"));
        for(int i=0;i<group1.size();i++){
            menu.add(R.id.group1, i, i, group1.get(i).getName())
                    .setIcon(group1.get(i).getIcon());
        }
        for(int i=0;i<group2.size();i++){
            menu.add(R.id.group2,i,i+group1.size(),group2.get(i).getName())
                    .setIcon(group2.get(i).getIcon());
        }
        for (int i = 0, count = navigationView.getChildCount(); i < count; i++) {
            final View child = navigationView.getChildAt(i);
            if (child != null && child instanceof ListView) {
                final ListView menuView = (ListView) child;
                final HeaderViewListAdapter adapter = (HeaderViewListAdapter) menuView.getAdapter();
                final BaseAdapter wrapped = (BaseAdapter) adapter.getWrappedAdapter();
                wrapped.notifyDataSetChanged();
            }
        }
    }
    void logout(){
        final AlertDialog.Builder dialog=new AlertDialog.Builder(this);
        dialog.setTitle("Logout");
        dialog.setMessage("Are you sure you want to Logout?");
        dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                httpGet = new HttpGet("http://people.iitr.ernet.in/logout/");
                httpGet.setHeader("Cookie", "csrftoken=" + settings.getString("csrftoken", ""));
                httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpGet.setHeader("Cookie", "CHANNELI_SESSID=" + settings.getString("CHANNELI_SESSID", ""));
                try {
                    new ConnectTaskHttpGet().execute(httpGet);
                    Toast.makeText(getApplicationContext(),
                            "Logged Out Successfully", Toast.LENGTH_SHORT).show();
                    editor.putString("CHANNELI_SESSID", "");
                    editor.putString("csrftoken", "");
                    editor.commit();
                    startActivity(new Intent(getApplicationContext(), SplashScreen.class));
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
/*    void changingFragment(){
        if (MainCategory.equals("Starred"))
            bottomBar.setVisibility(View.GONE);
        else
            bottomBar.setVisibility(View.VISIBLE);
        Fragment fragment = new DrawerClickFragment();
        Bundle args = new Bundle();
        args.putString("category", MainCategory);
        args.putString("noticetype", NoticeType);
        fragment.setArguments(args);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();
    }
*/
    private void selectItem() {

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                    //changingFragment();
                    changeList();
                    setTitle(MainCategory);
            }
        };
        handler.postDelayed(runnable, 300);
    }
    private void changeList(){
        if (MainCategory.equals("Starred")) {
            bottomBar.setVisibility(View.GONE);
            httpGet=new HttpGet(MainActivity.UrlOfNotice+"star_notice_list");
        }
        else {
            MainCategory = MainCategory.replaceAll(" ", "%20");
            bottomBar.setVisibility(View.VISIBLE);
            httpGet = new HttpGet(MainActivity.UrlOfNotice+"list_notices/"+NoticeType+"/"+MainCategory+"/All/0/20/0");
        }

        noticelist.clear();

        if (isOnline()){
            httpGet.setHeader("Cookie","csrftoken="+csrftoken);
            httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpGet.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
            httpGet.setHeader("X-CSRFToken", csrftoken);
            setContent();
        }
        else{
            ArrayList<NoticeObject> list=sqlHelper.getNotices(MainCategory);
            if(list!=null)
                noticelist.addAll(list);
            showNetworkError();
        }
        customAdapter.notifyDataSetChanged();
    }


    private void setContent(){
        String content = null;
        try {
            mTask = new ConnectTaskHttpGet().execute(httpGet);
            content = mTask.get();
            mTask.cancel(true);
            con = new Connections();
            parsing = new Parsing();
            ArrayList<NoticeObject> list = parsing.parseNotices(content);
            addToDB(list);
            noticelist.addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
            showNetworkError();
        }
    }

    private class RecyclerViewScrollListener extends RecyclerView.OnScrollListener{
        private int itemCount=0;
        private boolean isLoading=true;
        private LinearLayoutManager layoutManager;
        public RecyclerViewScrollListener(LinearLayoutManager manager){
            this.layoutManager=manager;
        }
        @Override
        public void onScrolled(RecyclerView view,int dx,int dy){
            int firstVisibleItem=layoutManager.findFirstVisibleItemPosition();
            int visibleItemCount=view.getChildCount();
            int totalItemCount=layoutManager.getItemCount();
            int lastVisibleItem=layoutManager.findLastVisibleItemPosition();


            if (totalItemCount < itemCount) {
                itemCount = totalItemCount;
                if (totalItemCount == 0) {
                    this.isLoading = true;
                }
            }

            if (isLoading && (totalItemCount > itemCount)) {
                isLoading = false;
                itemCount = totalItemCount;
            }

            if(!isLoading && lastVisibleItem>(totalItemCount-2)) {
                loadMore(totalItemCount);
                isLoading=true;
            }

        }
        public void loadMore(int totalItemsCount) {

            if (isOnline()) {
                String result = null;
                try {
                    httpGet = new HttpGet(MainActivity.UrlOfNotice +
                            "list_notices/" + NoticeType + "/" + MainCategory +
                            "/All/1/20/" + noticelist.get(totalItemsCount - 1).getId());
                    httpGet.setHeader("Cookie", "csrftoken=" + csrftoken);
                    httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                    httpGet.setHeader("Cookie", "CHANNELI_SESSID=" + CHANNELI_SESSID);
                    httpGet.setHeader("X-CSRFToken", csrftoken);
                    mTask = new ConnectTaskHttpGet().execute(httpGet);
                    result = mTask.get();
                    mTask.cancel(true);
                    if (result != null) {
                        int curSize = customAdapter.getItemCount();
                        ArrayList<NoticeObject> list = parsing.parseNotices(result);
                        addToDB(list);
                        noticelist.addAll(list);
                        customAdapter.notifyItemRangeInserted(curSize, list.size());
                        return;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class SwipeRefreshListener implements SwipeRefreshLayout.OnRefreshListener{
        @Override
        public void onRefresh() {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    /*if (isOnline()){
                        String result=null;
                        try {
                            httpGet = new HttpGet(MainActivity.UrlOfNotice+
                                    "list_notices/"+NoticeType+"/"+MainCategory+
                                    "/All/0/20/0");
                            httpGet.setHeader("Cookie","csrftoken="+csrftoken);
                            httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                            httpGet.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
                            httpGet.setHeader("X-CSRFToken",csrftoken);
                            mTask = new ConnectTaskHttpGet().execute(httpGet);
                            result = mTask.get();
                            mTask.cancel(true);
                            ArrayList<NoticeObject> list=parsing.parseNotices(result);
                            addToDB(list);
                            int size=noticelist.size();
                            noticelist.clear();
                            customAdapter.notifyItemRangeRemoved(0, size);
                            noticelist.addAll(list);
                            customAdapter.notifyItemRangeInserted(0,list.size());
                            swipeRefreshLayout.setRefreshing(false);
                            return;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                    showNetworkError();*/
                    changeList();
                    swipeRefreshLayout.setRefreshing(false);
                }
            });

        }
    }

    public void showNetworkError(){
        Snackbar.make(coordinatorLayout,"Check Newtwork Connection",Snackbar.LENGTH_LONG).show();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, SearchActivity.class)));
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
    @Override
    public void onBackPressed(){
        if (getSupportFragmentManager().getBackStackEntryCount()!=0){
            getSupportFragmentManager().popBackStack();
        }
        else{
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
    }
}