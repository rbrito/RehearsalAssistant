package urbanstew.RehearsalAssistant;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class RehearsalActivity extends Activity
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setTitle(getResources().getString(R.string.about));
    }
	
    public void onDestroy()
    {
    	if(mTimer != null)
    		mTimer.cancel();
    	super.onDestroy();
    }
    
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        
        mSettingsMenuItem = menu.add(R.string.settings).setIcon(android.R.drawable.ic_menu_preferences);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	if(item == mSettingsMenuItem)
    	{
    	    startActivity(new Intent(getApplication(), SettingsActivity.class));
    		return true;
    	}
    	else
    		return false;
    }
    
    protected void setTitleDelayed(String title)
    {
    	mTimer = new Timer();
    	mTimer.schedule(mTimerTask = new TitleTimerTask(this, title), 3000);
    }
    
    public void setTitle(CharSequence title)
    {
    	if(mTimerTask != null && !mTimerTask.hasRun())
    		mTimer.cancel();
    	super.setTitle(title);
    }
    public CharSequence finalTitle()
    {
    	if(mTimerTask == null)
    		return getTitle();
    	return mTimerTask.title();
    }
    
    Timer mTimer = null;
    TitleTimerTask mTimerTask = null;
    protected MenuItem mSettingsMenuItem; 
}

class TitleTimerTask extends TimerTask
{
	TitleTimerTask(Activity activity, CharSequence title)
	{
		mActivity = activity;
		mTitle = title;
	}
	public void run()
	{
		mActivity.runOnUiThread
		(
			new Runnable()
			{
				public void run()
				{
					mActivity.setTitle(mTitle);
					mHasRun = true;
				}				
			}
		);
	}
	
	public boolean hasRun()
	{	return mHasRun; }
	
	public CharSequence title()
	{	return mTitle; }
	
	public void setTitle(CharSequence title)
	{
		mTitle = title;
	}
	
	Activity mActivity;
	CharSequence mTitle;
	boolean mHasRun;
}
