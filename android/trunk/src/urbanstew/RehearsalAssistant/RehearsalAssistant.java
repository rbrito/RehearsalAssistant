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

import urbanstew.RehearsalAssistant.Rehearsal.AppData;
import urbanstew.RehearsalAssistant.Rehearsal.Sessions;
import urbanstew.RehearsalAssistant.Rehearsal.Projects;
import urbanstew.RehearsalAssistant.RehearsalRecord.State;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/** The RehearsalAssistant Activity is the top-level activity.
 */
public class RehearsalAssistant extends Activity
{
    public static final int MENU_ITEM_PLAYBACK = Menu.FIRST;
    public static final int MENU_ITEM_RECORD = Menu.FIRST + 1;
    public static final int MENU_ITEM_DELETE = Menu.FIRST + 2;

    private static final String[] PROJECTION = new String[]
    {
        "_id", // 0
        "title" // 1
    };
    
    /** Called when the activity is first created.
     *  
     *  For now, provides access to the recording and playback activities.
     *  
     */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        AppDataAccess appData = new AppDataAccess(getContentResolver());

        // View the current project
        startActivity
        (
        	new Intent
        	(
        		Intent.ACTION_VIEW,
        		ContentUris.withAppendedId(Projects.CONTENT_URI, appData.getCurrentProjectId())
        	)
        );

        // Display license if this is the first time running this version.
        String visitedVersion = appData.getVisitedVersion();
        if (visitedVersion == null || !visitedVersion.equals("0.3"))
        {
    		Request.notification(this,
    				"Warning",
    				getString(R.string.beta_warning));
    		Request.notification(this,
    				"License",
    				getString(R.string.license));
        }
    	if(visitedVersion == null)
    		appData.addVisitedVersion("0.3");
    	
    	// finish
    	finish();
    }
}