# TBB

##Features
- Interaction logs
- IO logs (requires root)
- Keystrokes log
- DOM Tree interfaces log
- Encryption

###Getting Started
1. Download and install
2. Go to accessibility services
3. To collect touch data go to settings
4. Turn on the service

Everytime the device goes into standby mode a session is colected and stored in the device external storage (/TBB/). In those folders you can find json files of all the types of logs. 
If encryption is enabled, the files will be encrypted periodically.

###Data Collected

####Interaction logs
desc - description found of the item clicked

app - activity where the event took place

type - type of accessibility event observed (i.e. only listening for Clicked and Focus)

```
{"records":[
  {"treeID":11 , "desc":"Configurações;" , "timestamp":1462531939747 , "app":"com.sec.android.app.launcher" , "type":"TYPE_VIEW_CLICKED" },
  {"treeID":11 , "desc":"Configurações;" , "timestamp":1462531939951 , "app":"com.sec.android.app.launcher" , "type":"TYPE_VIEW_CLICKED" },
  {"treeID":11 , "desc":"Configurações;" , "timestamp":1462531940881 , "app":"com.sec.android.app.launcher" , "type":"TYPE_VIEW_CLICKED" },
  {"treeID":11 , "desc":"Configurações;" , "timestamp":1462531941101 , "app":"com.sec.android.app.launcher" , "type":"TYPE_VIEW_CLICKED" },
  {"treeID":11 , "desc":"Apps;" , "timestamp":1462531941633 , "app":"com.sec.android.app.launcher" , "type":"TYPE_VIEW_CLICKED" },
  {"treeID":12 , "desc":"Root Browser;" , "timestamp":1462531942800 , "app":"com.sec.android.app.launcher" , "type":"TYPE_VIEW_CLICKED" },
{}]}
```
####IO
dev - device id that is being recorded

type - type of touch detected: 0 is down; 1 is move; 2 is up;

devTime - timestamp collected from the device
```
{"records":[
  {"treeID":11 , "dev":6 , "type":0 , "id":149 , "x":695 , "y":354 , "pressure":0 , "devTime":5708728 , "timestamp":1462531939641},
  {"treeID":11 , "dev":6 , "type":1 , "id":149 , "x":695 , "y":354 , "pressure":0 , "devTime":5708761 , "timestamp":1462531939671},
  {"treeID":11 , "dev":6 , "type":1 , "id":149 , "x":695 , "y":354 , "pressure":0 , "devTime":5708777 , "timestamp":1462531939688}
]}
````
####Keystrokes
```
{"records":[
  {"keystroke":"N" , "timestamp":1462533078008 , "text":"[N]"},
  {"keystroke":"o" , "timestamp":1462533078290 , "text":"[No]"},
  {"keystroke":"t" , "timestamp":1462533079750 , "text":"[Not]"},
  {"keystroke":"e" , "timestamp":1462533080081 , "text":"[Note]"},
  {"keystroke":" " , "timestamp":1462533080409 , "text":"[Note ]"},
  {"keystroke":"t" , "timestamp":1462533081238 , "text":"[Note t]"},
  {"keystroke":"e" , "timestamp":1462533081506 , "text":"[Note te]"},
  {"keystroke":"s" , "timestamp":1462533081728 , "text":"[Note tes]"},
  {"keystroke":"t" , "timestamp":1462533082030 , "text":"[Note test]"}
]}
````

