package com.afrinuts.farmos.ui.traceability;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.ui.farm.FarmDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Standard sheet for choosing a traceability document to generate. */
public class GenerateDocumentDialog extends FarmDialogFragment {

    public interface Listener {
        void onDocumentSelected(String documentType);
    }

    private Listener listener;

    public static GenerateDocumentDialog newInstance() {
        return new GenerateDocumentDialog();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_generate_document, null);
        bindOption(view, R.id.btnCertificateOfOrigin, "Certificate of Origin");
        bindOption(view, R.id.btnPackingList, "Packing List");
        bindOption(view, R.id.btnCommercialInvoice, "Commercial Invoice");
        bindOption(view, R.id.btnDocumentPackage, "Document Package");
        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }

    private void bindOption(View root, int buttonId, String documentType) {
        Button button = root.findViewById(buttonId);
        button.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                new Handler(Looper.getMainLooper()).post(() -> listener.onDocumentSelected(documentType));
            }
        });
    }
}
