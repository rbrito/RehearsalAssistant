package urbanstew.RehearsalAssistant;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Request
{
	public static void confirmation(Context context, String title, String content, DialogInterface.OnClickListener confirmation)
	{
		new Dialog(context, title, content, confirmation);
	}
	public static void notification(Context context, String title, String content)
	{
		new Dialog(context, title, content, null);
	}
}

class Dialog
{
	public Dialog(Context context, String title, String content, DialogInterface.OnClickListener confirmation)
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(context)
    	.setTitle(title)
    	.setMessage(content)
    	.setPositiveButton
    	(
    		"OK",
    		confirmation == null ?
	    		new DialogInterface.OnClickListener()
	    		{
	    		    public void onClick(DialogInterface dialog, int whichButton)
	    		    {
	    		    }
	    		}
    			: confirmation
    	);
    			
        dialog.show();
	}
}
