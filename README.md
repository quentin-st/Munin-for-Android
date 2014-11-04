# Munin for Android #

## Objects structure *(obj)* ##
* MuninMaster
* MuninServer
* MuninPlugin

* Grid
* GridItem
* Label
* SearchResult
* AlertsWidget
* GraphWidget

## Activities *(ui)* ##
* Activity_About
* Activity_Alerts
* Activity_AlertsPluginSelection
* Activity_GoPremium
* Activity_GraphView
* Activity_Grid
* Activity_Grids
* Activity_Label
* Activity_Labels
* Activity_Main
* Activity_Notifications
* Activity_Plugins
* Activity_Server
* Activity_Servers
* Activity_ServersEdit
* Activity_Settings
* MuninActivity *- Every class extends MuninActivity to avoid code redundancy *

## Helpers & Utils classes *(hlpr)* ##
* BillingService
* DatabaseHelper *- SQLite database interface*
* DigestUtils *- Apache digest auth util class*
* DocumentationHelper
* DrawerHelper
* GridDownloadHelper *- Grids simultaneous downloads helper*
* ImportExportHelper
* JSONHelper *- JSON import/export - not used for now*
* MediaScannerUtil *- warns Android system about a new picture on the filesystem*
* SQLite *- SQLite database top-level methods (saveServers(), ...)*
* Util *- Generic util methods*

## Adapters ##
* Adapter_ExpandableListView.java
* Adapter_GraphView.java
* Adapter_IconList.java
* Adapter_SeparatedList.java

## Widgets *(wdget)* ##
* Widget_AlertsWidget_Configure
* Widget_AlertsWidget_ViewsFactory
* Widget_AlertsWidget_WidgetProvider
* Widget_AlertsWidget_WidgetService
* Widget_GraphWidget_Configure
* Widget_GraphWidget_WidgetProvider

## Others ##
* BootReceiver
* CustomSSLFactory
* MuninFoo *- App Singleton*
* Service_Notifications
* HTTPResponse

## Custom Exceptions ##
* ImportExportWebServiceException
* NullMuninFooException

## WebServices ##
### Import / Export ###
Import/export servers configuration  
Target : munin-for-android.com/ws/importExport.php  
Generic Import/Export class : hlpr / ImportExportHelper.java  
Read more : importExport.php
