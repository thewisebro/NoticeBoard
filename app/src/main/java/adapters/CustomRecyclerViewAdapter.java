package adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import connections.ConnectTaskHttpGet;
import connections.ConnectTaskHttpPost;
import in.channeli.noticeboard.MainActivity;
import in.channeli.noticeboard.Notice;
import in.channeli.noticeboard.R;
import objects.NoticeInfo;
import objects.NoticeObject;
import objects.NoticeObjectViewHolder;
import utilities.Parsing;
import utilities.SQLHelper;

/**
 * Created by Ankush on 23-09-2016.
 */
public class CustomRecyclerViewAdapter extends RecyclerView.Adapter<NoticeObjectViewHolder> {
    private Context context;
    private int viewLayoutId;
    private ArrayList<NoticeObject> list;
    final String noticeurl = MainActivity.UrlOfNotice+"get_notice/";
    String csrftoken;
    String CHANNELI_SESSID;
    Parsing parsing;

    public CustomRecyclerViewAdapter(Context context,int viewLayoutId, ArrayList<NoticeObject> list){
        this.context=context;
        this.viewLayoutId=viewLayoutId;
        this.list=list;
        SharedPreferences settings= context.getSharedPreferences(MainActivity.PREFS_NAME,0);
        csrftoken=settings.getString("csrftoken","");
        CHANNELI_SESSID=settings.getString("CHANNELI_SESSID","");
        parsing=new Parsing();
    }
    @Override
    public NoticeObjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(viewLayoutId,parent,false);
        return new NoticeObjectViewHolder(view);
    }
    void setRead(int id){
        HttpPost post=new HttpPost(MainActivity.UrlOfNotice+"read_star_notice/"+
                id+"/add_read/");
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
    void setStar(int id,boolean b){
        String uri = MainActivity.UrlOfNotice + "read_star_notice/" + id;
        if (b)
            uri += "/add_starred/";
        else
            uri += "/remove_starred/";
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        httpPost.setHeader("Cookie", "csrftoken=" + csrftoken);
        httpPost.setHeader("Cookie", "CHANNELI_SESSID=" + CHANNELI_SESSID);
        httpPost.setHeader("CHANNELI_DEVICE", "android");
        httpPost.setHeader("X-CSRFToken=", csrftoken);
        ConnectTaskHttpPost starTask = (ConnectTaskHttpPost) new ConnectTaskHttpPost().execute(httpPost);
        String result = "";
        try {
            result = starTask.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    String getNoticeInfoResult(int id){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(noticeurl + id);
        String url = stringBuilder.toString();
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Cookie", "csrftoken=" + csrftoken);
        httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
        httpGet.setHeader("Cookie", "CHANNELI_SESSID=" + CHANNELI_SESSID);
        httpGet.setHeader("X-CSRFToken", csrftoken);
        String result = "";
        try {
            result = new ConnectTaskHttpGet().execute(httpGet).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    void stActivity(NoticeInfo noticeInfo){
        Intent intent = new Intent(context, Notice.class);
        intent.putExtra("noticeinfo", noticeInfo.getContent());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void onBindViewHolder(final NoticeObjectViewHolder holder, int position) {
        final NoticeObject noticeObject=list.get(position);
        holder.subject.setText(noticeObject.getSubject());
        holder.category.setText(noticeObject.getMain_category());

        //Set DateTime
        String[] date_time = noticeObject.getDatetime_modified().split("T");
        String date = date_time[0];
        String time = date_time[1];
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            String currentdate = simpleDateFormat.format(new Date());
            Date current = simpleDateFormat.parse(currentdate);
            Date strDate = simpleDateFormat.parse(date);

            if(strDate.equals(current)) {
                holder.datetime.setText(time);
            }
            else {
                date = new SimpleDateFormat("dd-MMM-yyyy").format(strDate);
                holder.datetime.setText(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            holder.datetime.setText(noticeObject.getDatetime_modified());
        }

        //Highlight read notices
        if(noticeObject.getRead())
            holder.view.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.read_notice_bg));
        else
            holder.view.setBackgroundDrawable(null);

        holder.star.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    setStar(noticeObject.getId(), b);
                    noticeObject.setStar(b);
            }
        });
        holder.star.setChecked(noticeObject.getStar());

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SQLHelper db = new SQLHelper(context);
                if (isOnline()) {
                    String result = getNoticeInfoResult(noticeObject.getId());
                    if (!result.equals("")) {
                        if (!noticeObject.getRead()) {
                            holder.view.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.read_notice_bg));
                            noticeObject.setRead(true);
                            setRead(noticeObject.getId());
                        }
                        NoticeInfo noticeInfo = parsing.parseNoticeInfo(result);
                        db.addNoticeInfo(noticeInfo);
                        stActivity(noticeInfo);
                    }
                } else if (db.checkNoticeContent(noticeObject.getId())) {
                    //NoticeInfo noticeInfo = db.getNoticeInfo(noticeObject.getId());
                    stActivity(db.getNoticeInfo(noticeObject.getId()));
                } else {
                    Toast toast = Toast.makeText(context,
                            "Cannot Connect to Internet", Toast.LENGTH_SHORT);
                    toast.show();
                }

                db.close();
            }

        });

    }
    @Override
    public int getItemCount() {
        return list.size();
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
