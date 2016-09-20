package adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

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
import utilities.Parsing;
import utilities.SQLHelper;

public class CustomFragmentAdapter extends ArrayAdapter<NoticeObject> {
    private Context context;
    private ArrayList<NoticeObject> noticeArrayList;
    private int layout;
    final String noticeurl = MainActivity.UrlOfNotice+"get_notice/";
    String csrftoken;
    String CHANNELI_SESSID;
    Parsing parsing;

    public CustomFragmentAdapter(Context context, int layout,ArrayList<NoticeObject> list){
        super(context, layout);
        this.context = context;
        this.layout = layout;
        SharedPreferences settings= getContext().getSharedPreferences(MainActivity.PREFS_NAME,0);
        csrftoken=settings.getString("csrftoken","");
        CHANNELI_SESSID=settings.getString("CHANNELI_SESSID","");
        parsing=new Parsing();
        noticeArrayList=list;
    }
    public void  addList(ArrayList<NoticeObject> list){
        this.addAll(list);
        this.notifyDataSetChanged();
        SQLHelper db=new SQLHelper(context);
        try {
            db.addNoticesList(list);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        db.close();
    }
    @Override
    public int getCount(){
        return noticeArrayList.size();
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

    @Override
    public NoticeObject getItem(int position){ return this.noticeArrayList.get(position);}

    public View getView(int position, View ConvertView, ViewGroup parent){
        View row=ConvertView;
        LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        row = inflater.inflate(layout, parent, false);
        final NoticeObject notice=getItem(position);
        TextView category = (TextView) row.findViewById(R.id.category);
        category.setText(notice.getCategory());
        TextView subject = (TextView) row.findViewById(R.id.subject);
        subject.setText(notice.getSubject());
        TextView datetime = (TextView) row.findViewById(R.id.datetime);
        String[] date_time = notice.getDatetime_modified().split("T");
        String date = date_time[0];
        String time = date_time[1];
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            String currentdate = simpleDateFormat.format(new Date());
            Date current = simpleDateFormat.parse(currentdate);
            Date strDate = simpleDateFormat.parse(date);

            if(strDate.equals(current)) {
                datetime.setText(time);
            }
            else {
                date = new SimpleDateFormat("dd-MMM-yyyy").format(strDate);
                datetime.setText(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (notice.getRead()){
            row.setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.read_notice_bg));
        }
        final ToggleButton star= (ToggleButton) row.findViewById(R.id.star_button);
        star.setChecked(notice.getStar());
        star.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                String uri = MainActivity.UrlOfNotice + "read_star_notice/" + notice.getId();
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
        });
        final View finalRow = row;
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(noticeurl + notice.getId());
                String url = stringBuilder.toString();
                HttpGet httpGet= new HttpGet(url);
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
                if (!result.equals("")) {
                    if (!notice.getRead()) {
                        finalRow.setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.read_notice_bg));
                        notice.setRead(true);
                        setRead(notice.getId());
                    }
                    NoticeInfo noticeInfo = parsing.parseNoticeInfo(result);
                    Intent intent = new Intent(getContext(), Notice.class);
                    intent.putExtra("noticeinfo", noticeInfo.getContent());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } else {
                    Toast toast = Toast.makeText(getContext(),
                            "Cannot connect to internet", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }

        });

        return row;
    }
}