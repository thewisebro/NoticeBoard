package objects;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import in.channeli.noticeboard.R;

/**
 * Created by Ankush on 23-09-2016.
 */
public class NoticeObjectViewHolder extends RecyclerView.ViewHolder {
    public View view;
    public TextView category;
    public TextView datetime;
    public TextView subject;
    public ToggleButton star;
    public NoticeObjectViewHolder(View view) {
        super(view);
        this.view=view;
        category= (TextView) view.findViewById(R.id.category);
        datetime= (TextView) view.findViewById(R.id.datetime);
        subject= (TextView) view.findViewById(R.id.subject);
        star= (ToggleButton) view.findViewById(R.id.star_button);
    }
}
