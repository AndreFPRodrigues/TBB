package tbb.core.service.configuration;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kyle Montague on 05/10/2015.
 */
public class AppListItem {




    protected String label;
    protected String packageName;
    protected Drawable image;
    protected boolean shouldTrack;

    public AppListItem(String appName, String appPackage, Drawable appIcon){
        label = appName;
        packageName = appPackage;
        image = appIcon;
        shouldTrack = true;
    }

    public AppListItem(String appName, String appPackage, Drawable appIcon, Boolean tracking){
        label = appName;
        packageName = appPackage;
        image = appIcon;
        shouldTrack = tracking;
    }


    public static List<AppListItem> getAppPermissions(Context context, DataPermissions appPermissions){
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> items = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<AppListItem> appListItems = new ArrayList<>();
        for(ApplicationInfo item: items){
            boolean trackingState = appPermissions.shouldLog(item.packageName);
            appListItems.add(new AppListItem(item.loadLabel(pm).toString(), item.packageName, item.loadIcon(pm),trackingState));
        }
        return appListItems;
    }
}
