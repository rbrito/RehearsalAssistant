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

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;


/** The NewRun Activity inserts a new run into the
 * 	current project.
 */
public class NewRun extends Activity implements View.OnClickListener
{
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // setup the display and callbacks
        setContentView(R.layout.new_run);
        findViewById(R.id.create).setOnClickListener(this);
        
        EditText title = (EditText) findViewById(R.id.name);
    	title.setText(DateFormat.getDateTimeInstance().format(new Date()));
    	title.selectAll();
    }

    /** Called when the create button is pushed */
    public void onClick(View v)
    {
    	// prepare the insert request - get title from the widget
    	EditText title = (EditText) findViewById(R.id.name);
    	ContentValues values = new ContentValues();
    	values.put("title", title.getText().toString());
    	
    	// insert the result and go to the record activity
    	Uri result = getContentResolver().insert(getIntent().getData(), values);
    	startActivity(new Intent(Intent.ACTION_EDIT, result));
        finish();
    }
}