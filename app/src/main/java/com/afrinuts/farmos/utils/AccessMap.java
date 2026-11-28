package com.afrinuts.farmos.utils;

import com.afrinuts.farmos.data.local.entity.UserEntity.UserRole;

/** Single source of truth for application access and edit capabilities. */
public final class AccessMap {
    private AccessMap() {}

    public static boolean canOpenScreen(String screen, UserRole role) {
        if (role == null || screen == null) return false;
        switch (screen) {
            case "SettingsActivity":
            case "WeatherActivity":
            case "ReportsActivity":
                return true;
            case "IntegrityCenterActivity":
                return role == UserRole.OWNER || role == UserRole.CEO || role == UserRole.MANAGER
                        || role == UserRole.COMPLIANCE_OFFICER || role == UserRole.FIELD_OFFICER;
            case "IntegrityFindingDetailActivity":
                return role == UserRole.OWNER || role == UserRole.CEO || role == UserRole.MANAGER
                        || role == UserRole.COMPLIANCE_OFFICER;
            case "ComplianceHubActivity":
            case "FinanceHubActivity":
                return role == UserRole.OWNER || role == UserRole.MANAGER || role == UserRole.CEO;
            case "UsersListActivity":
            case "UserDetailActivity":
                return role == UserRole.OWNER;
            case "SupplierDetailActivity":
                return role == UserRole.OWNER || role == UserRole.MANAGER
                        || role == UserRole.FIELD_OFFICER || role == UserRole.CEO;
            case "FarmHubActivity":
                return role == UserRole.OWNER || role == UserRole.MANAGER || role == UserRole.WORKER
                        || role == UserRole.FIELD_OFFICER || role == UserRole.CEO;
            case "AnalyticsHubActivity":
                return role != UserRole.FIELD_OFFICER;
            case "BlocksActivity":
            case "BlockDetailActivity":
            case "DocumentDetailActivity":
            case "AuditDetailActivity":
            case "InspectionDetailActivity":
            case "VerificationDetailActivity":
            case "CertificationDetailActivity":
            case "ExpenseDetailActivity":
            case "ExpensesListActivity":
            case "PhotoDetailActivity":
            case "PhotoGalleryActivity":
            case "QualityBatchDetailActivity":
            case "RevenueDetailActivity":
            case "RevenuesListActivity":
            case "TasksListActivity":
            case "TaskDetailActivity":
            case "TraceabilityDetailActivity":
            case "MasterTraceabilityDetailActivity":
            case "WorkerDashboardActivity":
                return role != UserRole.INVESTOR;
            default:
                return false;
        }
    }

    public static boolean canManageUsers(UserRole role) { return role == UserRole.OWNER; }
    public static boolean canManageAccounts(UserRole role) { return role == UserRole.OWNER; }
    public static boolean canEditData(UserRole role) {
        return role == UserRole.OWNER || role == UserRole.MANAGER || role == UserRole.CEO;
    }
    public static boolean canOnboardSuppliers(UserRole role) {
        return role == UserRole.OWNER || role == UserRole.MANAGER
                || role == UserRole.FIELD_OFFICER || role == UserRole.CEO;
    }
    public static boolean canChangeCertificationStatus(UserRole role) {
        return role == UserRole.OWNER || role == UserRole.CEO || role == UserRole.COMPLIANCE_OFFICER;
    }
    public static boolean canResolveIntegrityFinding(UserRole role) {
        return role == UserRole.OWNER || role == UserRole.MANAGER || role == UserRole.CEO
                || role == UserRole.COMPLIANCE_OFFICER;
    }
    public static boolean canViewAnalytics(UserRole role) { return role != null && role != UserRole.FIELD_OFFICER; }

    public static boolean canOpenHub(int hub, UserRole role) {
        switch (hub) {
            case 0: return role != null;
            case 1: return role == UserRole.OWNER || role == UserRole.MANAGER || role == UserRole.WORKER
                    || role == UserRole.FIELD_OFFICER || role == UserRole.CEO;
            case 2: return role == UserRole.OWNER || role == UserRole.MANAGER || role == UserRole.CEO;
            case 3: return canViewAnalytics(role);
            case 4: return role == UserRole.OWNER || role == UserRole.MANAGER || role == UserRole.CEO;
            default: return false;
        }
    }

    public static String hubDeniedMessage(int hub) {
        switch (hub) {
            case 1: return "Farm operations are only accessible to Owners, Managers, CEO, Workers, and Field Officers";
            case 2: return "Expenses are only accessible to Owners, Managers and CEO";
            case 4: return "Compliance is only accessible to Owners, Managers and CEO";
            default: return "You don't have permission to access this section";
        }
    }

    public static String[] farmTabs(UserRole role) {
        if (role == UserRole.OWNER || role == UserRole.MANAGER || role == UserRole.CEO) {
            return new String[]{"BLOCKS", "SUPPLIERS", "TASKS", "WORKERS"};
        }
        if (role == UserRole.WORKER) return new String[]{"TASKS"};
        if (role == UserRole.FIELD_OFFICER) return new String[]{"SUPPLIERS"};
        return new String[0];
    }

    public static String[] hubTabs(int hub, UserRole role) {
        if (!canOpenHub(hub, role)) return new String[0];
        switch (hub) {
            case 1: return farmTabs(role);
            case 2: return new String[]{"REVENUE", "EXPENSES"};
            case 3: return new String[]{"QUALITY", "TRACEABILITY", "BULK", "REPORTS"};
            case 4: return new String[]{"PROGRAMMES", "CHECKS", "DOCUMENTS", "DUE_EXPIRING"};
            default: return new String[0];
        }
    }

    public static boolean canUseFab(String action, UserRole role) {
        if (role == null || action == null) return false;
        switch (action) {
            case "ADD_BLOCK":
                return role == UserRole.OWNER || role == UserRole.MANAGER || role == UserRole.CEO;
            case "ADD_TASK":
                return role == UserRole.OWNER || role == UserRole.MANAGER || role == UserRole.CEO
                        || role == UserRole.WORKER;
            case "ADD_SUPPLIER":
                return canOnboardSuppliers(role);
            case "ADD_CERTIFICATION":
            case "ADD_AUDIT":
            case "ADD_INSPECTION":
            case "ADD_VERIFICATION":
            case "ADD_DOCUMENT":
            case "ADD_REVENUE":
            case "ADD_EXPENSE":
                return role == UserRole.OWNER || role == UserRole.MANAGER || role == UserRole.CEO;
            case "ANALYTICS_EDIT":
                return canEditData(role);
            default:
                return false;
        }
    }
}
