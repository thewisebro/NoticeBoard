package utilities;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import objects.NoticeInfo;
import objects.NoticeObject;
import objects.noticeNotification;

/**
 * Created by Ankush on 27-08-2016.
 */
public class SQLHelper extends SQLiteOpenHelper{
    private static String NAME="NOTICE_BOARD";
    private static int VERSION=1;
    private static int MAX_NOTICES=30;
    private static String TABLE_NOTICES="NOTICES";
    private static String TABLE_PROFILE_PIC="PROFILE_PIC";
    private static String TABLE_NOTIFICATIONS="NOTIFICATIONS";
    private static String ROW_BITMAP_STRING="BITMAP_STRING";
    private static String ROW_BITMAP_ID="BITMAP_ID";
    private static String ROW_ID="ID";
    private static String ROW_SUBJECT="SUBJECT";
    private static String ROW_EXPIRY_DATE="EXP_DATE";
    private static String ROW_REFERENCE="REFERENCE";
    private static String ROW_CONTENT="CONTENT";
    private static String ROW_DATETIME="DATETIME";
    private static String ROW_UPLOADER="UPLOADER";
    private static String ROW_READ_STATUS="READ_STATUS";
    private static String ROW_STAR_STATUS="STAR_STATUS";
    private static String ROW_CATEGORY="CATEGORY";
    private static String ROW_MAIN_CATEGORY="MAIN_CATEGORY";
    private static String CREATE_TABLE_NOTICES=
            "CREATE TABLE IF NOT EXISTS "+TABLE_NOTICES+" ( "+
                    ROW_ID+" INT,"+
                    ROW_SUBJECT+" VARCHAR(100),"+
                    ROW_CATEGORY+" VARCHAR(20),"+
                    ROW_MAIN_CATEGORY+" VARCHAR(20),"+
                    ROW_REFERENCE+" VARCHAR(100),"+
                    ROW_EXPIRY_DATE+" DATETIME,"+
                    ROW_CONTENT+" TEXT,"+
                    ROW_UPLOADER+" VARCHAR(100),"+
                    ROW_DATETIME+" DATETIME,"+
                    ROW_READ_STATUS+" BOOLEAN,"+
                    ROW_STAR_STATUS+" BOOLEAN, "+
                    "PRIMARY KEY("+ROW_ID+") ON CONFLICT REPLACE "+
                    ");";
    private static String CREATE_TABLE_PROFILE_PIC=
            "CREATE TABLE IF NOT EXISTS "+TABLE_PROFILE_PIC+" ( "+
                    ROW_BITMAP_ID + " INT,"+
                    ROW_BITMAP_STRING + " TEXT, "+
                    "PRIMARY KEY("+ROW_BITMAP_ID+") ON CONFLICT REPLACE "+
                    ");";
    private static String CREATE_TABLE_NOTIFICATIONS=
            "CREATE TABLE IF NOT EXISTS "+TABLE_NOTIFICATIONS+" ( "+
                    ROW_MAIN_CATEGORY+" VARCHAR(20),"+
                    ROW_CATEGORY+" VARCHAR(20),"+
                    ROW_SUBJECT+" VARCHAR(100)"+
                    " );";
    private static String DELETE_TABLE_NOTICES=
            "DROP TABLE OF EXISTS "+TABLE_NOTICES;
    public SQLHelper(Context context) {
        super(context, NAME, null, VERSION);
        SQLiteDatabase db=this.getWritableDatabase();
        //db.execSQL(DELETE_TABLE_NOTICES);
        db.execSQL(CREATE_TABLE_NOTICES);
        db.execSQL(CREATE_TABLE_PROFILE_PIC);
        db.execSQL(CREATE_TABLE_NOTIFICATIONS);
        //db.close();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }
    public void addProfilePic(Bitmap bitmap){
        if (bitmap==null)
            return;
        SQLiteDatabase db= this.getWritableDatabase();
        ByteArrayOutputStream stream= new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        String bitmapString64= Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);
        ContentValues values=new ContentValues();
        values.put(ROW_BITMAP_ID,1);
        values.put(ROW_BITMAP_STRING,bitmapString64);
        db.insert(TABLE_PROFILE_PIC, null, values);
        //db.close();
    }
    public Bitmap getProfilePic(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor=db.query(TABLE_PROFILE_PIC, null, null, null, null, null, null);
        if (cursor.getCount()==0)
            return null;
        cursor.moveToFirst();
        String bitmapString64=cursor.getString(cursor.getColumnIndex(ROW_BITMAP_STRING));
        if(bitmapString64==null || bitmapString64=="")
            return null;
        byte[] bitmapByte= Base64.decode(bitmapString64, Base64.DEFAULT);
        if (bitmapByte.length==0)
            return null;
        Bitmap bitmap= BitmapFactory.decodeByteArray(bitmapByte, 0, bitmapByte.length);
        return bitmap;
    }
    public int countNotices(){
        SQLiteDatabase db=this.getReadableDatabase();
        Cursor cursor=db.query(TABLE_NOTICES, null, null, null, null, null, null);
        return cursor.getCount();
    }
    private void deleteLastNotice(){
        SQLiteDatabase db=this.getWritableDatabase();
        db.delete(TABLE_NOTICES,ROW_DATETIME + "= MIN("+ ROW_DATETIME + ")",null);
        //db.close();
    }
    public void clear(){
        SQLiteDatabase db=this.getWritableDatabase();
        db.delete(TABLE_NOTICES,null,null);
        db.delete(TABLE_PROFILE_PIC,null,null);
        db.delete(TABLE_NOTIFICATIONS,null,null);
        //db.close();
    }
    public ArrayList<NoticeObject> getNotices(){
        SQLiteDatabase db=this.getReadableDatabase();
        ArrayList<NoticeObject> list=new ArrayList<NoticeObject>();
        Cursor cursor=db.query(TABLE_NOTICES,new String[]{ROW_ID,ROW_SUBJECT,ROW_DATETIME,ROW_CATEGORY
                ,ROW_MAIN_CATEGORY,ROW_READ_STATUS,ROW_STAR_STATUS},null,null,null,null,null);
        if(cursor.moveToFirst()){
            do{
                NoticeObject object=new NoticeObject();
                object.setId(cursor.getInt(0));
                object.setSubject(cursor.getString(1));
                object.setDatetime_modified(cursor.getString(2));
                object.setCategory(cursor.getString(3));
                object.setMain_category(cursor.getString(4));
                object.setRead(cursor.getInt(5) > 0);
                object.setStar(cursor.getInt(6) > 0);
                list.add(object);
            }while (cursor.moveToNext());
        }
        //db.close();
        return list;
    }
    public ArrayList<NoticeObject> getNotices(String mainCategory, String category){
        SQLiteDatabase db=this.getReadableDatabase();
        ArrayList<NoticeObject> list=new ArrayList<NoticeObject>();
        String CONDITION=null;
        category = category.replaceAll("%20"," ");
        mainCategory= mainCategory.replaceAll("%20"," ");
        if (mainCategory.matches("Starred"))
            CONDITION=ROW_STAR_STATUS+"= 1";
        else if (!mainCategory.matches("All"))
            CONDITION=ROW_MAIN_CATEGORY+" = '"+mainCategory+"' AND "+ROW_CATEGORY+" = '"+category+"'";

        Cursor cursor=db.query(TABLE_NOTICES,new String[]{ROW_ID,ROW_SUBJECT,ROW_DATETIME,ROW_CATEGORY
                ,ROW_MAIN_CATEGORY,ROW_READ_STATUS,ROW_STAR_STATUS}, CONDITION,null,null,null,ROW_DATETIME + " DESC");
        if(cursor.moveToFirst()){
            do{
                NoticeObject object=new NoticeObject();
                object.setId(cursor.getInt(0));
                object.setSubject(cursor.getString(1));
                object.setDatetime_modified(cursor.getString(2));
                object.setCategory(cursor.getString(3));
                object.setMain_category(cursor.getString(4));
                object.setRead(cursor.getInt(5) > 0);
                object.setStar(cursor.getInt(6) > 0);
                list.add(object);
            }while (cursor.moveToNext());
        }
        //db.close();
        return list;
    }

