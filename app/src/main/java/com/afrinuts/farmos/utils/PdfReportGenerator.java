package com.afrinuts.farmos.utils;

import android.content.Context;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.ui.reports.ReportData;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfReportGenerator {

    private final Context context;
    private final ReportData reportData;

    private static final DeviceRgb GREEN = new DeviceRgb(14, 107, 60);       // #0E6B3C
    private static final DeviceRgb DARK = new DeviceRgb(28, 27, 26);         // #1C1B1A
    private static final DeviceRgb MUTED = new DeviceRgb(89, 84, 76);        // #59544C
    private static final DeviceRgb CREAM = new DeviceRgb(246, 241, 231);     // #F6F1E7

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
    private PdfFont regularFont;
    private PdfFont boldFont;

    public PdfReportGenerator(Context context, ReportData reportData) {
        this.context = context;
        this.reportData = reportData;
    }

    public File generateFarmSummaryReport() throws Exception {
        loadFonts();
        String fileName = "Farm_Summary_" + fileDateFormat.format(new Date()) + ".pdf";
        File pdfFile = new File(context.getExternalFilesDir(null), fileName);

        PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(36, 36, 36, 36);
        document.setFont(regularFont);

        addHeader(document, "Farm Summary Report");

        addSectionHeader(document, "Farm Information");
        addFarmInfoTable(document);

        addSectionHeader(document, "Summary Statistics");
        addSummaryStatsTable(document);

        addSectionHeader(document, "Blocks");
        addBlocksTable(document);

        addSectionHeader(document, "Profit Overview");
        addProfitTable(document);

        addSectionHeader(document, "Recent Activity");
        addRecentActivityTable(document);

        addSectionHeader(document, "Tasks");
        addTasksTable(document);

        addFooter(document);
        document.close();
        return pdfFile;
    }

    public File generateFinancialStatement() throws Exception {
        loadFonts();
        String fileName = "Financial_Statement_" + fileDateFormat.format(new Date()) + ".pdf";
        File pdfFile = new File(context.getExternalFilesDir(null), fileName);

        PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(36, 36, 36, 36);
        document.setFont(regularFont);

        addHeader(document, "Financial Statement");
        addDateRange(document);

        addSectionHeader(document, "Revenue Summary");
        addRevenueTable(document);

        addSectionHeader(document, "Expense Summary");
        addExpenseTable(document);

        addSectionHeader(document, "Expenses by Category");
        addExpensesByCategoryTable(document);

        addSectionHeader(document, "Profit & Loss");
        addProfitTable(document);

        addFooter(document);
        document.close();
        return pdfFile;
    }

    public File generateQualityReport() throws Exception {
        loadFonts();
        File pdfFile = new File(context.getExternalFilesDir(null),
                "Quality_Report_" + fileDateFormat.format(new Date()) + ".pdf");
        Document document = new Document(new PdfDocument(new PdfWriter(new FileOutputStream(pdfFile))), PageSize.A4);
        document.setMargins(36, 36, 36, 36);
        document.setFont(regularFont);
        addHeader(document, "Quality Report");
        addDateRange(document);
        List<QualityBatchEntity> batches = reportData.getQualityBatches();
        double kor = 0, moisture = 0, defective = 0;
        int premium = 0, standard = 0, reject = 0;
        for (QualityBatchEntity batch : batches) {
            kor += batch.getKorGPerKg(); moisture += batch.getMoistureContent(); defective += batch.getDefectiveRate();
            if (batch.getQualityGrade() == QualityBatchEntity.QualityGrade.PREMIUM) premium++;
            else if (batch.getQualityGrade() == QualityBatchEntity.QualityGrade.REJECT) reject++;
            else standard++;
        }
        int count = batches.size();
        addSectionHeader(document, "Summary Statistics");
        Table summary = new Table(UnitValue.createPercentArray(new float[]{55, 45})).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(18);
        summary.addHeaderCell(headerCell("Metric")); summary.addHeaderCell(headerCell("Value"));
        addStatRow(summary, "Batches graded", String.valueOf(count));
        addStatRow(summary, "Average KOR", String.format(Locale.getDefault(), "%.1f g/kg", count == 0 ? 0 : kor / count));
        addStatRow(summary, "Average moisture", String.format(Locale.getDefault(), "%.1f%%", count == 0 ? 0 : moisture / count));
        addStatRow(summary, "Average defective", String.format(Locale.getDefault(), "%.1f%%", count == 0 ? 0 : defective / count));
        addStatRow(summary, "Premium / Standard / Reject", premium + " / " + standard + " / " + reject);
        addStatRow(summary, "Grade shares", gradeShare(premium,count) + " / " + gradeShare(standard,count) + " / " + gradeShare(reject,count));
        document.add(summary);
        addSectionHeader(document, "Batch Results");
        Table table = new Table(UnitValue.createPercentArray(new float[]{18, 20, 11, 13, 11, 13, 14})).setWidth(UnitValue.createPercentValue(100));
        String[] headers = {"Batch", "Supplier", "KOR g/kg", "Grade", "Moisture", "Defective", "Nut count/kg"};
        for (String header : headers) table.addHeaderCell(headerCell(header));
        for (QualityBatchEntity batch : batches) {
            table.addCell(bodyCell(valueOrDash(batch.getBatchNumber())));
            table.addCell(bodyCell(valueOrDash(batch.getSupplierName())));
            table.addCell(bodyCell(String.format(Locale.getDefault(), "%.1f", batch.getKorGPerKg())));
            table.addCell(bodyCell(batch.getQualityGrade() == null ? "—" : batch.getQualityGrade().getDisplayName()));
            table.addCell(bodyCell(String.format(Locale.getDefault(), "%.1f%%", batch.getMoistureContent())));
            table.addCell(bodyCell(String.format(Locale.getDefault(), "%.1f%%", batch.getDefectiveRate())));
            table.addCell(bodyCell(String.valueOf(batch.getNutCountPerKg())));
        }
        document.add(table);
        addFooter(document); document.close(); return pdfFile;
    }

    public File generateTraceabilityReport() throws Exception {
        loadFonts();
        File pdfFile = new File(context.getExternalFilesDir(null),
                "Traceability_Report_" + fileDateFormat.format(new Date()) + ".pdf");
        Document document = new Document(new PdfDocument(new PdfWriter(new FileOutputStream(pdfFile))), PageSize.A4);
        document.setMargins(36, 36, 36, 36);
        document.setFont(regularFont);
        addHeader(document, "Traceability Report");
        addDateRange(document);
        List<BatchTraceabilityEntity> lots = reportData.getTraceabilities();
        List<MasterBatchTraceabilityEntity> masters = reportData.getMasters();
        int exported = 0; double weight = 0;
        for (BatchTraceabilityEntity lot : lots) { if (lot.getExportDate() != null && lot.getExportDate() > 0) exported++; weight += lot.getWeightKg(); }
        addSectionHeader(document, "Summary Statistics");
        Table summary = new Table(UnitValue.createPercentArray(new float[]{55,45})).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(18);
        summary.addHeaderCell(headerCell("Metric")); summary.addHeaderCell(headerCell("Value"));
        addStatRow(summary,"Traced lots",String.valueOf(lots.size())); addStatRow(summary,"Exported lots",String.valueOf(exported));
        addStatRow(summary,"Total weight",String.format(Locale.getDefault(),"%,.0f kg",weight)); document.add(summary);
        addSectionHeader(document,"Traceability Lots");
        Table table = new Table(UnitValue.createPercentArray(new float[]{18,18,17,15,14,18})).setWidth(UnitValue.createPercentValue(100));
        String[] headers={"Lot / batch","Buyer","Destination","Export date","Grade","Weight"};
        for(String header:headers)table.addHeaderCell(headerCell(header));
        for(BatchTraceabilityEntity lot:lots){
            table.addCell(bodyCell(valueOrDash(lot.getBatchNumber()))); table.addCell(bodyCell(valueOrDash(lot.getBuyer())));
            table.addCell(bodyCell(valueOrDash(lot.getDestination())));
            table.addCell(bodyCell(lot.getExportDate()==null||lot.getExportDate()<=0?"—":dateFormat.format(new Date(lot.getExportDate()))));
            table.addCell(bodyCell(valueOrDash(lot.getGrade()))); table.addCell(bodyCell(String.format(Locale.getDefault(),"%,.0f kg",lot.getWeightKg())));
        }
        document.add(table);
        addSectionHeader(document,"Master Shipments");
        Table masterTable = new Table(UnitValue.createPercentArray(new float[]{18,18,17,15,12,20})).setWidth(UnitValue.createPercentValue(100));
        String[] masterHeaders={"Master number","Buyer","Destination","Export date","Batches","Weight"};
        for(String header:masterHeaders)masterTable.addHeaderCell(headerCell(header));
        for(MasterBatchTraceabilityEntity master:masters){
            masterTable.addCell(bodyCell(valueOrDash(master.getMasterBatchNumber()))); masterTable.addCell(bodyCell(valueOrDash(master.getBuyer())));
            masterTable.addCell(bodyCell(valueOrDash(master.getDestination())));
            masterTable.addCell(bodyCell(master.getExportDate()<=0?"—":dateFormat.format(new Date(master.getExportDate()))));
            masterTable.addCell(bodyCell(String.valueOf(master.getTotalBatches())));
            masterTable.addCell(bodyCell(String.format(Locale.getDefault(),"%,.0f kg",master.getTotalWeightKg())));
        }
        document.add(masterTable); addFooter(document); document.close(); return pdfFile;
    }

    private String gradeShare(int value, int total) {
        return String.format(Locale.getDefault(), "%.1f%%", total == 0 ? 0 : value * 100.0 / total);
    }

    private void loadFonts() throws Exception {
        regularFont = loadFont(R.font.manrope_regular);
        boldFont = loadFont(R.font.manrope_bold);
    }

    private PdfFont loadFont(int resourceId) throws Exception {
        try (InputStream input = context.getResources().openRawResource(resourceId);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return PdfFontFactory.createFont(output.toByteArray(), PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
        }
    }

    private void addHeader(Document document, String title) {
        Paragraph header = new Paragraph()
                .add(new Text("AfriNuts Farm OS\n").setFont(boldFont).setFontSize(24).setFontColor(GREEN))
                .add(new Text(title + "\n").setFont(boldFont).setFontSize(18).setFontColor(DARK))
                .add(new Text(safe(reportData.getFarm().getName()) + " - "
                        + safe(reportData.getFarm().getLocation()) + "\n")
                        .setFont(regularFont).setFontSize(12).setFontColor(DARK))
                .add(new Text("Report Period: " + reportData.getFormattedDateRange() + "\n\n")
                        .setFont(regularFont).setFontSize(10).setFontColor(DARK))
                .setTextAlignment(TextAlignment.CENTER);
        document.add(header);

        Table line = new Table(1);
        line.setWidth(UnitValue.createPercentValue(100));
        line.addCell(new Cell().setHeight(2).setBackgroundColor(GREEN).setBorder(Border.NO_BORDER));
        document.add(line);
        document.add(new Paragraph("\n").setFont(regularFont));
    }

    private void addSectionHeader(Document document, String title) {
        document.add(new Paragraph(title + "\n").setFont(boldFont).setFontSize(14).setFontColor(GREEN));
    }

    private void addFarmInfoTable(Document document) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);

        addKeyValueRow(table, "Farm Name:", safe(reportData.getFarm().getName()));
        addKeyValueRow(table, "Location:", safe(reportData.getFarm().getLocation()));
        addKeyValueRow(table, "Total Hectares:", String.format(Locale.getDefault(), "%.0f ha", reportData.getFarm().getTotalHectares()));
        addKeyValueRow(table, "Cashew Hectares:", String.format(Locale.getDefault(), "%.0f ha", reportData.getFarm().getCashewHectares()));
        addKeyValueRow(table, "Planting Year:", String.valueOf(reportData.getFarm().getPlantingYear()));
        addKeyValueRow(table, "Total Trees:", String.format(Locale.getDefault(), "%,d", reportData.getTotalTrees()));
        document.add(table);
    }

    private void addSummaryStatsTable(Document document) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);
        table.addHeaderCell(headerCell("Metric"));
        table.addHeaderCell(headerCell("Value"));

        addStatRow(table, "Total Blocks:", String.valueOf(reportData.getTotalBlocks()));
        addStatRow(table, "Planted Blocks:", reportData.getPlantedBlocks() + "/" + reportData.getTotalBlocks());
        addStatRow(table, "Trees Alive:", reportData.getAliveTrees() + "/" + reportData.getTotalTrees());
        addStatRow(table, "Survival Rate:", String.format(Locale.getDefault(), "%.1f%%",
                reportData.getTotalTrees() > 0 ? reportData.getAliveTrees() * 100.0 / reportData.getTotalTrees() : 0));
        document.add(table);
    }

    private void addBlocksTable(Document document) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{22, 14, 24, 18, 22}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);
        table.setKeepTogether(false);
        table.addHeaderCell(headerCell("Block"));
        table.addHeaderCell(headerCell("Hectares"));
        table.addHeaderCell(headerCell("Trees alive / total"));
        table.addHeaderCell(headerCell("Survival"));
        table.addHeaderCell(headerCell("Status"));

        for (BlockEntity block : reportData.getBlocks()) {
            int aliveTrees = block.getAliveTrees();
            table.addCell(bodyCell(safe(block.getBlockName())));
            table.addCell(bodyCell(String.format(Locale.getDefault(), "%.1f ha", block.getHectareSize())));
            table.addCell(bodyCell(aliveTrees + " / 100"));
            table.addCell(bodyCell(String.format(Locale.getDefault(), "%.1f%%", block.getSurvivalRate())));
            table.addCell(bodyCell(block.getStatus() == null ? "—" : block.getStatus().getDisplayName()));
        }
        document.add(table);
    }

    private void addProfitTable(Document document) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);
        table.addHeaderCell(headerCell("Metric"));
        table.addHeaderCell(headerCell("Amount"));

        addProfitRow(table, "Total Revenue:", reportData.getFormattedTotalRevenue(), GREEN);
        addProfitRow(table, "Total Expenses:", reportData.getFormattedTotalExpenses(), MUTED);

        Cell dividerCell = new Cell(1, 2).setHeight(2).setBackgroundColor(DARK).setBorder(Border.NO_BORDER);
        table.addCell(dividerCell);

        addProfitRow(table, "Net Profit:", reportData.getFormattedNetProfit(), GREEN);
        addProfitRow(table, "Profit Margin:", reportData.getFormattedProfitMargin(), GREEN);
        document.add(table);
    }

    private void addRevenueTable(Document document) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{16, 20, 14, 14, 18, 18}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);
        table.addHeaderCell(headerCell("Date"));
        table.addHeaderCell(headerCell("Buyer"));
        table.addHeaderCell(headerCell("Quality"));
        table.addHeaderCell(headerCell("Quantity"));
        table.addHeaderCell(headerCell("Price per kg"));
        table.addHeaderCell(headerCell("Amount"));

        for (RevenueEntity revenue : reportData.getRevenues()) {
            table.addCell(bodyCell(dateFormat.format(new Date(revenue.getCreatedAt()))));
            table.addCell(bodyCell(valueOrDash(revenue.getBuyer())));
            table.addCell(bodyCell(revenue.getQuality() == null ? "—" : revenue.getQuality().getDisplayName()));
            table.addCell(bodyCell(String.format(Locale.getDefault(), "%.1f kg", revenue.getQuantityKg())));
            String pricePerKg = revenue.getQuantityKg() == 0
                    ? "—"
                    : String.format(Locale.getDefault(), "%,.0f XAF/kg", revenue.getTotalAmount() / revenue.getQuantityKg());
            table.addCell(bodyCell(pricePerKg));
            table.addCell(bodyCell(String.format(Locale.getDefault(), "%,.0f XAF", revenue.getTotalAmount())));
        }

        table.addCell(headerCell("Total Revenue:", 1, 5));
        table.addCell(headerCell(reportData.getFormattedTotalRevenue()));
        document.add(table);
    }

    private void addExpenseTable(Document document) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 30, 30}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);
        table.addHeaderCell(headerCell("Date"));
        table.addHeaderCell(headerCell("Category"));
        table.addHeaderCell(headerCell("Amount"));

        for (ExpenseEntity expense : reportData.getExpenses()) {
            table.addCell(bodyCell(dateFormat.format(new Date(expense.getDate()))));
            table.addCell(bodyCell(categoryName(expense)));
            table.addCell(bodyCell(String.format(Locale.getDefault(), "%,.0f XAF", expense.getAmount())));
        }

        table.addCell(headerCell("Total Expenses:", 1, 2));
        table.addCell(headerCell(reportData.getFormattedTotalExpenses()));
        document.add(table);
    }

    private void addExpensesByCategoryTable(Document document) {
        Map<String, Double> totalsByCategory = new HashMap<>();
        for (ExpenseEntity expense : reportData.getExpenses()) {
            String category = categoryName(expense);
            totalsByCategory.put(category, totalsByCategory.getOrDefault(category, 0.0) + expense.getAmount());
        }

        List<Map.Entry<String, Double>> entries = new ArrayList<>(totalsByCategory.entrySet());
        entries.sort((first, second) -> Double.compare(second.getValue(), first.getValue()));

        Table table = new Table(UnitValue.createPercentArray(new float[]{62, 38}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);
        table.addHeaderCell(headerCell("Category"));
        table.addHeaderCell(headerCell("Amount"));

        for (Map.Entry<String, Double> entry : entries) {
            table.addCell(bodyCell(entry.getKey()));
            table.addCell(bodyCell(String.format(Locale.getDefault(), "%,.0f XAF", entry.getValue())));
        }

        table.addCell(headerCell("Total Expenses:", 1, 1));
        table.addCell(headerCell(reportData.getFormattedTotalExpenses()));
        document.add(table);
    }

    private void addRecentActivityTable(Document document) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);
        table.addHeaderCell(headerCell("Date"));
        table.addHeaderCell(headerCell("Type"));
        table.addHeaderCell(headerCell("Description"));
        table.addHeaderCell(headerCell("Amount"));

        int count = 0;
        for (RevenueEntity revenue : reportData.getRevenues()) {
            if (count++ >= 10) break;
            String quality = revenue.getQuality() == null ? "—" : revenue.getQuality().getDisplayName();
            String description = quality + " · " + valueOrDash(revenue.getBuyer());
            table.addCell(bodyCell(dateFormat.format(new Date(revenue.getCreatedAt()))));
            table.addCell(bodyCell("Revenue"));
            table.addCell(bodyCell(description));
            table.addCell(bodyCell(String.format(Locale.getDefault(), "%,.0f XAF", revenue.getTotalAmount())));
        }

        for (ExpenseEntity expense : reportData.getExpenses()) {
            if (count++ >= 10) break;
            table.addCell(bodyCell(dateFormat.format(new Date(expense.getDate()))));
            table.addCell(bodyCell("Expense"));
            table.addCell(bodyCell(categoryName(expense)));
            table.addCell(bodyCell(String.format(Locale.getDefault(), "%,.0f XAF", expense.getAmount())));
        }
        document.add(table);
    }

    private void addTasksTable(Document document) {
        List<TaskEntity> tasks = reportData.getTasks();
        if (tasks == null || tasks.isEmpty()) {
            document.add(new Paragraph("No tasks in this period.").setFont(regularFont).setFontSize(10).setFontColor(DARK));
            return;
        }

        int completedInPeriod = 0;
        for (TaskEntity task : tasks) {
            Long completedDate = task.getCompletedDate();
            if (task.getStatus() == TaskEntity.TaskStatus.COMPLETED && completedDate != null
                    && isInReportPeriod(completedDate)) {
                completedInPeriod++;
            }
        }
        double completionPercent = completedInPeriod * 100.0 / tasks.size();
        document.add(new Paragraph(String.format(Locale.getDefault(), "Completed %d of %d tasks in the period (%.1f%%)",
                completedInPeriod, tasks.size(), completionPercent))
                .setFont(regularFont).setFontSize(10).setFontColor(DARK).setMarginBottom(6));

        Table table = new Table(UnitValue.createPercentArray(new float[]{36, 20, 20, 24}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);
        table.addHeaderCell(headerCell("Task"));
        table.addHeaderCell(headerCell("Type"));
        table.addHeaderCell(headerCell("Status"));
        table.addHeaderCell(headerCell("Due / Completed date"));

        for (TaskEntity task : tasks) {
            table.addCell(bodyCell(valueOrDash(task.getTitle())));
            table.addCell(bodyCell(task.getType() == null ? "—" : task.getType().getDisplayName()));
            table.addCell(bodyCell(task.getStatus() == null ? "—" : task.getStatus().getDisplayName()));

            Long completedDate = task.getCompletedDate();
            boolean taskCompletedInPeriod = task.getStatus() == TaskEntity.TaskStatus.COMPLETED
                    && completedDate != null && isInReportPeriod(completedDate);
            String date = taskCompletedInPeriod
                    ? "Completed " + dateFormat.format(new Date(completedDate))
                    : "Due " + dateFormat.format(new Date(task.getDueDate()));
            table.addCell(bodyCell(date));
        }
        document.add(table);
    }

    private void addDateRange(Document document) {
        document.add(new Paragraph("Period: " + reportData.getFormattedDateRange() + "\n\n")
                .setFont(regularFont).setFontSize(12).setFontColor(DARK));
    }

    private void addFooter(Document document) {
        document.add(new Paragraph("\nGenerated on " + dateFormat.format(new Date())
                + " by AfriNuts Farm OS")
                .setFont(regularFont).setFontSize(8).setFontColor(MUTED)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private void addKeyValueRow(Table table, String key, String value) {
        table.addCell(boldCell(key).setBorder(Border.NO_BORDER));
        table.addCell(bodyCell(value).setBorder(Border.NO_BORDER));
    }

    private void addStatRow(Table table, String label, String value) {
        table.addCell(bodyCell(label));
        table.addCell(bodyCell(value));
    }

    private void addProfitRow(Table table, String label, String value, DeviceRgb valueColor) {
        table.addCell(bodyCell(label));
        table.addCell(new Cell().add(new Paragraph(safe(value))
                .setFont(boldFont).setFontColor(valueColor)));
    }

    private Cell bodyCell(String value) {
        return new Cell().add(new Paragraph(safe(value)).setFont(regularFont).setFontColor(DARK));
    }

    private Cell boldCell(String value) {
        return new Cell().add(new Paragraph(safe(value)).setFont(boldFont).setFontColor(DARK));
    }

    private Cell headerCell(String value) {
        return headerCell(value, 1, 1);
    }

    private Cell headerCell(String value, int rowSpan, int colSpan) {
        return new Cell(rowSpan, colSpan)
                .add(new Paragraph(safe(value)).setFont(boldFont).setFontColor(DARK))
                .setBackgroundColor(CREAM);
    }

    private String categoryName(ExpenseEntity expense) {
        return expense.getCategory() == null ? "—" : expense.getCategory().getDisplayName();
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "—" : value;
    }

    private boolean isInReportPeriod(long timestamp) {
        return timestamp >= reportData.getStartDate().getTime()
                && timestamp <= reportData.getEndDate().getTime();
    }
}
