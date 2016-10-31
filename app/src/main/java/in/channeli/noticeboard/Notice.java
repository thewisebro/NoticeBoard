package in.channeli.noticeboard;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import objects.NoticeInfo;

public class Notice extends AppCompatActivity {
    NoticeInfo noticeInfo;
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notice);
        Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Intent intent = getIntent();
        noticeInfo= (NoticeInfo) intent.getSerializableExtra("noticeinfo");
        final View view=findViewById(R.id.notice);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setHeaderViews();
                setContent();
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }
    public void setHeaderViews(){
        TextView subject= (TextView) findViewById(R.id.notice_subject);
        TextView category= (TextView) findViewById(R.id.notice_category);
        TextView date= (TextView) findViewById(R.id.notice_date);
        subject.setText(noticeInfo.getSubject());
        category.setText(noticeInfo.getCategory());
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("hh:mm a dd-MMM-yyyy");
        try {
            Date idate= inputDateFormat.parse(noticeInfo.getDatetime_modified());
            String odate= outputDateFormat.format(idate);
            date.setText(odate);

        } catch (ParseException e) {
            e.printStackTrace();
            date.setText(noticeInfo.getDatetime_modified());
        }

    }
    public void setContent(){
        String result=noticeInfo.getContent();
        //String result = intent.getStringExtra("noticeinfo");
        StringBuffer stringBuffer;

        if(result.contains("<img")  || result.contains("href")) {
            ArrayList<Integer> count = new ArrayList<>();
            stringBuffer = new StringBuffer(result);
            //String add = "https://channeli.in";
            String add= "http://people.iitr.ernet.in";

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
            int startIndex=index;
            while(result.charAt(startIndex) != '>'){
                startIndex++;
            }

            startIndex++;

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
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setSupportZoom(true);
        webView.loadData(result, "text/html", "utf-8");
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
