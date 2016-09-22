package utilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import objects.Category;
import objects.NoticeInfo;
import objects.NoticeObject;

public class Parsing {
    JSONObject jsonObject, jsonObject2;
    JSONArray jsonArray;
    ArrayList<Category> categorieslist;
    ArrayList<NoticeObject> noticeslist;
    ArrayList<NoticeInfo> noticeInfoList;
    Category categories;
    NoticeObject notice;
    NoticeInfo noticeInfo;

    public ArrayList<Category> parse_constants(String constants){
        categorieslist = new ArrayList<>();
        try {
            jsonObject = new JSONObject(constants);
            jsonArray = jsonObject.getJSONArray("order");
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
        noticeslist = new ArrayList<>();
        try{
            jsonArray = new JSONArray(notices);

            for(int i=0;i< jsonArray.length();i++){
                jsonObject2 = jsonArray.getJSONObject(i);
                notice = new NoticeObject();
                notice.setId(jsonObject2.getInt("id"));
                notice.setSubject(jsonObject2.getString("subject"));
                notice.setDatetime_modified(jsonObject2.getString("datetime_modified"));
                notice.setCategory(jsonObject2.getString("category"));
                notice.setMain_category(jsonObject2.getString("main_category"));
                notice.setStar(jsonObject2.getBoolean("starred_status"));
                notice.setRead(jsonObject2.getBoolean("read_status"));
                noticeslist.add(notice);
            }
        }
        catch(JSONException e){
            e.printStackTrace();
        }
        return noticeslist;
    }

    public ArrayList<NoticeInfo> parseSearchedNotices(String result){
        noticeInfoList = new ArrayList<>();
        try {

            jsonArray = new JSONArray(result);
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

    public NoticeInfo parseNoticeInfo(String noticeinfo){
        noticeInfo = new NoticeInfo();
        try{
            jsonObject2 = new JSONObject(noticeinfo);
            noticeInfo.setId(jsonObject2.getInt("id"));
            noticeInfo.setContent(jsonObject2.getString("content"));
            noticeInfo.setSubject(jsonObject2.getString("subject"));
            noticeInfo.setDatetime_modified(jsonObject2.getString("datetime_modified"));
            //noticeInfo.setUsername(jsonObject2.getString("username"));
            noticeInfo.setCategory(jsonObject2.getString("category"));
            }
        catch(JSONException e){
            e.printStackTrace();
        }
        return noticeInfo;
    }
}
