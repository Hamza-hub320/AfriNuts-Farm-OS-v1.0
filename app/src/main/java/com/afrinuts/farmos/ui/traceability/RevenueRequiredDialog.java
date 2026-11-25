package com.afrinuts.farmos.ui.traceability;

import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.farm.FarmDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Standard sheet prompting users to add the revenue required for an invoice. */
public class RevenueRequiredDialog extends FarmDialogFragment {

    public interface Listener {
        void onAddRevenueRequested();
    }

    private Listener listener;

    public static RevenueRequiredDialog newInstance() {
        return new RevenueRequiredDialog();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_revenue_required, null);
        Button cancel = view.findViewById(R.id.btnCancel);
        Button addRevenue = view.findViewById(R.id.btnAddRevenue);
        cancel.setOnClickListener(v -> dismiss());
        addRevenue.setOnClickListener(v -> {
            dismiss();
            if (listener != null) new Handler(Looper.getMainLooper()).post(listener::onAddRevenueRequested);
        });
        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }
}
