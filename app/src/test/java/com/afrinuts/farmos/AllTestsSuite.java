package com.afrinuts.farmos;

import com.afrinuts.farmos.data.entity.FarmEntityTest;
import com.afrinuts.farmos.data.entity.BlockEntityTest;
import com.afrinuts.farmos.data.entity.ExpenseEntityTest;
import com.afrinuts.farmos.data.entity.RevenueEntityTest;
import com.afrinuts.farmos.data.entity.TaskEntityTest;
import com.afrinuts.farmos.ui.profit.ProfitAnalyticsTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        FarmEntityTest.class,
        BlockEntityTest.class,
        ExpenseEntityTest.class,
        RevenueEntityTest.class,
        TaskEntityTest.class,
        ProfitAnalyticsTest.class
})
public class AllTestsSuite {
    // This class remains empty, used only as a holder for the above annotations
}