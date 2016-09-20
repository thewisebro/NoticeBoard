package in.channeli.noticeboard;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ListView;

import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import adapters.CustomFragmentAdapter;
import connections.ConnectTaskHttpGet;
import connections.Connections;
import objects.NoticeObject;
import utilities.Parsing;
import utilities.SQLHelper;

public class DrawerClickFragment extends Fragment {

    private HttpGet httpGet;
    private ListView listView;
    private CustomFragmentAdapter customFragmentAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Connections con;
    private Parsing parsing;
    private String category;
    private String noticetype;
    private ArrayList<NoticeObject> noticelist;
    private AsyncTask<HttpGet, Void, String> mTask;
    private SQLHelper sqlHelper;
    private String csrftoken, CHANNELI_SESSID;

    @TargetApi(21)
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.list_view, container, false);
        Bundle args = getArguments();
        category = args.getString("category", "All");
        category = category.replaceAll(" ", "%20");
        noticetype = args.getString("noticetype","new");
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
            String content_first_time_notice = null;
            try {
                mTask = new ConnectTaskHttpGet().execute(httpGet);
                content_first_time_notice = mTask.get();
                mTask.cancel(true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            con = new Connections();
            parsing = new Parsing();
            noticelist.addAll(parsing.parseNotices(content_first_time_notice));
        }
        else{
            ArrayList<NoticeObject> list=sqlHelper.getNotices();
            if(list!=null)
                noticelist.addAll(list);
        }

        listView = (ListView) view.findViewById(R.id.my_list_view);
        customFragmentAdapter = new CustomFragmentAdapter(getActivity().getApplicationContext(),
                R.layout.list_itemview,noticelist);
        listView.setAdapter(customFragmentAdapter);

        listView.setOnScrollListener(new ListViewScrollListener(2) {
            @Override
            public void loadMore(int page, int totalItemsCount) {
                if (isOnline()){
                    String result = null;
                    try {
                        httpGet = new HttpGet(MainActivity.UrlOfNotice +
                                "list_notices/" + noticetype + "/" + category +
                                "/All/1/20/" + noticelist.get(totalItemsCount - 1).getId());
                        httpGet.setHeader("Cookie","csrftoken="+csrftoken);
                        httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
                        httpGet.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
                        httpGet.setHeader("X-CSRFToken",csrftoken);
                        mTask = new ConnectTaskHttpGet().execute(httpGet);
                        result = mTask.get();
                        mTask.cancel(true);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    ArrayList<NoticeObject> list=parsing.parseNotices(result);
                    noticelist.addAll(list);
                    customFragmentAdapter.addList(list);
                }
            }
        });
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeColors(
                Color.RED, Color.BLUE, Color.BLACK);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshListener());

        View v = getActivity().getCurrentFocus();
        if(v != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
        return view;
    }

    private abstract class ListViewScrollListener implements ListView.OnScrollListener{

        private int bufferItemCount = 2;
        private int currentPage = 0;
        private int itemCount = 0;
        private boolean isLoading = true;

        public ListViewScrollListener(int bufferItemCount){
            this.bufferItemCount = bufferItemCount;
        }

        public abstract void loadMore(int page, int totalItemsCount);

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (totalItemCount < itemCount) {
                this.itemCount = totalItemCount;
                if (totalItemCount == 0) {
                    this.isLoading = true; }
            }

            if (isLoading && (totalItemCount > itemCount)) {
                isLoading = false;
                itemCount = totalItemCount;
                currentPage++;
            }

            if (!isLoading && (totalItemCount - visibleItemCount)<=(firstVisibleItem + bufferItemCount)) {
                loadMore(currentPage + 1, totalItemCount);
                isLoading = true;
            }
        }
    }

    private class SwipeRefreshListener implements SwipeRefreshLayout.OnRefreshListener{

        @Override
        public void onRefresh() {
            new Handler().postDelayed(new Runnable() {
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
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                        ArrayList<NoticeObject> list=parsing.parseNotices(result);
                        noticelist.clear();
                        customFragmentAdapter.clear();
                        noticelist.addAll(list);
                        customFragmentAdapter.addList(list);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
            },3000);

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
