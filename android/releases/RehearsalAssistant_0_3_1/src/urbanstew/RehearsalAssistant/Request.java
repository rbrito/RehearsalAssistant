package urbanstew.RehearsalAssistant;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Request
{
	public static boolean confirmation(Context context, String title, String content)
	{
		Dialog dialog = new Dialog(context, title, content, true);
		return dialog.answer();
	}
	public static void notification(Context context, String title, String content)
	{
		new Dialog(context, title, content, false);
	}
}

class Dialog
{
	public Dialog(Context context, String title, String content, boolean permit_cancel)
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(context)
    	.setTitle(title)
    	.setMessage(content)
    	.setPositiveButton("OK", ok);
    	
		if(permit_cancel)
			dialog.setNegativeButton("Cancel", cancel);
		
        dialog.show();
	}
	public boolean answer()
	{
		return ok.chosen;
	}
	Choosable ok = new Choosable();
	Choosable cancel = new Choosable();
}

class Choosable implements DialogInterface.OnClickListener
{
    public void onClick(DialogInterface dialog, int whichButton) {
    	chosen = true;
    }
    public boolean chosen = false;
}

