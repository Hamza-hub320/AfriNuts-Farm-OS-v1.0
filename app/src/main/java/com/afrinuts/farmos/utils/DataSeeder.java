package com.afrinuts.farmos.utils;

import android.content.Context;
import android.util.Log;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class DataSeeder {

    private static final String TAG = "DataSeeder";

    public static void seedInitialExpenses(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);

            // Get the farm
            FarmEntity farm = db.farmDao().getFirstFarm();
            if (farm == null) {
                Log.e(TAG, "No farm found. Please run app first to create farm.");
                return;
            }

            // Check if expenses already exist
            double totalExpenses = db.expenseDao().getTotalExpenses(farm.getId());
            if (totalExpenses > 0) {
                Log.d(TAG, "Expenses already seeded. Skipping...");
                return;
            }

            Log.d(TAG, "Seeding initial expenses for AfriNuts farm...");

            // Parse dates (using current year as reference)
            int currentYear = 2024; // Your planting year

            // 1. Land Clearing - 35 hectares @ 150,000 XAF/hectare
            long landClearingDate = getDate(currentYear, 1, 15); // Jan 15, 2024
            ExpenseEntity landClearing = new ExpenseEntity(
                    farm.getId(),
                    null, // Farm-wide expense
                    ExpenseEntity.ExpenseCategory.LAND_CLEARING,
                    35 * 150000, // 5,250,000 XAF
                    landClearingDate,
                    "Land clearing for 35-hectare cashew plantation"
            );
            db.expenseDao().insert(landClearing);

            // 2. Perimeter Fencing - 35 hectares
            long fencingDate = getDate(currentYear, 2, 10); // Feb 10, 2024
            ExpenseEntity fencing = new ExpenseEntity(
                    farm.getId(),
                    null,
                    ExpenseEntity.ExpenseCategory.FENCING,
                    8500000, // 8,500,000 XAF (estimated)
                    fencingDate,
                    "Perimeter fencing around 35-hectare plantation"
            );
            db.expenseDao().insert(fencing);

            // 3. Plowing - 35 hectares @ 75,000 XAF/hectare
            long plowingDate = getDate(currentYear, 2, 25); // Feb 25, 2024
            ExpenseEntity plowing = new ExpenseEntity(
                    farm.getId(),
                    null,
                    ExpenseEntity.ExpenseCategory.PLOWING,
                    35 * 75000, // 2,625,000 XAF
                    plowingDate,
                    "Tractor plowing for entire plantation"
            );
            db.expenseDao().insert(plowing);

            // 4. Seedlings - 4,000 trees @ 2,500 XAF/seedling
            long seedlingsDate = getDate(currentYear, 3, 5); // Mar 5, 2024
            ExpenseEntity seedlings = new ExpenseEntity(
                    farm.getId(),
                    null,
                    ExpenseEntity.ExpenseCategory.SEEDLINGS,
                    4000 * 2500, // 10,000,000 XAF
                    seedlingsDate,
                    "Cashew seedlings (3,500 main + 500 replacements)"
            );
            db.expenseDao().insert(seedlings);

            // 5. Security Guard - 1 year contract
            long securityDate = getDate(currentYear, 3, 1); // Mar 1, 2024
            ExpenseEntity security = new ExpenseEntity(
                    farm.getId(),
                    null,
                    ExpenseEntity.ExpenseCategory.SECURITY,
                    3600000, // 3,600,000 XAF (300,000/month)
                    securityDate,
                    "Annual security guard contract"
            );
            db.expenseDao().insert(security);

            // 6. Labor & Supervision (first 3 months)
            long laborDate1 = getDate(currentYear, 1, 31); // Jan 31, 2024
            ExpenseEntity laborJan = new ExpenseEntity(
                    farm.getId(),
                    null,
                    ExpenseEntity.ExpenseCategory.LABOR,
                    1250000, // 1,250,000 XAF
                    laborDate1,
                    "January labor - clearing and preparation"
            );
            db.expenseDao().insert(laborJan);

            long laborDate2 = getDate(currentYear, 2, 28); // Feb 28, 2024
            ExpenseEntity laborFeb = new ExpenseEntity(
                    farm.getId(),
                    null,
                    ExpenseEntity.ExpenseCategory.LABOR,
                    1450000, // 1,450,000 XAF
                    laborDate2,
                    "February labor - plowing and fencing"
            );
            db.expenseDao().insert(laborFeb);

            long laborDate3 = getDate(currentYear, 3, 31); // Mar 31, 2024
            ExpenseEntity laborMar = new ExpenseEntity(
                    farm.getId(),
                    null,
                    ExpenseEntity.ExpenseCategory.LABOR,
                    1850000, // 1,850,000 XAF
                    laborDate3,
                    "March labor - planting and irrigation"
            );
            db.expenseDao().insert(laborMar);

            // 7. Block-specific expenses (example: extra labor on specific blocks)
            List<BlockEntity> blocks = db.blockDao().getBlocksByFarmId(farm.getId());

            if (!blocks.isEmpty()) {
                // Find Block A1 for example
                for (BlockEntity block : blocks) {
                    if (block.getBlockName().equals("A1")) {
                        // Extra fertilization for Block A1
                        long blockExpenseDate = getDate(currentYear, 4, 10); // Apr 10, 2024
                        ExpenseEntity blockFertilizer = new ExpenseEntity(
                                farm.getId(),
                                block.getId(),
                                ExpenseEntity.ExpenseCategory.FERTILIZER,
                                250000, // 250,000 XAF
                                blockExpenseDate,
                                "Initial fertilization for Block A1"
                        );
                        db.expenseDao().insert(blockFertilizer);
                        break;
                    }
                }
            }

            // 8. Processing Center (separate - 15 hectares)
            long processingDate = getDate(currentYear, 5, 15); // May 15, 2024
            ExpenseEntity processingCenter = new ExpenseEntity(
                    farm.getId(),
                    null,
                    ExpenseEntity.ExpenseCategory.PROCESSING_CENTER,
                    15000000, // 15,000,000 XAF (estimated)
                    processingDate,
                    "Processing center infrastructure (15 hectares)"
            );
            db.expenseDao().insert(processingCenter);

            // Calculate totals
            double total = db.expenseDao().getTotalExpenses(farm.getId());
            double farmWide = db.expenseDao().getTotalFarmWideExpenses(farm.getId());

            Log.d(TAG, String.format(Locale.getDefault(),
                    "✅ Seeded %d expenses\n" +
                            "   Total Investment: %,d XAF\n" +
                            "   Farm-wide: %,d XAF\n" +
                            "   Processing Center: 15,000,000 XAF",
                    8, (int) total, (int) farmWide));
        });
    }

    private static long getDate(int year, int month, int day) {
        // month is 1-based (January = 1)
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(year, month - 1, day, 0, 0, 0);
        return calendar.getTimeInMillis();
    }
}