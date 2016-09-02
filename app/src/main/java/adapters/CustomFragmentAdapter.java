package adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.apache.http.client.methods.HttpPost;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import connections.ConnectTaskHttpPost;
import in.channeli.noticeboard.MainActivity;
import in.channeli.noticeboard.R;
import objects.NoticeObject;

/*
 Created by manohar on 30/8/15.
 */
public class CustomFragmentAdapter extends ArrayAdapter<NoticeObject> {
    private Context context;
    private ArrayList<NoticeObject> noticeArrayList;
    private int layout;

    public CustomFragmentAdapter(Context context, int layout, ArrayList<NoticeObject> noticeArrayList){
        super(context, layout, noticeArrayList);
        this.context = context;
        this.noticeArrayList = noticeArrayList;
        this.layout = layout;
    }

    public int getCount(){
        return noticeArrayList.size();
    }

    public View getView(int position, View ConvertView, ViewGroup parent){

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View searchlist_view = inflater.inflate(layout, null);
        final NoticeObject notice=noticeArrayList.get(position);
        TextView category = (TextView) searchlist_view.findViewById(R.id.recycler_list_category);
        category.setText(notice.getCategory());
        TextView subject = (TextView) searchlist_view.findViewById(R.id.recycler_list_subject);
        subject.setText(notice.getSubject());
        TextView datetime = (TextView) searchlist_view.findViewById(R.id.recycler_list_datetime);
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

        final ToggleButton star= (ToggleButton) searchlist_view.findViewById(R.id.star_button);
        star.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                String uri= MainActivity.UrlOfNotice+"read_star_notice/" +notice.getId();
                if(b)
                    uri+="/add_starred/";
                else
                    uri+="/remove_starred/";
                HttpPost httpPost=new HttpPost(uri);
                SharedPreferences settings = getContext().getSharedPreferences(MainActivity.PREFS_NAME, 0);
                httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpPost.setHeader("Cookie","csrftoken="+settings.getString("csrftoken",""));
                httpPost.setHeader("Cookie","CHANNELI_SESSID="+settings.getString("CHANNELI_SESSID",""));
                httpPost.setHeader("X-CSRFToken=",settings.getString("csrftoken",""));
                ConnectTaskHttpPost starTask= (ConnectTaskHttpPost) new ConnectTaskHttpPost().execute(httpPost);
                String result="";
                try {
                    result=starTask.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return searchlist_view;
    }
}