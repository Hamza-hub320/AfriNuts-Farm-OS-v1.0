package com.afrinuts.farmos.ui.farm;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

/** Shared cream-page header and section/button styling for Farm record details. */
public final class FarmDetailChrome {
    private FarmDetailChrome() { }

    public static void apply(Activity activity, TextView title, ImageButton back, ImageButton more, String label) {
        title.setText(label);
        back.setBackgroundResource(com.afrinuts.farmos.R.drawable.bg_farm_action_circle);
        back.setOnClickListener(v -> activity.finish());
        View gradeView = activity.findViewById(com.afrinuts.farmos.R.id.tvGrade);
        if (gradeView instanceof Chip) {
            Chip grade = (Chip) gradeView;
            grade.setTextColor(0xFF1C1B1A);
            grade.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(0xFFF3EAD8));
            grade.setChipStrokeColor(android.content.res.ColorStateList.valueOf(0xFFE9E1CF));
            grade.setChipStrokeWidth(dp(grade, 1));
        }
        ViewGroup root = activity.findViewById(android.R.id.content);
        List<Button> actions = new ArrayList<>();
        collectActions(root, actions);
        ImageButton edit = activity.findViewById(com.afrinuts.farmos.R.id.farmDetailEdit);
        Button editAction = null;
        for (Button action : actions) {
            CharSequence text = action.getText();
            if (text != null && text.toString().toLowerCase(java.util.Locale.ROOT).contains("edit")) {
                editAction = action;
                break;
            }
        }
        if (edit != null) {
            Button target = editAction;
            edit.setVisibility(target == null ? View.GONE : View.VISIBLE);
            if (target != null) edit.setOnClickListener(v -> target.performClick());
        }
        if (editAction != null) actions.remove(editAction);
        if (actions.isEmpty()) {
            more.setVisibility(View.GONE);
        } else {
            more.setVisibility(View.VISIBLE);
            more.setOnClickListener(anchor -> {
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(activity, anchor);
                int id = 0;
                for (Button button : actions) {
                    CharSequence labelText = button.getText();
                    if (labelText == null || labelText.toString().trim().isEmpty()) continue;
                    popup.getMenu().add(0, ++id, id, labelText).setOnMenuItemClickListener(item -> {
                        button.performClick();
                        return true;
                    });
                }
                if (popup.getMenu().size() > 0) popup.show();
            });
        }
        styleSurfaces(root);
        styleExistingStats(root);
    }

    public static void addHero(Activity activity, int nameId, int subtitleId, int statusId) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        ScrollView scroll = findScroll(root);
        if (scroll == null || scroll.getChildCount() == 0 || !(scroll.getChildAt(0) instanceof ViewGroup)) return;
        ViewGroup content = (ViewGroup) scroll.getChildAt(0);
        if (content.findViewWithTag("farm-detail-hero") != null) return;
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            if (child instanceof CardView && child.getVisibility() == View.VISIBLE) {
                child.setVisibility(View.GONE);
                break;
            }
        }
        CardView hero = new CardView(activity);
        hero.setTag("farm-detail-hero");
        hero.setCardBackgroundColor(Color.WHITE);
        hero.setRadius(dp(hero, 28));
        hero.setCardElevation(0);
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroParams.setMargins(0, dp(hero, 2), 0, dp(hero, 12));
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(body, 20), dp(body, 20), dp(body, 20), dp(body, 20));
        hero.addView(body);
        LinearLayout top = new LinearLayout(activity);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        body.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView avatar = new TextView(activity);
        avatar.setGravity(android.view.Gravity.CENTER);
        avatar.setTextColor(0xFF0A5530);
        avatar.setTextSize(16);
        avatar.setTypeface(android.graphics.Typeface.create(ResourcesCompat.getFont(avatar.getContext(), com.afrinuts.farmos.R.font.manrope_extrabold), android.graphics.Typeface.NORMAL));
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setColor(0xFFE3F0E7);
        avatarBg.setCornerRadius(dp(avatar, 999));
        avatar.setBackground(avatarBg);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(avatar, 54), dp(avatar, 54));
        avatarParams.setMargins(0, 0, dp(avatar, 14), 0);
        top.addView(avatar, avatarParams);
        LinearLayout info = new LinearLayout(activity);
        info.setOrientation(LinearLayout.VERTICAL);
        top.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView name = new TextView(activity);
        name.setTag("farm-detail-hero-name");
        name.setTextColor(0xFF1C1B1A);
        name.setTextSize(21);
        name.setTypeface(android.graphics.Typeface.create(ResourcesCompat.getFont(name.getContext(), com.afrinuts.farmos.R.font.manrope_extrabold), android.graphics.Typeface.NORMAL));
        name.setMaxLines(2);
        info.addView(name);
        TextView subtitle = new TextView(activity);
        subtitle.setTag("farm-detail-hero-subtitle");
        subtitle.setTextColor(0xFF59544C);
        subtitle.setTextSize(13.5f);
        subtitle.setTypeface(android.graphics.Typeface.create(ResourcesCompat.getFont(subtitle.getContext(), com.afrinuts.farmos.R.font.manrope_medium), android.graphics.Typeface.NORMAL));
        subtitle.setMaxLines(1);
        subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(subtitle, 3);
        info.addView(subtitle, subtitleParams);
        LinearLayout statusRow = new LinearLayout(activity);
        statusRow.setTag("farm-detail-hero-status-row");
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams statusRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusRowParams.topMargin = dp(statusRow, 14);
        body.addView(statusRow, statusRowParams);
        TextView status = new TextView(activity);
        status.setTag("farm-detail-hero-status");
        status.setTextSize(12.5f);
        status.setTypeface(android.graphics.Typeface.create(ResourcesCompat.getFont(status.getContext(), com.afrinuts.farmos.R.font.manrope_bold), android.graphics.Typeface.NORMAL));
        status.setTextColor(0xFF0A5530);
        status.setPadding(dp(status, 12), dp(status, 6), dp(status, 12), dp(status, 6));
        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setColor(0xFFE3F0E7);
        statusBg.setCornerRadius(dp(status, 999));
        status.setBackground(statusBg);
        statusRow.addView(status);
        content.addView(hero, 0, heroParams);
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable[] refresh = new Runnable[1];
        int[] attempts = {0};
        refresh[0] = () -> {
            TextView sourceName = activity.findViewById(nameId);
            TextView sourceSubtitle = activity.findViewById(subtitleId);
            TextView sourceStatus = activity.findViewById(statusId);
            String nameText = sourceName == null || sourceName.getText() == null ? "" : sourceName.getText().toString().trim();
            String subtitleText = sourceSubtitle == null || sourceSubtitle.getText() == null ? "" : sourceSubtitle.getText().toString().trim();
            String statusText = sourceStatus == null || sourceStatus.getText() == null ? "" : sourceStatus.getText().toString().trim();
            name.setText(nameText.isEmpty() ? "—" : nameText);
            avatar.setText(initials(nameText));
            subtitle.setText(subtitleText);
            subtitle.setVisibility(subtitleText.isEmpty() ? View.GONE : View.VISIBLE);
            status.setText(statusText.isEmpty() ? "" : "•  " + statusText);
            statusRow.setVisibility(statusText.isEmpty() ? View.GONE : View.VISIBLE);
            if (sourceStatus != null && sourceStatus.isClickable()) {
                status.setClickable(true);
                status.setFocusable(true);
                status.setOnClickListener(v -> sourceStatus.performClick());
            }
            if (++attempts[0] < 16 && !activity.isFinishing()) handler.postDelayed(refresh[0], 300);
        };
        handler.post(refresh[0]);
    }

    private static String initials(String value) {
        if (value == null || value.trim().isEmpty()) return "•";
        String[] words = value.trim().split("\\s+");
        if (words.length == 1) return words[0].substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        return (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase(java.util.Locale.ROOT);
    }

    private static void styleExistingStats(View view) {
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        ArrayList<CardView> cards = new ArrayList<>();
        for (int i = 0; i < group.getChildCount(); i++) if (group.getChildAt(i) instanceof CardView) cards.add((CardView) group.getChildAt(i));
        if (cards.size() == 3) {
            for (int i = 0; i < cards.size(); i++) {
                CardView card = cards.get(i);
                card.setRadius(dp(card, 20));
                card.setCardElevation(0);
                if (i == 0) {
                    GradientDrawable green = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                            new int[]{0xFF14794A, 0xFF0E6B3C, 0xFF0A5530});
                    green.setCornerRadius(dp(card, 20));
                    card.setCardBackgroundColor(Color.TRANSPARENT);
                    if (card.getChildCount() > 0) card.getChildAt(0).setBackground(green);
                } else card.setCardBackgroundColor(Color.WHITE);
                styleStatTexts(card, i == 0);
            }
        }
        for (int i = 0; i < group.getChildCount(); i++) styleExistingStats(group.getChildAt(i));
    }

    private static void styleStatTexts(View view, boolean primary) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            text.setTextColor(primary ? Color.WHITE : 0xFF59544C);
            if (text.getTextSize() / text.getResources().getDisplayMetrics().scaledDensity >= 16) {
                text.setTextSize(22);
                text.setTypeface(android.graphics.Typeface.create(ResourcesCompat.getFont(text.getContext(), com.afrinuts.farmos.R.font.manrope_extrabold), android.graphics.Typeface.NORMAL));
            } else text.setTextSize(12);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) styleStatTexts(group.getChildAt(i), primary);
        }
    }

    public static void addStats(Activity activity, String[] labels, int[] sourceIds) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        ScrollView scroll = findScroll(root);
        if (scroll == null || !(scroll.getChildAt(0) instanceof ViewGroup)) return;
        ViewGroup content = (ViewGroup) scroll.getChildAt(0);
        if (content.findViewWithTag("farm-detail-stats") != null || content.getChildCount() == 0) return;

        LinearLayout row = new LinearLayout(activity);
        row.setTag("farm-detail-stats");
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(row, 12));
        for (int i = 0; i < Math.min(labels.length, sourceIds.length); i++) {
            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(card, 14), dp(card, 14), dp(card, 14), dp(card, 14));
            GradientDrawable bg = new GradientDrawable();
            if (i == 0) {
                bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF14794A, 0xFF0E6B3C, 0xFF0A5530});
                bg.setCornerRadius(dp(card, 20));
            } else {
                bg.setColor(Color.WHITE);
                bg.setCornerRadius(dp(card, 20));
            }
            card.setBackground(bg);
            TextView value = new TextView(activity);
            value.setTag("farm-detail-stat-value:" + sourceIds[i]);
            value.setTextColor(i == 0 ? Color.WHITE : 0xFF1C1B1A);
            value.setTextSize(22);
            value.setTypeface(android.graphics.Typeface.create(ResourcesCompat.getFont(value.getContext(), com.afrinuts.farmos.R.font.manrope_extrabold), android.graphics.Typeface.NORMAL));
            TextView unit = new TextView(activity);
            unit.setTag("farm-detail-stat-unit:" + sourceIds[i]);
            unit.setTextColor(i == 0 ? 0xC7FFFFFF : 0xFF59544C);
            unit.setTextSize(13);
            unit.setTypeface(android.graphics.Typeface.create(ResourcesCompat.getFont(unit.getContext(), com.afrinuts.farmos.R.font.manrope_bold), android.graphics.Typeface.NORMAL));
            card.addView(unit);
            TextView caption = new TextView(activity);
            caption.setText(labels[i]);
            caption.setTextColor(i == 0 ? 0xC7FFFFFF : 0xFF59544C);
            caption.setTextSize(12);
            caption.setTypeface(android.graphics.Typeface.create(ResourcesCompat.getFont(caption.getContext(), com.afrinuts.farmos.R.font.manrope_bold), android.graphics.Typeface.NORMAL));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(0, dp(card, 96), 1f);
            cardParams.setMargins(i == 0 ? 0 : dp(card, 10), 0, 0, 0);
            row.addView(card, cardParams);
            card.addView(value);
            card.addView(caption);
        }
        content.addView(row, Math.min(1, content.getChildCount()), rowParams);
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable[] refresh = new Runnable[1];
        int[] attempts = {0};
        refresh[0] = () -> {
            for (int id : sourceIds) {
                TextView source = activity.findViewById(id);
                TextView target = row.findViewWithTag("farm-detail-stat-value:" + id);
                if (target != null) {
                    CharSequence text = source == null ? null : source.getText();
                    String value = text == null ? "" : text.toString().trim();
                    if (value.isEmpty()) value = "—";
                    value = value.replaceFirst("(?i)^(total|alive|total tasks|completion rate|grade|quality score|weight|trust score|batches|total volume|survival rate|total harvest|due date|assigned worker)\\s*:?\\s*", "");
                    value = value.replaceFirst("(?i)\\s+batch(?:es)?$", "");
                    target.setText(value);
                }
            }
            if (++attempts[0] < 12 && !activity.isFinishing()) handler.postDelayed(refresh[0], 350);
        };
        handler.post(refresh[0]);
    }

    public static void moveTaskStatusToHero(Activity activity) {
        TextView status = activity.findViewById(com.afrinuts.farmos.R.id.tvCurrentStatus);
        TextView taskType = activity.findViewById(com.afrinuts.farmos.R.id.tvTaskType);
        if (status == null || taskType == null || !(taskType.getParent() instanceof ViewGroup)) return;
        ViewGroup heroText = (ViewGroup) taskType.getParent();
        if (status.getParent() instanceof ViewGroup) ((ViewGroup) status.getParent()).removeView(status);
        status.setTextSize(12);
        status.setTypeface(android.graphics.Typeface.create(ResourcesCompat.getFont(status.getContext(), com.afrinuts.farmos.R.font.manrope_bold), android.graphics.Typeface.NORMAL));
        status.setTextColor(0xFF0E6B3C);
        status.setPadding(dp(status, 12), dp(status, 6), dp(status, 12), dp(status, 6));
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(0xFFE8F1E8);
        pill.setCornerRadius(dp(status, 999));
        status.setBackground(pill);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(status, 8);
        heroText.addView(status, params);
    }

    public static void addBottomActions(Activity activity, int secondaryId, int primaryId) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content.getChildCount() == 0 || !(content.getChildAt(0) instanceof ViewGroup)) return;
        ViewGroup page = (ViewGroup) content.getChildAt(0);
        if (page.findViewWithTag("farm-detail-actions") != null) return;
        View secondarySource = activity.findViewById(secondaryId);
        View primarySource = activity.findViewById(primaryId);
        if (secondarySource == null || primarySource == null) return;
        String secondaryText = secondarySource instanceof TextView ? ((TextView) secondarySource).getText().toString() : "";
        String primaryText = primarySource instanceof TextView ? ((TextView) primarySource).getText().toString() : "";
        if (secondaryText.trim().isEmpty() || primaryText.trim().isEmpty()) return;

        LinearLayout actions = new LinearLayout(activity);
        actions.setTag("farm-detail-actions");
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(actions, 20), dp(actions, 8), dp(actions, 20), dp(actions, 12));
        actions.setBackgroundColor(0xFFF6F1E7);
        MaterialButton secondary = makeAction(activity, secondaryText, false);
        MaterialButton primary = makeAction(activity, primaryText, true);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(actions, 52), 1f);
        left.setMargins(0, 0, dp(actions, 8), 0);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, dp(actions, 52), 1f);
        actions.addView(secondary, left);
        actions.addView(primary, right);
        page.addView(actions, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        secondarySource.setVisibility(View.GONE);
        primarySource.setVisibility(View.GONE);
        secondary.setOnClickListener(v -> secondarySource.performClick());
        primary.setOnClickListener(v -> primarySource.performClick());
    }

    private static MaterialButton makeAction(Activity activity, String text, boolean primary) {
        MaterialButton button = new MaterialButton(activity);
        button.setText(text);
        button.setTextSize(15);
        button.setTypeface(android.graphics.Typeface.create(ResourcesCompat.getFont(button.getContext(), com.afrinuts.farmos.R.font.manrope_extrabold), android.graphics.Typeface.NORMAL));
        button.setTextColor(primary ? Color.WHITE : 0xFF1C1B1A);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary ? 0xFF0E6B3C : 0xFFF3EAD8));
        button.setCornerRadius(dp(button, 999));
        button.setRippleColor(android.content.res.ColorStateList.valueOf(0x220E6B3C));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        return button;
    }

    private static ScrollView findScroll(View view) {
        if (view instanceof ScrollView) return (ScrollView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ScrollView result = findScroll(group.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
    }

    private static void collectActions(View view, List<Button> actions) {
        if (view instanceof Button && view.getId() != com.afrinuts.farmos.R.id.farmDetailMore
                && view.getVisibility() == View.VISIBLE && view.isEnabled()) {
            actions.add((Button) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collectActions(group.getChildAt(i), actions);
        }
    }

    private static void styleSurfaces(View view) {
        if (view instanceof CardView) {
            CardView card = (CardView) view;
            card.setRadius(dp(view, 24));
            card.setCardBackgroundColor(Color.WHITE);
            card.setCardElevation(0);
            card.setPreventCornerOverlap(true);
            TextView heading = firstText(card);
            if (heading != null) {
                heading.setTextSize(16);
                heading.setTextColor(0xFF1C1B1A);
                heading.setTypeface(android.graphics.Typeface.create(ResourcesCompat.getFont(heading.getContext(), com.afrinuts.farmos.R.font.manrope), android.graphics.Typeface.BOLD));
            }
        }
        if (view instanceof MaterialButton) {
            MaterialButton button = (MaterialButton) view;
            button.setCornerRadius(dp(view, 999));
            button.setMinHeight(dp(view, 52));
            button.setTextSize(Math.max(14, button.getTextSize() / view.getResources().getDisplayMetrics().scaledDensity));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) styleSurfaces(group.getChildAt(i));
        }
    }

    private static TextView firstText(View view) {
        if (view instanceof TextView && !(view instanceof Button)) {
            CharSequence value = ((TextView) view).getText();
            if (value != null && value.length() > 0) return (TextView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = firstText(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }
}
