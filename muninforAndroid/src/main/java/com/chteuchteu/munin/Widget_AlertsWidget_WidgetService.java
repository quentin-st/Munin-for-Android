package com.chteuchteu.munin;


import android.content.Intent;
import android.widget.RemoteViewsService;

public class Widget_AlertsWidget_WidgetService extends RemoteViewsService {
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return (new Widget_AlertsWidget_ViewsFactory(getApplicationContext(), intent));
	}
}
