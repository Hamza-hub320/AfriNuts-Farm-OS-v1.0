package com.afrinuts.farmos.data.local.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration25To26 extends Migration {

    private static final String TAG = "Migration25To26";

    public Migration25To26() {
        super(25, 26);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "🚀 Starting migration from version 25 to 26 - Adding Compliance tables");

        try {
            // ========================================
            // 1. Create certifications table
            // ========================================
            database.execSQL("CREATE TABLE IF NOT EXISTS certifications (" +
                    "certificationId TEXT PRIMARY KEY NOT NULL, " +
                    "farmId INTEGER NOT NULL, " +
                    "certificationName TEXT, " +
                    "certificationBody TEXT, " +
                    "status TEXT, " +
                    "scope TEXT, " +
                    "applicableStandardsJson TEXT, " +
                    "targetDate INTEGER NOT NULL DEFAULT 0, " +
                    "approvalDate INTEGER NOT NULL DEFAULT 0, " +
                    "expiryDate INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            // Create index for faster queries
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_certifications_farmId ON certifications(farmId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_certifications_status ON certifications(status)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_certifications_expiryDate ON certifications(expiryDate)");

            Log.d(TAG, "✅ Certifications table created successfully");

            // ========================================
            // 2. Create audits table
            // ========================================
            database.execSQL("CREATE TABLE IF NOT EXISTS audits (" +
                    "auditId TEXT PRIMARY KEY NOT NULL, " +
                    "certificationId TEXT NOT NULL, " +
                    "auditType TEXT, " +
                    "auditDate INTEGER NOT NULL DEFAULT 0, " +
                    "auditorName TEXT, " +
                    "auditorBody TEXT, " +
                    "scope TEXT, " +
                    "findingsJson TEXT, " +
                    "status TEXT, " +
                    "reportUrl TEXT, " +
                    "farmId INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(certificationId) REFERENCES certifications(certificationId) ON DELETE CASCADE, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            database.execSQL("CREATE INDEX IF NOT EXISTS idx_audits_certificationId ON audits(certificationId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_audits_farmId ON audits(farmId)");

            Log.d(TAG, "✅ Audits table created successfully");

            // ========================================
            // 3. Create inspections table
            // ========================================
            database.execSQL("CREATE TABLE IF NOT EXISTS inspections (" +
                    "inspectionId TEXT PRIMARY KEY NOT NULL, " +
                    "certificationId TEXT NOT NULL, " +
                    "certificationName TEXT, " +
                    "requirementCategory TEXT, " +
                    "inspectionType TEXT, " +
                    "inspectionDate INTEGER NOT NULL DEFAULT 0, " +
                    "inspectorName TEXT, " +
                    "inspectorBody TEXT, " +
                    "status TEXT, " +
                    "remarks TEXT, " +
                    "correctiveActionsJson TEXT, " +
                    "photosJson TEXT, " +
                    "reportUrl TEXT, " +
                    "farmId INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(certificationId) REFERENCES certifications(certificationId) ON DELETE CASCADE, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            database.execSQL("CREATE INDEX IF NOT EXISTS idx_inspections_certificationId ON inspections(certificationId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_inspections_farmId ON inspections(farmId)");

            Log.d(TAG, "✅ Inspections table created successfully");

            // ========================================
            // 4. Create compliance_verifications table
            // ========================================
            database.execSQL("CREATE TABLE IF NOT EXISTS compliance_verifications (" +
                    "verificationId TEXT PRIMARY KEY NOT NULL, " +
                    "certificationId TEXT NOT NULL, " +
                    "verificationType TEXT, " +
                    "verificationDate INTEGER NOT NULL DEFAULT 0, " +
                    "verifiedBy TEXT, " +
                    "status TEXT, " +
                    "evidenceJson TEXT, " +
                    "comment TEXT, " +
                    "farmId INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(certificationId) REFERENCES certifications(certificationId) ON DELETE CASCADE, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            database.execSQL("CREATE INDEX IF NOT EXISTS idx_verifications_certificationId ON compliance_verifications(certificationId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_verifications_farmId ON compliance_verifications(farmId)");

            Log.d(TAG, "✅ Compliance verifications table created successfully");

            // ========================================
            // 5. Create compliance_documents table
            // ========================================
            database.execSQL("CREATE TABLE IF NOT EXISTS compliance_documents (" +
                    "documentId TEXT PRIMARY KEY NOT NULL, " +
                    "certificationId TEXT NOT NULL, " +
                    "documentType TEXT, " +
                    "documentName TEXT, " +
                    "issueDate INTEGER NOT NULL DEFAULT 0, " +
                    "expiryDate INTEGER NOT NULL DEFAULT 0, " +
                    "fileUrl TEXT, " +
                    "status TEXT, " +
                    "farmId INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(certificationId) REFERENCES certifications(certificationId) ON DELETE CASCADE, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            database.execSQL("CREATE INDEX IF NOT EXISTS idx_documents_certificationId ON compliance_documents(certificationId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_documents_farmId ON compliance_documents(farmId)");

            Log.d(TAG, "✅ Compliance documents table created successfully");

            // ========================================
            // 6. Create organic_inputs table
            // ========================================
            database.execSQL("CREATE TABLE IF NOT EXISTS organic_inputs (" +
                    "inputId TEXT PRIMARY KEY NOT NULL, " +
                    "certificationId TEXT NOT NULL, " +
                    "certificationName TEXT, " +
                    "inputType TEXT, " +
                    "inputName TEXT, " +
                    "inputSource TEXT, " +
                    "applicationDate INTEGER NOT NULL DEFAULT 0, " +
                    "blockId TEXT, " +
                    "quantity REAL NOT NULL DEFAULT 0, " +
                    "unit TEXT, " +
                    "operatorId TEXT, " +
                    "certificationCompliance INTEGER NOT NULL DEFAULT 0, " +
                    "approvalDate INTEGER NOT NULL DEFAULT 0, " +
                    "documentUrl TEXT, " +
                    "farmId INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(certificationId) REFERENCES certifications(certificationId) ON DELETE CASCADE, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            database.execSQL("CREATE INDEX IF NOT EXISTS idx_organic_inputs_certificationId ON organic_inputs(certificationId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_organic_inputs_farmId ON organic_inputs(farmId)");

            Log.d(TAG, "✅ Organic inputs table created successfully");

            // ========================================
            // 7. Create halal_inputs table
            // ========================================
            database.execSQL("CREATE TABLE IF NOT EXISTS halal_inputs (" +
                    "halalInputId TEXT PRIMARY KEY NOT NULL, " +
                    "certificationId TEXT NOT NULL, " +
                    "certificationName TEXT, " +
                    "inputType TEXT, " +
                    "inputName TEXT, " +
                    "inputSource TEXT, " +
                    "inputOrigin TEXT, " +
                    "applicationDate INTEGER NOT NULL DEFAULT 0, " +
                    "quantity REAL NOT NULL DEFAULT 0, " +
                    "unit TEXT, " +
                    "operatorId TEXT, " +
                    "batchId TEXT, " +
                    "crossContaminationRisk TEXT, " +
                    "crossContaminationMitigation TEXT, " +
                    "verifiedBy TEXT, " +
                    "verificationDate INTEGER NOT NULL DEFAULT 0, " +
                    "remarks TEXT, " +
                    "farmId INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(certificationId) REFERENCES certifications(certificationId) ON DELETE CASCADE, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            database.execSQL("CREATE INDEX IF NOT EXISTS idx_halal_inputs_certificationId ON halal_inputs(certificationId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_halal_inputs_farmId ON halal_inputs(farmId)");

            Log.d(TAG, "✅ Halal inputs table created successfully");

            // ========================================
            // 8. Create haccp_plans table
            // ========================================
            database.execSQL("CREATE TABLE IF NOT EXISTS haccp_plans (" +
                    "haccpId TEXT PRIMARY KEY NOT NULL, " +
                    "planName TEXT, " +
                    "version TEXT, " +
                    "approvalDate INTEGER NOT NULL DEFAULT 0, " +
                    "lastReviewDate INTEGER NOT NULL DEFAULT 0, " +
                    "documentUrl TEXT, " +
                    "farmId INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            database.execSQL("CREATE INDEX IF NOT EXISTS idx_haccp_plans_farmId ON haccp_plans(farmId)");

            Log.d(TAG, "✅ HACCP plans table created successfully");

            // ========================================
            // 9. Create prps table (Prerequisite Programs)
            // ========================================
            database.execSQL("CREATE TABLE IF NOT EXISTS prps (" +
                    "prpId TEXT PRIMARY KEY NOT NULL, " +
                    "programName TEXT, " +
                    "implementationDate INTEGER NOT NULL DEFAULT 0, " +
                    "verificationDate INTEGER NOT NULL DEFAULT 0, " +
                    "verifiedBy TEXT, " +
                    "status TEXT, " +
                    "remarks TEXT, " +
                    "documentUrl TEXT, " +
                    "farmId INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            database.execSQL("CREATE INDEX IF NOT EXISTS idx_prps_farmId ON prps(farmId)");

            Log.d(TAG, "✅ PRPs table created successfully");

            // ========================================
            // 10. Create ccp_monitoring table
            // ========================================
            database.execSQL("CREATE TABLE IF NOT EXISTS ccp_monitoring (" +
                    "ccpId TEXT PRIMARY KEY NOT NULL, " +
                    "ccpName TEXT, " +
                    "criticalLimit TEXT, " +
                    "monitoringFrequency TEXT, " +
                    "monitoringMethod TEXT, " +
                    "batchId TEXT, " +
                    "monitoringDate INTEGER NOT NULL DEFAULT 0, " +
                    "monitoringTime TEXT, " +
                    "operatorId TEXT, " +
                    "actualValue TEXT, " +
                    "status TEXT, " +
                    "correctiveAction TEXT, " +
                    "verifiedBy TEXT, " +
                    "farmId INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            database.execSQL("CREATE INDEX IF NOT EXISTS idx_ccp_monitoring_farmId ON ccp_monitoring(farmId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_ccp_monitoring_batchId ON ccp_monitoring(batchId)");

            Log.d(TAG, "✅ CCP monitoring table created successfully");

            // ========================================
            // 11. Create premium_distributions table
            // ========================================
            database.execSQL("CREATE TABLE IF NOT EXISTS premium_distributions (" +
                    "premiumId TEXT PRIMARY KEY NOT NULL, " +
                    "certificationId TEXT NOT NULL, " +
                    "distributionDate INTEGER NOT NULL DEFAULT 0, " +
                    "amount REAL NOT NULL DEFAULT 0, " +
                    "currency TEXT, " +
                    "purpose TEXT, " +
                    "recipient TEXT, " +
                    "projectDescription TEXT, " +
                    "beneficiaries TEXT, " +
                    "verifiedBy TEXT, " +
                    "verificationDate INTEGER NOT NULL DEFAULT 0, " +
                    "verificationStatus TEXT, " +
                    "documentUrl TEXT, " +
                    "farmId INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(certificationId) REFERENCES certifications(certificationId) ON DELETE CASCADE, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            database.execSQL("CREATE INDEX IF NOT EXISTS idx_premium_distributions_certificationId ON premium_distributions(certificationId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_premium_distributions_farmId ON premium_distributions(farmId)");

            Log.d(TAG, "✅ Premium distributions table created successfully");

            // ========================================
            // 12. Create cooperative_members table
            // ========================================
            database.execSQL("CREATE TABLE IF NOT EXISTS cooperative_members (" +
                    "memberId TEXT PRIMARY KEY NOT NULL, " +
                    "certificationId TEXT NOT NULL, " +
                    "farmerName TEXT, " +
                    "farmerId TEXT, " +
                    "cooperativeName TEXT, " +
                    "joinDate INTEGER NOT NULL DEFAULT 0, " +
                    "membershipStatus TEXT, " +
                    "votingRights INTEGER NOT NULL DEFAULT 0, " +
                    "votingParticipation INTEGER NOT NULL DEFAULT 0, " +
                    "cropVolume REAL NOT NULL DEFAULT 0, " +
                    "unit TEXT, " +
                    "registeredBy TEXT, " +
                    "lastUpdated INTEGER NOT NULL DEFAULT 0, " +
                    "farmId INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                    "isSynced INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(certificationId) REFERENCES certifications(certificationId) ON DELETE CASCADE, " +
                    "FOREIGN KEY(farmId) REFERENCES farms(id) ON DELETE CASCADE)");

            database.execSQL("CREATE INDEX IF NOT EXISTS idx_cooperative_members_certificationId ON cooperative_members(certificationId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_cooperative_members_farmId ON cooperative_members(farmId)");

            Log.d(TAG, "✅ Cooperative members table created successfully");

            Log.d(TAG, "✅✅✅ Migration 25->26 completed successfully! All 12 compliance tables created.");

        } catch (Exception e) {
            Log.e(TAG, "❌ Migration 25->26 failed!", e);
            throw e;
        }
    }
}