####DOM Tree
```
{"records":[
  {"treeID":40 , "eventType":"TYPE_WINDOW_CONTENT_CHANGED" , "eventDesc":"" , "timestamp":1462533653992 , "activity":"com.android.launcher2.Launcher" , "encripted":false , "tree":{"node":"984" , "boundsP":"Rect(0, 0 - 1280, 752)" , "boundsS":"Rect(0, 0 - 1280, 752)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"com.android.internal.policy.impl.PhoneWindow$DecorView" , "package":"com.sec.android.app.launcher" , "text":"null" , "content":"null" , "enabled":true, "actions":"12" , 
  "children": [
    {"node":"1015" , "boundsP":"Rect(0, 0 - 1280, 752)" , "boundsS":"Rect(0, 0 - 1280, 752)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"android.widget.LinearLayout" , "package":"com.sec.android.app.launcher" , "text":"null" , "content":"null" , "enabled":true, "actions":"12" ,
      "children": [
        {"node":"1046" , "boundsP":"Rect(0, 0 - 1280, 752)" , "boundsS":"Rect(0, 0 - 1280, 752)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"android.widget.FrameLayout" , "package":"com.sec.android.app.launcher" , "text":"null" , "content":"null" , "enabled":true, "actions":"12" ,
          "children": [
            {"node":"1108" , "boundsP":"Rect(0, 0 - 1280, 752)" , "boundsS":"Rect(0, 0 - 1280, 752)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"com.android.launcher2.AnimationLayer" , "package":"com.sec.android.app.launcher" , "text":"null" , "content":"null" , "enabled":true, "actions":"12" ,
              "children": [
                {"node":"1170" , "boundsP":"Rect(0, 0 - 1280, 752)" , "boundsS":"Rect(0, 0 - 1280, 752)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"com.android.launcher2.DarkenView" , "package":"com.sec.android.app.launcher" , "text":"null" , "content":"null" , "enabled":true, "actions":"12" , "children":[]},
                {"node":"1201" , "boundsP":"Rect(0, 0 - 1280, 752)" , "boundsS":"Rect(0, 0 - 1280, 752)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"com.android.launcher2.DragLayer" , "package":"com.sec.android.app.launcher" , "text":"null" , "content":"null" , "enabled":true, "actions":"12" ,
                  "children": [
                    {"node":"1139" , "boundsP":"Rect(2560, 0 - 3840, 752)" , "boundsS":"Rect(0, 0 - 1280, 752)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"com.android.launcher2.Workspace" , "package":"com.sec.android.app.launcher" , "text":"null" , "content":"null" , "focusable":true , "focused":true , "enabled":true , "scrollable":true, "actions":"14" ,
                      "children": [
                        {"node":"4797" , "boundsP":"Rect(0, 0 - 1032, 724)" , "boundsS":"Rect(124, 28 - 1156, 752)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"com.android.launcher2.CellLayoutWithResizableWidgets" , "package":"com.sec.android.app.launcher" , "text":"null" , "content":"null" , "clickable":true , "longClickable":true , "enabled":true, "actions":"12" ,
                          "children": [
                            {"node":"5262" , "boundsP":"Rect(0, 0 - 992, 684)" , "boundsS":"Rect(144, 48 - 1136, 732)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"com.android.launcher2.CellLayoutChildren" , "package":"com.sec.android.app.launcher" , "text":"null" , "content":"null" , "enabled":true, "actions":"12" ,
                              "children": [
                                {"node":"5293" , "boundsP":"Rect(524243, 0 - 524339, 98)" , "boundsS":"Rect(144, 636 - 240, 734)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"com.android.launcher2.AppIconView" , "package":"com.sec.android.app.launcher" , "text":"Readers Hub" , "content":"Readers Hub" , "focusable":true , "clickable":true , "longClickable":true , "enabled":true, "actions":"13" , "children":[]},
                                {"node":"5324" , "boundsP":"Rect(524243, 0 - 524339, 98)" , "boundsS":"Rect(272, 636 - 368, 734)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"com.android.launcher2.AppIconView" , "package":"com.sec.android.app.launcher" , "text":"S Planner" , "content":"S Planner" , "focusable":true , "clickable":true , "longClickable":true , "enabled":true, "actions":"13" , "children":[]},
                                {"node":"5355" , "boundsP":"Rect(524243, 0 - 524339, 98)" , "boundsS":"Rect(400, 636 - 496, 734)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"com.android.launcher2.AppIconView" , "package":"com.sec.android.app.launcher" , "text":"Câmera" , "content":"Câmera" , "focusable":true , "clickable":true , "longClickable":true , "enabled":true, "actions":"13" , "children":[]},
                                {"node":"5386" , "boundsP":"Rect(524243, 0 - 524339, 98)" , "boundsS":"Rect(528, 636 - 624, 734)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"com.android.launcher2.AppIconView" , "package":"com.sec.android.app.launcher" , "text":"YouTube" , "content":"YouTube" , "focusable":true , "clickable":true , "longClickable":true , "enabled":true, "actions":"13" , "children":[]},
                                {"node":"5417" , "boundsP":"Rect(524243, 0 - 524339, 98)" , "boundsS":"Rect(656, 636 - 752, 734)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"com.android.launcher2.AppIconView" , "package":"com.sec.android.app.launcher" , "text":"Mapas" , "content":"Mapas" , "focusable":true , "clickable":true , "longClickable":true , "enabled":true, "actions":"13" , "children":[]},
                                {"node":"5448" , "boundsP":"Rect(524243, 0 - 524339, 98)" , "boundsS":"Rect(784, 636 - 880, 734)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"com.android.launcher2.AppIconView" , "package":"com.sec.android.app.launcher" , "text":"Internet" , "content":"Internet" , "focusable":true , "clickable":true , "longClickable":true , "enabled":true, "actions":"13" , "children":[]},
                                {"node":"5479" , "boundsP":"Rect(524234, 0 - 524330, 98)" , "boundsS":"Rect(912, 636 - 1008, 734)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"com.android.launcher2.AppIconView" , "package":"com.sec.android.app.launcher" , "text":"Samsung Apps" , "content":"Samsung Apps" , "focusable":true , "clickable":true , "longClickable":true , "enabled":true, "actions":"13" , "children":[]},
                                {"node":"5510" , "boundsP":"Rect(524243, 0 - 524339, 98)" , "boundsS":"Rect(1040, 636 - 1136, 734)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"com.android.launcher2.AppIconView" , "package":"com.sec.android.app.launcher" , "text":"Play Store" , "content":"Play Store" , "focusable":true , "clickable":true , "longClickable":true , "enabled":true, "actions":"13" , "children":[]}]}]}]},
                        {"node":"4766" , "boundsP":"Rect(0, 0 - 1280, 70)" , "boundsS":"Rect(0, 0 - 1280, 70)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"android.widget.FrameLayout" , "package":"com.sec.android.app.launcher" , "text":"null" , "content":"null" , "enabled":true, "actions":"12" ,
                          "children": [
                            {"node":"11090" , "boundsP":"Rect(0, 0 - 130, 70)" , "boundsS":"Rect(0, 0 - 130, 70)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"android.widget.LinearLayout" , "package":"com.sec.android.app.launcher" , "text":"null" , "content":"Pesquisar" , "clickable":true , "enabled":true, "actions":"12" ,
                              "children": [
                                {"node":"11152" , "boundsP":"Rect(0, 0 - 48, 70)" , "boundsS":"Rect(0, 0 - 48, 70)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"android.widget.ImageView" , "package":"com.sec.android.app.launcher" , "text":"null" , "content":"null" , "enabled":true, "actions":"12" , "children":[]},
                                {"node":"11183" , "boundsP":"Rect(0, 0 - 82, 70)" , "boundsS":"Rect(48, 0 - 130, 70)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"android.widget.ImageView" , "package":"com.sec.android.app.launcher" , "text":"null" , "content":"null" , "enabled":true, "actions":"12" , "children":[]}]},
                            {"node":"11121" , "boundsP":"Rect(0, 0 - 80, 70)" , "boundsS":"Rect(1200, 0 - 1280, 70)" , "package":"com.sec.android.app.launcher" , "package":"com.sec.android.app.launcher" , "class":"android.widget.ImageView" , "package":"com.sec.android.app.launcher" , "text":"null" , "content":"Apps" , "focusable":true , "clickable":true , "enabled":true, "actions":"13" , "children":[]}]}]}]}]}]}]} },
{}]}
```
For more details and any feedback please contact afrodrigues@fc.ul.pt

##Upcoming features:
- Cloud Storage
- Privacy Control
- Customizable encryption
- Web-based analyser with interaction replays 
- More Documentation =D
