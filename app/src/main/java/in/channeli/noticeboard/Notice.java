package in.channeli.noticeboard;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.webkit.WebView;

import java.util.ArrayList;

public class Notice extends AppCompatActivity {
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notice);
        Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Intent intent = getIntent();
        String result = intent.getStringExtra("noticeinfo");
        StringBuffer stringBuffer;

        if(result.contains("<img")  || result.contains("href")) {
            ArrayList<Integer> count = new ArrayList<>();
            stringBuffer = new StringBuffer(result);
            String add = "https://channeli.in";

            for(int index = result.indexOf("/media");
                    index >= 0;
                    index = result.indexOf("/media",index + 1)) {
                count.add(index);

            }
            for(int index = result.indexOf("/notices/userfiles");
                index >= 0;
                index = result.indexOf("/notices",index + 1)) {
                count.add(index);
            }

            int prev = 0;
            for(int i=0; i< count.size(); i++){
                stringBuffer = stringBuffer.insert(prev + count.get(i), add);
                prev = (i+1)*add.length();
            }
            result = stringBuffer.toString();
        }
        if(result.contains("</a>") && result.contains("<img")){
            stringBuffer = new StringBuffer(result);
            String add = "Download Attachment";
            int index = result.indexOf("<a href");
            while(result.charAt(index) != '>'){
                index++;
            }
            int startIndex = index+1;

            int endIndex = result.indexOf("</a>");
            //String toBeReplaced = result.substring(startIndex,endIndex);
            stringBuffer.replace(startIndex,endIndex, add);
            result = stringBuffer.toString();
        }
        //Log.e("notice",result);
        WebView webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(true);
        webView.getSettings().setSupportZoom(true);
        webView.loadData(result, "text/html", "utf-8");

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
