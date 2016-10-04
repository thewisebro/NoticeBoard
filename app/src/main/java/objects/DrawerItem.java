package objects;

/**
 * Created by Ankush on 05-10-2016.
 */
public class DrawerItem {
    private String name;
    private int logo;
    public DrawerItem(String name, int i){
        this.name=name;
        this.logo=i;
    }
    public  DrawerItem(String name){
        this.name=name;
    }
    private void setName(){

    }
    public String getName(){
        return this.name;
    }
    public int getIcon(){
        return this.logo;
    }
}
