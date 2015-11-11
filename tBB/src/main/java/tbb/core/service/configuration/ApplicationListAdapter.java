package tbb.core.service.configuration;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import blackbox.tinyblackbox.R;

/**
 * Created by Kyle Montague on 29/09/15.
 */
public class ApplicationListAdapter implements ListAdapter {


    List<AppListItem> items;
    Context mContext;
    DataPermissions mData;
    private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

    public ApplicationListAdapter(Context context){
        mData = DataPermissions.getSharedInstance(context);
        items = AppListItem.getAppPermissions(context, mData);
        mContext = context;
        registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                mData.savePermissions();
            }
        });
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        observers.add(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        observers.remove(observer);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public AppListItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.app_list_row_item, parent, false);
            holder = new ViewHolder();
            convertView.setTag(holder); // setting Holder as arbitrary object for row
            holder.checkBox = (CheckBox)convertView.findViewById(R.id.item_checkbox);
            holder.textView = (TextView)convertView.findViewById(R.id.item_title);
            holder.imageView = (ImageView)convertView.findViewById(R.id.item_icon);
        }
        else { // view recycling
            // row already contains Holder object
            holder = (ViewHolder) convertView.getTag();
        }

        holder.checkBox.setOnCheckedChangeListener(null); //need to disable the previous onCheckedChangeListener before calling setChecked.
        holder.checkBox.setChecked(items.get(position).shouldTrack);
        holder.textView.setText(items.get(position).label);
        holder.imageView.setImageDrawable(items.get(position).image);

        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AppListItem item = items.get(position);
                item.shouldTrack = isChecked;
                mData.setPermission(item.packageName,item.shouldTrack);
                notifyDataSetChanged();
            }
        });
        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return (items == null || items.size() == 0);
    }

    public void notifyDataSetChanged(){
        for (DataSetObserver observer: observers) {
            observer.onChanged();
        }
    }

    private class ViewHolder{
        CheckBox checkBox;
        TextView textView;
        ImageView imageView;
    }
}
