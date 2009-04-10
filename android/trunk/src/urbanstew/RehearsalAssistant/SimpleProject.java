package urbanstew.RehearsalAssistant;

import urbanstew.RehearsalAssistant.Rehearsal.Sessions;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

public class SimpleProject extends ProjectBase
{

    public void onCreate(Bundle savedInstanceState)
    {
        setContentView(R.layout.simple);

        super.onCreate(savedInstanceState);
        
        findViewById(R.id.button).setOnClickListener(mClickListener);
        
        // a simple project must have exactly one session
        Cursor cursor = managedQuery(Sessions.CONTENT_URI, sessionsProjection, Sessions.PROJECT_ID + "=" + projectId(), null,
                Sessions.DEFAULT_SORT_ORDER);
        // add the session if it is not there
        if(cursor.getCount() < 1)
        {
        	ContentValues values = new ContentValues();
        	values.put(Sessions.PROJECT_ID, projectId());
        	values.put(Sessions.TITLE, "Simple Session");
      		values.put(Sessions.START_TIME, 0);
        	getContentResolver().insert(Sessions.CONTENT_URI, values);
        }
        cursor.moveToFirst();
        
        mSessionRecord = new SessionRecord(getIntent().getData(), getContentResolver());
        
        sessionId = cursor.getLong(SESSIONS_ID);        
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

    long sessionId;
    
    SessionRecord mSessionRecord;

}
