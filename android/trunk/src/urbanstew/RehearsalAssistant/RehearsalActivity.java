package urbanstew.RehearsalAssistant;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;

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
    protected void setTitleDelayed(String title)
    {
    	mTimer = new Timer();
    	mTimer.schedule(new TitleTimerTask(this, title), 3000);
    }
    
    Timer mTimer = null;
}

class TitleTimerTask extends TimerTask
{
	TitleTimerTask(Activity activity, String title)
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
				}				
			}
		);
	}
	
	Activity mActivity;
	String mTitle;
}
