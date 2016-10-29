package adapters;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONObject;

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
    boolean setStar(int id,boolean b){
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
            if (result!=null && result!="") {
                JSONObject jsonObject = new JSONObject(result);
                if ("true".equals(jsonObject.getString("success")))
                    return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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

    @Override
    public void onBindViewHolder(final NoticeObjectViewHolder holder, int position) {
        final NoticeObject noticeObject=list.get(position);
        holder.subject.setText(noticeObject.getSubject());
        holder.category.setText(noticeObject.getCategory());

        //Set DateTime
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("hh:mm a dd-MMM-yyyy");
        try {
            Date idate= inputDateFormat.parse(noticeObject.getDatetime_modified());
            String odate= outputDateFormat.format(idate);
            holder.datetime.setText(odate);

        } catch (ParseException e) {
            e.printStackTrace();
            holder.datetime.setText(noticeObject.getDatetime_modified());
        }


        //Highlight read notices
        if(noticeObject.getRead()) {
            holder.view.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.read_background));
            holder.subject.setTypeface(null, Typeface.NORMAL);
        }
        else {
            holder.view.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.background));
            holder.subject.setTypeface(null,Typeface.BOLD);
        }

        final boolean[] checkChangeFlag = {false};
        holder.star.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                checkChangeFlag[0] = true;
                return false;
            }
        });
        holder.star.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton compoundButton, final boolean b) {
                if (checkChangeFlag[0]) {
                    if (isOnline()) {
                        new Thread() {
                            @Override
                            public void run() {
                                if (setStar(noticeObject.getId(), b)) {
                                    noticeObject.setStar(b);
                                    //showMessage("starred");
                                } else {
                                    ((Activity)context).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            showNetworkError();
                                            checkChangeFlag[0]=false;
                                            compoundButton.setChecked(!b);
                                        }
                                    });
                                }
                            }
                        }.start();

                    } else {
                        showNetworkError();
                        compoundButton.setChecked(!b);
                    }
                    checkChangeFlag[0]=false;
                }
            }
        });
        holder.star.setChecked(noticeObject.getStar());

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final ProgressDialog progressDialog= ProgressDialog.show(context,null,"Opening...",true,false);
                new Thread(){
                    @Override
                    public void run(){
                        SQLHelper db = new SQLHelper(context);
                        if (db.checkNoticeContent(noticeObject.getId(),noticeObject.getDatetime_modified())) {
                            NoticeInfo noticeInfo = db.getNoticeInfo(noticeObject.getId(),noticeObject.getDatetime_modified());
                            Intent intent = new Intent(context, Notice.class);
                            intent.putExtra("noticeinfo", noticeInfo.getContent());
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                            progressDialog.dismiss();
                            return;
                        }
                        if (isOnline()) {
                            String result = getNoticeInfoResult(noticeObject.getId());
                            if (!result.equals("")) {
                                if (!noticeObject.getRead()) {
                                    ((Activity)context).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            holder.view.setBackgroundDrawable(
                                                    context.getResources().getDrawable(R.drawable.read_notice_bg));
                                        }
                                    });
                                    noticeObject.setRead(true);
                                    setRead(noticeObject.getId());
                                }
                                NoticeInfo noticeInfo = parsing.parseNoticeInfo(result);
                                db.addNoticeInfo(noticeInfo);
                                Intent intent = new Intent(context, Notice.class);
                                intent.putExtra("noticeinfo", noticeInfo.getContent());
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                                progressDialog.dismiss();
                                return;
                            }
                        }
                        if (db.checkNoticeContent(noticeObject.getId())) {
                            NoticeInfo noticeInfo = db.getNoticeInfo(noticeObject.getId());
                            Intent intent = new Intent(context, Notice.class);
                            intent.putExtra("noticeinfo", noticeInfo.getContent());
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        } else {
                            showNetworkError();
                        }
                        db.close();
                        progressDialog.dismiss();
                    }
                }.start();
            }

        });

    }
    @Override
    public int getItemCount() {
        return list.size();
    }
    public void showNetworkError(){
        showMessage("Check Network Connection!");
    }
    public void showMessage(String msg){
        CoordinatorLayout coordinatorLayout= (CoordinatorLayout) ((Activity)context).findViewById(R.id.main_content);
        Snackbar snackbar=Snackbar.make(coordinatorLayout,msg,Snackbar.LENGTH_SHORT);
        TextView tv= (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(context.getResources().getColor(R.color.colorPrimary));
        tv.setHeight((int) context.getResources().getDimension(R.dimen.bottomBarHeight));
        tv.setTypeface(null, Typeface.BOLD);
        snackbar.show();
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
