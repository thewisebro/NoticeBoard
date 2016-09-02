package objects;

/*
Created by manohar on 4/2/15.
 */
public class NoticeObject {
    int id;
    String subject;
    String datetime_modified;
    //String username;
    String category;
    String main_category;
    boolean star;
    boolean read;

    public String getSubject(){return this.subject;}
    public int getId(){return this.id;}
    public String getCategory(){return this.category;}
    public String getDatetime_modified(){return this.datetime_modified;}
    public boolean getStar(){ return this.star; }
    public boolean getRead(){ return this.read;}

    public void setId(int id){this.id = id;}
    public void setSubject(String subject){this.subject = subject;}
    public void setDatetime_modified(String datetime_modified){this.datetime_modified = datetime_modified;}
    public void setCategory(String category){this.category = category;}
    public void setMain_category(String main_category){this.main_category = main_category;}
    public void toggleStar(){
        if(this.star)
            this.star=false;
        else
            this.star=true;
    }
    public void setStar(boolean flag){ this.star=flag; }
    public void setRead(boolean flag){ this.read=flag;}
}
