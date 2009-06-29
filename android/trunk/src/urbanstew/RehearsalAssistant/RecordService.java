package urbanstew.RehearsalAssistant;

import urbanstew.RehearsalAssistant.Rehearsal.Sessions;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class RecordService extends Service
{
	public void onCreate()
	{
        long sessionId = SimpleProject.getSessionId(getContentResolver(), SimpleProject.getProjectId(getContentResolver()));
        
        mSessionRecord = new SessionRecord(ContentUris.withAppendedId(Sessions.CONTENT_URI, sessionId), getContentResolver());
	}
	public void onDestroy()
	{
		if(mSessionRecord.state() == SessionRecord.State.RECORDING)
		{
			mSessionRecord.stopRecording();
			updateViews();
		}
		mSessionRecord.onDestroy();
	}
	public void onStart(Intent intent, int startId)
	{
		toggleRecording();
	}
	void toggleRecording()
	{		
		if(mSessionRecord.state() == SessionRecord.State.STARTED)
			mSessionRecord.startRecording();
		else
			mSessionRecord.stopRecording();
		
		updateViews();
	}
	
	void updateViews()
	{
		this.sendBroadcast
		(
			new Intent
			(
				mSessionRecord.state() == SessionRecord.State.STARTED
					?	"urbanstew.RehearsalAssistant.RecordWidget.update"
					:	"urbanstew.RehearsalAssistant.RecordWidget.update_recording"
			)
		);
	}
	
	public IBinder onBind(Intent arg0)
	{
		return mBinder;
	}

	SessionRecord mSessionRecord;
	
    /**
     * A secondary interface to the service.
     */
    private final IRecordService.Stub mBinder = new IRecordService.Stub() {
		public long getTimeInRecording() throws RemoteException
		{
			return mSessionRecord.timeInRecording();
		}
		public void stopRecording() throws RemoteException
		{
			if(mSessionRecord.state() == SessionRecord.State.RECORDING);
				RecordService.this.toggleRecording();
		}
		public int getState() throws RemoteException
		{
			return mSessionRecord.state().ordinal();
		}
		public void toggleRecording() throws RemoteException
		{
			RecordService.this.toggleRecording();
		}
		public int getMaxAmplitude() throws RemoteException
		{
			return mSessionRecord.getMaxAmplitude();
		}
    };
}
