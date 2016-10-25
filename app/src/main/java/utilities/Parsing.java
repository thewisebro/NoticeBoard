package utilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

import objects.DrawerItem;
import objects.NoticeInfo;
import objects.NoticeObject;

public class Parsing {

    public ArrayList<DrawerItem> parseConstants(String constants){
        ArrayList<DrawerItem> list = new ArrayList<>();
        ArrayList<String> constantsList=new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(constants);
            JSONArray jsonArray = jsonObject.getJSONArray("order");
            for(int i=0;i<jsonArray.length();i++){
                constantsList.add(jsonArray.getString(i));
                //list.add(new DrawerItem(jsonArray.getString(i)));
            }
            Collections.sort(constantsList);
            for (String s: constantsList){
                list.add(new DrawerItem(s));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
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
                //notice.setStar(jsonObject.getBoolean("starred_status"));
                //notice.setRead(jsonObject.getBoolean("read_status"));
                notice.setStar(true);
                notice.setRead(false);
                noticeslist.add(notice);
            }
        }
        catch(JSONException e){
            e.printStackTrace();
        }
        return noticeslist;
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
                notice.setMain_category(jsonObject.getString("main_category"));
                notice.setId(jsonObject.getInt("id"));
                notice.setDatetime_modified(jsonObject.getString("datetime_modified"));
                notice.setCategory(jsonObject.getString("category"));
                notice.setRead(false);
                notice.setStar(true);
                //notice.setRead(jsonObject.getBoolean("read_status"));
                //notice.setStar(jsonObject.getBoolean("starred_status"));
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
            noticeInfo.setCategory(jsonObject.getString("category"));
            }
        catch(JSONException e){
            e.printStackTrace();
        }
        return noticeInfo;
    }

}
