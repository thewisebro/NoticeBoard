package utilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import objects.DrawerItem;
import objects.NoticeInfo;
import objects.NoticeObject;

public final class Parsing {

    public static List<DrawerItem> parseConstants(String constants) throws JSONException {
        List<DrawerItem> list = new ArrayList<>();
        List<String> constantsList=new ArrayList<>();
        JSONObject jsonObject = new JSONObject(constants);
        JSONArray jsonArray = jsonObject.getJSONArray("order");
        for(int i=0;i<jsonArray.length();i++){
            constantsList.add(jsonArray.getString(i));
        }
        Collections.sort(constantsList);
        for (String s: constantsList){
            List<String> categories=new ArrayList<>();
            JSONArray array=jsonObject.getJSONArray(s);
            for(int i=0;i<array.length();i++) categories.add(array.getString(i));
            list.add(new DrawerItem(s,categories));
        }
        return list;
    }

    private static boolean checkStar(int id, Set<NoticeObject> list){
        for (NoticeObject n:list)
            if (n.getId()==id) return true;
        return false;
    }
    private static boolean checkRead(int id, Set<Integer> list){
        return list.contains(id);
    }
    public static List<NoticeObject> parseNotices(String notices,Set<NoticeObject> starredList, Set<Integer> readList) throws JSONException {
        List<NoticeObject> noticeslist = new ArrayList<>();
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
        return noticeslist;
    }
    public static Set<NoticeObject> parseStarredNotices(String notices) throws JSONException {
        Set<NoticeObject> noticeslist = new LinkedHashSet<>();
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
        return noticeslist;
    }
    public static Set<NoticeObject> parseStarredNotices(String notices,Set<Integer> readNotices) throws JSONException {
        Set<NoticeObject> noticeslist = new LinkedHashSet<>();
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
        return noticeslist;
    }
    public static Set<Integer> parseReadNotices(String ids) throws JSONException {
        Set<Integer> list = new HashSet<>();
        JSONObject jsonObject=new JSONObject(ids);
        Iterator<String> keys=jsonObject.keys();
        while(keys.hasNext()){
            String key=keys.next();
            list.add(new Integer(jsonObject.getInt(key)));
        }
        return list;
    }

    public static List<NoticeObject> parseSearchNotices(String result,Set<NoticeObject> starredList, Set<Integer> readList) throws JSONException {
        List<NoticeObject> noticeList = new ArrayList<NoticeObject>();
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
        return noticeList;
    }

    public static NoticeInfo parseNoticeInfo(String noticeinfo) throws JSONException {
        NoticeInfo noticeInfo = new NoticeInfo();
        JSONObject jsonObject = new JSONObject(noticeinfo);
        noticeInfo.setId(jsonObject.getInt("id"));
        noticeInfo.setContent(jsonObject.getString("content"));
        noticeInfo.setSubject(jsonObject.getString("subject"));
        noticeInfo.setDatetime_modified(jsonObject.getString("datetime_modified"));
        noticeInfo.setCategory(jsonObject.getString("category"));
        return noticeInfo;
    }

}
