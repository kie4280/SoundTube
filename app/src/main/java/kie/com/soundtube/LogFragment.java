package kie.com.soundtube;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by kieChang on 2017/11/26.
 */

public class LogFragment extends DialogFragment {

    Buttonlistener buttonlistener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String error = getArguments().getString("error_message");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(error);
        builder.setTitle(R.string.submit_report);
        builder.setPositiveButton(R.string.submit_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                buttonlistener.onclicked(true);

            }
        });
        builder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                buttonlistener.onclicked(false);
            }
        });

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Buttonlistener) {
            buttonlistener = (Buttonlistener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        buttonlistener = null;
    }

    public interface Buttonlistener {
        void onclicked(boolean submit);
    }
}
