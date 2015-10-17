package tbb.core.service.configuration;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;

import blackbox.tinyblackbox.R;


public class AppPermissionListActivity extends Activity implements AdapterView.OnItemClickListener {


    private int mID = 1;

    protected ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        mListView = (ListView)findViewById(R.id.app_listview);
        ApplicationListAdapter mAdapter = new ApplicationListAdapter(getApplicationContext());

        mListView.setAdapter(mAdapter);
        mListView.setEnabled(true);
        mListView.setOnItemClickListener(this);


        this.setTitle(getString(R.string.title_encryption_level));
    }


    @Override
    protected void onResume(){
        super.onResume();
        notificationItem();
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        NotificationManager mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(mID);
    }


    private void notificationItem(){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setContentTitle(getString(R.string.app_name)).setContentText(getString(R.string.notification_active)).setSmallIcon(android.R.drawable.ic_dialog_info).setOngoing(true)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setCategory(NotificationCompat.CATEGORY_SERVICE).addAction(android.R.drawable.ic_media_pause,getString(R.string.pause_one_hour),null);
        Notification not = mBuilder.build();
        NotificationManager mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(mID,not);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d("TAP", "view:" + position);
        view.setSelected(true);
        CheckBox checkBox = (CheckBox)view.findViewById(R.id.item_checkbox);
        checkBox.setChecked(!checkBox.isChecked());

    }
}
