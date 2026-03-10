package com.afrinuts.farmos.utils;

import android.content.Context;
import android.graphics.Color;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.ui.reports.ReportData;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PdfReportGenerator {

    private Context context;
    private ReportData reportData;

    // Brand colors (matching your styles.xml)
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(90, 116, 17);    // #5A7411
    private static final DeviceRgb ACCENT_COLOR = new DeviceRgb(224, 78, 0);      // #E04E00
    private static final DeviceRgb NAVY_COLOR = new DeviceRgb(37, 55, 70);        // #253746
    private static final DeviceRgb GREY_COLOR = new DeviceRgb(222, 218, 212);     // #DEDAD4

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    public PdfReportGenerator(Context context, ReportData reportData) {
        this.context = context;
        this.reportData = reportData;
    }

    public File generateFarmSummaryReport() throws Exception {
        String fileName = "Farm_Summary_" + fileDateFormat.format(new Date()) + ".pdf";
        File pdfFile = new File(context.getExternalFilesDir(null), fileName);

        PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(36, 36, 36, 36);

        // Add header
        addHeader(document, "Farm Summary Report");

        // Farm Information
        addSectionHeader(document, "Farm Information");
        addFarmInfoTable(document);

        // Summary Statistics
        addSectionHeader(document, "Summary Statistics");
        addSummaryStatsTable(document);

        // Profit Overview
        addSectionHeader(document, "Profit Overview");
        addProfitTable(document);

        // Recent Activity
        addSectionHeader(document, "Recent Activity");
        addRecentActivityTable(document);

        // Footer with generation date
        addFooter(document);

        document.close();
        return pdfFile;
    }

    public File generateFinancialStatement() throws Exception {
        String fileName = "Financial_Statement_" + fileDateFormat.format(new Date()) + ".pdf";
        File pdfFile = new File(context.getExternalFilesDir(null), fileName);

        PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(36, 36, 36, 36);

        // Add header
        addHeader(document, "Financial Statement");

        // Date Range
        addDateRange(document);

        // Revenue Summary
        addSectionHeader(document, "Revenue Summary");
        addRevenueTable(document);

        // Expense Summary
        addSectionHeader(document, "Expense Summary");
        addExpenseTable(document);

        // Profit & Loss
        addSectionHeader(document, "Profit & Loss");
        addProfitTable(document);

        // Footer
        addFooter(document);

        document.close();
        return pdfFile;
    }

    private void addHeader(Document document, String title) {
        Paragraph header = new Paragraph()
                .add(new Text("AfriNuts Farm OS\n").setFontSize(24).setBold().setFontColor(PRIMARY_COLOR))
                .add(new Text(title + "\n").setFontSize(18).setFontColor(NAVY_COLOR))
                .add(new Text(reportData.getFarm().getName() + " - " + reportData.getFarm().getLocation() + "\n")
                        .setFontSize(12).setFontColor(NAVY_COLOR))
                .add(new Text("Report Period: " + reportData.getFormattedDateRange() + "\n\n")
                        .setFontSize(10).setFontColor(NAVY_COLOR))
                .setTextAlignment(TextAlignment.CENTER);

        document.add(header);

        // Add horizontal line
        Table line = new Table(1);
        line.addCell(new Cell().setHeight(2).setBackgroundColor(PRIMARY_COLOR).setBorder(Border.NO_BORDER));
        document.add(line);
        document.add(new Paragraph("\n"));
    }

    private void addSectionHeader(Document document, String title) {
        Paragraph header = new Paragraph()
                .add(new Text(title + "\n").setFontSize(14).setBold().setFontColor(PRIMARY_COLOR));
        document.add(header);
    }

    private void addFarmInfoTable(Document document) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);

        addKeyValueRow(table, "Farm Name:", reportData.getFarm().getName());
        addKeyValueRow(table, "Location:", reportData.getFarm().getLocation());
        addKeyValueRow(table, "Total Hectares:", String.format("%.0f ha", reportData.getFarm().getTotalHectares()));
        addKeyValueRow(table, "Cashew Hectares:", String.format("%.0f ha", reportData.getFarm().getCashewHectares()));
        addKeyValueRow(table, "Planting Year:", String.valueOf(reportData.getFarm().getPlantingYear()));
        addKeyValueRow(table, "Total Trees:", String.format("%,d", reportData.getTotalTrees()));

        document.add(table);
    }

    private void addSummaryStatsTable(Document document) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);

        // Header row
        Cell header1 = new Cell().add(new Paragraph("Metric").setBold()).setBackgroundColor(GREY_COLOR);
        Cell header2 = new Cell().add(new Paragraph("Value").setBold()).setBackgroundColor(GREY_COLOR);
        table.addHeaderCell(header1);
        table.addHeaderCell(header2);

        // Data rows
        addStatRow(table, "Total Blocks:", String.valueOf(reportData.getTotalBlocks()));
        addStatRow(table, "Planted Blocks:", reportData.getPlantedBlocks() + "/" + reportData.getTotalBlocks());
        addStatRow(table, "Trees Alive:", reportData.getAliveTrees() + "/" + reportData.getTotalTrees());
        addStatRow(table, "Survival Rate:", String.format("%.1f%%",
                (reportData.getAliveTrees() * 100.0 / reportData.getTotalTrees())));

        document.add(table);
    }

    private void addProfitTable(Document document) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);

        // Header row
        Cell header1 = new Cell().add(new Paragraph("Metric").setBold()).setBackgroundColor(GREY_COLOR);
        Cell header2 = new Cell().add(new Paragraph("Amount").setBold()).setBackgroundColor(GREY_COLOR);
        table.addHeaderCell(header1);
        table.addHeaderCell(header2);

        // Data rows
        addProfitRow(table, "Total Revenue:", reportData.getFormattedTotalRevenue(), PRIMARY_COLOR);
        addProfitRow(table, "Total Expenses:", reportData.getFormattedTotalExpenses(), ACCENT_COLOR);

        // Divider
        Cell dividerCell = new Cell(1, 2).setHeight(2).setBackgroundColor(NAVY_COLOR).setBorder(Border.NO_BORDER);
        table.addCell(dividerCell);

        addProfitRow(table, "Net Profit:", reportData.getFormattedNetProfit(), PRIMARY_COLOR);
        addProfitRow(table, "Profit Margin:", reportData.getFormattedProfitMargin(), PRIMARY_COLOR);

        document.add(table);
    }

    private void addRevenueTable(Document document) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 20, 20, 20}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);

        // Header row
        table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()).setBackgroundColor(GREY_COLOR));
        table.addHeaderCell(new Cell().add(new Paragraph("Quality").setBold()).setBackgroundColor(GREY_COLOR));
        table.addHeaderCell(new Cell().add(new Paragraph("Quantity").setBold()).setBackgroundColor(GREY_COLOR));
        table.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()).setBackgroundColor(GREY_COLOR));

        for (RevenueEntity revenue : reportData.getRevenues()) {
            table.addCell(new Cell().add(new Paragraph(dateFormat.format(new Date(revenue.getHarvestDate())))));
            table.addCell(new Cell().add(new Paragraph(revenue.getQuality().getDisplayName())));
            table.addCell(new Cell().add(new Paragraph(String.format("%.1f kg", revenue.getQuantityKg()))));
            table.addCell(new Cell().add(new Paragraph(String.format("%,.0f XAF", revenue.getTotalAmount()))));
        }

        // Total row
        Cell totalLabelCell = new Cell(1, 3).add(new Paragraph("Total Revenue:").setBold()).setBackgroundColor(GREY_COLOR);
        Cell totalValueCell = new Cell().add(new Paragraph(reportData.getFormattedTotalRevenue()).setBold())
                .setBackgroundColor(GREY_COLOR);
        table.addCell(totalLabelCell);
        table.addCell(totalValueCell);

        document.add(table);
    }

    private void addExpenseTable(Document document) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 30, 30}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);

        // Header row
        table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()).setBackgroundColor(GREY_COLOR));
        table.addHeaderCell(new Cell().add(new Paragraph("Category").setBold()).setBackgroundColor(GREY_COLOR));
        table.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()).setBackgroundColor(GREY_COLOR));

        for (ExpenseEntity expense : reportData.getExpenses()) {
            table.addCell(new Cell().add(new Paragraph(dateFormat.format(new Date(expense.getDate())))));
            table.addCell(new Cell().add(new Paragraph(expense.getCategory().getDisplayName())));
            table.addCell(new Cell().add(new Paragraph(String.format("%,.0f XAF", expense.getAmount()))));
        }

        // Total row
        Cell totalLabelCell = new Cell(1, 2).add(new Paragraph("Total Expenses:").setBold()).setBackgroundColor(GREY_COLOR);
        Cell totalValueCell = new Cell().add(new Paragraph(reportData.getFormattedTotalExpenses()).setBold())
                .setBackgroundColor(GREY_COLOR);
        table.addCell(totalLabelCell);
        table.addCell(totalValueCell);

        document.add(table);
    }

    private void addRecentActivityTable(Document document) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);

        // Header row
        table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()).setBackgroundColor(GREY_COLOR));
        table.addHeaderCell(new Cell().add(new Paragraph("Type").setBold()).setBackgroundColor(GREY_COLOR));
        table.addHeaderCell(new Cell().add(new Paragraph("Description").setBold()).setBackgroundColor(GREY_COLOR));
        table.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()).setBackgroundColor(GREY_COLOR));

        // Show last 10 transactions (mix of revenue and expense)
        int count = 0;
        for (RevenueEntity revenue : reportData.getRevenues()) {
            if (count++ >= 10) break;
            table.addCell(new Cell().add(new Paragraph(dateFormat.format(new Date(revenue.getHarvestDate())))));
            table.addCell(new Cell().add(new Paragraph("Revenue")));
            table.addCell(new Cell().add(new Paragraph(revenue.getQuality().getDisplayName())));
            table.addCell(new Cell().add(new Paragraph(String.format("%,.0f XAF", revenue.getTotalAmount()))));
        }

        for (ExpenseEntity expense : reportData.getExpenses()) {
            if (count++ >= 10) break;
            table.addCell(new Cell().add(new Paragraph(dateFormat.format(new Date(expense.getDate())))));
            table.addCell(new Cell().add(new Paragraph("Expense")));
            table.addCell(new Cell().add(new Paragraph(expense.getCategory().getDisplayName())));
            table.addCell(new Cell().add(new Paragraph(String.format("%,.0f XAF", expense.getAmount()))));
        }

        document.add(table);
    }

    private void addDateRange(Document document) {
        Paragraph dateRange = new Paragraph()
                .add(new Text("Period: " + reportData.getFormattedDateRange() + "\n\n"))
                .setFontSize(12)
                .setFontColor(NAVY_COLOR);
        document.add(dateRange);
    }

    private void addFooter(Document document) {
        Paragraph footer = new Paragraph()
                .add(new Text("\nGenerated on " + dateFormat.format(new Date()) + " by AfriNuts Farm OS"))
                .setFontSize(8)
                .setFontColor(NAVY_COLOR)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(footer);
    }

    private void addKeyValueRow(Table table, String key, String value) {
        table.addCell(new Cell().add(new Paragraph(key).setBold()).setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(value)).setBorder(Border.NO_BORDER));
    }

    private void addStatRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label)));
        table.addCell(new Cell().add(new Paragraph(value)));
    }

    private void addProfitRow(Table table, String label, String value, DeviceRgb color) {
        table.addCell(new Cell().add(new Paragraph(label)));
        table.addCell(new Cell().add(new Paragraph(value).setFontColor(color).setBold()));
    }
}