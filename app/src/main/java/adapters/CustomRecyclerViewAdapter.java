package adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;

import objects.NoticeObject;
import objects.NoticeObjectViewHolder;

/**
 * Created by Ankush on 23-09-2016.
 */
public class CustomRecyclerViewAdapter extends RecyclerView.Adapter<NoticeObjectViewHolder> {
    private Context context;
    private int viewLayoutId;
    private ArrayList<NoticeObject> list;

    public CustomRecyclerViewAdapter(Context context,int viewLayoutId, ArrayList<NoticeObject> list){
        this.context=context;
        this.viewLayoutId=viewLayoutId;
        this.list=list;
    }
    @Override
    public NoticeObjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(NoticeObjectViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
