package in.channeli.noticeboard;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import org.apache.http.client.methods.HttpGet;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import adapters.CustomRecyclerViewAdapter;
import connections.ConnectTaskHttpGet;
import objects.NoticeObject;
import utilities.Parsing;


public class SearchActivity extends ActionBarActivity {
    String[] type = {"New","Old"};
    String query;
    String noticetype;
    RecyclerView recyclerView;
    ArrayList<NoticeObject> noticeList;
    CustomRecyclerViewAdapter adapter;
    String searchURL;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        noticeList=new ArrayList<NoticeObject>();
        recyclerView= (RecyclerView) findViewById(R.id.search_list_view);
        adapter=new CustomRecyclerViewAdapter(this,R.layout.list_itemview,
                                                    noticeList);
        recyclerView.setAdapter(adapter);
        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent){
        if(Intent.ACTION_SEARCH.equals(intent.getAction())){
            query=intent.getStringExtra(SearchManager.QUERY);
            query = query.replaceAll(" ", "%20");
            search();
        }
    }
    private void search(){
        searchURL=MainActivity.UrlOfNotice+"search/"+noticetype+"/All/All/?q="+query;
        ProgressDialog progressDialog=new ProgressDialog(this);
        progressDialog.setMessage("Loading, please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        HttpGet httpGet=new HttpGet(searchURL);
        Parsing parsing=new Parsing();
        try {
            String result=new ConnectTaskHttpGet(this).execute(httpGet).get();
            noticeList.clear();
            noticeList.addAll(parsing.parseSearchNotices(result));
            adapter.notifyDataSetChanged();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        progressDialog.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        //searchView.setSearchableInfo(searchManager.getSearchableInfo(
        //            new ComponentName(this, SearchResultsActivity.class)));
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconified(false);
        searchView.setSubmitButtonEnabled(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
