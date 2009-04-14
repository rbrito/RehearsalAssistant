package urbanstew.RehearsalAssistant;

import java.util.Timer;
import java.util.TimerTask;

import urbanstew.RehearsalAssistant.Rehearsal.Sessions;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class SimpleProject extends ProjectBase
{

    public void onCreate(Bundle savedInstanceState)
    {
        setContentView(R.layout.simple);

        super.onCreate(savedInstanceState);
        
        findViewById(R.id.button).setOnClickListener(mClickListener);
        mCurrentTime = (TextView) findViewById(R.id.playback_time);

        // a simple project must have exactly one session
        Cursor cursor = getContentResolver().query(Sessions.CONTENT_URI, sessionsProjection, Sessions.PROJECT_ID + "=" + projectId(), null,
                Sessions.DEFAULT_SORT_ORDER);
        // add the session if it is not there
        if(cursor.getCount() < 1)
        {
        	Log.w("Rehearsal Assistant", "Inserting Session for Simple Project ID: " + projectId());
        	ContentValues values = new ContentValues();
        	values.put(Sessions.PROJECT_ID, projectId());
        	values.put(Sessions.TITLE, "Simple Session");
      		values.put(Sessions.START_TIME, 0);
        	getContentResolver().insert(Sessions.CONTENT_URI, values);
        	cursor.requery();
        }
        if(cursor.getCount() < 1)
        {
        	Log.w("Rehearsal Assistant", "Can't create session for simple project ID: " + projectId());
    		Toast.makeText(this, "There was a problem switching to simple mode.", Toast.LENGTH_LONG).show();
        	finish();
        }
        cursor.moveToFirst();
        
        sessionId = cursor.getLong(SESSIONS_ID);

        mSessionRecord = new SessionRecord(ContentUris.withAppendedId(Sessions.CONTENT_URI, sessionId), getContentResolver());
        
        cursor.close();
        
        mSessionPlayback = new SessionPlayback(savedInstanceState, this, ContentUris.withAppendedId(Sessions.CONTENT_URI, sessionId));
        
		mTimer.scheduleAtFixedRate(
				mCurrentTimeTask,
				0,
				100);
    }
    
    public void onDestroy()
    {
    	mTimer.cancel();
    	mSessionPlayback.onDestroy();

    	super.onDestroy();
    }
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    	mSessionPlayback.onRestoreInstanceState(savedInstanceState);
    }

    protected void onSaveInstanceState(Bundle outState)
    {
    	super.onSaveInstanceState(outState);
    	mSessionPlayback.onSaveInstanceState(outState);
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return mSessionPlayback.onCreateOptionsMenu(menu);
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
		if(item == mHelpMenuItem)
		{
			Request.notification(this, "Instructions", getResources().getString(R.string.simple_instructions));
			return true;
		}
    	return mSessionPlayback.onOptionsItemSelected(item);
    }
    
	public boolean onContextItemSelected(MenuItem item)
	{
		return mSessionPlayback.onContextItemSelected(item);
	}

    void startRecording()
    {
		mSessionRecord.startRecording();
		((android.widget.Button)findViewById(R.id.button)).setText(R.string.stop_recording);
    }
    
    void stopRecording()
    {
    	mSessionRecord.stopRecording();
    	((android.widget.Button)findViewById(R.id.button)).setText(R.string.record);
    }
    
    /** Called when the button is pushed */
    View.OnClickListener mClickListener = new View.OnClickListener()
    {
        public void onClick(View v)
        {
        	switch(mSessionRecord.state())
        	{
        	case STARTED:
        		startRecording();
	            break;
        	default:
            	stopRecording();
        	}
        }
    };
    
    TimerTask mCurrentTimeTask = new TimerTask()
	{
		public void run()
		{
			SimpleProject.this.runOnUiThread(new Runnable()
			{
				public void run()
				{
					if(mSessionRecord.state() == SessionRecord.State.RECORDING)
						mCurrentTime.setText(mSessionPlayback.playTimeFormatter().format(mSessionRecord.timeInRecording()));
				}
			});                            
		}
	};
    TextView mCurrentTime;
    Timer mTimer = new Timer();
    
    long sessionId;
    
    SessionRecord mSessionRecord;
    SessionPlayback mSessionPlayback;

}
