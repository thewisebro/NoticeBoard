package adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
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

import com.channeli.noticeboard.Notice;
import com.channeli.noticeboard.Notices;
import com.channeli.noticeboard.R;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import connections.AsynchronousPost;
import objects.NoticeObject;
import objects.NoticeObjectViewHolder;
import okhttp3.Cookie;
import okhttp3.Headers;
import okhttp3.OkHttpClient;

/**
 * Created by Ankush on 23-09-2016.
 */
public class CustomRecyclerViewAdapter extends RecyclerView.Adapter<NoticeObjectViewHolder> {
    private Context mContext;
    private List<NoticeObject> mNoticeList;
    private Set<NoticeObject> mStarredList;
    private Set<Integer> mReadList;
    private SharedPreferences mSharedPreferences;
    private PersistentCookieJar mCookieJar;
    private String mSessid;
    private String mCsrfToken;

    public int viewLayoutId;

    public CustomRecyclerViewAdapter(Context context, int layoutId, List<NoticeObject> list
            , Set<NoticeObject> starredNotices, Set<Integer> readNotices){
        mContext=context;
        viewLayoutId=layoutId;
        mNoticeList=list;
        mStarredList=starredNotices;
        mReadList=readNotices;
        mSharedPreferences= context.getSharedPreferences(Notices.PREFS_NAME,0);
        mCsrfToken=mSharedPreferences.getString("csrftoken","");
        mSessid=mSharedPreferences.getString("CHANNELI_SESSID","");

        //Set up Cookies for networking
        SetCookieCache cookieCache = new SetCookieCache();
        CookiePersistor cookiePersistor = new SharedPrefsCookiePersistor(mContext);
        if (cookiePersistor.loadAll().isEmpty()){
            List<Cookie> cookieList = new ArrayList<Cookie>(2);
            cookieList.add(new Cookie.Builder().name(Notices.CSRF_TOKEN).value(mCsrfToken).domain(Notices.HOST_URL).build());
            cookieList.add(new Cookie.Builder().name(Notices.CHANNELI_SESSID).value(mSessid).domain(Notices.HOST_URL).build());
            cookieCache.addAll(cookieList);
            cookiePersistor.saveAll(cookieList);
        }
        mCookieJar = new PersistentCookieJar(cookieCache,cookiePersistor);

    }
    @Override
    public NoticeObjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(mContext).inflate(viewLayoutId,parent,false);
        return new NoticeObjectViewHolder(view);
    }
    void setRead(final NoticeObject noticeObject, final NoticeObjectViewHolder holder){
        if (noticeObject == null) return;
        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("X-CSRFToken", mCsrfToken);
        new AsynchronousPost() {
            @Override
            public OkHttpClient setClient() {
                return new OkHttpClient.Builder()
                        .cookieJar(mCookieJar)
                        .build();
            }

            @Override
            public void onSuccess(String responseBody, Headers responseHeaders, int responseCode) {
                noticeObject.setRead(true);
                mReadList.add(noticeObject.getId());
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        holder.view.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.read_notice_bg));
                        holder.subject.setTypeface(null, Typeface.NORMAL);
                        holder.datetime.setTypeface(null, Typeface.NORMAL);
                    }
                });
            }

            @Override
            public void onFail(Exception e) {

            }
        }.getResponse(Notices.READ_STAR_NOTICE_URL+noticeObject.getId()+"/add_read/",headers,null);
    }
    void setStar(final NoticeObject noticeObject, final boolean flag, final CompoundButton button){
        if(noticeObject == null) return;
        noticeObject.setStar(flag);
        String url = Notices.READ_STAR_NOTICE_URL + noticeObject.getId();
        if (flag) url += "/add_starred/";
        else url += "/remove_starred/";
        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("X-CSRFToken", mCsrfToken);
        new AsynchronousPost() {
            @Override
            public OkHttpClient setClient() {
                return new OkHttpClient.Builder()
                        .cookieJar(mCookieJar)
                        .build();
            }

            @Override
            public void onSuccess(String responseBody, Headers responseHeaders, int responseCode) {
                if(flag) mStarredList.add(noticeObject);
                else mStarredList.remove(noticeObject);
            }

            @Override
            public void onFail(Exception e) {
            }
        }.getResponse(url,headers,null);
    }


    @Override
    public void onBindViewHolder(final NoticeObjectViewHolder holder, int position) {
        final NoticeObject noticeObject=mNoticeList.get(position);
        holder.subject.setText(noticeObject.getSubject());
        holder.category.setText(noticeObject.getCategory());

        //Set DateTime
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("hh:mm a MMM dd, yy");
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
            holder.view.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.read_background));
            holder.subject.setTypeface(null, Typeface.NORMAL);
            //holder.datetime.setTextColor(context.getResources().getColor(R.color.colorPrimaryDark));
            holder.datetime.setTypeface(null, Typeface.NORMAL);
        }
        else {
            holder.view.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.background));
            holder.subject.setTypeface(null, Typeface.BOLD);
            //holder.datetime.setTextColor(context.getResources().getColor(R.color.colorAccentDark));
            holder.datetime.setTypeface(null,Typeface.BOLD);
        }


        final boolean[] checkChangeFlag = {false};
        holder.star.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                checkChangeFlag[0] = true;
                return false;
            }
        });
        holder.view.findViewById(R.id.star_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkChangeFlag[0]=true;
                holder.star.performClick();
            }
        });
        holder.star.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton compoundButton, final boolean b) {
                if (checkChangeFlag[0]) {
                    checkChangeFlag[0] = false;
                    setStar(noticeObject,b,compoundButton);
                }
            }
        });
        holder.star.setChecked(noticeObject.getStar());

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRead(noticeObject,holder);
                Intent intent = new Intent(mContext, Notice.class);
                intent.putExtra("id",noticeObject.getId());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            }
        });

    }
    @Override
    public int getItemCount() {
        return mNoticeList.size();
    }
    public void showMessage(String msg){
        CoordinatorLayout coordinatorLayout= (CoordinatorLayout) ((Activity)mContext).findViewById(R.id.main_content);
        Snackbar snackbar=Snackbar.make(coordinatorLayout,msg,Snackbar.LENGTH_SHORT);
        TextView tv= (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(mContext.getResources().getColor(R.color.colorPrimary));
        tv.setHeight((int) mContext.getResources().getDimension(R.dimen.bottomBarHeight));
        tv.setTypeface(null, Typeface.BOLD);
        snackbar.show();
    }
}
