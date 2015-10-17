package tbb.core.service.configuration;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;

import blackbox.tinyblackbox.R;


public class AppPermissionListActivity extends Activity implements AdapterView.OnItemClickListener {




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
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();

    }




    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d("TAP", "view:" + position);
        view.setSelected(true);
        CheckBox checkBox = (CheckBox)view.findViewById(R.id.item_checkbox);
        checkBox.setChecked(!checkBox.isChecked());

    }
}
