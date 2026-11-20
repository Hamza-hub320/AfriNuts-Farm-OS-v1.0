package com.afrinuts.farmos.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.afrinuts.farmos.R;

/** Attaches the existing master list beside existing detail Activity content on sw600dp. */
public final class TabletPane {
    private TabletPane() {}

    public static void attachList(AppCompatActivity activity, Bundle state, Fragment listFragment) {
        if (state != null || activity.findViewById(R.id.detailListPane) == null) return;
        activity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.detailListPane, listFragment, "tablet-detail-master")
                .commit();
    }
}
