package com.afrinuts.farmos.ui.compliance;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afrinuts.farmos.ui.farm.FarmDialogFragment;

/** Standard Farm sheet for choosing which kind of compliance check to add. */
public class CheckChooserDialog extends FarmDialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        TextView title = new TextView(requireContext());
        title.setText("Add check");
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(0, 0, Math.round(52 * density), 0);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.bottomMargin = Math.round(18 * density);
        content.addView(title, titleParams);

        String[] choices = {"Add audit", "Add compliance inspection", "Add verification"};
        for (int i = 0; i < choices.length; i++) {
            final int choice = i;
            Button button = new Button(requireContext());
            button.setText(choices[i]);
            button.setAllCaps(false);
            button.setTextSize(15);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, Math.round(52 * density));
            if (i > 0) params.topMargin = Math.round(10 * density);
            content.addView(button, params);
            button.setOnClickListener(v -> {
                if (getActivity() instanceof ComplianceHubActivity) {
                    ((ComplianceHubActivity) getActivity()).showCheckChoice(choice);
                }
                dismiss();
            });
        }

        Button cancel = new Button(requireContext());
        cancel.setText("Cancel");
        cancel.setAllCaps(false);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(-1, Math.round(52 * density));
        cancelParams.topMargin = Math.round(14 * density);
        content.addView(cancel, cancelParams);
        cancel.setOnClickListener(v -> dismiss());

        return new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .setCancelable(true)
                .create();
    }
}
