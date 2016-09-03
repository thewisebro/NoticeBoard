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

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import adapters.CustomFragmentAdapter;
import connections.ConnectTaskHttpGet;
import connections.Connections;
import objects.NoticeObject;
import utilities.Parsing;

/*
Created by manohar on 2/2/15.
 */
public class DrawerClickFragment extends Fragment {

    HttpGet httpPost;
    ListView listView;
    CustomFragmentAdapter customFragmentAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    Connections con;
    Parsing parsing;
    final String noticeurl = MainActivity.UrlOfNotice+"get_notice/";
    String category;
    String noticetype;
    ArrayList<NoticeObject> noticelist;
    AsyncTask<HttpGet, Void, String> mTask;

    String csrftoken, CHANNELI_SESSID;

    @TargetApi(21)
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.list_view, container, false);
        Bundle args = getArguments();
        category = args.getString("category","All");
        category = category.replaceAll(" ","%20");
        noticetype = args.getString("noticetype","new");
        httpPost = new HttpGet(MainActivity.UrlOfNotice+"list_notices/"+noticetype+"/"+category+"/All/0/20/0");

        SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        //SharedPreferences.Editor editor = settings.edit();
        csrftoken = settings.getString("csrftoken","");
        CHANNELI_SESSID = settings.getString("CHANNELI_SESSID","");

        httpPost.setHeader("Cookie","csrftoken="+csrftoken);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        httpPost.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
        httpPost.setHeader("X-CSRFToken",csrftoken);
        String content_first_time_notice = null;

        try {
            mTask = new ConnectTaskHttpGet().execute(httpPost);
            content_first_time_notice = mTask.get();
            mTask.cancel(true);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        con = new Connections();
        parsing = new Parsing();
        noticelist = parsing.parseNotices(content_first_time_notice);

        listView = (ListView) view.findViewById(R.id.my_list_view);
        customFragmentAdapter = new CustomFragmentAdapter(getActivity().getApplicationContext(),
                R.layout.list_itemview,noticelist);
        customFragmentAdapter.addAll(noticelist);
        listView.setAdapter(customFragmentAdapter);
        //listView.setOnItemClickListener(new ListViewItemClickListener());

        listView.setOnScrollListener(new ListViewScrollListener(2) {
            @Override
            public void loadMore(int page, int totalItemsCount) {
                String result = null;
                try {
                    httpPost = new HttpGet(MainActivity.UrlOfNotice +
                            "list_notices/" + noticetype + "/" + category +
                            "/All/1/20/" + noticelist.get(totalItemsCount - 1).getId());
                    httpPost.setHeader("Cookie","csrftoken="+csrftoken);
                    httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                    httpPost.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
                    httpPost.setHeader("X-CSRFToken",csrftoken);
                    mTask = new ConnectTaskHttpGet().execute(httpPost);
                    result = mTask.get();
                    mTask.cancel(true);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                noticelist.addAll(parsing.parseNotices(result));
                customFragmentAdapter.notifyDataSetChanged();
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

    /*void setRead(int position,View view){
        view.setBackgroundDrawable(getResources().getDrawable(R.drawable.read_notice_bg));
        noticelist.get(position).setRead(true);
        HttpPost post=new HttpPost(MainActivity.UrlOfNotice+"read_star_notice/"+
                    noticelist.get(position).getId()+"/add_read/");
        post.setHeader("Cookie","csrftoken="+csrftoken);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
        post.setHeader("CHANNELI_DEVICE","android");
        post.setHeader("X-CSRFToken",csrftoken);
        ConnectTaskHttpPost readTask= (ConnectTaskHttpPost) new ConnectTaskHttpPost().execute(post);
        String result="";
        try {
            result=readTask.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private class ListViewItemClickListener implements ListView.OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int selectedId=noticelist.get(position).getId();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(noticeurl+selectedId);
            String url = stringBuilder.toString();
            httpPost = new HttpGet(url);
            httpPost.setHeader("Cookie","csrftoken="+csrftoken);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
            httpPost.setHeader("X-CSRFToken",csrftoken);
            String result = null;
            try {
                result = new ConnectTaskHttpGet(getActivity()).execute(httpPost).get();

            } catch (Exception e) {
                e.printStackTrace();

            }
            if(!result.equals("")) {
                if(!noticelist.get(position).getRead())
                    setRead(position,view);
                NoticeInfo noticeInfo = parsing.parseNoticeInfo(result);
                Intent intent = new Intent(getActivity(), Notice.class);
                intent.putExtra("noticeinfo", noticeInfo.getContent());
                startActivity(intent);
            }
            else {
                Toast toast = Toast.makeText(getActivity(),
                        "Cannot connect to internet", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }*/

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
                    String result=null;
                    try {
                        httpPost = new HttpGet(MainActivity.UrlOfNotice+
                                "list_notices/"+noticetype+"/"+category+
                                "/All/0/20/0");
                        httpPost.setHeader("Cookie","csrftoken="+csrftoken);
                        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                        httpPost.setHeader("Cookie","CHANNELI_SESSID="+CHANNELI_SESSID);
                        httpPost.setHeader("X-CSRFToken",csrftoken);
                        mTask = new ConnectTaskHttpGet().execute(httpPost);
                        result = mTask.get();
                        mTask.cancel(true);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    noticelist.clear();
                    noticelist.addAll(parsing.parseNotices(result));
                    customFragmentAdapter.notifyDataSetChanged();

                    swipeRefreshLayout.setRefreshing(false);
                }
            },3000);

        }
    }
}
