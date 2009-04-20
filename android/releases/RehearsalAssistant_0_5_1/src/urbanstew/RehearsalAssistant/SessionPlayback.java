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

import java.util.Date;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import urbanstew.RehearsalAssistant.Rehearsal.Annotations;
import urbanstew.RehearsalAssistant.Rehearsal.Sessions;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.media.AudioManager;
import android.media.MediaPlayer;

/** The RehearsalPlayback Activity provides playback access for
 * 	annotations in a particular project.
 */
public class SessionPlayback
{
	private static final int ANNOTATIONS_ID = 0;
	private static final int ANNOTATIONS_START_TIME = 1;
	private static final int ANNOTATIONS_END_TIME = 2;
	private static final int ANNOTATIONS_FILE_NAME = 3;
	private static final int ANNOTATIONS_LABEL = 4;
	private static final int ANNOTATIONS_VIEWED = 5;
	
	static final int SESSIONS_ID = 0;
	static final int SESSIONS_TITLE = 1;
	static final int SESSIONS_START_TIME = 2;
	static final int SESSIONS_END_TIME = 3;
	
    /** Called when the activity is first created. */
    public SessionPlayback(Bundle savedInstanceState, Activity activity, Uri uri)
    {
    	mActivity = activity;
    	        
        String[] projection =
        {
        	Annotations._ID,
        	Annotations.START_TIME,
        	Annotations.END_TIME,
        	Annotations.FILE_NAME,
        	Annotations.LABEL,
        	Annotations.VIEWED
        };
        String[] sessionProjection =
        {
        	Sessions._ID,
        	Sessions.TITLE,
        	Sessions.START_TIME,
        	Sessions.END_TIME
        };

        String session_id = uri.getPathSegments().get(1);

        ContentResolver resolver = activity.getContentResolver();
        mSessionCursor = resolver.query(Sessions.CONTENT_URI, sessionProjection, Sessions._ID + "=" + session_id, null, Sessions.DEFAULT_SORT_ORDER);
        mSessionCursor.moveToFirst();
        
        if(mSessionCursor.getLong(SESSIONS_START_TIME) != 0)
        {
        	formatter = new SimpleDateFormat("HH:mm:ss");
        	formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        	mSessionTiming = true;
        }
        else
        {
        	formatter = DateFormat.getDateTimeInstance();
        	mSessionTiming = false;
        }
        
        mAnnotationsCursor = resolver.query(Annotations.CONTENT_URI, projection, Annotations.SESSION_ID + "=" + session_id + " AND " + Annotations.END_TIME + " IS NOT NULL", null,
                Annotations.DEFAULT_SORT_ORDER);
        Log.w("RehearsalAssistant", "Read " + mAnnotationsCursor.getCount() + " annotations.");

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(activity.getApplication(), R.layout.annotationslist_item, mAnnotationsCursor,
                new String[] { Annotations.START_TIME}, new int[] { android.R.id.text1 });
        
        adapter.setViewBinder(new ViewBinder()
        {
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex)
			{
				TextView v = (TextView)view;
				v.setText(formatter.format(new Date(cursor.getLong(columnIndex))) + " " + cursor.getString(ANNOTATIONS_LABEL), TextView.BufferType.SPANNABLE);
				if(cursor.getInt(ANNOTATIONS_VIEWED) == 0)
				{
					v.setTextAppearance(mActivity.getApplicationContext(), android.R.attr.textAppearanceLarge);
					Spannable str = (Spannable) v.getText();
					str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				return true;
			}
        });
        ListView list = (ListView)mActivity.findViewById(R.id.annotation_list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(mSelectedListener);
        list.setOnCreateContextMenuListener(mCreateContextMenuListener);	
                
        AudioManager audioManager = (AudioManager) mActivity.getApplication().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0)
      		Toast.makeText(mActivity, "Warning: music volume is muted.  To increase the volume, use the volume adjustment buttons while playing a recording.", Toast.LENGTH_LONG).show();
        
        mCurrentTime = (TextView) mActivity.findViewById(R.id.playback_time);
        mPlayTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		mTimer.scheduleAtFixedRate(
				mCurrentTimeTask,
				0,
				100);
    }