    public ArrayList<NoticeObject> getNotices(String category){
        SQLiteDatabase db=this.getReadableDatabase();
        ArrayList<NoticeObject> list=new ArrayList<NoticeObject>();
        String CONDITION=null;
        category = category.replaceAll("%20"," ");
        if (category.matches("Starred"))
            CONDITION=ROW_STAR_STATUS+"= 1";
        else if (!category.matches("All"))
            CONDITION=ROW_MAIN_CATEGORY+" = '"+category+"'";

        Cursor cursor=db.query(TABLE_NOTICES,new String[]{ROW_ID,ROW_SUBJECT,ROW_DATETIME,ROW_CATEGORY
                ,ROW_MAIN_CATEGORY,ROW_READ_STATUS,ROW_STAR_STATUS}, CONDITION,null,null,null,ROW_DATETIME + " DESC");
        if(cursor.moveToFirst()){
            do{
                NoticeObject object=new NoticeObject();
                object.setId(cursor.getInt(0));
                object.setSubject(cursor.getString(1));
                object.setDatetime_modified(cursor.getString(2));
                object.setCategory(cursor.getString(3));
                object.setMain_category(cursor.getString(4));
                object.setRead(cursor.getInt(5) > 0);
                object.setStar(cursor.getInt(6) > 0);
                int a=cursor.getInt(6);
                list.add(object);
            }while (cursor.moveToNext());
        }
        //db.close();
        return list;
    }
    public ArrayList<Integer> getReadNotices(){
        SQLiteDatabase db=this.getReadableDatabase();
        ArrayList<Integer> list=new ArrayList<Integer>();

        Cursor cursor=db.query(TABLE_NOTICES,new String[]{ROW_ID}, ROW_READ_STATUS+" = 1 ",null,null,null,ROW_DATETIME + " DESC");
        if(cursor.moveToFirst()){
            do{
                list.add(new Integer(cursor.getInt(0)));
            }while (cursor.moveToNext());
        }
        //db.close();
        return list;
    }
    public NoticeInfo getNoticeInfo(int id){
        if (!checkNoticeContent(id))
            return null;
        SQLiteDatabase db=this.getReadableDatabase();
        Cursor cursor=db.query(TABLE_NOTICES,new String[]{ROW_ID,ROW_SUBJECT,ROW_DATETIME,ROW_CATEGORY,ROW_REFERENCE,ROW_CONTENT}
                ,ROW_ID + "=" + id,null,null,null,null);
        if(cursor.moveToFirst()){
            NoticeInfo info=new NoticeInfo();
            info.setId(cursor.getInt(0));
            info.setSubject(cursor.getString(1));
            info.setDatetime_modified(cursor.getString(2));
            info.setCategory(cursor.getString(3));
            info.setReference(cursor.getString(4));
            info.setContent(cursor.getString(5));
            return info;
        }
        return null;
    }
    public NoticeInfo getNoticeInfo(int id, String date){
        if (!checkNoticeContent(id))
            return null;
        SQLiteDatabase db=this.getReadableDatabase();
        Cursor cursor=db.query(TABLE_NOTICES,new String[]{ROW_ID,ROW_SUBJECT,ROW_DATETIME,ROW_CATEGORY,ROW_REFERENCE,ROW_CONTENT}
                ,ROW_ID + "=" + id + " AND "+ROW_DATETIME+" = '"+ date +"'" ,null,null,null,null);
        if(cursor.moveToFirst()){
            NoticeInfo info=new NoticeInfo();
            info.setId(cursor.getInt(0));
            info.setSubject(cursor.getString(1));
            info.setDatetime_modified(cursor.getString(2));
            info.setCategory(cursor.getString(3));
            info.setReference(cursor.getString(4));
            info.setContent(cursor.getString(5));
            return info;
        }
        return null;
    }
    private void limit(){
        SQLiteDatabase db=this.getWritableDatabase();
        String query="DELETE FROM " + TABLE_NOTICES +
                " WHERE "+ROW_ID+" NOT IN ( "+
                    " SELECT "+ROW_ID+" FROM "+TABLE_NOTICES+
                    " ORDER BY "+ROW_DATETIME+" DESC "+
                    " LIMIT "+MAX_NOTICES+
                " );";
        db.execSQL(query);
        //db.close();
    }
    private void addNotice(NoticeObject object) throws ParseException {
        //if (checkNotice(object.getId()))
        //    return;
        SQLiteDatabase db=this.getWritableDatabase();
        ContentValues values=new ContentValues();
        values.put(ROW_ID,object.getId());
        values.put(ROW_SUBJECT,object.getSubject());
        values.put(ROW_CATEGORY,object.getCategory());
        values.put(ROW_MAIN_CATEGORY,object.getMain_category());
        values.put(ROW_DATETIME, object.getDatetime_modified());
        values.put(ROW_READ_STATUS,object.getRead());
        values.put(ROW_STAR_STATUS,object.getStar());
        db.insert(TABLE_NOTICES, null, values);
        //db.close();
    }
    public void addNoticesList(ArrayList<NoticeObject> list) throws ParseException {
        for(NoticeObject object: list)
            addNotice(object);
        limit();
    }
    private boolean checkNotice(int id){
        SQLiteDatabase db=this.getReadableDatabase();
        Cursor cursor=db.query(TABLE_NOTICES, null, ROW_ID + "=" + id, null, null, null, null);
        int count=cursor.getCount();
        //db.close();
        return (!(count==0));
    }
    public boolean checkNoticeContent(int id){
        SQLiteDatabase db=this.getReadableDatabase();
        Cursor cursor=db.query(TABLE_NOTICES, new String[]{ROW_CONTENT}, ROW_ID + "=" + id, null, null, null, null);
        if(cursor.getCount()==0)
            return false;
        cursor.moveToFirst();
        String c=cursor.getString(0);
        //db.close();
        return (!(c==null || c==""));
    }
    public boolean checkNoticeContent(int id,String date){
        SQLiteDatabase db=this.getReadableDatabase();
        Cursor cursor=db.query(TABLE_NOTICES, new String[]{ROW_CONTENT}
                , ROW_ID + "=" + id + " AND "+ROW_DATETIME+" = '"+ date +"'", null, null, null, null);
        if(cursor.getCount()==0)
            return false;
        cursor.moveToFirst();
        String c=cursor.getString(0);
        //db.close();
        return (!(c==null || c==""));
    }
    public void addNoticeInfo(NoticeInfo info){
        SQLiteDatabase db=this.getWritableDatabase();
        ContentValues values=new ContentValues();
        values.put(ROW_CONTENT,info.getContent());
        db.update(TABLE_NOTICES, values, ROW_ID + "=" + info.getId(), null);
        //db.close();
    }
    public void setStar(int id, boolean b){
        SQLiteDatabase db= this.getWritableDatabase();
        ContentValues values=new ContentValues();
        values.put(ROW_STAR_STATUS,b);
        db.update(TABLE_NOTICES, values, ROW_ID + " = " + id, null);
        //db.close();
    }
    public void setRead(int id){
        SQLiteDatabase db= this.getWritableDatabase();
        ContentValues values=new ContentValues();
        values.put(ROW_READ_STATUS,true);
        db.update(TABLE_NOTICES, values, ROW_ID + " = " + id, null);
        //db.close();
    }
    public List<noticeNotification> getNotifications(){
        List<noticeNotification> notifications=new ArrayList<>();
        SQLiteDatabase db=this.getReadableDatabase();
        Cursor cursor=db.query(false,TABLE_NOTIFICATIONS,new String[]{ROW_MAIN_CATEGORY,ROW_CATEGORY,ROW_SUBJECT},null,null,null,null,null,null);
        if (cursor.moveToFirst()){
            do{
                noticeNotification notification=
                        new noticeNotification(cursor.getString(0),cursor.getString(1),cursor.getString(2));
                notifications.add(notification);
            }while(cursor.moveToNext());
        }
        //db.close();
        return notifications;
    }
    public void clearNotifications(){
        SQLiteDatabase db=this.getWritableDatabase();
        db.delete(TABLE_NOTIFICATIONS,null,null);
        //db.close();
    }
    public void addNotification(noticeNotification notification){
        SQLiteDatabase db=this.getWritableDatabase();
        ContentValues values=new ContentValues();
        values.put(ROW_MAIN_CATEGORY,notification.getMain_category());
        values.put(ROW_CATEGORY,notification.getCategory());
        values.put(ROW_SUBJECT,notification.getSubject());
        db.insert(TABLE_NOTIFICATIONS,null,values);
        //db.close();
    }
}
