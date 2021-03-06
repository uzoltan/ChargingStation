package eu.arrowhead.demo.chargingstation.fragments;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import eu.arrowhead.demo.chargingstation.R;

public class ConfirmStopChargingDialog extends DialogFragment {

    public static final String TAG = "ConfirmStopChargingFragment";

    public interface ConfirmStopChargingListener {
        public void onStopChargingPositiveClick(DialogFragment dialog);
    }

    ConfirmStopChargingListener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View content = inflater.inflate(R.layout.dialog_confirm_stop_charging, null);

        builder.setView(content)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onStopChargingPositiveClick(ConfirmStopChargingDialog.this);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity activity = getActivity();
        try {
            mListener = (ConfirmStopChargingListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ConfirmStopChargingListener");
        }
    }
}
