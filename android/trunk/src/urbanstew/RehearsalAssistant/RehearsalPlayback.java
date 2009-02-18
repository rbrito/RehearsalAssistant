/*
 *  Author:
 *      Stjepan Rajko
 *      urbanSTEW
 *
 *  Copyright 2008 Stjepan Rajko.
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import urbanstew.RehearsalAssistant.Rehearsal.Annotations;
import urbanstew.RehearsalAssistant.Rehearsal.Sessions;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.media.MediaPlayer;

/** The RehearsalPlayback Activity provides playback access for
 * 	annotations in a particular project.
 */
public class RehearsalPlayback extends Activity
{
	static int ANNOTATIONS_ID = 0;
	static int ANNOTATIONS_START_TIME = 1;
	static int ANNOTATIONS_END_TIME = 2;
	static int ANNOTATIONS_FILE_NAME = 3;
	static int ANNOTATIONS_VIEWED = 4;
	
	static int SESSIONS_ID = 0;
	static int SESSIONS_TITLE = 1;
	static int SESSIONS_START_TIME = 2;
	static int SESSIONS_END_TIME = 3;
	
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
				v.setText(formatter.format(new Date(cursor.getInt(columnIndex))), TextView.BufferType.SPANNABLE);
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
        //list.setOnClickListener()
        
        
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
            	messageText += " start time: " + formatter.format(new Date(mAnnotationsCursor.getLong(ANNOTATIONS_START_TIME))) + "\n";
//            	messageText += " ending at " + formatter.format(new Date(mAnnotationsCursor.getLong(ANNOTATIONS_END_TIME))) + "\n";
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

    /** Called when the user selects a list item. */
    AdapterView.OnItemClickListener mSelectedListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id)
        {
			mAnnotationsCursor.moveToPosition(position);
			
			ContentValues values = new ContentValues();
        	values.put(Annotations.VIEWED, true);
    		getContentResolver().update(ContentUris.withAppendedId(Annotations.CONTENT_URI,mAnnotationsCursor.getLong(0)), values, null, null);

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
    };

    Cursor mAnnotationsCursor;
    Cursor mSessionCursor;
    MediaPlayer player = null;
    List<String> mStrings = new LinkedList<String>();
    ArrayAdapter<String> listAdapter;
    
    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

}