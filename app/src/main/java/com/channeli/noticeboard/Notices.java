package com.channeli.noticeboard;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.util.TimeUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.GlideContext;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.squareup.leakcanary.LeakCanary;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import connections.AsynchronousGet;
import objects.DrawerItem;
import objects.User;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import utilities.Parsing;
import utilities.RoundImageView;
import utilities.SQLHelper;

public class Notices extends AppCompatActivity {


    public static final String UrlOfHost="http://people.iitr.ernet.in/";
    public static final String UrlOfNotice = UrlOfHost+"notices/";
    public static final String UrlOfLogin = UrlOfHost+"login/";
    public static final String UrlOfPeopleSearch = UrlOfHost+"peoplesearch/";
    public static final String UrlOfFCMRegistration = UrlOfHost+"push_subscription_sync/";
    public static final String UrlOfPhoto = UrlOfHost+"photo/";
    public static final String PREFS_NAME = "MyPrefsFile";
    public static String NoticeType = "new", MainCategory = "All", Category="All";

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private User mUser;
    private CookieJar mCookieJar;
    private SQLHelper sqlHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(getApplication());
        setContentView(R.layout.main);

        //Initialize instance variables
        sqlHelper = new SQLHelper(getApplicationContext());
        mSharedPreferences = getSharedPreferences(Notices.PREFS_NAME,0);
        mEditor = mSharedPreferences.edit();
        mUser = new User(mSharedPreferences.getString("username","14115019"),
                mSharedPreferences.getString("csrftoken",""), mSharedPreferences.getString("CHANNELI_SESSID","")
        );
        //Set up Cookies for networking
        SetCookieCache cookieCache = new SetCookieCache();
        CookiePersistor cookiePersistor = new SharedPrefsCookiePersistor(getApplication());
        if (cookiePersistor.loadAll().isEmpty()){
            List<Cookie> cookieList = new ArrayList<Cookie>(2);
            cookieList.add(new Cookie.Builder().name("csrftoken").value(mUser.getCsrfToken()).domain(Notices.UrlOfHost).build());
            cookieList.add(new Cookie.Builder().name("CHANNELI_SESSID").value(mUser.getChanneliSessid()).domain(Notices.UrlOfHost).build());
            cookieCache.addAll(cookieList);
            cookiePersistor.saveAll(cookieList);
        }
        mCookieJar = new PersistentCookieJar(cookieCache,cookiePersistor);

        //TODO: Set up views
        setDrawerProfile();
        setDrawerList();
        setListView();

        //TODO: GET user details, constants, notices
        fetchUserDetails();
        fetchConstants();
        fetchNotices();
    }

    private void fetchNotices(){

    }
    private void fetchConstants(){
        new AsynchronousGet() {
            @Override
            public OkHttpClient setClient() {
                return new OkHttpClient.Builder()
                        .cookieJar(mCookieJar)
                        .build();
            }

            @Override
            public void onSuccess(String responseBody, Headers responseHeaders, int responseCode) {
                ArrayList<DrawerItem> list=new ArrayList<>();
                list.addAll(new Parsing().parseConstants(responseBody));
                if(list.size()>0){
                    Set<String> constantSet=new HashSet<>(list.size());
                    for(int i=0;i<list.size();i++) {
                        Set<String> s=new HashSet<>(list.get(i).getCategories());
                        mEditor.putStringSet(list.get(i).getName(),s);
                        constantSet.add(list.get(i).getName());
                    }
                    mEditor.putStringSet("constants", constantSet);
                    mEditor.apply();
                }
            }

            @Override
            public void onFail(Exception e) {

            }
        }.getResponse(Notices.UrlOfNotice + "get_constants/",null,null);
    }
    private void fetchUserDetails(){
        new AsynchronousGet() {
            @Override
            public void onSuccess(String responseBody, Headers responseHeaders, int responseCode) {
                try {
                    JSONObject response = new JSONObject(responseBody);
                    if (response.getString("msg")!="YES") onFail(new JSONException(""));

                    mUser.setName(response.getString("_name"));
                    mUser.setInfo(response.getString("info"));
                    mUser.setEnrollmentNo(response.getString("enrollment_no"));

                    mEditor.putString("name", mUser.getName());
                    mEditor.putString("info", mUser.getInfo());
                    mEditor.putString("enrollment_no", mUser.getEnrollmentNo());
                    mEditor.apply();

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) findViewById(R.id.name)).setText(mUser.getName());
                            ((TextView) findViewById(R.id.info)).setText(mUser.getInfo());
                            fetchUserPhoto();
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    onFail(new JSONException(""));
                } catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(Exception e) {

            }

            @Override
            public OkHttpClient setClient() {
                return new OkHttpClient.Builder()
                        .cookieJar(mCookieJar)
                        .build();
            }
        }.getResponse(Notices.UrlOfPeopleSearch + "return_details/?username=" + mUser.getUsername(), null, null);
    }
    private void fetchUserPhoto(){
        RoundImageView imageView = (RoundImageView) findViewById(R.id.profile_picture);
        Glide.with(Notices.this).load(UrlOfPhoto+mUser.getEnrollmentNo()).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                return false;
            }
            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                //Convert drawable to bitmap
                Bitmap bitmap = null;

                if (resource instanceof BitmapDrawable) {
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) resource;
                    if(bitmapDrawable.getBitmap() != null) {
                        sqlHelper.addProfilePic(bitmapDrawable.getBitmap());
                        return false;
                    }
                }

                if(resource.getIntrinsicWidth() <= 0 || resource.getIntrinsicHeight() <= 0) {
                    bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
                } else {
                    bitmap = Bitmap.createBitmap(resource.getIntrinsicWidth(), resource.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                }

                Canvas canvas = new Canvas(bitmap);
                resource.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                resource.draw(canvas);
                sqlHelper.addProfilePic(bitmap);

                return false;
            }
        }).into(imageView);
//        .with(Notices.this).placeholder(R.drawable.ic_person_black_24dp).fitCenter().into(imageView);
    }

    private void setListView(){

    }
    private void setDrawerList(){

    }
    private void setDrawerProfile(){

    }

    private void populateListView(){

    }
    private void populateDrawerList(){

    }
    private void populateDrawerProfile(){

    }
}

