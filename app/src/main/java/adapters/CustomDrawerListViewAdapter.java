package adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.channeli.noticeboard.R;

import java.util.ArrayList;

import objects.DrawerItem;

/**
 * Created by Ankush on 25-10-2016.
 */
public class CustomDrawerListViewAdapter extends BaseExpandableListAdapter {
    public ArrayList<DrawerItem> drawerItemList;
    Context context;
    LayoutInflater inflater;
    public int groupPos=0;
    public int childPos=-1;
    public CustomDrawerListViewAdapter(ArrayList<DrawerItem> list,Context context){
        this.drawerItemList=list;
        this.context=context;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    public long totalItemCount(){
        long count=0;
        for (DrawerItem i:drawerItemList){
            count++;
            if(i.getCategories()!=null)
                count+=i.getCategories().size();
        }
        return count;
    }
    @Override
    public int getGroupCount() {
        return drawerItemList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (drawerItemList.get(groupPosition).getCategories()==null)
            return 0;
        return drawerItemList.get(groupPosition).getCategories().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return drawerItemList.get(groupPosition).getName();
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return drawerItemList.get(groupPosition).getCategories().get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    public void OnGroupItemClick(boolean isExpanded, int groupPosition){

    }
    public void OnIndicatorClick(boolean isExpanded, int groupPosition){

    }
    public void OnChildItemClick(int groupPosition, int childPosition){

    }
    public void itemCheck(){

    }
    @Override
    public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView, ViewGroup parent) {
        //if (drawerItemList.get(groupPosition).getName()=="")
        //    return inflater.inflate(R.layout.drawer_line,null);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.drawer_group_item,
                    null);
        }
        if (drawerItemList.get(groupPosition).getName()==""){
            convertView.findViewById(R.id.drawer_list_item).setVisibility(View.GONE);
            convertView.findViewById(R.id.drawer_line).setVisibility(View.VISIBLE);
            return convertView;
        }
        else {
            convertView.findViewById(R.id.drawer_list_item).setVisibility(View.VISIBLE);
            convertView.findViewById(R.id.drawer_line).setVisibility(View.GONE);
        }
        TextView tv= (TextView) convertView.findViewById(R.id.drawer_group_text);
        tv.setText(drawerItemList.get(groupPosition).getName());
        ImageView icon= (ImageView) convertView.findViewById(R.id.drawer_group_icon);
        icon.setImageResource(drawerItemList.get(groupPosition).getIcon());
        ImageView indicator= (ImageView) convertView.findViewById(R.id.drawer_group_indicator);
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnGroupItemClick(isExpanded,groupPosition);
            }
        });
        if (drawerItemList.get(groupPosition).getCategories()==null || drawerItemList.get(groupPosition).getCategories().size()==0) {
            //indicator.setImageResource(android.R.color.transparent);
            indicator.setVisibility(View.GONE);
        }
        else {
            indicator.setVisibility(View.VISIBLE);
            if (isExpanded)
                indicator.setImageResource(R.drawable.ic_keyboard_arrow_up_black_24dp);
            else
                indicator.setImageResource(R.drawable.ic_keyboard_arrow_down_black_24dp);

            indicator.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OnIndicatorClick(isExpanded,groupPosition);
                }
            });
        }
        if (groupPosition==groupPos) {
            icon.setColorFilter(context.getResources().getColor(R.color.colorAccentDark));
            tv.setTextColor(context.getResources().getColor(R.color.colorAccentDark));
            convertView.setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryLight));
        }
        else {
            tv.setTextColor(context.getResources().getColor(R.color.blue_grey_900));
            icon.clearColorFilter();
            convertView.setBackground(null);
        }
        return convertView;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.drawer_child_item,
                    null);
        }
        TextView tv= (TextView) convertView.findViewById(R.id.drawer_item_text);
        tv.setText(drawerItemList.get(groupPosition).getCategories().get(childPosition));
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnChildItemClick(groupPosition,childPosition);
            }
        });
        if (childPosition==childPos && groupPosition==groupPos){
            tv.setTextColor(context.getResources().getColor(R.color.colorAccentDark));
        }
        else{
            tv.setTextColor(context.getResources().getColor(R.color.black));
        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return (drawerItemList.size()==0);
    }
}
