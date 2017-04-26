package urbanstew.RehearsalAssistant;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

import urbanstew.RehearsalAssistant.Rehearsal.Projects;

public class ProjectManager extends ListActivity {
    private static final int MENU_ITEM_RENAME = Menu.FIRST;
    private static final int MENU_ITEM_DELETE = Menu.FIRST + 1;
    public static final String TAG = "RehearsalAssistant";
    private AlertDialog mNewProjectDialog;
    private SimpleCursorAdapter mListAdapter;
    private Cursor mProjectCursor;
    private MenuItem mInstructionsItem;
    private MenuItem mNewProjectItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.project_manager);

        super.onCreate(savedInstanceState);

        setTitle(this.getString(R.string.app_name) + " - " + this.getString(R.string.project_manager));

        String[] projectProjection =
                {
                        Projects._ID,
                        Projects.TITLE,
                        Projects.TYPE
                };

        AppDataAccess appData = new AppDataAccess(this);

        mProjectCursor = managedQuery(Projects.CONTENT_URI, projectProjection, null, null, Projects.DEFAULT_SORT_ORDER);

        mListAdapter = new SimpleCursorAdapter(this, R.layout.projectslist_item, mProjectCursor,
                new String[]{Projects.TITLE, Projects.TYPE, Projects._ID}, new int[]{android.R.id.text1, android.R.id.text2, R.id.recorder_widget_icon});

        setListAdapter(mListAdapter);

        mListAdapter.setViewBinder(new ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor,
                                        int columnIndex) {
                if (view.getId() == android.R.id.text1)
                    return false;
                if (view.getId() == android.R.id.text2) {
                    ((TextView) view).setText(ProjectManager.this.getString(R.string.project_type) + " " + ProjectManager.this.getString(cursor.getLong(2) == Projects.TYPE_SESSION ? R.string.session : R.string.memo));
                    return true;
                }
                Log.d(TAG, "view.getID() returned an unsupported value (relative to the old, non-free widget)");
                return true;
            }
        });

        // ***
        // * Display Project Manager instructions if needed
        // **
        // Project Manager was introduced at version 0.8.0
        if (appData.lastVisitedVersionOlderThan("project_manager", 0.8f))
            Request.notification(this, getString(R.string.instructions), getString(R.string.project_manager_instructions));

        // ***
        // * Creation of project context menus
        // **
        getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                //menu.add(Menu.NONE, MENU_ITEM_RENAME, 0, "rename");
                menu.add(Menu.NONE, MENU_ITEM_DELETE, 1, getString(R.string.delete));
                mProjectCursor.moveToPosition(info.position);
                long project_type = mProjectCursor.getLong(2);
            }
        });
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // restore label edit dialog if needed
        if (savedInstanceState.getBoolean("newProjectDialogShown")) {
            displayNewProjectDialog
                    (
                            savedInstanceState.getString("newProjectDialogTitle"),
                            savedInstanceState.getInt("newProjectDialogType")
                    );
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        if (mNewProjectDialog != null && mNewProjectDialog.isShowing()) {
            outState.putBoolean("newProjectDialogShown", true);
            outState.putString
                    (
                            "newProjectDialogTitle",
                            ((EditText) mNewProjectDialog.findViewById(R.id.project_title_text)).getText().toString()
                    );
            outState.putInt
                    (
                            "newProjectDialogType",
                            ((RadioGroup) mNewProjectDialog.findViewById(R.id.project_type)).getCheckedRadioButtonId()
                    );
        }
    }

    protected void onDestroy() {
        if (mNewProjectDialog != null)
            mNewProjectDialog.cancel();
        super.onDestroy();
    }

    /**
     * Process project context menu events
     */
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        mProjectCursor.moveToPosition(info.position);
        switch (item.getItemId()) {
            case MENU_ITEM_RENAME:
                break;

            case MENU_ITEM_DELETE:
                Request.cancellable_confirmation(this, this.getString(R.string.warning), this.getString(R.string.warning_erase_project_recordings),
                        new OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                long projectId = mProjectCursor.getLong(0);
                                getContentResolver().delete(ContentUris.withAppendedId(Projects.CONTENT_URI, mProjectCursor.getLong(0)), null, null);
                            }
                        });
                break;
        }
        return true;
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        startActivity
                (
                        new Intent
                                (
                                        Intent.ACTION_VIEW,
                                        ContentUris.withAppendedId(Projects.CONTENT_URI, id)
                                )
                );
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        mNewProjectItem = menu.add(this.getString(R.string.new_project)).setIcon(android.R.drawable.ic_menu_add);
        mInstructionsItem = menu.add(R.string.help).setIcon(android.R.drawable.ic_menu_help);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item == mNewProjectItem) {
            displayNewProjectDialog(this.getString(R.string.my_new_project), R.id.project_type_simple);
            return true;
        }
        if (item == mInstructionsItem) {
            Request.notification(this, this.getString(R.string.instructions), this.getString(R.string.project_manager_instructions));
            return true;
        }
        return false;

    }

    private void displayNewProjectDialog(String content, int type) {
        LayoutInflater factory = LayoutInflater.from(this);
        final View dialogView = factory.inflate(R.layout.new_project_dialog, null);
        mNewProjectDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(this.getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ContentValues values = new ContentValues();
                        values.put(Projects.TITLE, ((EditText) mNewProjectDialog.findViewById(R.id.project_title_text)).getText().toString());
                        values.put(Projects.TYPE, ((RadioGroup) mNewProjectDialog.findViewById(R.id.project_type)).getCheckedRadioButtonId() == R.id.project_type_simple ? Projects.TYPE_SIMPLE : Projects.TYPE_SESSION);
                        getContentResolver().insert(Projects.CONTENT_URI, values);
                        mNewProjectDialog = null;
                    }
                })
                .setNegativeButton(this.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mNewProjectDialog = null;
                    }
                })
                .create();
        mNewProjectDialog.show();
        EditText title = (EditText) mNewProjectDialog.findViewById(R.id.project_title_text);
        title.setText(content);
        title.selectAll();
        ((RadioGroup) mNewProjectDialog.findViewById(R.id.project_type)).check(type);
    }
}
