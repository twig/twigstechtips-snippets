package twig.nguyen.mustachify2.activities.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.TextView;



public class ExampleDialogFragment extends DialogFragment {
	private static final String KEY_ARG_A = "argumentA";
	private static final String KEY_ARG_B = "argumentB";

	/**
	 * Interface to link back to the activity.
	 */
	public interface ExampleDialogResponses {
		public void doPositiveClick(long timestamp);
	}


    /**
     * Pass in variables, store for later use.
     */
    public static ExampleDialogFragment newInstance(int argA, String argB) {
    	ExampleDialogFragment f = new ExampleDialogFragment();

        Bundle args = new Bundle();
        args.putInt(KEY_ARG_A, argA);
        args.putString(KEY_ARG_B, argB);
        f.setArguments(args);

        return f;
    }

    /**
     * Override the creation of the dialog.
     */
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Fetch the information set earlier
		int argA = getArguments().getInt(KEY_ARG_A);
		String argB = getArguments().getString(KEY_ARG_B);

		// Generate your dialog view or layout here
		// view = ...
		TextView view = new TextView(getActivity());
		view.setText(argB + String.valueOf(argA));

    	// Display the dialog
    	return new AlertDialog.Builder(getActivity())
	        .setTitle("Choose OK or Cancel")
	        .setView(view)

	        // Cancel
	        .setNegativeButton(android.R.string.cancel, null)

            // Click "OK" button handler
	        .setPositiveButton(android.R.string.ok,
	            new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	long timestamp = System.currentTimeMillis();
	                    ((ExampleDialogResponses) getActivity()).doPositiveClick(timestamp);
	                }
	            }
	        )
	        .create();
    }
}
