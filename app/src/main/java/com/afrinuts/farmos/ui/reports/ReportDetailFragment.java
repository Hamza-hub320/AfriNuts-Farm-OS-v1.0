package com.afrinuts.farmos.ui.reports;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.*;
import com.afrinuts.farmos.utils.PdfReportGenerator;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tablet-only report detail pane. The report list remains in the adjacent ViewPager pane. */
public class ReportDetailFragment extends Fragment {
    private static final String ARG_TYPE = "report_type";
    private String reportType = "SUMMARY";
    private AppDatabase database;
    private FarmEntity farm;
    private List<BlockEntity> blocks = new ArrayList<>();
    private List<ExpenseEntity> expenses = new ArrayList<>();
    private List<RevenueEntity> revenues = new ArrayList<>();
    private List<TaskEntity> tasks = new ArrayList<>();
    private List<QualityBatchEntity> quality = new ArrayList<>();
    private List<BatchTraceabilityEntity> traces = new ArrayList<>();
    private List<MasterBatchTraceabilityEntity> masters = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Calendar start = Calendar.getInstance(), end = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private TextView title, subtitle, startDate, endDate, previewTitle, previewMeta;
    private LinearLayout previewContent;
    private View generateButton, downloadButton, shareButton;
    private File generatedFile;
    private final androidx.activity.result.ActivityResultLauncher<Intent> savePdfLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null && generatedFile != null) {
                    Uri target = result.getData().getData();
                    try (InputStream in = new java.io.FileInputStream(generatedFile);
                         OutputStream out = requireContext().getContentResolver().openOutputStream(target)) {
                        if (out == null) throw new java.io.IOException("Could not open destination");
                        byte[] buffer = new byte[8192]; int count;
                        while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                        Toast.makeText(requireContext(), "PDF saved", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) { Toast.makeText(requireContext(), "Could not save PDF: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
                }
            });

    public static ReportDetailFragment newInstance(String type) {
        ReportDetailFragment fragment = new ReportDetailFragment();
        Bundle args = new Bundle(); args.putString(ARG_TYPE, type); fragment.setArguments(args); return fragment;
    }

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (getArguments() != null) reportType = getArguments().getString(ARG_TYPE, "SUMMARY");
        // Match the phone Reports tab's initial date fields. Selecting All Time
        // explicitly expands the range back to the beginning of the timeline.
        end.setTime(new Date());
        start.setTime(end.getTime());
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        View root = inflater.inflate(R.layout.fragment_report_detail_tablet, container, false);
        database = AppDatabase.getInstance(requireContext());
        title = root.findViewById(R.id.tvReportTitle); subtitle = root.findViewById(R.id.tvReportSubtitle);
        startDate = root.findViewById(R.id.etStartDate); endDate = root.findViewById(R.id.etEndDate);
        previewTitle = root.findViewById(R.id.tvPreviewTitle); previewMeta = root.findViewById(R.id.tvPreviewMeta);
        previewContent = root.findViewById(R.id.reportPreviewContent);
        generateButton = root.findViewById(R.id.btnGenerateReportPdf); downloadButton = root.findViewById(R.id.btnDownloadReportPdf); shareButton = root.findViewById(R.id.btnShareReportPdf);
        root.findViewById(R.id.btnAllTime).setOnClickListener(v -> setRange(R.id.btnAllTime));
        root.findViewById(R.id.btnThisYear).setOnClickListener(v -> setRange(R.id.btnThisYear));
        root.findViewById(R.id.btnThisMonth).setOnClickListener(v -> setRange(R.id.btnThisMonth));
        startDate.setOnClickListener(v -> pickDate(true)); endDate.setOnClickListener(v -> pickDate(false));
        generateButton.setOnClickListener(v -> generate());
        downloadButton.setOnClickListener(v -> savePdf()); shareButton.setOnClickListener(v -> sharePdf());
        setRangeStyles(R.id.btnAllTime); updateLabels(); updateTypeUi();
        downloadButton.setEnabled(false); shareButton.setEnabled(false); downloadButton.setAlpha(.5f); shareButton.setAlpha(.5f);
        loadData();
        return root;
    }

    public void setReportType(String type) {
        if (!reportType.equals(type)) {
            generatedFile = null;
            if (downloadButton != null) { downloadButton.setEnabled(false); downloadButton.setAlpha(.5f); }
            if (shareButton != null) { shareButton.setEnabled(false); shareButton.setAlpha(.5f); }
        }
        reportType = type;
        if (title != null) { updateTypeUi(); renderPreview(); }
    }

    private void updateTypeUi() {
        if (title == null) return;
        String name = getReportName(reportType);
        title.setText(name); previewTitle.setText(name); updateLabels();
    }

    private String getReportName(String type) {
        switch (type) {
            case "FINANCIAL": return "Financial Statement";
            case "QUALITY": return "Quality Report";
            case "TRACEABILITY": return "Traceability Report";
            default: return "Farm Summary";
        }
    }

    private void setRange(int selectedId) {
        Calendar now = Calendar.getInstance(); end.setTime(now.getTime());
        if (selectedId == R.id.btnAllTime) {
            start.set(1970, Calendar.JANUARY, 1, 0, 0, 0); start.set(Calendar.MILLISECOND, 0);
        } else if (selectedId == R.id.btnThisYear) {
            start.set(now.get(Calendar.YEAR), Calendar.JANUARY, 1, 0, 0, 0); start.set(Calendar.MILLISECOND, 0);
        } else {
            start.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1, 0, 0, 0); start.set(Calendar.MILLISECOND, 0);
        }
        setRangeStyles(selectedId); updateLabels(); renderPreview();
    }

    private void setRangeStyles(int selectedId) {
        if (getView() == null) return;
        int[] ids = {R.id.btnAllTime, R.id.btnThisYear, R.id.btnThisMonth};
        for (int id : ids) {
            TextView chip = getView().findViewById(id); boolean selected = id == selectedId;
            chip.setBackgroundResource(selected ? R.drawable.bg_report_range_selected : android.R.color.transparent);
            chip.setTextColor(selected ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#59544C"));
        }
    }

    private void pickDate(boolean isStart) {
        Calendar c = isStart ? start : end;
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().setTitleText(isStart ? "Select Start Date" : "Select End Date").setSelection(c.getTimeInMillis()).build();
        picker.addOnPositiveButtonClickListener(value -> { c.setTimeInMillis(value); updateLabels(); renderPreview(); });
        picker.show(getParentFragmentManager(), isStart ? "REPORT_START_DATE" : "REPORT_END_DATE");
    }

    private void updateLabels() {
        if (startDate == null) return;
        startDate.setText(dateFormat.format(start.getTime())); endDate.setText(dateFormat.format(end.getTime()));
        subtitle.setText("Generated report · " + dateFormat.format(start.getTime()) + " – " + dateFormat.format(end.getTime()));
    }

    private void loadData() {
        executor.execute(() -> {
            FarmEntity loadedFarm = database.farmDao().getFirstFarm();
            if (loadedFarm == null || !isAdded()) return;
            List<BlockEntity> loadedBlocks = database.blockDao().getBlocksByFarmId(loadedFarm.getId());
            List<ExpenseEntity> loadedExpenses = database.expenseDao().getExpensesByFarmId(loadedFarm.getId());
            List<RevenueEntity> loadedRevenues = database.revenueDao().getRevenuesByFarmId(loadedFarm.getId());
            List<TaskEntity> loadedTasks = database.taskDao().getTasksByFarmId(loadedFarm.getId());
            List<QualityBatchEntity> loadedQuality = database.qualityBatchDao().getBatchesByFarmId(loadedFarm.getId());
            for (QualityBatchEntity batch : loadedQuality) if (batch.getSupplierId() != null) {
                SupplierEntity supplier = database.supplierDao().getSupplierById(batch.getSupplierId());
                if (supplier != null) batch.setSupplierName(supplier.getName());
            }
            List<BatchTraceabilityEntity> loadedTraces = database.batchTraceabilityDao().getByFarmId(loadedFarm.getId());
            List<MasterBatchTraceabilityEntity> loadedMasters = new ArrayList<>();
            for (MasterBatchTraceabilityEntity master : database.masterTraceabilityDao().getAll()) if (master.getFarmId() == loadedFarm.getId()) loadedMasters.add(master);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                farm=loadedFarm; blocks=loadedBlocks; expenses=loadedExpenses; revenues=loadedRevenues; tasks=loadedTasks;
                quality=loadedQuality; traces=loadedTraces; masters=loadedMasters; renderPreview();
            });
        });
    }

    private boolean inRange(long timestamp) {
        Calendar last = (Calendar) end.clone(); last.add(Calendar.DAY_OF_MONTH, 1); last.add(Calendar.MILLISECOND, -1);
        return timestamp >= start.getTimeInMillis() && timestamp <= last.getTimeInMillis();
    }

    private List<QualityBatchEntity> filteredQuality() { List<QualityBatchEntity> r=new ArrayList<>(); for(QualityBatchEntity b:quality)if(inRange(b.getRelevantDate()))r.add(b); return r; }
    private List<BatchTraceabilityEntity> filteredTraces() { List<BatchTraceabilityEntity> r=new ArrayList<>(); for(BatchTraceabilityEntity b:traces){long d=b.getExportDate()!=null&&b.getExportDate()>0?b.getExportDate():b.getProcessingDate();if(inRange(d))r.add(b);} return r; }
    private List<MasterBatchTraceabilityEntity> filteredMasters() { List<MasterBatchTraceabilityEntity> r=new ArrayList<>(); for(MasterBatchTraceabilityEntity m:masters)if(m.getExportDate()>0&&inRange(m.getExportDate()))r.add(m); return r; }

    private ReportData buildReportData() {
        List<ExpenseEntity> e=new ArrayList<>(); for(ExpenseEntity x:expenses)if(inRange(x.getDate()))e.add(x);
        List<RevenueEntity> r=new ArrayList<>(); for(RevenueEntity x:revenues)if(inRange(x.getCreatedAt()))r.add(x);
        List<TaskEntity> t=new ArrayList<>(); for(TaskEntity x:tasks)if(inRange(x.getDueDate())||(x.getCompletedDate()!=null&&inRange(x.getCompletedDate())))t.add(x);
        return new ReportData(farm,blocks,e,r,t,filteredQuality(),filteredTraces(),filteredMasters(),start.getTime(),end.getTime());
    }

    private void generate() {
        if (farm == null) { Snackbar.make(requireView(), "Loading report data…", Snackbar.LENGTH_SHORT).show(); return; }
        if (start.after(end)) { Snackbar.make(requireView(), "Start date cannot be after end date", Snackbar.LENGTH_SHORT).show(); return; }
        generateButton.setEnabled(false); ((TextView) generateButton).setText("Generating…");
        ReportData data = buildReportData(); String type = reportType; android.content.Context appContext = requireContext().getApplicationContext();
        executor.execute(() -> {
            try {
                PdfReportGenerator generator = new PdfReportGenerator(appContext, data);
                File file;
                switch(type){case "FINANCIAL":file=generator.generateFinancialStatement();break;case "QUALITY":file=generator.generateQualityReport();break;case "TRACEABILITY":file=generator.generateTraceabilityReport();break;default:file=generator.generateFarmSummaryReport();}
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    generatedFile=file; generateButton.setEnabled(true); ((TextView)generateButton).setText("Generate PDF");
                    previewMeta.setText(dateFormat.format(start.getTime())+" – "+dateFormat.format(end.getTime())+" · PDF · generated");
                    downloadButton.setEnabled(true); shareButton.setEnabled(true); downloadButton.setAlpha(1); shareButton.setAlpha(1); renderPreview();
                    Toast.makeText(requireContext(), "PDF generated", Toast.LENGTH_SHORT).show();
                });
            } catch(Exception ex) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> { generateButton.setEnabled(true); ((TextView)generateButton).setText("Generate PDF"); Snackbar.make(requireView(), "Could not generate PDF: "+ex.getMessage(), Snackbar.LENGTH_LONG).show(); });
            }
        });
    }

    private void renderPreview() {
        if (previewContent == null) return;
        previewContent.removeAllViews();
        if (farm == null) { addPreviewRow("Report data", "Loading…"); return; }
        String range = dateFormat.format(start.getTime()) + " – " + dateFormat.format(end.getTime());
        previewMeta.setText(range + " · PDF" + (generatedFile == null ? " · ready to generate" : " · generated"));
        if ("QUALITY".equals(reportType)) {
            List<QualityBatchEntity> list=filteredQuality(); double kor=0,moisture=0,defective=0; int p=0,s=0,r=0;
            double score=0; int scoreCount=0;
            for(QualityBatchEntity b:list){kor+=b.getKorGPerKg();moisture+=b.getMoistureContent();defective+=b.getDefectiveRate();if(b.getQualityScore()>0){score+=b.getQualityScore();scoreCount++;}if(b.getQualityGrade()==QualityBatchEntity.QualityGrade.PREMIUM)p++;else if(b.getQualityGrade()==QualityBatchEntity.QualityGrade.REJECT)r++;else s++;}
            addKpiCards(new String[]{"Batches graded", "Average KOR", "Avg quality"}, new String[]{
                    String.valueOf(list.size()), format(list.isEmpty()?0:kor/list.size()," g/kg"),
                    scoreCount==0?"—":String.format(Locale.getDefault(),"%.0f / 100",score/scoreCount)});
            addPreviewRow("Batches graded",String.valueOf(list.size())); addPreviewRow("Average KOR",format(list.isEmpty()?0:kor/list.size()," g/kg"));
            addPreviewRow("Average moisture",format(list.isEmpty()?0:moisture/list.size(),"%")); addPreviewRow("Average defective",format(list.isEmpty()?0:defective/list.size(),"%"));
            addPreviewRow("Premium · Standard · Reject",p+" · "+s+" · "+r);
            addPreviewRow("Batch results", "Batch · Supplier · KOR · Grade · Moisture · Defective · Nut count");
            for(int i=0;i<Math.min(8,list.size());i++){QualityBatchEntity b=list.get(i);addPreviewRow(value(b.getBatchNumber()),value(b.getSupplierName())+" · "+format(b.getKorGPerKg()," g/kg")+" · "+(b.getQualityGrade()==null?"—":b.getQualityGrade().getDisplayName())+" · "+format(b.getMoistureContent(),"%")+" · "+format(b.getDefectiveRate(),"%")+" · "+b.getNutCountPerKg()+" / kg");}
        } else if ("TRACEABILITY".equals(reportType)) {
            List<BatchTraceabilityEntity> list=filteredTraces(); int exported=0;double weight=0;for(BatchTraceabilityEntity b:list){if(b.getExportDate()!=null&&b.getExportDate()>0)exported++;weight+=b.getWeightKg();}
            addKpiCards(new String[]{"Traced lots", "Exported lots", "Total weight"}, new String[]{
                    String.valueOf(list.size()), String.valueOf(exported), String.format(Locale.getDefault(),"%,.0f kg",weight)});
            addPreviewRow("Traced lots",String.valueOf(list.size()));addPreviewRow("Exported lots",String.valueOf(exported));addPreviewRow("Total weight",String.format(Locale.getDefault(),"%,.0f kg",weight));
            addPreviewRow("Lot results","Lot · Buyer · Destination · Export date · Grade · Weight");
            for(int i=0;i<Math.min(8,list.size());i++){BatchTraceabilityEntity b=list.get(i);addPreviewRow(value(b.getBatchNumber()),value(b.getBuyer())+" · "+value(b.getDestination())+" · "+(b.getExportDate()==null||b.getExportDate()<=0?"—":dateFormat.format(new Date(b.getExportDate())))+" · "+value(b.getGrade())+" · "+String.format(Locale.getDefault(),"%,.0f kg",b.getWeightKg()));}
            addPreviewRow("Master shipments",String.valueOf(filteredMasters().size()));
            for(MasterBatchTraceabilityEntity m:filteredMasters())addPreviewRow(value(m.getMasterBatchNumber()),value(m.getBuyer())+" · "+value(m.getDestination())+" · "+m.getTotalBatches()+" batches · "+String.format(Locale.getDefault(),"%,.0f kg",m.getTotalWeightKg()));
        } else if ("FINANCIAL".equals(reportType)) {
            double income=0,out=0;for(RevenueEntity e:revenues)if(inRange(e.getCreatedAt()))income+=e.getTotalAmount();for(ExpenseEntity e:expenses)if(inRange(e.getDate()))out+=e.getAmount();
            addKpiCards(new String[]{"Revenue", "Expenses", "Net profit"}, new String[]{
                    String.format(Locale.getDefault(),"%,.0f XAF",income), String.format(Locale.getDefault(),"%,.0f XAF",out),
                    String.format(Locale.getDefault(),"%,.0f XAF",income-out)});
            addPreviewRow("Revenue",String.format(Locale.getDefault(),"%,.0f XAF",income));addPreviewRow("Expenses",String.format(Locale.getDefault(),"%,.0f XAF",out));addPreviewRow("Net profit",String.format(Locale.getDefault(),"%,.0f XAF",income-out));
        } else {
            List<QualityBatchEntity> graded = filteredQuality();
            double totalWeight = 0, qualityTotal = 0;
            int qualityCount = 0;
            for (QualityBatchEntity batch : graded) {
                totalWeight += batch.getWeightKg();
                if (batch.getQualityScore() > 0) {
                    qualityTotal += batch.getQualityScore();
                    qualityCount++;
                }
            }
            addSummaryKpis(graded.size(), totalWeight,
                    qualityCount == 0 ? 0 : qualityTotal / qualityCount);
            addPreviewRow("Farm",value(farm.getName()));addPreviewRow("Location",value(farm.getLocation()));addPreviewRow("Blocks",String.valueOf(blocks.size()));
            addPreviewRow("Season revenue",String.format(Locale.getDefault(),"%,.0f XAF",sumRevenue()));addPreviewRow("Season expenses",String.format(Locale.getDefault(),"%,.0f XAF",sumExpenses()));
            addPreviewRow("Quality batches graded",String.valueOf(graded.size()));
        }
    }

    private void addSummaryKpis(int batchCount, double totalWeight, double averageQuality) {
        addKpiCards(new String[]{"Batches graded", "Total weight", "Avg quality"}, new String[]{
                String.valueOf(batchCount), String.format(Locale.getDefault(), "%,.0f kg", totalWeight),
                averageQuality <= 0 ? "—" : String.format(Locale.getDefault(), "%.0f / 100", averageQuality)});
    }

    private void addKpiCards(String[] labels, String[] values) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, dp(10));
        for (int i = 0; i < labels.length; i++) {
            android.widget.FrameLayout card = new android.widget.FrameLayout(requireContext());
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(i == 0 ? android.graphics.Color.parseColor("#0D6B3E") : android.graphics.Color.parseColor("#F5F0E6"));
            bg.setCornerRadius(dp(18));
            if (i > 0) bg.setStroke(dp(1), android.graphics.Color.parseColor("#E3D8C3"));
            card.setBackground(bg);
            LinearLayout contents = new LinearLayout(requireContext());
            contents.setPadding(dp(12), dp(12), dp(8), dp(12));
            contents.setGravity(android.view.Gravity.CENTER_VERTICAL);
            contents.setOrientation(LinearLayout.VERTICAL);
            TextView label = new TextView(requireContext());
            label.setText(labels[i]); label.setTextSize(12);
            label.setTextColor(android.graphics.Color.parseColor(i == 0 ? "#CDE5D8" : "#706B62"));
            label.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.manrope_medium));
            TextView number = new TextView(requireContext());
            number.setText(values[i]); number.setTextSize(19);
            number.setTextColor(android.graphics.Color.parseColor(i == 0 ? "#FFFFFF" : "#1C1B1A"));
            number.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.manrope_extrabold));
            contents.addView(label);
            contents.addView(number);
            card.addView(contents);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(84), 1);
            if (i > 0) params.setMargins(dp(8), 0, 0, 0);
            row.addView(card, params);
        }
        previewContent.addView(row);
    }

    private double sumRevenue(){double n=0;for(RevenueEntity e:revenues)if(inRange(e.getCreatedAt()))n+=e.getTotalAmount();return n;}
    private double sumExpenses(){double n=0;for(ExpenseEntity e:expenses)if(inRange(e.getDate()))n+=e.getAmount();return n;}
    private String value(String s){return s==null||s.trim().isEmpty()?"—":s;}
    private String format(double n,String suffix){return String.format(Locale.getDefault(),"%.1f%s",n,suffix);}
    private void addPreviewRow(String label,String value){
        LinearLayout row=new LinearLayout(requireContext());row.setGravity(android.view.Gravity.CENTER_VERTICAL);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(4,12,4,12);
        TextView l=new TextView(requireContext());l.setText(label);l.setTextColor(android.graphics.Color.parseColor("#59544C"));l.setTextSize(13);l.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(),R.font.manrope_medium));
        TextView v=new TextView(requireContext());v.setText(value);v.setTextColor(android.graphics.Color.parseColor("#1C1B1A"));v.setTextSize(13);v.setGravity(android.view.Gravity.END);v.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(),R.font.manrope_bold));
        row.addView(l,new LinearLayout.LayoutParams(0,-2,1));row.addView(v,new LinearLayout.LayoutParams(0,-2,1));previewContent.addView(row);
        View divider=new View(requireContext());divider.setBackgroundColor(android.graphics.Color.parseColor("#E9E1CF"));previewContent.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}

    private void savePdf(){
        if(generatedFile==null)return;
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/pdf");intent.putExtra(Intent.EXTRA_TITLE,generatedFile.getName());savePdfLauncher.launch(intent);
    }
    private void sharePdf(){
        if(generatedFile==null)return;
        try{Uri uri=FileProvider.getUriForFile(requireContext(),"com.afrinuts.farmos.fileprovider",generatedFile);Intent send=new Intent(Intent.ACTION_SEND);send.setType("application/pdf");send.putExtra(Intent.EXTRA_STREAM,uri);send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(send,"Share PDF"));}
        catch(Exception e){Toast.makeText(requireContext(),"Could not share PDF: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }

    @Override public void onDestroy(){super.onDestroy();executor.shutdown();}
}