    public void onDestroy()
    {
    	mTimer.cancel();
    	mAnnotationsCursor.close();
    	mSessionCursor.close();
    	if(mAnnotationLabelDialog != null && mAnnotationLabelDialog.isShowing())
    		mAnnotationLabelDialog.dismiss();
    }
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
		// restore label edit dialog if needed
		if(savedInstanceState.getBoolean("annotationLabelDialogShown"))
		{
			displayAnnotationLabelDialog
				(
					savedInstanceState.getString("annotationLabelDialogText"),
					savedInstanceState.getLong("annotationLabelDialogShownId")
				);
		}
    }

    protected void onSaveInstanceState(Bundle outState)
    {
    	if(mAnnotationLabelDialog != null && mAnnotationLabelDialog.isShowing())
    	{
    		outState.putBoolean("annotationLabelDialogShown", true);
    		outState.putString
    			(
    				"annotationLabelDialogText",
    				((EditText)mAnnotationLabelDialog.findViewById(R.id.annotation_label_text)).getText().toString()
    			);
    		outState.putLong("annotationLabelDialogShownId", mAnnotationLabelId);
    	}
    }
    
    public boolean onCreateOptionsMenu(Menu menu)
    {
      	menu.add("E-Mail Session").setIcon(android.R.drawable.ic_dialog_email);
        return true;
    }
    
    boolean createSessionArchive(String archiveFilename)
    {
        byte[] buffer = new byte[1024];
        
        try
        {
            ZipOutputStream archive = new ZipOutputStream(new FileOutputStream(archiveFilename));
        
            for(mAnnotationsCursor.moveToFirst(); !mAnnotationsCursor.isAfterLast(); mAnnotationsCursor.moveToNext())
            {
                FileInputStream in = new FileInputStream(mAnnotationsCursor.getString(ANNOTATIONS_FILE_NAME));
                archive.putNextEntry(new ZipEntry("audio" + (mAnnotationsCursor.getPosition() + 1) + ".3gpp"));
        
                int length;
                while ((length = in.read(buffer)) > 0)
                	archive.write(buffer, 0, length);
        
                archive.closeEntry();
                in.close();
            }
        
            // Complete the ZIP file
            archive.close();
        } catch (IOException e)
        {
    		Toast.makeText(mActivity, "Problem creating ZIP archive: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        	return false;
        }
        return true;
    }
    String annotationTextInfo(String label)
    {
        String text = label + " " + (mAnnotationsCursor.getPosition() + 1) + "\n";
        text += " label: " + mAnnotationsCursor.getString(ANNOTATIONS_LABEL) + "\n";
        text += " start time: " + formatter.format(new Date(mAnnotationsCursor.getLong(ANNOTATIONS_START_TIME))) + "\n";
        text += " end time: " + formatter.format(new Date(mAnnotationsCursor.getLong(ANNOTATIONS_END_TIME))) + "\n";
        text += " filename: " + mAnnotationsCursor.getString(ANNOTATIONS_FILE_NAME) + "\n\n";

        return text;
    }
    void sendEmail(boolean wholeSession)
    {
        Intent emailSession = new Intent(Intent.ACTION_SEND);
        if(wholeSession)
        	emailSession.putExtra(Intent.EXTRA_SUBJECT, "Rehearsal Assistant session \"" + mSessionCursor.getString(1) + "\"");
        else
        	emailSession.putExtra(Intent.EXTRA_SUBJECT, "Rehearsal Assistant recording \"" + formatter.format(new Date(mAnnotationsCursor.getLong(ANNOTATIONS_START_TIME))) + "\"");
        
    	String messageText = new String();
    	if(wholeSession)
    	{
	    	messageText += "Session title: " + mSessionCursor.getString(SESSIONS_TITLE) + "\n";
	    	messageText += "Session start time: " + DateFormat.getDateTimeInstance().format(new Date(mSessionCursor.getLong(SESSIONS_START_TIME))) + "\n";
	    	messageText += "Session end time: " + DateFormat.getDateTimeInstance().format(new Date(mSessionCursor.getLong(SESSIONS_END_TIME))) + "\n\n";
    	}
    	if(wholeSession)
    	{
            String archiveFilename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/rehearsal/session.zip";
            // If there are no annotations we don't need an archive
            if(mAnnotationsCursor.getCount() == 0 || createSessionArchive(archiveFilename))
            {    	    	
    	    	// If there are no annotations, say so.
    	    	if(mAnnotationsCursor.getCount() == 0)
    	    	{
    	    		messageText += mActivity.getResources().getString(R.string.no_annotations) + "\n";
    	    		emailSession.setType("message/rfc822");
    	    	}
    	    	else // otherwise, attach the file.
    	    	{
    		    	emailSession.putExtra(Intent.EXTRA_STREAM, Uri.parse ("file://" + archiveFilename));
    		    	emailSession.setType("application/zip");
    	    	}
            }
        	// Add annotation information
            for(mAnnotationsCursor.moveToFirst(); !mAnnotationsCursor.isAfterLast(); mAnnotationsCursor.moveToNext())
            	messageText += annotationTextInfo("Annotation");
    	}
    	else
    	{
	    	emailSession.putExtra(Intent.EXTRA_STREAM, Uri.parse ("file://" + mAnnotationsCursor.getString(ANNOTATIONS_FILE_NAME)));
	    	emailSession.setType("audio/3gpp");

    		messageText += annotationTextInfo("Recording");
    	}
        emailSession.putExtra(Intent.EXTRA_TEXT, messageText);
    	
      	emailSession = Intent.createChooser(emailSession, wholeSession ? "E-Mail Session" : "E-Mail Recording");
      	
      	try
      	{
      		mActivity.startActivity(emailSession);
      	} catch (ActivityNotFoundException e)
      	{
      		Toast.makeText(mActivity, "Unable to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
      	}
    }
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	sendEmail(true);
		return true;
    }
    
    public static final int MENU_ITEM_PLAYBACK = Menu.FIRST;
    public static final int MENU_ITEM_LABEL = Menu.FIRST+1;
    public static final int MENU_ITEM_EMAIL = Menu.FIRST+2;
    public static final int MENU_ITEM_DELETE = Menu.FIRST+3;

    View.OnCreateContextMenuListener mCreateContextMenuListener = new View.OnCreateContextMenuListener()
    {
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo)
		{
			menu.add(Menu.NONE, MENU_ITEM_PLAYBACK, 0, "play");
			menu.add(Menu.NONE, MENU_ITEM_LABEL, 1, "edit label");
			menu.add(Menu.NONE, MENU_ITEM_EMAIL, 2, "e-mail");
			menu.add(Menu.NONE, MENU_ITEM_DELETE, 3, "delete");
		}
    	
    };
    
    void displayAnnotationLabelDialog(String content, long id)
    {
    	mAnnotationLabelId = id;
        LayoutInflater factory = LayoutInflater.from(mActivity);
        final View textEntryView = factory.inflate(R.layout.alert_annotation_label_entry, null);
        mAnnotationLabelDialog = new AlertDialog.Builder(mActivity)
            .setView(textEntryView)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	EditText label = (EditText)mAnnotationLabelDialog.findViewById(R.id.annotation_label_text);

                	ContentValues values = new ContentValues();
                	values.put(Annotations.LABEL, label.getText().toString());
                	mActivity.getContentResolver().update(ContentUris.withAppendedId(Annotations.CONTENT_URI,mAnnotationLabelId), values, null, null);
            		mAnnotationLabelDialog = null;
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
            		mAnnotationLabelDialog = null;
                }
            })
            .create();
        mAnnotationLabelDialog.show();
    	EditText label = (EditText)mAnnotationLabelDialog.findViewById(R.id.annotation_label_text);
    	label.setText(content);
    }

	public boolean onContextItemSelected(MenuItem item)
	{
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e("Rehearsal Assistant", "bad menuInfo", e);
            return false;
        }

        switch(item.getItemId())
        {
        case MENU_ITEM_PLAYBACK:
        	playItem(info.position);
        	break;
        case MENU_ITEM_LABEL:
        	mAnnotationsCursor.moveToPosition(info.position);
        	displayAnnotationLabelDialog(mAnnotationsCursor.getString(ANNOTATIONS_LABEL), mAnnotationsCursor.getLong(ANNOTATIONS_ID));
        	break;
        case MENU_ITEM_EMAIL:
        	mAnnotationsCursor.moveToPosition(info.position);
        	sendEmail(false);
        	break;
        case MENU_ITEM_DELETE:
        	mAnnotationsCursor.moveToPosition(info.position);
        	mActivity.getContentResolver().delete
        	(
        		ContentUris.withAppendedId(Annotations.CONTENT_URI, mAnnotationsCursor.getLong(ANNOTATIONS_ID)),
        		null,
        		null
        	);
        	break;
        }
        return true;
	}

	void playItem(int position)
	{
		mAnnotationsCursor.moveToPosition(position);
		mActiveAnnotationStartTime = mAnnotationsCursor.getLong(ANNOTATIONS_START_TIME);
		
		ContentValues values = new ContentValues();
    	values.put(Annotations.VIEWED, true);
    	mActivity.getContentResolver().update(ContentUris.withAppendedId(Annotations.CONTENT_URI,mAnnotationsCursor.getLong(ANNOTATIONS_ID)), values, null, null);

		String state = android.os.Environment.getExternalStorageState();
    	if(!state.equals(android.os.Environment.MEDIA_MOUNTED)
    			&& !state.equals(android.os.Environment.MEDIA_MOUNTED_READ_ONLY))
    	{
        	Request.notification(mActivity,
            		"Media Missing",
            		"Your external media (e.g., sdcard) is not mounted (it is " + state + ").  Rehearsal Assistant cannot access the saved file."
            	);
        	return;
    	}
    	
    	if(player != null)
    	{
    		player.stop();
    		player.release();
    	}
        try
        {
        	player = new MediaPlayer();
        	player.setDataSource(mAnnotationsCursor.getString(ANNOTATIONS_FILE_NAME));
        	player.prepare();
        	player.start();
        }
        catch(java.io.IOException e)
        {
        	if(e.getMessage()!=null)
        		Toast.makeText(mActivity, e.getMessage(),
        				Toast.LENGTH_SHORT).show();

        }
	}
    /** Called when the user selects a list item. */
    AdapterView.OnItemClickListener mSelectedListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id)
        {
			playItem(position);
        }
    };
    
    Timer mTimer = new Timer();
    TimerTask mCurrentTimeTask = new TimerTask()
	{
		public void run()
		{
			mActivity.runOnUiThread(new Runnable()
			{
				public void run()
				{
					if(player != null && player.isPlaying())
						if(mSessionTiming)
							mCurrentTime.setText(formatter.format(player.getCurrentPosition() + mActiveAnnotationStartTime));
						else
							mCurrentTime.setText(mPlayTimeFormatter.format(player.getCurrentPosition()));
				}
			});                                
		}
	};
	
	public Cursor annotationsCursor()
	{	return mAnnotationsCursor; }

	public Cursor sessionCursor()
	{	return mSessionCursor; }
	
	DateFormat playTimeFormatter()
	{	return mPlayTimeFormatter; }
	
	Activity mActivity;
	
    TextView mCurrentTime;

    Cursor mAnnotationsCursor;
    Cursor mSessionCursor;
    MediaPlayer player = null;
    List<String> mStrings = new LinkedList<String>();
    ArrayAdapter<String> listAdapter;
    
    SimpleDateFormat mPlayTimeFormatter = new SimpleDateFormat("HH:mm:ss");
    DateFormat formatter;
    
    long mActiveAnnotationStartTime = 0;
    AlertDialog mAnnotationLabelDialog = null;
    long mAnnotationLabelId;
    
    boolean mSessionTiming;
}