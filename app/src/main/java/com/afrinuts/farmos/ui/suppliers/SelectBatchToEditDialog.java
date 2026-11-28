package com.afrinuts.farmos.ui.suppliers;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SelectBatchToEditDialog extends com.afrinuts.farmos.ui.farm.FarmDialogFragment {

    private static final String ARG_SUPPLIER_ID = "supplier_id";

    private long supplierId;
    private QualityBatchRepository qualityBatchRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<QualityBatchEntity> batches;
    private OnBatchSelectedListener listener;

    public interface OnBatchSelectedListener {
        void onBatchSelected(QualityBatchEntity batch);
    }

    public static SelectBatchToEditDialog newInstance(long supplierId) {
        SelectBatchToEditDialog dialog = new SelectBatchToEditDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_SUPPLIER_ID, supplierId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnBatchSelectedListener(OnBatchSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            supplierId = getArguments().getLong(ARG_SUPPLIER_ID);
        }

        long farmId = 1;
        qualityBatchRepository = new QualityBatchRepository(requireContext(), farmId);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_select_batch_to_edit, null);

        ListView listView = view.findViewById(R.id.listViewBatches);
        LinearLayout layoutEmpty = view.findViewById(R.id.layoutEmpty);

        // Load batches
        executor.execute(() -> {
            batches = qualityBatchRepository.getBatchesBySupplierIdSync(supplierId);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (batches == null || batches.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        listView.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);

                        BatchSelectionAdapter adapter = new BatchSelectionAdapter(
                                requireContext(),
                                batches,
                                batch -> {
                                    // ✅ Handle click directly in adapter
                                    if (listener != null) {
                                        listener.onBatchSelected(batch);
                                    }
                                    dismiss();
                                }
                        );
                        listView.setAdapter(adapter);
                    }
                });
            }
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view);

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        Window window = dialog == null ? null : dialog.getWindow();
        if (window == null) return;
        float density = requireContext().getResources().getDisplayMetrics().density;
        int screenWidth = requireContext().getResources().getDisplayMetrics().widthPixels;
        int sideMargin = Math.round(32 * density);
        int maxWidth = Math.round(720 * density);
        window.setLayout(Math.min(screenWidth - sideMargin, maxWidth),
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (qualityBatchRepository != null) {
            qualityBatchRepository.shutdown();
        }
        executor.shutdown();
    }

    // Inner Adapter for batch list with click listener
    private static class BatchSelectionAdapter extends ArrayAdapter<QualityBatchEntity> {
        private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        private LayoutInflater inflater;
        private OnBatchClickListener clickListener;

        interface OnBatchClickListener {
            void onBatchClick(QualityBatchEntity batch);
        }

        BatchSelectionAdapter(@NonNull android.content.Context context,
                              @NonNull List<QualityBatchEntity> batches,
                              OnBatchClickListener clickListener) {
            super(context, 0, batches);
            this.inflater = LayoutInflater.from(context);
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_batch_selection, parent, false);
            }

            QualityBatchEntity batch = getItem(position);
            if (batch != null) {
                TextView tvBatchInfo = view.findViewById(R.id.tvBatchInfo);
                TextView tvBatchDate = view.findViewById(R.id.tvBatchDate);
                TextView tvBatchWeight = view.findViewById(R.id.tvBatchWeight);
                TextView tvBatchGrade = view.findViewById(R.id.tvBatchGrade);
                ImageView ivGradeIcon = view.findViewById(R.id.ivGradeIcon);
                View cardView = view.findViewById(R.id.cardView);

                // Batch info
                tvBatchInfo.setText("#" + batch.getBatchNumber());

                // Date
                tvBatchDate.setText(dateFormat.format(batch.getProcessingDate()));

                // Weight
                tvBatchWeight.setText(String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()));

                // Grade with icon
                switch (batch.getQualityGrade()) {
                    case PREMIUM:
                        tvBatchGrade.setText("Premium");
                        tvBatchGrade.setTextColor(view.getContext().getColor(R.color.accent_ink));
                        ivGradeIcon.setImageResource(R.drawable.ic_star);
                        ivGradeIcon.setColorFilter(view.getContext().getColor(R.color.accent_ink));
                        break;
                    case STANDARD:
                        tvBatchGrade.setText("Standard");
                        tvBatchGrade.setTextColor(view.getContext().getColor(R.color.primary));
                        ivGradeIcon.setImageResource(R.drawable.ic_check_circle);
                        ivGradeIcon.setColorFilter(view.getContext().getColor(R.color.primary));
                        break;
                    case REJECT:
                        tvBatchGrade.setText("Reject");
                        tvBatchGrade.setTextColor(view.getContext().getColor(R.color.navy));
                        ivGradeIcon.setImageResource(R.drawable.ic_factory);
                        ivGradeIcon.setColorFilter(view.getContext().getColor(R.color.navy));
                        break;
                }

                // ✅ Set click listener on the card view
                if (cardView != null) {
                    cardView.setOnClickListener(v -> {
                        if (clickListener != null) {
                            clickListener.onBatchClick(batch);
                        }
                    });
                }
            }

            return view;
        }
    }
}
