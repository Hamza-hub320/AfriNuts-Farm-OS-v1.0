package com.afrinuts.farmos.ui.reports;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;  // Change import
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.utils.PdfReportGenerator;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportsFragment extends Fragment {

    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private List<ExpenseEntity> allExpenses;
    private List<RevenueEntity> allRevenues;
    private List<TaskEntity> allTasks;
    private List<QualityBatchEntity> allQualityBatches;
    private List<BatchTraceabilityEntity> allTraceabilities;
    private List<MasterBatchTraceabilityEntity> allMasters;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    // UI Elements - Changed from MaterialCardView to CardView
    private CardView cardFarmSummary;
    private CardView cardFinancialStatement;
    private CardView cardQualityReport;
    private CardView cardTraceabilityReport;
    private LinearLayout toggleDateRange;
    private TextInputEditText etStartDate;
    private TextInputEditText etEndDate;
    private LinearLayout layoutProgress;
    private TextView tvProgressMessage;
    private ProgressBar progressBar;
    private View dimOverlay;

    private Calendar startCalendar = Calendar.getInstance();
    private Calendar endCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private boolean isDateRangeValid = false;
    private String selectedReportType = "SUMMARY";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        database = AppDatabase.getInstance(requireContext());
        initViews(view);
        setupDateDefaults();
        setupClickListeners();
        loadData();
        updateReportCardsState();

        return view;
    }

    private void initViews(View view) {
        // Now casting to CardView (matches layout)
        cardFarmSummary = view.findViewById(R.id.cardFarmSummary);
        cardFinancialStatement = view.findViewById(R.id.cardFinancialStatement);
        cardQualityReport = view.findViewById(R.id.cardQualityReport);
        cardTraceabilityReport = view.findViewById(R.id.cardTraceabilityReport);
        toggleDateRange = view.findViewById(R.id.toggleDateRange);
        etStartDate = view.findViewById(R.id.etStartDate);
        etEndDate = view.findViewById(R.id.etEndDate);
        layoutProgress = view.findViewById(R.id.layoutProgress);
        tvProgressMessage = view.findViewById(R.id.tvProgressMessage);
        progressBar = view.findViewById(R.id.progressBar);
        dimOverlay = view.findViewById(R.id.dimOverlay);

        updateReportCardsState();
    }

    private void setupDateDefaults() {
        endCalendar.setTime(new Date());
        startCalendar.setTime(new Date());
        startCalendar.set(Calendar.DAY_OF_MONTH, 1);

        updateDateLabels();
        validateDateRange();

        etStartDate.setOnClickListener(v -> showDatePicker(true));
        etEndDate.setOnClickListener(v -> showDatePicker(false));

        // Highlight selected chip
        int[] rangeButtonIds = {R.id.btnAllTime, R.id.btnThisYear, R.id.btnThisMonth};
        for (int buttonId : rangeButtonIds) {
            View button = toggleDateRange.findViewById(buttonId);
            button.setOnClickListener(v -> {
                updateDateRangeChipStyles(v.getId());
                updateDateRangeFromChip(v.getId());
                validateDateRange();
            });
        }
        updateDateRangeChipStyles(R.id.btnAllTime);
    }

    private void updateDateRangeChipStyles(int checkedId) {
        for (int index = 0; index < toggleDateRange.getChildCount(); index++) {
            View child = toggleDateRange.getChildAt(index);
            boolean selected = child.getId() == checkedId;
            child.setBackgroundResource(selected
                    ? R.drawable.bg_report_range_selected
                    : android.R.color.transparent);
            ((TextView) child).setTextColor(selected
                    ? Color.WHITE : Color.parseColor("#59544C"));
        }
    }

    private void updateDateRangeFromChip(int checkedId) {
        if (checkedId == R.id.btnAllTime) {
            startCalendar.set(1970, Calendar.JANUARY, 1); // Beginning of time
            endCalendar.setTime(new Date());
        } else if (checkedId == R.id.btnThisYear) {
            startCalendar.set(Calendar.getInstance().get(Calendar.YEAR), Calendar.JANUARY, 1);
            endCalendar.setTime(new Date());
        } else if (checkedId == R.id.btnThisMonth) {
            startCalendar.set(Calendar.getInstance().get(Calendar.YEAR),
                    Calendar.getInstance().get(Calendar.MONTH), 1);
            endCalendar.setTime(new Date());
        }
        updateDateLabels();
    }

    private void showDatePicker(boolean isStart) {
        Calendar calendar = isStart ? startCalendar : endCalendar;

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isStart ? "Select Start Date" : "Select End Date")
                .setSelection(calendar.getTimeInMillis())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            calendar.setTimeInMillis(selection);
            updateDateLabels();
            validateDateRange();
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void updateDateLabels() {
        etStartDate.setText(dateFormat.format(startCalendar.getTime()));
        etEndDate.setText(dateFormat.format(endCalendar.getTime()));
    }

    private void validateDateRange() {
        isDateRangeValid = !startCalendar.getTime().after(endCalendar.getTime());

        if (!isDateRangeValid) {
            Snackbar.make(requireView(),
                    "Start date cannot be after end date", Snackbar.LENGTH_SHORT).show();
        }

        updateReportCardsState();
    }

    private void updateReportCardsState() {
        boolean enabled = isDateRangeValid && currentFarm != null;

        cardFarmSummary.setEnabled(enabled);
        cardFinancialStatement.setEnabled(enabled);
        if (cardQualityReport != null) cardQualityReport.setEnabled(enabled);
        if (cardTraceabilityReport != null) cardTraceabilityReport.setEnabled(enabled);
        cardFarmSummary.setAlpha(enabled ? 1.0f : 0.5f);
        cardFinancialStatement.setAlpha(enabled ? 1.0f : 0.5f);
        if (cardQualityReport != null) cardQualityReport.setAlpha(enabled ? 1.0f : 0.5f);
        if (cardTraceabilityReport != null) cardTraceabilityReport.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void setupClickListeners() {
        cardFarmSummary.setOnClickListener(v -> {
            if (isDateRangeValid) {
                selectedReportType = "SUMMARY";
                generateReport();
            }
        });

        cardFinancialStatement.setOnClickListener(v -> {
            if (isDateRangeValid) {
                selectedReportType = "FINANCIAL";
                generateReport();
            }
        });
        if (cardQualityReport != null) cardQualityReport.setOnClickListener(v -> {
            if (isDateRangeValid) { selectedReportType = "QUALITY"; generateReport(); }
        });
        if (cardTraceabilityReport != null) cardTraceabilityReport.setOnClickListener(v -> {
            if (isDateRangeValid) { selectedReportType = "TRACEABILITY"; generateReport(); }
        });
    }

    private void loadData() {
        databaseExecutor.execute(() -> {
            FarmEntity farm = database.farmDao().getFirstFarm();
            if (farm != null) {
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(farm.getId());
                List<ExpenseEntity> expenses = database.expenseDao().getExpensesByFarmId(farm.getId());
                List<RevenueEntity> revenues = database.revenueDao().getRevenuesByFarmId(farm.getId());
                List<TaskEntity> tasks = database.taskDao().getTasksByFarmId(farm.getId());
                List<QualityBatchEntity> qualityBatches = database.qualityBatchDao().getBatchesByFarmId(farm.getId());
                for (QualityBatchEntity batch : qualityBatches) {
                    if (batch.getSupplierId() != null) {
                        com.afrinuts.farmos.data.local.entity.SupplierEntity supplier =
                                database.supplierDao().getSupplierById(batch.getSupplierId());
                        if (supplier != null) batch.setSupplierName(supplier.getName());
                    }
                }
                List<BatchTraceabilityEntity> traceabilities = database.batchTraceabilityDao().getByFarmId(farm.getId());
                List<MasterBatchTraceabilityEntity> masters = new java.util.ArrayList<>();
                for (MasterBatchTraceabilityEntity master : database.masterTraceabilityDao().getAll()) {
                    if (master.getFarmId() == farm.getId()) masters.add(master);
                }

                requireActivity().runOnUiThread(() -> {
                    currentFarm = farm;
                    allBlocks = blocks;
                    allExpenses = expenses;
                    allRevenues = revenues;
                    allTasks = tasks;
                    allQualityBatches = qualityBatches;
                    allTraceabilities = traceabilities;
                    allMasters = masters;
                    updateReportCardsState();
                });
            }
        });
    }

    private void generateReport() {
        // Show loading state
        dimOverlay.setVisibility(View.VISIBLE);
        layoutProgress.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        tvProgressMessage.setText("Generating PDF...");

        // Disable interactions
        setViewsEnabled(false);

        databaseExecutor.execute(() -> {
            try {
                List<ExpenseEntity> filteredExpenses = filterExpensesByDate();
                List<RevenueEntity> filteredRevenues = filterRevenuesByDate();
                List<TaskEntity> filteredTasks = filterTasksByDate();
                List<QualityBatchEntity> filteredQuality = filterQualityByDate();
                List<BatchTraceabilityEntity> filteredTraceability = filterTraceabilityByDate();
                List<MasterBatchTraceabilityEntity> filteredMasters = filterMastersByDate();

                ReportData reportData = new ReportData(
                        currentFarm,
                        allBlocks,
                        filteredExpenses,
                        filteredRevenues,
                        filteredTasks,
                        filteredQuality,
                        filteredTraceability,
                        filteredMasters,
                        startCalendar.getTime(),
                        endCalendar.getTime()
                );

                PdfReportGenerator generator = new PdfReportGenerator(requireContext(), reportData);
                File pdfFile;

                if (selectedReportType.equals("SUMMARY")) {
                    pdfFile = generator.generateFarmSummaryReport();
                } else if (selectedReportType.equals("FINANCIAL")) {
                    pdfFile = generator.generateFinancialStatement();
                } else if (selectedReportType.equals("QUALITY")) {
                    pdfFile = generator.generateQualityReport();
                } else {
                    pdfFile = generator.generateTraceabilityReport();
                }

                requireActivity().runOnUiThread(() -> {
                    // Hide loading
                    dimOverlay.setVisibility(View.GONE);
                    layoutProgress.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    setViewsEnabled(true);

                    showSuccessDialog(pdfFile);
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    dimOverlay.setVisibility(View.GONE);
                    layoutProgress.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    setViewsEnabled(true);

                    Snackbar.make(requireView(),
                            "Error generating PDF: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setViewsEnabled(boolean enabled) {
        cardFarmSummary.setEnabled(enabled);
        cardFinancialStatement.setEnabled(enabled);
        if (cardQualityReport != null) cardQualityReport.setEnabled(enabled);
        if (cardTraceabilityReport != null) cardTraceabilityReport.setEnabled(enabled);
        toggleDateRange.setEnabled(enabled);
        for (int index = 0; index < toggleDateRange.getChildCount(); index++) {
            toggleDateRange.getChildAt(index).setEnabled(enabled);
        }
        etStartDate.setEnabled(enabled);
        etEndDate.setEnabled(enabled);
    }

    private List<QualityBatchEntity> filterQualityByDate() {
        List<QualityBatchEntity> filtered = new java.util.ArrayList<>();
        if (allQualityBatches == null) return filtered;
        long start = startCalendar.getTimeInMillis(), end = endCalendar.getTimeInMillis();
        for (QualityBatchEntity batch : allQualityBatches) {
            long date = batch.getRelevantDate();
            if (date >= start && date <= end) filtered.add(batch);
        }
        return filtered;
    }

    private List<BatchTraceabilityEntity> filterTraceabilityByDate() {
        List<BatchTraceabilityEntity> filtered = new java.util.ArrayList<>();
        if (allTraceabilities == null) return filtered;
        long start = startCalendar.getTimeInMillis(), end = endCalendar.getTimeInMillis();
        for (BatchTraceabilityEntity batch : allTraceabilities) {
            long date = batch.getExportDate() != null && batch.getExportDate() > 0
                    ? batch.getExportDate() : batch.getProcessingDate();
            if (date >= start && date <= end) filtered.add(batch);
        }
        return filtered;
    }

    private List<MasterBatchTraceabilityEntity> filterMastersByDate() {
        List<MasterBatchTraceabilityEntity> filtered = new java.util.ArrayList<>();
        if (allMasters == null) return filtered;
        long start = startCalendar.getTimeInMillis(), end = endCalendar.getTimeInMillis();
        for (MasterBatchTraceabilityEntity master : allMasters) {
            if (master.getExportDate() >= start && master.getExportDate() <= end) filtered.add(master);
        }
        return filtered;
    }

    private List<ExpenseEntity> filterExpensesByDate() {
        List<ExpenseEntity> filtered = new java.util.ArrayList<>();
        long startTime = startCalendar.getTimeInMillis();
        long endTime = endCalendar.getTimeInMillis();

        for (ExpenseEntity expense : allExpenses) {
            if (expense.getDate() >= startTime && expense.getDate() <= endTime) {
                filtered.add(expense);
            }
        }
        return filtered;
    }

    private List<RevenueEntity> filterRevenuesByDate() {
        List<RevenueEntity> filtered = new java.util.ArrayList<>();
        long startTime = startCalendar.getTimeInMillis();
        long endTime = endCalendar.getTimeInMillis();

        for (RevenueEntity revenue : allRevenues) {
            if (revenue.getCreatedAt() >= startTime && revenue.getCreatedAt() <= endTime) {
                filtered.add(revenue);
            }
        }
        return filtered;
    }

    private List<TaskEntity> filterTasksByDate() {
        List<TaskEntity> filtered = new java.util.ArrayList<>();
        long startTime = startCalendar.getTimeInMillis();
        long endTime = endCalendar.getTimeInMillis();

        if (allTasks == null) return filtered;
        for (TaskEntity task : allTasks) {
            boolean dueInRange = task.getDueDate() >= startTime && task.getDueDate() <= endTime;
            Long completedDate = task.getCompletedDate();
            boolean completedInRange = completedDate != null
                    && completedDate >= startTime && completedDate <= endTime;
            if (dueInRange || completedInRange) {
                filtered.add(task);
            }
        }
        return filtered;
    }

    private void showSuccessDialog(File pdfFile) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_report_generated, null, false);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnShareReport).setOnClickListener(v -> {
            dialog.dismiss();
            sharePdf(pdfFile);
        });
        dialogView.findViewById(R.id.btnOpenReport).setOnClickListener(v -> {
            dialog.dismiss();
            openPdf(pdfFile);
        });
        dialogView.findViewById(R.id.btnCloseReport).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = Math.round(getResources().getDisplayMetrics().widthPixels * 0.90f);
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void sharePdf(File pdfFile) {
        try {
            Uri pdfUri = FileProvider.getUriForFile(requireContext(),
                    "com.afrinuts.farmos.fileprovider",
                    pdfFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share PDF"));
        } catch (IllegalArgumentException e) {
            Toast.makeText(requireContext(), "Error sharing file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openPdf(File pdfFile) {
        try {
            Uri pdfUri = FileProvider.getUriForFile(requireContext(),
                    "com.afrinuts.farmos.fileprovider",
                    pdfFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (IllegalArgumentException e) {
            Toast.makeText(requireContext(), "Error opening file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdown();
    }
}
