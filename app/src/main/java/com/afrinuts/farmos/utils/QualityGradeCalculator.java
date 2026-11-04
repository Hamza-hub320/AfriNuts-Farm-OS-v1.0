package com.afrinuts.farmos.utils;

import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;

/**
 * Auto-computes qualityGrade from measurements. The grade is NEVER typed in.
 *
 * Two rule sets, two scores. RCN and kernel inputs are never averaged together.
 *
 * Thresholds are industry-standard planning figures (per the Phase One model
 * and exporter specs as of 2026-10). Confirm with Muhamadue against real intake
 * data before locking them in — change any value in the Thresholds class below
 * and every caller picks it up.
 */
public final class QualityGradeCalculator {

    public enum Grade { PREMIUM, STANDARD, REJECT }

    /** All thresholds in one place so they're trivially reviewable and editable. */
    public static final class Thresholds {
        // --- RCN ---
        // KOR at purchase (%)
        public static final double RCN_KOR_PREMIUM_MIN  = 48.0;
        public static final double RCN_KOR_REJECT_BELOW = 44.0;
        // Moisture (%)
        public static final double RCN_MOISTURE_PREMIUM_MAX = 8.0;
        public static final double RCN_MOISTURE_REJECT_ABOVE = 10.0;
        // Nut count (/kg)
        public static final int    RCN_NUTCOUNT_PREMIUM_MAX = 180;
        public static final int    RCN_NUTCOUNT_REJECT_ABOVE = 210;
        // Total defective (%)
        public static final double RCN_DEFECTIVE_PREMIUM_MAX = 5.0;
        public static final double RCN_DEFECTIVE_REJECT_ABOVE = 10.0;
        // Mouldy (%)
        public static final double RCN_MOULDY_PREMIUM_MAX = 1.0;
        public static final double RCN_MOULDY_REJECT_ABOVE = 3.0;
        // Foreign matter (%)
        public static final double RCN_FOREIGN_PREMIUM_MAX = 0.5;
        public static final double RCN_FOREIGN_REJECT_ABOVE = 1.0;

        // --- Kernel ---
        public static final double KERNEL_BROKEN_PREMIUM_MAX = 5.0;
        public static final double KERNEL_BROKEN_REJECT_ABOVE = 15.0;
        public static final double KERNEL_MOISTURE_PREMIUM_MAX = 4.0;
        public static final double KERNEL_MOISTURE_REJECT_ABOVE = 5.0;
        public static final double KERNEL_AFLATOXIN_PREMIUM_MAX = 0.5;
        public static final double KERNEL_AFLATOXIN_REJECT_AT = 4.0;
        public static final double KERNEL_HEAVYMETALS_PREMIUM_MAX = 0.1;
        public static final double KERNEL_HEAVYMETALS_REJECT_AT = 0.5;
        public static final double KERNEL_PESTICIDE_PREMIUM_MAX = 0.1;
        public static final double KERNEL_PESTICIDE_REJECT_AT = 0.5;

        private Thresholds() {}
    }

    private QualityGradeCalculator() {}

    /** Dispatch on productType. Defaults to RCN when unset (new intake). */
    public static Grade grade(QualityBatchEntity b) {
        if (b == null) return Grade.REJECT;
        if (b.isKernel()) return gradeKernel(b);
        return gradeRcn(b);
    }

    // ========================================
    // RCN
    // ========================================

    private static Grade gradeRcn(QualityBatchEntity b) {
        double kor       = b.getKorPercent();
        double moisture  = b.getMoistureContent();
        int    nutCount  = b.getNutCountPerKg();
        double defective = b.getDefectiveRate();
        double mouldy    = b.getMouldyRate();
        double foreign   = b.getForeignMatter();

        // Hard floors — REJECT before anything else.
        if (kor < Thresholds.RCN_KOR_REJECT_BELOW
                || moisture > Thresholds.RCN_MOISTURE_REJECT_ABOVE
                || nutCount > Thresholds.RCN_NUTCOUNT_REJECT_ABOVE
                || defective > Thresholds.RCN_DEFECTIVE_REJECT_ABOVE
                || mouldy > Thresholds.RCN_MOULDY_REJECT_ABOVE
                || foreign > Thresholds.RCN_FOREIGN_REJECT_ABOVE) {
            return Grade.REJECT;
        }

        // Premium — every condition must hold.
        boolean premium =
                kor >= Thresholds.RCN_KOR_PREMIUM_MIN
                        && moisture <= Thresholds.RCN_MOISTURE_PREMIUM_MAX
                        && nutCount <= Thresholds.RCN_NUTCOUNT_PREMIUM_MAX
                        && defective <= Thresholds.RCN_DEFECTIVE_PREMIUM_MAX
                        && mouldy <= Thresholds.RCN_MOULDY_PREMIUM_MAX
                        && foreign <= Thresholds.RCN_FOREIGN_PREMIUM_MAX;

        return premium ? Grade.PREMIUM : Grade.STANDARD;
    }

    // ========================================
    // Kernel
    // ========================================

