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
public class RehearsalPlayback extends Activity
{
	private static final int ANNOTATIONS_ID = 0;
	private static final int ANNOTATIONS_START_TIME = 1;
	private static final int ANNOTATIONS_END_TIME = 2;
	private static final int ANNOTATIONS_FILE_NAME = 3;
	private static final int ANNOTATIONS_LABEL = 4;
	private static final int ANNOTATIONS_VIEWED = 5;
	
	private static final int SESSIONS_ID = 0;
	private static final int SESSIONS_TITLE = 1;
	private static final int SESSIONS_START_TIME = 2;
	private static final int SESSIONS_END_TIME = 3;
	
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.playback);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        
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
        String session_id = getIntent().getData().getPathSegments().get(1);

        mSessionCursor = managedQuery(Sessions.CONTENT_URI, sessionProjection, Sessions._ID + "=" + session_id, null, Sessions.DEFAULT_SORT_ORDER);
        mSessionCursor.moveToFirst();
        
        mAnnotationsCursor = managedQuery(Annotations.CONTENT_URI, projection, Annotations.SESSION_ID + "=" + session_id, null,
                Annotations.DEFAULT_SORT_ORDER);
        Log.w("RehearsalAssistant", "Read " + mAnnotationsCursor.getCount() + " annotations.");

        if(mAnnotationsCursor.getCount() == 0)
        {
        	TextView instructions = (TextView)findViewById(R.id.no_annotations);
        	instructions.setText(R.string.no_annotations);
        }

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getApplication(), R.layout.annotationslist_item, mAnnotationsCursor,
                new String[] { Annotations.START_TIME}, new int[] { android.R.id.text1 });
        
        adapter.setCursorToStringConverter(new CursorToStringConverter()
        {
			public CharSequence convertToString(Cursor cursor)
			{
				return formatter.format(cursor.getString(0));
			}	
        });
        
        adapter.setViewBinder(new ViewBinder()
        {
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex)
			{
				TextView v = (TextView)view;
				v.setText(formatter.format(new Date(cursor.getInt(columnIndex))) + " " + cursor.getString(ANNOTATIONS_LABEL), TextView.BufferType.SPANNABLE);
				if(cursor.getInt(ANNOTATIONS_VIEWED) == 0)
				{
					v.setTextAppearance(getApplicationContext(), android.R.attr.textAppearanceLarge);
					Spannable str = (Spannable) v.getText();
					str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				return true;
			}
        });
        ListView list = (ListView)findViewById(R.id.annotation_list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(mSelectedListener);
        list.setOnCreateContextMenuListener(mCreateContextMenuListener);	
                
        AudioManager audioManager = (AudioManager) this.getApplication().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0)
      		Toast.makeText(this, "Warning: music volume is muted.  To increase the volume, use the volume adjustment buttons while playing a recording.", Toast.LENGTH_LONG).show();
        
        mCurrentTime = (TextView) findViewById(R.id.playback_time);
		mTimer.scheduleAtFixedRate(
				mCurrentTimeTask,
				0,
				100);
    }

    public void onDestroy()
    {
    	super.onDestroy();
    	mTimer.cancel();
    	mSessionCursor.close();
    	mAnnotationsCursor.close();
    }
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
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
    
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
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
    		Toast.makeText(this, "Problem creating ZIP archive: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        	return false;
        }
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) 
    {                
        String archiveFilename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/rehearsal/session.zip";
        // If there are no annotations we don't need an archive
        if(mAnnotationsCursor.getCount() == 0 || createSessionArchive(archiveFilename))
        {
	        Intent emailSession = new Intent(Intent.ACTION_SEND);
	        emailSession.putExtra(Intent.EXTRA_SUBJECT, "Rehearsal Assistant session \"" + mSessionCursor.getString(1) + "\"");
	        
	    	String messageText = new String();
	    	messageText += "Session title: " + mSessionCursor.getString(SESSIONS_TITLE) + "\n";
	    	messageText += "Session start time: " + DateFormat.getDateTimeInstance().format(new Date(mSessionCursor.getLong(SESSIONS_START_TIME))) + "\n";
	    	messageText += "Session end time: " + DateFormat.getDateTimeInstance().format(new Date(mSessionCursor.getLong(SESSIONS_END_TIME))) + "\n\n";
	    	
	    	// If there are no annotations, say so.
	    	if(mAnnotationsCursor.getCount() == 0)
	    	{
	    		messageText += R.string.no_annotations + "\n";
	    		emailSession.setType("message/rfc822");
	    	}
	    	else // otherwise, attach the file.
	    	{
		    	emailSession.putExtra(Intent.EXTRA_STREAM, Uri.parse ("file://" + archiveFilename));
		    	emailSession.setType("application/zip");
	    	}
	    	// Add annotation information
            for(mAnnotationsCursor.moveToFirst(); !mAnnotationsCursor.isAfterLast(); mAnnotationsCursor.moveToNext())
            {
            	messageText += "Annotation " + (mAnnotationsCursor.getPosition() + 1) + "\n";
            	messageText += " label: " + mAnnotationsCursor.getString(ANNOTATIONS_LABEL) + "\n";
            	messageText += " start time: " + formatter.format(new Date(mAnnotationsCursor.getLong(ANNOTATIONS_START_TIME))) + "\n";
            	messageText += " end time: " + formatter.format(new Date(mAnnotationsCursor.getLong(ANNOTATIONS_END_TIME))) + "\n";
            	messageText += " filename: " + mAnnotationsCursor.getString(ANNOTATIONS_FILE_NAME) + "\n\n";
            }
            emailSession.putExtra(Intent.EXTRA_TEXT, messageText);
	
	      	emailSession = Intent.createChooser(emailSession, "E-Mail Session");
	      	
	      	try
	      	{
	      		startActivity(emailSession);
	      	} catch (ActivityNotFoundException e)
	      	{
	      		Toast.makeText(this, "Unable to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
	      	}
        }
		return true;
    }
    
    public static final int MENU_ITEM_PLAYBACK = Menu.FIRST;
    public static final int MENU_ITEM_LABEL = Menu.FIRST+1;

    View.OnCreateContextMenuListener mCreateContextMenuListener = new View.OnCreateContextMenuListener()
    {
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo)
		{
			menu.add(Menu.NONE, MENU_ITEM_PLAYBACK, 0, "play");
			menu.add(Menu.NONE, MENU_ITEM_LABEL, 1, "edit label");
		}
    	
    };
    
    void displayAnnotationLabelDialog(String content, long id)
    {
    	mAnnotationLabelId = id;
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.alert_annotation_label_entry, null);
        mAnnotationLabelDialog = new AlertDialog.Builder(this)
            .setView(textEntryView)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	EditText label = (EditText)mAnnotationLabelDialog.findViewById(R.id.annotation_label_text);

                	ContentValues values = new ContentValues();
                	values.put(Annotations.LABEL, label.getText().toString());
            		getContentResolver().update(ContentUris.withAppendedId(Annotations.CONTENT_URI,mAnnotationLabelId), values, null, null);
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

        if(item.getItemId() == MENU_ITEM_PLAYBACK)
        	playItem(info.position);
        else
        {
        	mAnnotationsCursor.moveToPosition(info.position);
        	displayAnnotationLabelDialog(mAnnotationsCursor.getString(ANNOTATIONS_LABEL), mAnnotationsCursor.getLong(ANNOTATIONS_ID));
        }
        return true;
	}

	void playItem(int position)
	{
		mAnnotationsCursor.moveToPosition(position);
		mActiveAnnotationStartTime = mAnnotationsCursor.getLong(ANNOTATIONS_START_TIME);
		
		ContentValues values = new ContentValues();
    	values.put(Annotations.VIEWED, true);
		getContentResolver().update(ContentUris.withAppendedId(Annotations.CONTENT_URI,mAnnotationsCursor.getLong(ANNOTATIONS_ID)), values, null, null);

		String state = android.os.Environment.getExternalStorageState();
    	if(!state.equals(android.os.Environment.MEDIA_MOUNTED)
    			&& !state.equals(android.os.Environment.MEDIA_MOUNTED_READ_ONLY))
    	{
        	Request.notification(RehearsalPlayback.this,
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
        		Toast.makeText(RehearsalPlayback.this, e.getMessage(),
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
			RehearsalPlayback.this.runOnUiThread(new Runnable()
			{
				public void run()
				{
					if(player != null)
						mCurrentTime.setText(formatter.format(player.getCurrentPosition() + mActiveAnnotationStartTime));
				}
			});                                
		}
	};

    TextView mCurrentTime;

    Cursor mAnnotationsCursor;
    Cursor mSessionCursor;
    MediaPlayer player = null;
    List<String> mStrings = new LinkedList<String>();
    ArrayAdapter<String> listAdapter;
    
    SimpleDateFormat playTimeFormatter = new SimpleDateFormat("mm:ss");
    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    
    long mActiveAnnotationStartTime = 0;
    AlertDialog mAnnotationLabelDialog = null;
    long mAnnotationLabelId;
}