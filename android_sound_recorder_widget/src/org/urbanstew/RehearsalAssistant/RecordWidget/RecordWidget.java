package org.urbanstew.RehearsalAssistant.RecordWidget;

import org.urbanstew.RehearsalAssistant.RecordWidget.R;
import android.app.PendingIntent;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

public class RecordWidget extends AppWidgetProvider
{
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds)
    {
    	updateWidget(context, false);
    }
    
    public void onReceive(Context context, Intent intent)
    {
    	if(intent.getAction().equals("org.urbanstew.RehearsalAssistant.RecordWidget.update")
    			|| intent.getAction().equals("urbanstew.RehearsalAssistant.RecordWidget.update"))
    		updateWidget(context, false);
    	else if(intent.getAction().equals("org.urbanstew.RehearsalAssistant.RecordWidget.update_recording")
    			|| intent.getAction().equals("urbanstew.RehearsalAssistant.RecordWidget.update_recording"))
    		updateWidget(context, true);
    	else if(intent.getAction().equals("org.urbanstew.RehearsalAssistant.RecordWidget.record"))
    	{
    		if(null == context.startService(new Intent("urbanstew.RehearsalAssistant.record")))
    			missingRehearsalAssistant(context);
    	}
      	else if(intent.getAction().equals("org.urbanstew.RehearsalAssistant.RecordWidget.simple_mode"))
      	{
      		try
      		{
      			context.startActivity(new Intent("urbanstew.RehearsalAssistant.simple_mode").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      		} catch (ActivityNotFoundException e)
      		{
      			missingRehearsalAssistant(context);
      		}
      	}
    	else
        	super.onReceive(context, intent);
    }
    
    void updateWidget(Context context, boolean recording)
    {
    	RemoteViews updateViews = new RemoteViews(context.getPackageName(), recording ? R.layout.widget_recording : R.layout.widget);
    	updateViews.setOnClickPendingIntent
    	(
    		R.id.record_button,
    		PendingIntent.getBroadcast
    		(
    			context,
    			0,
    			new Intent("org.urbanstew.RehearsalAssistant.RecordWidget.record"),
    			0
    		)
    	);
    	updateViews.setOnClickPendingIntent
    	(
    		R.id.play_button,
    		PendingIntent.getBroadcast
    		(
    			context,
    			0,
    			new Intent("org.urbanstew.RehearsalAssistant.RecordWidget.simple_mode"),
    			0
    		)
    	);

        // Push update for this widget to the home screen
        ComponentName thisWidget = new ComponentName(context, RecordWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(thisWidget, updateViews);

    }
    
    void missingRehearsalAssistant(Context context)
    {
		try
		{
			context.startActivity
            (
            	new Intent
            	(
            		Intent.ACTION_VIEW,
            		Uri.parse("market://search?q=pname:urbanstew.RehearsalAssistant")
            	).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            );
		} catch (ActivityNotFoundException e)
		{
		}

    	Toast.makeText(context, "This widget requires the FREE 'Rehearsal Assistant / VoiceRecrd' app.  Please download it through the Market", Toast.LENGTH_LONG).show();
    }
}