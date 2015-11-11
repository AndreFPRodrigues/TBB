package tbb.core.logger;

import android.graphics.Rect;
import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;

/**
 * Created by andre on 02-Oct-15.
 */
public class Tree {
    private String eventType;
    private String eventDescription;
    private long timestamp;
    private String activity;
    private int treeSequence;
    private String treeStructure;

    public Tree (String eventType, String desc, long timestamp , String activity, int treeSequence){
        this.eventType=eventType;
        this.eventDescription=desc;
        this.timestamp=timestamp;
        this.activity=activity;
        this.treeSequence=treeSequence+1;
    }

    public void setTreeStructure(AccessibilityNodeInfo parent){
        Rect boundsP = new Rect() ;
        Rect boundsS = new Rect();
        parent.getBoundsInParent(boundsP);
        parent.getBoundsInScreen(boundsS);
        String viewId="";
        String actionList="";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            viewId=parent.getViewIdResourceName();
        }else {
            viewId = parent.hashCode() + "";
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            actionList=parent.getActionList().toString();
        }else {
            actionList = parent.getActions() + "";
        }

            treeStructure= "{\"node\":\"" + viewId +"\"" +
                    " , \"boundsP\":"+"\"" +boundsP +"\""+
                    " , \"boundsS\":" +"\""+boundsS +"\""+
                " , \"package\":\"" +parent.getPackageName()+"\"" +
                " , \"package\":\"" +parent.getPackageName()+"\"" +
                " , \"class\":\"" +parent.getClassName()+"\"" +
                " , \"package\":\"" +parent.getPackageName()+"\"" +
                " , \"text\":\"" +AccessibilityScrapping.cleanText(parent.getText()+"")+"\"" +
                " , \"content\":\"" +AccessibilityScrapping.cleanText(parent.getContentDescription()+"") + "\"" +
                getStates(parent) +
                ", \"actions\":\"" + actionList +"\" " +
                 ", " + getChildren(parent,0)+
                 "}";
    }

    private String getStates(AccessibilityNodeInfo parent) {
        String state = "";
        if(parent.isCheckable())
            state+=" , \"checkable\":" +true;
        if(parent.isChecked())
            state+=" , \"checked\":" +true;
        if(parent.isFocusable())
            state+=" , \"focusable\":" +true;
        if(parent.isFocused())
            state+=" , \"focused\":" +true;
        if(parent.isSelected())
            state+=" , \"selected\":" +true;
        if(parent.isClickable())
            state+=" , \"clickable\":" +true;
        if(parent.isLongClickable())
            state+=" , \"longClickable\":" +true;
        if(parent.isEnabled())
            state+=" , \"enabled\":" +true;
        if(parent.isPassword())
            state+=" , \"password\":" +true;
        if(parent.isScrollable())
            state+=" , \"scrollable\":" +true;

        return state;
    }

    private String getChildren(AccessibilityNodeInfo node, int childLevel) {
        StringBuilder sb = new StringBuilder();
        if (node.getChildCount() > 0) {
            sb.append("\"children\": [");
            for (int i = 0; i < node.getChildCount(); i++) {
                //sb.append("{");

                if (node.getChild(i) != null) {
                    if (i > 0)
                        sb.append(",");
                    sb.append(getDescription(node.getChild(i)));

                    if (node.getChild(i).getChildCount() > 0 && node.getChild(i)!=null)
                        sb.append(","+ getChildren(node.getChild(i), childLevel + 1));
                    else{
                        sb.append( ", \"children\":[]");
                    }
                }
                sb.append("}" );

            }

            sb.append("]");
        }

        return sb.toString();
    }

    public String getDescription(AccessibilityNodeInfo node){
        String result="";
        Rect boundsP = new Rect() ;
        Rect boundsS = new Rect();
        node.getBoundsInParent(boundsP);
        node.getBoundsInScreen(boundsS);
        String viewId="";
        String actionList="";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            viewId=node.getViewIdResourceName();
        }else {
            viewId = node.hashCode() + "";
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            actionList=node.getActionList().toString();
        }else {
            actionList = node.getActions() + "";
        }
        result = "{\"node\":\"" +viewId +"\"" +
                " , \"boundsP\":"+"\"" +boundsP +"\""+
                " , \"boundsS\":" +"\""+boundsS +"\""+
                " , \"package\":\"" +node.getPackageName()+"\"" +
                " , \"package\":\"" +node.getPackageName()+"\"" +
                " , \"class\":\"" +node.getClassName()+"\"" +
                " , \"package\":\"" +node.getPackageName()+"\"" +
                " , \"text\":\"" +AccessibilityScrapping.cleanText(node.getText()+"")+"\"" +
                " , \"content\":\"" +AccessibilityScrapping.cleanText(node.getContentDescription()+"") + "\"" +
                getStates(node) +
                ", \"actions\":\"" + actionList +"\" " ;  /*+
             ", \"children\":\"" + getChildren(node,)+
                "\"}";*/
        return result;
    }
     @Override
    public String toString(){
         return "{\"treeID\":"+ treeSequence+
                 " , \"eventType\":\"" + eventType+ "\""+
                 " , \"eventDesc\":\"" + eventDescription + "\"" +
                 " , \"timestamp\":" + timestamp +
                 " , \"activity\":\"" + activity + "\"" +
                 " , \"tree\":"+ treeStructure +
                 " },";
     }

}
