package utilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import objects.Category;
import objects.NoticeInfo;
import objects.NoticeObject;

public class Parsing {

    public ArrayList<Category> parse_constants(String constants){
        ArrayList<Category> categorieslist = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(constants);
            JSONArray jsonArray = jsonObject.getJSONArray("order");
            Category categories=null;
            for(int i=0;i<jsonArray.length();i++){
                categories = new Category(jsonArray.getString(i),
                        jsonObject.getJSONArray(jsonArray.getString(i)));
                categorieslist.add(categories);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return categorieslist;
    }

    public ArrayList<NoticeObject> parseNotices(String notices){
        ArrayList<NoticeObject> noticeslist = new ArrayList<>();
        try{
            JSONArray jsonArray = new JSONArray(notices);
            NoticeObject notice=null;
            for(int i=0;i< jsonArray.length();i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                notice = new NoticeObject();
                notice.setId(jsonObject.getInt("id"));
                notice.setSubject(jsonObject.getString("subject"));
                notice.setDatetime_modified(jsonObject.getString("datetime_modified"));
                notice.setCategory(jsonObject.getString("category"));
                notice.setMain_category(jsonObject.getString("main_category"));
                notice.setStar(jsonObject.getBoolean("starred_status"));
                notice.setRead(jsonObject.getBoolean("read_status"));
                noticeslist.add(notice);
            }
        }
        catch(JSONException e){
            e.printStackTrace();
        }
        return noticeslist;
    }

    public ArrayList<NoticeInfo> parseSearchedNotices(String result){
        ArrayList<NoticeInfo> noticeInfoList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(result);
            NoticeInfo noticeInfo=null;
            JSONObject jsonObject=null;
            for(int i=0;i<jsonArray.length();i++){
                jsonObject = jsonArray.getJSONObject(i);
                noticeInfo = parseNoticeInfo(jsonObject.toString());
                noticeInfoList.add(noticeInfo);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return noticeInfoList;
    }
    public ArrayList<NoticeObject> parseSearchNotices(String result){
        ArrayList<NoticeObject> noticeList = new ArrayList<NoticeObject>();
        try {
            JSONArray jsonArray = new JSONArray(result);
            NoticeObject notice=null;
            JSONObject jsonObject=null;
            for(int i=0;i<jsonArray.length();i++){
                jsonObject = jsonArray.getJSONObject(i);
                notice=new NoticeObject();
                notice.setSubject(jsonObject.getString("subject"));
                notice.setMain_category(jsonObject.getString("category"));
                notice.setId(jsonObject.getInt("id"));
                notice.setDatetime_modified(jsonObject.getString("datetime_modified"));
                noticeList.add(notice);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return noticeList;
    }

    public NoticeInfo parseNoticeInfo(String noticeinfo){
        NoticeInfo noticeInfo = new NoticeInfo();
        try{
            JSONObject jsonObject = new JSONObject(noticeinfo);
            noticeInfo.setId(jsonObject.getInt("id"));
            noticeInfo.setContent(jsonObject.getString("content"));
            noticeInfo.setSubject(jsonObject.getString("subject"));
            noticeInfo.setDatetime_modified(jsonObject.getString("datetime_modified"));
            //noticeInfo.setUsername(jsonObject.getString("username"));
            noticeInfo.setCategory(jsonObject.getString("category"));
            }
        catch(JSONException e){
            e.printStackTrace();
        }
        return noticeInfo;
    }

}
