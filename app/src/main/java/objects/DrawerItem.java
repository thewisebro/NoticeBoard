package objects;

import com.channeli.noticeboard.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ankush on 05-10-2016.
 */
public class DrawerItem {
    private String name;
    private int logo;
    private List<String> categories;
    public  DrawerItem(String name,List<String> categories){
        this.name=name;
        this.categories=categories;
        setIcon();
    }
    private void setIcon(){
        if(name.contains("Authorities")) {
            this.logo=(R.drawable.ic_account_balance_black_24dp);
        }
        else if(name.contains("All")) {
            this.logo=(R.drawable.ic_home_black_24dp);
        }
        else if(name.contains("Placement")) {
            this.logo=(R.drawable.ic_assignment_ind_black_24dp);
        }
        else if(name.contains("Department")) {
            this.logo=(R.drawable.ic_school_black_24dp);
        }
        else if(name.contains("Logout")) {
            this.logo=(R.drawable.ic_power_settings_new_black_24dp);
        }
        else if(name.contains("Feedback")){
            this.logo=(R.drawable.ic_info_black_24dp);
        }
        else if(name.contains("Starred")){
            this.logo=R.drawable.ic_star_black_24dp;
        }
        else if(name.contains("Archived")){
            this.logo=R.drawable.ic_archive_black_24dp;
        }
        else if (name.contains("Notifications")){
            this.logo=R.drawable.ic_settings_black_24dp;
        }
        else
            logo=0;
    }
    public String getName(){
        return this.name;
    }
    public int getIcon(){
        return this.logo;
    }
    public List<String> getCategories() { return this.categories; }
}
