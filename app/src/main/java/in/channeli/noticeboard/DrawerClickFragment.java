package in.channeli.noticeboard;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import adapters.CustomRecyclerViewAdapter;
import connections.ConnectTaskHttpGet;
import connections.Connections;
import objects.NoticeObject;
import utilities.Parsing;
import utilities.SQLHelper;

public class DrawerClickFragment extends Fragment {

    private HttpGet httpGet;
    private RecyclerView recyclerView;
    private CustomRecyclerViewAdapter customAdapter=null;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Connections con;
    private Parsing parsing;
    private String category;
    private String noticetype;
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

    @TargetApi(21)
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.list_view, container, false);
        Bundle args = getArguments();
        category = args.getString("category", "All");
        category = category.replaceAll(" ", "%20");
        noticetype = args.getString("noticetype","new");
        if(category.matches("Starred"))
            httpGet=new HttpGet(MainActivity.UrlOfNotice+"star_notice_list");
        else
            httpGet = new HttpGet(MainActivity.UrlOfNotice+"list_notices/"+noticetype+"/"+category+"/All/0/20/0");
        sqlHelper=new SQLHelper(getActivity());
        noticelist=new ArrayList<NoticeObject>();
        SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        csrftoken = settings.getString("csrftoken","");
        CHANNELI_SESSID = settings.getString("CHANNELI_SESSID","");

        httpGet.setHeader("Cookie","csrftoken="+csrftoken);
        httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
        httpGet.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
        httpGet.setHeader("X-CSRFToken",csrftoken);
        if (isOnline()){
            final ProgressDialog pd = new ProgressDialog(getActivity());
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.setMessage("Loading...");
            pd.setIndeterminate(true);
            pd.setCancelable(false);
            //pd.show();
            setContent();
            //pd.dismiss();
        }
        else{
            ArrayList<NoticeObject> list=sqlHelper.getNotices(category);
            if(list!=null)
                noticelist.addAll(list);
            showNetworkError();
        }

        recyclerView= (RecyclerView) view.findViewById(R.id.list_view);
        customAdapter=new CustomRecyclerViewAdapter(getActivity(),
                R.layout.list_itemview,noticelist);
        recyclerView.setAdapter(customAdapter);
        LinearLayoutManager layoutManager=new LinearLayoutManager(getActivity().getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setOnScrollListener(new RecyclerViewScrollListener(layoutManager) {
            @Override
            public void loadMore(int totalItemsCount) {

                if (isOnline()) {
                    String result = null;
                    try {
                        httpGet = new HttpGet(MainActivity.UrlOfNotice +
                                "list_notices/" + noticetype + "/" + category +
                                "/All/1/20/" + noticelist.get(totalItemsCount - 1).getId());
                        httpGet.setHeader("Cookie", "csrftoken=" + csrftoken);
                        httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                        httpGet.setHeader("Cookie", "CHANNELI_SESSID=" + CHANNELI_SESSID);
                        httpGet.setHeader("X-CSRFToken", csrftoken);
                        mTask = new ConnectTaskHttpGet().execute(httpGet);
                        result = mTask.get();
                        mTask.cancel(true);
                        if (result!=null){
                            int curSize=customAdapter.getItemCount();
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
        });

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeColors(
                Color.RED, Color.BLUE, Color.BLACK);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshListener());

        /*View v = getActivity().getCurrentFocus();
        if(v != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }*/
        return view;
    }
    private void setContent(){
        String content_first_time_notice = null;
        try {
            mTask = new ConnectTaskHttpGet().execute(httpGet);
            content_first_time_notice = mTask.get();
            mTask.cancel(true);
            con = new Connections();
            parsing = new Parsing();
            ArrayList<NoticeObject> list = parsing.parseNotices(content_first_time_notice);
            addToDB(list);
            noticelist.addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
            showNetworkError();
        }
    }
    public void showNetworkError(){
        CoordinatorLayout coordinatorLayout= (CoordinatorLayout) getActivity().findViewById(R.id.main_content);
        Snackbar.make(coordinatorLayout,"Check Newtwork Connection",Snackbar.LENGTH_LONG).show();
    }
    private abstract class RecyclerViewScrollListener extends RecyclerView.OnScrollListener{
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
        public abstract void loadMore(int totalItemsCount);
    }

    private class SwipeRefreshListener implements SwipeRefreshLayout.OnRefreshListener{
        @Override
        public void onRefresh() {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    if (isOnline()){
                        String result=null;
                        try {
                            httpGet = new HttpGet(MainActivity.UrlOfNotice+
                                    "list_notices/"+noticetype+"/"+category+
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
                    showNetworkError();
                    swipeRefreshLayout.setRefreshing(false);
                }
            });

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
}
