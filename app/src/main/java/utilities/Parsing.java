package utilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

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
            }
            Collections.sort(constantsList);
            for (String s: constantsList){
                ArrayList<String> categories=new ArrayList<>();
                JSONArray array=jsonObject.getJSONArray(s);
                for(int i=0;i<array.length();i++)
                    categories.add(array.getString(i));
                list.add(new DrawerItem(s,categories));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    private boolean checkStar(int id, ArrayList<NoticeObject> list){
        for (NoticeObject n:list)
            if (n.getId()==id)
                return true;
        return false;
    }
    private boolean checkRead(int id, ArrayList<Integer> list){
        for (Integer i:list)
            if(i.intValue()==id)
                return true;
        return false;
    }
    public ArrayList<NoticeObject> parseNotices(String notices,ArrayList<NoticeObject> starredList, ArrayList<Integer> readList){
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
                notice.setStar(checkStar(notice.getId(), starredList));
                notice.setRead(checkRead(notice.getId(),readList));
                noticeslist.add(notice);
            }
        }
        catch(JSONException e){
            e.printStackTrace();
            return null;
        }
        return noticeslist;
    }
    public ArrayList<NoticeObject> parseStarredNotices(String notices){
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
                notice.setStar(true);
                notice.setRead(false);
                noticeslist.add(notice);
            }
        }
        catch(JSONException e){
            e.printStackTrace();
            return null;
        }
        return noticeslist;
    }
    public ArrayList<NoticeObject> parseStarredNotices(String notices,ArrayList<Integer> readNotices){
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
                notice.setStar(true);
                notice.setRead(checkRead(notice.getId(),readNotices));
                noticeslist.add(notice);
            }
        }
        catch(JSONException e){
            e.printStackTrace();
            return null;
        }
        return noticeslist;
    }
    public ArrayList<Integer> parseReadNotices(String ids){
        ArrayList<Integer> list = new ArrayList<>();
        try{
            JSONObject jsonObject=new JSONObject(ids);
            Iterator<String> keys=jsonObject.keys();
            while(keys.hasNext()){
                String key=keys.next();
                list.add(new Integer(jsonObject.getInt(key)));
            }
        }
        catch(JSONException e){
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<NoticeObject> parseSearchNotices(String result,ArrayList<NoticeObject> starredList, ArrayList<Integer> readList){
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
                notice.setRead(checkRead(notice.getId(),readList));
                notice.setStar(checkStar(notice.getId(),starredList));
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
            return noticeInfo;
            }
        catch(JSONException e){
            e.printStackTrace();
            return null;
        }
    }

}