    private static Grade gradeKernel(QualityBatchEntity b) {
        double broken     = b.getBrokenKernels();
        double moisture   = b.getMoistureContent();
        double aflatoxin  = b.getAflatoxinPpb();
        double heavyMetal = b.getHeavyMetalsPpb();
        double pesticide  = b.getPesticideResiduePpb();

        if (broken > Thresholds.KERNEL_BROKEN_REJECT_ABOVE
                || moisture > Thresholds.KERNEL_MOISTURE_REJECT_ABOVE
                || aflatoxin >= Thresholds.KERNEL_AFLATOXIN_REJECT_AT
                || heavyMetal >= Thresholds.KERNEL_HEAVYMETALS_REJECT_AT
                || pesticide >= Thresholds.KERNEL_PESTICIDE_REJECT_AT) {
            return Grade.REJECT;
        }

        boolean premium =
                broken <= Thresholds.KERNEL_BROKEN_PREMIUM_MAX
                        && moisture <= Thresholds.KERNEL_MOISTURE_PREMIUM_MAX
                        && aflatoxin <= Thresholds.KERNEL_AFLATOXIN_PREMIUM_MAX
                        && heavyMetal <= Thresholds.KERNEL_HEAVYMETALS_PREMIUM_MAX
                        && pesticide <= Thresholds.KERNEL_PESTICIDE_PREMIUM_MAX;

        return premium ? Grade.PREMIUM : Grade.STANDARD;
    }

    // ========================================
    // Scores (separate, never averaged)
    // ========================================

    /**
     * RCN score, 0–100. Simple weighted blend of the metrics that drive price.
     * Starts at 100 and subtracts for each deviation.
     */
    public static double rcnScore(QualityBatchEntity b) {
        if (b == null) return 0;

        double score = 100.0;

        // KOR: full marks at ≥48, linear penalty down to 44 (REJECT).
        if (b.getKorPercent() < Thresholds.RCN_KOR_PREMIUM_MIN) {
            double gap = Thresholds.RCN_KOR_PREMIUM_MIN - b.getKorPercent();
            double maxGap = Thresholds.RCN_KOR_PREMIUM_MIN - Thresholds.RCN_KOR_REJECT_BELOW;
            score -= (gap / maxGap) * 30.0;   // up to −30
        }

        // Moisture: full marks at ≤8, linear to 10 (REJECT).
        if (b.getMoistureContent() > Thresholds.RCN_MOISTURE_PREMIUM_MAX) {
            double gap = b.getMoistureContent() - Thresholds.RCN_MOISTURE_PREMIUM_MAX;
            double maxGap = Thresholds.RCN_MOISTURE_REJECT_ABOVE - Thresholds.RCN_MOISTURE_PREMIUM_MAX;
            score -= (gap / maxGap) * 15.0;   // up to −15
        }

        // Defective: subtract linearly, cap at 15 points.
        score -= Math.min(15.0, b.getDefectiveRate() * 1.5);

        // Mouldy: heavily penalised, cap at 20 points.
        score -= Math.min(20.0, b.getMouldyRate() * 6.0);

        // Foreign matter: cap at 10 points.
        score -= Math.min(10.0, b.getForeignMatter() * 10.0);

        // Nut count: subtract if above 180.
        if (b.getNutCountPerKg() > Thresholds.RCN_NUTCOUNT_PREMIUM_MAX) {
            double excess = b.getNutCountPerKg() - Thresholds.RCN_NUTCOUNT_PREMIUM_MAX;
            score -= Math.min(10.0, excess * 0.5);
        }

        return clamp(score);
    }

    /** Kernel score, 0–100. Independent of the RCN score. */
    public static double kernelScore(QualityBatchEntity b) {
        if (b == null) return 0;

        double score = 100.0;

        score -= Math.min(30.0, b.getBrokenKernels() * 2.0);

        if (b.getMoistureContent() > Thresholds.KERNEL_MOISTURE_PREMIUM_MAX) {
            double gap = b.getMoistureContent() - Thresholds.KERNEL_MOISTURE_PREMIUM_MAX;
            score -= Math.min(15.0, gap * 15.0);
        }

        // Aflatoxin dominates kernel price. Scale against the reject ceiling.
        if (b.getAflatoxinPpb() > 0) {
            double ratio = Math.min(1.0, b.getAflatoxinPpb() / Thresholds.KERNEL_AFLATOXIN_REJECT_AT);
            score -= ratio * 25.0;
        }
        if (b.getPesticideResiduePpb() > 0) {
            double ratio = Math.min(1.0, b.getPesticideResiduePpb() / Thresholds.KERNEL_PESTICIDE_REJECT_AT);
            score -= ratio * 10.0;
        }
        if (b.getHeavyMetalsPpb() > 0) {
            double ratio = Math.min(1.0, b.getHeavyMetalsPpb() / Thresholds.KERNEL_HEAVYMETALS_REJECT_AT);
            score -= ratio * 10.0;
        }

        return clamp(score);
    }

    /** Convenience: whichever score belongs to this batch's product type. */
    public static double score(QualityBatchEntity b) {
        if (b == null) return 0;
        return b.isKernel() ? kernelScore(b) : rcnScore(b);
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(100.0, v));
    }

    // ========================================
    // Defect-sum consistency check
    // ========================================

    /**
     * Returns true if the four defect rates add up to defectiveRate
     * within ±0.5%. Only meaningful for RCN batches.
     * Callers should hold the batch for re-inspection if this is false.
     */
    public static boolean defectsConsistent(QualityBatchEntity b) {
        if (b == null || b.isKernel()) return true;
        double sum = b.getMouldyRate()
                + b.getImmatureRate()
                + b.getInsectDamagedRate()
                + b.getShrivelledRate();
        return Math.abs(sum - b.getDefectiveRate()) <= 0.5;
    }
}