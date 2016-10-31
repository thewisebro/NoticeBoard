package objects;

import java.io.Serializable;

public class NoticeInfo implements Serializable {
    int id;
    String reference;
    String subject;
    String category;
    String content;
    String datetime_modified;
    public NoticeInfo(){
        this.subject="No Notices Available!";
        this.category="";
        this.datetime_modified="T";
    };
    public String getSubject(){return subject;}
    public String getCategory(){return category;}
    public String getDatetime_modified(){return datetime_modified;}
    public String getContent(){return content;}
    public String getReference(){ return reference;}
    public int getId(){ return id;}

    public void setId(int id){this.id = id;}
    public void setReference(String reference){this.reference = reference;}
    public void setSubject(String subject){this.subject = subject;}
    public void setCategory(String category){this.category = category;}
    public void setContent(String content){this.content = content;}
    public void setDatetime_modified(String datetime_modified){this.datetime_modified = datetime_modified;}
}
