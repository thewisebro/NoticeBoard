package objects;

/**
 * Created by Ankush on 28-11-2016.
 */
public class noticeNotification {
    private String subject;
    private String category;
    private String main_category;
    public noticeNotification(String main_category,String category, String subject){
        this.main_category=main_category;
        this.category=category;
        this.subject=subject;
    }
    public String getSubject(){return this.subject;}
    public String getCategory(){return this.category;}
    public String getMain_category(){return this.main_category;}
}
