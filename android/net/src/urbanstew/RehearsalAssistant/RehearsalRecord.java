/*
 *  Author:
 *      Stjepan Rajko
 *      urbanSTEW
 *
 *  Copyright 2008,2009 Stjepan Rajko.
 *
 *  This file is part of the Android version of Rehearsal Assistant.
 *
 *  Rehearsal Assistant is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 3 of the License,
 *  or (at your option) any later version.
 *
 *  Rehearsal Assistant is distributed in the hope that it will be
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Rehearsal Assistant.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package urbanstew.RehearsalAssistant;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPort;
import com.illposed.osc.OSCPortOut;

/** The RehearsalRecord Activity handles recording annotations
 * 	for a particular project.
 */
public class RehearsalRecord extends RehearsalActivity
{
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
                
        setContentView(R.layout.record);
        
        findViewById(R.id.button).setOnClickListener(mClickListener);
        
        mCurrentTime = (TextView) findViewById(R.id.current_time);
        mLeftRecordIndicator = ((android.widget.ImageView)findViewById(R.id.left_record_indicator));
        mRightRecordIndicator = ((android.widget.ImageView)findViewById(R.id.right_record_indicator));

        mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        RehearsalAssistant.checkSdCard(this);

        mSessionRecord = new SessionRecord(getIntent().getData(), getContentResolver());

    	// Find out whether the session is already going
        if(mSessionRecord.state() == SessionRecord.State.STARTED)
        {
    		((TextView)findViewById(R.id.record_instructions)).setText(R.string.recording_instructions_started);
        	startedSession();
        }
        
        setTitle("Rehearsal Assistant - " + mSessionRecord.getSessionTitle());
        
		try
		{
			byte[] address = {(byte) 192, (byte) 168, 1, 101};
			mSender = new OSCPortOut(java.net.InetAddress.getByAddress(address), 12345);
		} catch (SocketException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public boolean onCreateOptionsMenu(Menu menu) {        
      	menu.add("Stop Session").setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        //super.onCreateOptionsMenu(menu);

        return true;
    }

    public void onResume()
    {
    	super.onResume();
    	
    	mCurrentTimeTask = new TimerTask()
    	{
    		public void run()
    		{
    			RehearsalRecord.this.runOnUiThread(new Runnable()
    			{
    				public void run()
    				{
    					if(mSessionRecord.state() != SessionRecord.State.READY)
    						mCurrentTime.setText(mFormatter.format(System.currentTimeMillis() - mSessionRecord.timeAtStart()));
    				}
    			});
    		}
    	};
		mTimer.scheduleAtFixedRate(
				mCurrentTimeTask,
				0,
				100);
    }

    public void onPause()
    {
    	mCurrentTimeTask.cancel();    	
    	super.onPause();    	
    }
    
    public void onDestroy()
    {
    	mTimer.cancel();
    	mSessionRecord.onDestroy();
    	super.onDestroy();
    }
    
    /* User interaction events */
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	switch(mSessionRecord.state())
    	{
    	case RECORDING:
    		stopRecording();
    	case STARTED:
    		stopSession();
    	}
		return true;
    }
    
    /** Called when the button is pushed */
    View.OnClickListener mClickListener = new View.OnClickListener()
    {
        public void onClick(View v)
        {
        	switch(mSessionRecord.state())
        	{
        	case READY:
        		startSession();
        		return;
        	case STARTED:
        		startRecording();
	            return;
        	default:
            	stopRecording();
        	}
        }
    };

    /** State changes. */
    void startSession()
    {
    	mSessionRecord.startSession();
    	
    	startedSession();
    	
    	if(mSender != null)
    	{
	    	try
			{
		        OSCMessage msg = new OSCMessage("/RehearsalAssistant/sessionStarted", null);
	            mSender.send(msg);
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
    
    void startedSession()
    {
		((android.widget.Button)findViewById(R.id.button)).setText(R.string.record);
		((android.widget.Button)findViewById(R.id.button)).setKeepScreenOn(true);
    }

    void stopSession()
    {
		mSessionRecord.stopSession();
		
		startActivity(new Intent(Intent.ACTION_VIEW, getIntent().getData()));
		finish();

    	if(mSender != null)
    	{
	    	try
			{
		        OSCMessage msg = new OSCMessage("/RehearsalAssistant/sessionStopped", null);
	            mSender.send(msg);
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
    
    void startRecording()
    {
    	mSessionRecord.startRecording();

        ((android.widget.Button)findViewById(R.id.button)).setText(R.string.stop_recording);
        mLeftRecordIndicator.setVisibility(View.VISIBLE);
        mRightRecordIndicator.setVisibility(View.VISIBLE);
    }
    
    void stopRecording()
    {
    	mSessionRecord.stopRecording();

    	((android.widget.Button)findViewById(R.id.button)).setText(R.string.record);
    	mLeftRecordIndicator.setVisibility(View.INVISIBLE);
    	mRightRecordIndicator.setVisibility(View.INVISIBLE);
    }
    
    TimerTask mCurrentTimeTask;

    TextView mCurrentTime;
    SimpleDateFormat mFormatter = new SimpleDateFormat("HH:mm:ss");
    
    Timer mTimer = new Timer();

	android.widget.ImageView mLeftRecordIndicator, mRightRecordIndicator;
	
    RehearsalData data;
    
    SessionRecord mSessionRecord = null;

    long project_id;
    
    OSCPortOut mSender = null;

}