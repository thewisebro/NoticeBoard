package com.channeli.noticeboard;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class SubscriptionSettings extends ActionBarActivity {
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    Set<String> subscriptions=new HashSet<>();
    Set<String> constants=new HashSet<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subscription_settings);
        Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Notification Settings");
        preferences=getSharedPreferences(MainActivity.PREFS_NAME, 0);
        editor=preferences.edit();

        Set<String> constantSet=preferences.getStringSet("constants", new HashSet<String>());
        constants.addAll(constantSet);
        if (constants.size()==0){
            constants.add("Placement Office");
            constants.add("Authorities");
            constants.add("Departments");
        }
        else {
            constants.remove("All");
        }
        List<String> listConstants=new ArrayList<>(constants);
        Collections.sort(listConstants);

        LinearLayout listview= (LinearLayout) findViewById(R.id.subscriptions);
        Set<String> subscriptionSet=preferences.getStringSet("subscriptions",constants);
        subscriptions.addAll(subscriptionSet);

        for (final String s:listConstants){
            LinearLayout rowView= (LinearLayout) getLayoutInflater().inflate(R.layout.subscription_row, null);
            TextView textView= (TextView) rowView.findViewById(R.id.subsc_row_text);
            textView.setText(s);
            CheckBox checkBox= (CheckBox) rowView.findViewById(R.id.subsc_row_checkbox);
            checkBox.setChecked(subscriptions.contains(s));
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        FirebaseMessaging.getInstance().subscribeToTopic(s.replace(" ", "%20"));
                        showMessage("Subscribed : " + s);
                        subscriptions.add(s);
                    } else {
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(s.replace(" ", "%20"));
                        showMessage("Unsubscribed : " + s);
                        subscriptions.remove(s);
                    }
                    editor.putStringSet("subscriptions",subscriptions);
                    editor.apply();
                }
            });
            listview.addView(rowView);
        }
    }

    public void showMessage(String msg){
        CoordinatorLayout coordinatorLayout= (CoordinatorLayout) findViewById(R.id.subsc_coordinateLayout);
        Snackbar snackbar=Snackbar.make(coordinatorLayout, msg, Snackbar.LENGTH_SHORT);
        TextView tv= (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(getResources().getColor(R.color.colorPrimary));
        tv.setHeight((int) getResources().getDimension(R.dimen.bottomBarHeight));
        tv.setTypeface(null, Typeface.BOLD);
        snackbar.show();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_subscription_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
