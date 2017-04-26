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

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.text.DateFormat;
import java.util.Date;

import urbanstew.RehearsalAssistant.Rehearsal.Sessions;


/**
 * The NewRun Activity inserts a new run into the
 * current project.
 */
public class NewRun extends RehearsalActivity implements View.OnClickListener {
    /**
     * Returns a String filled in according with the template given by stringResourceId and the resources
     * given in args, all being resources of the type string.
     *
     * FIXME: Should be static.
     * FIXME: Should be way more general.
     *
     * @param stringResourceId
     * @param args
     * @return
     */
    public String formattedStringFromStringResources(int stringResourceId, Integer... args) {
        Resources res = this.getResources();
        String template = res.getString(stringResourceId);

        String[] args_as_strings = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            args_as_strings[i] = res.getString(args[i]);
        }

        // List<String> args_as_strings = Arrays.asList(args.length);
        // args_as_strings.foreach();

        return String.format(template, (Object []) args_as_strings);
    }


    /**
     * Called when the activity is first created.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(this.getString(R.string.new_session));

        // setup the display and callbacks
        setContentView(R.layout.new_run);
        findViewById(R.id.create).setOnClickListener(this);
        findViewById(R.id.create_and_start).setOnClickListener(this);

        EditText title = (EditText) findViewById(R.id.name);

        String date = DateFormat.getDateTimeInstance().format(new Date());
        String text = String.format(this.getString(R.string.session_date_title), date, this.getString(R.string.session));
        title.setText(text);
        title.selectAll();
    }

    /**
     * Called when the create button is pushed
     */
    public void onClick(View v) {
        // prepare the insert request - get title from the widget
        EditText title = (EditText) findViewById(R.id.name);
        ContentValues values = new ContentValues();

        values.put(Sessions.PROJECT_ID, getIntent().getExtras().getLong("project_id"));

        values.put(Sessions.TITLE, title.getText().toString());
        if (v == findViewById(R.id.create_and_start))
            values.put(Sessions.START_TIME, System.currentTimeMillis());

        // insert the result and go to the record activity
        Uri result = getContentResolver().insert(getIntent().getData(), values);
        startActivity(new Intent(Intent.ACTION_EDIT, result));
        finish();
    }
}
