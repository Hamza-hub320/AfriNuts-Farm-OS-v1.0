package com.afrinuts.farmos.ui.finance;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Finance-only detail presentation. Keeps the shared Farm detail components untouched. */
public final class FinanceDetailChrome {
    private FinanceDetailChrome() { }

    public static void apply(Activity activity, TextView title, ImageButton back, ImageButton edit,
                             ImageButton more, String label) {
        title.setText(label);
        circle(back);
        circle(edit);
        circle(more);
        back.setOnClickListener(v -> activity.finish());

        List<Button> actions = new ArrayList<>();
        collectActions(activity.findViewById(android.R.id.content), actions);
        Button editAction = null;
        for (Button action : actions) {
            CharSequence text = action.getText();
            if (text != null && text.toString().toLowerCase(Locale.ROOT).contains("edit")) {
                editAction = action;
                break;
            }
        }
        final Button editTarget = editAction;
        edit.setVisibility(editTarget == null ? View.GONE : View.VISIBLE);
        if (editTarget != null) edit.setOnClickListener(v -> editTarget.performClick());
        if (editTarget != null) actions.remove(editTarget);
        more.setVisibility(actions.isEmpty() ? View.GONE : View.VISIBLE);
        more.setOnClickListener(anchor -> {
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(activity, anchor);
            int itemId = 0;
            for (Button action : actions) {
                CharSequence text = action.getText();
                if (text == null || text.toString().trim().isEmpty()) continue;
                popup.getMenu().add(0, ++itemId, itemId, text).setOnMenuItemClickListener(item -> {
                    action.performClick();
                    return true;
                });
            }
            if (popup.getMenu().size() > 0) popup.show();
        });
        styleSectionCards(findScroll(activity.findViewById(android.R.id.content)));
    }

    public static void addHero(Activity activity, int nameId, int subtitleId, int gradeId) {
        LinearLayout content = content(activity);
        if (content == null || content.findViewWithTag("finance-detail-hero") != null) return;
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            if (child instanceof CardView && child.getVisibility() == View.VISIBLE) {
                child.setVisibility(View.GONE);
                break;
            }
        }

        CardView hero = new CardView(activity);
        hero.setTag("finance-detail-hero");
        hero.setCardBackgroundColor(Color.WHITE);
        hero.setRadius(dp(activity, 28));
        hero.setCardElevation(0);
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 20));
        hero.addView(body);

        TextView name = new TextView(activity);
        name.setTag("finance-detail-name");
        name.setTextColor(0xFF1C1B1A);
        name.setTextSize(21);
        name.setLineSpacing(0, 1f);
        name.setTypeface(font(activity, R.font.manrope_extrabold));
        name.setMaxLines(2);
        body.addView(name);

        TextView subtitle = new TextView(activity);
        subtitle.setTag("finance-detail-subtitle");
        subtitle.setTextColor(0xFF59544C);
        subtitle.setTextSize(13.5f);
        subtitle.setTypeface(font(activity, R.font.manrope_semibold));
        subtitle.setMaxLines(1);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(activity, 3);
        body.addView(subtitle, subParams);

        TextView grade = new TextView(activity);
        grade.setTag("finance-detail-grade");
        grade.setTextSize(12.5f);
        grade.setTypeface(font(activity, R.font.manrope_extrabold));
        grade.setPadding(dp(activity, 12), dp(activity, 6), dp(activity, 12), dp(activity, 6));
        LinearLayout.LayoutParams gradeParams = new LinearLayout.LayoutParams(-2, -2);
        gradeParams.topMargin = dp(activity, 14);
        body.addView(grade, gradeParams);

        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, -2);
        heroParams.bottomMargin = dp(activity, 12);
        content.addView(hero, 0, heroParams);

        bindHero(activity, nameId, subtitleId, gradeId);
    }

    public static void addStats(Activity activity, String[] labels, int[] sourceIds) {
        LinearLayout content = content(activity);
        if (content == null || content.findViewWithTag("finance-detail-stats") != null) return;
        LinearLayout row = new LinearLayout(activity);
        row.setTag("finance-detail-stats");
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.bottomMargin = dp(activity, 12);

        int count = Math.min(labels.length, sourceIds.length);
        for (int i = 0; i < count; i++) {
            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(activity, 12), dp(activity, 14), dp(activity, 12), dp(activity, 14));
            card.setMinimumHeight(dp(activity, 96));
            GradientDrawable bg;
            if (i == 0) {
                bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF14794A, 0xFF0E6B3C, 0xFF0A5530});
            } else {
                bg = new GradientDrawable();
                bg.setColor(Color.WHITE);
            }
            bg.setCornerRadius(dp(activity, 20));
            card.setBackground(bg);

            TextView label = new TextView(activity);
            label.setText(labels[i]);
            label.setTextColor(i == 0 ? 0xC7FFFFFF : 0xFF59544C);
            label.setTextSize(12);
            label.setTypeface(font(activity, R.font.manrope_bold));
            label.setMaxLines(1);
            card.addView(label);

            TextView value = new TextView(activity);
            value.setTag("finance-detail-stat:" + sourceIds[i]);
            value.setTextColor(i == 0 ? Color.WHITE : 0xFF1C1B1A);
            value.setTextSize(20);
            value.setTypeface(font(activity, R.font.manrope_extrabold));
            value.setMaxLines(1);
            value.setAutoSizeTextTypeUniformWithConfiguration(11, 20, 1,
                    android.util.TypedValue.COMPLEX_UNIT_SP);
            LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(-1, -2);
            valueParams.topMargin = dp(activity, 3);
            card.addView(value, valueParams);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(0, -2, 1f);
            if (i > 0) cardParams.leftMargin = dp(activity, 10);
            row.addView(card, cardParams);
        }
        content.addView(row, Math.min(1, content.getChildCount()), rowParams);
        bindStats(activity, row, sourceIds);
    }

    public static void splitRevenueSections(Activity activity) {
        LinearLayout content = content(activity);
        if (content == null) return;

        View supplierSection = activity.findViewById(R.id.layoutProcurementInfo);
        if (supplierSection instanceof LinearLayout && supplierSection.getVisibility() == View.VISIBLE) {
            LinearLayout section = (LinearLayout) supplierSection;
            hideSectionHeadingAndDivider(section, "Procurement Details");
            contentSection(activity, content, section, "Supplier", activity.findViewById(R.id.btnViewSupplier));
        }

        View notesSection = activity.findViewById(R.id.notesLayout);
        if (notesSection instanceof LinearLayout && notesSection.getVisibility() == View.VISIBLE) {
            LinearLayout section = (LinearLayout) notesSection;
            hideSectionHeadingAndDivider(section, "Notes");
            contentSection(activity, content, section, "Notes", null);
        }

        TextView batchTitle = findText(content, "Batch");
        View viewBatch = activity.findViewById(R.id.btnViewBatch);
        if (batchTitle != null && batchTitle.getParent() instanceof LinearLayout) {
            LinearLayout header = (LinearLayout) batchTitle.getParent();
            TextView seeAll = makeSeeAll(activity);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
            params.leftMargin = dp(activity, 12);
            header.addView(seeAll, params);
            if (viewBatch != null) seeAll.setOnClickListener(v -> viewBatch.performClick());
        }
    }


    /** Replaces legacy sale stats inside the Batch section with fields from the linked batch. */
    public static void bindRevenueBatch(Activity activity, QualityBatchEntity batch) {
        LinearLayout content = content(activity);
        TextView title = content == null ? null : findText(content, "Batch");
        if (title == null || !(title.getParent() instanceof ViewGroup)) return;
        ViewGroup header = (ViewGroup) title.getParent();
        if (!(header.getParent() instanceof LinearLayout)) return;
        LinearLayout body = (LinearLayout) header.getParent();
        boolean afterHeader = false;
        for (int i = body.getChildCount() - 1; i >= 0; i--) {
            View child = body.getChildAt(i);
            if (child == header) { afterHeader = true; break; }
            body.removeViewAt(i);
        }
        if (!afterHeader) return;
        String number = batch == null || TextUtils.isEmpty(batch.getBatchNumber()) ? "Not linked" : batch.getBatchNumber();
        String grade = batch == null || batch.getQualityGrade() == null ? "Not recorded" : batch.getQualityGrade().getDisplayName();
        String kor = batch == null || batch.getKorGPerKg() <= 0 ? "Not recorded"
                : String.format(Locale.getDefault(), "%.0f g/kg", batch.getKorGPerKg());
        String received = batch == null || batch.getProcessingDate() <= 0 ? "Not recorded"
                : new java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    .format(new java.util.Date(batch.getProcessingDate()));
        addKeyValueRow(activity, body, "Batch number", number);
        addKeyValueRow(activity, body, "Grade", grade);
        addKeyValueRow(activity, body, "KOR", kor);
        addKeyValueRow(activity, body, "Received", received);
    }

    private static void addKeyValueRow(Activity activity, LinearLayout body, String label, String value) {
        if (body.getChildCount() > 1) {
            View divider = new View(activity);
            divider.setBackgroundColor(0xFFE9E1CF);
            body.addView(divider, new LinearLayout.LayoutParams(-1, dp(activity, 1)));
        }
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(activity, 11), 0, dp(activity, 11));
        TextView key = new TextView(activity);
        key.setText(label);
        key.setTextColor(0xFF59544C);
        key.setTextSize(13);
        key.setTypeface(font(activity, R.font.manrope_bold));
        row.addView(key, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView val = new TextView(activity);
        val.setText(value);
        val.setTextColor(0xFF1C1B1A);
        val.setTextSize(14.5f);
        val.setTypeface(font(activity, R.font.manrope_bold));
        val.setGravity(Gravity.END);
        row.addView(val, new LinearLayout.LayoutParams(-2, -2));
        body.addView(row);
    }

    private static void contentSection(Activity activity, LinearLayout content, LinearLayout source,
                                       String title, View target) {
        ViewGroup parent = (ViewGroup) source.getParent();
        if (parent != null) parent.removeView(source);
        source.setVisibility(View.VISIBLE);
        source.setPadding(0, 0, 0, 0);

        CardView card = new CardView(activity);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(activity, 24));
        card.setCardElevation(0);
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(activity, 20), dp(activity, 18), dp(activity, 20), dp(activity, 18));
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView heading = new TextView(activity);
        heading.setText(title);
        heading.setTextColor(0xFF1C1B1A);
        heading.setTextSize(16);
        heading.setTypeface(font(activity, R.font.manrope_extrabold));
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView seeAll = makeSeeAll(activity);
        header.addView(seeAll, new LinearLayout.LayoutParams(-2, -2));
        if (target != null) seeAll.setOnClickListener(v -> target.performClick());
        body.addView(header);
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(-1, -2);
        sourceParams.topMargin = dp(activity, 8);
        body.addView(source, sourceParams);
        card.addView(body);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(activity, 12);
        content.addView(card, cardParams);
    }

    private static void hideSectionHeadingAndDivider(LinearLayout section, String heading) {
        for (int i = 0; i < section.getChildCount(); i++) {
            View child = section.getChildAt(i);
            if (child instanceof TextView) {
                CharSequence value = ((TextView) child).getText();
                if (value != null && heading.equalsIgnoreCase(value.toString().trim())) child.setVisibility(View.GONE);
            } else if (child.getClass() == View.class) {
                ViewGroup.LayoutParams params = child.getLayoutParams();
                if (params != null && params.height == dp(section, 1)) child.setVisibility(View.GONE);
            } else if (child instanceof LinearLayout && i == 0) {
                child.setVisibility(View.GONE);
            }
        }
    }

    private static TextView makeSeeAll(Activity activity) {
        TextView view = new TextView(activity);
        view.setText("See all");
        view.setTextColor(0xFF0E6B3C);
        view.setTextSize(13.5f);
        view.setTypeface(font(activity, R.font.manrope_extrabold));
        return view;
    }

    private static TextView findText(View view, String value) {
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && value.equalsIgnoreCase(text.toString().trim())) return (TextView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findText(group.getChildAt(i), value);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void bindHero(Activity activity, int nameId, int subtitleId, int gradeId) {
        View root = activity.findViewById(android.R.id.content);
        TextView name = root.findViewWithTag("finance-detail-name");
        TextView subtitle = root.findViewWithTag("finance-detail-subtitle");
        TextView grade = root.findViewWithTag("finance-detail-grade");
        TextView nameSource = activity.findViewById(nameId);
        TextView subtitleSource = activity.findViewById(subtitleId);
        TextView gradeSource = gradeId == View.NO_ID ? null : activity.findViewById(gradeId);
        String title = text(nameSource);
        String sub = text(subtitleSource);
        String gradeText = text(gradeSource);
        name.setText(title.isEmpty() ? "—" : title);
        subtitle.setText(sub);
        subtitle.setVisibility(sub.isEmpty() ? View.GONE : View.VISIBLE);
        if (gradeText.isEmpty()) {
            grade.setVisibility(View.GONE);
        } else {
            boolean premium = gradeText.toLowerCase(Locale.ROOT).contains("premium");
            boolean standard = gradeText.toLowerCase(Locale.ROOT).contains("standard");
            grade.setText(gradeText.replace(" Grade", ""));
            grade.setTextColor(premium ? 0xFF0A5530 : (standard ? 0xFF7A5F22 : 0xFF59544C));
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(premium ? 0xFFE3F0E7 : (standard ? 0xFFF3EAD8 : 0xFFEFE8D8));
            pill.setCornerRadius(dp(activity, 999));
            grade.setBackground(pill);
            grade.setVisibility(View.VISIBLE);
        }
    }

    private static void bindStats(Activity activity, LinearLayout row, int[] sourceIds) {
        for (int id : sourceIds) {
            TextView source = activity.findViewById(id);
            TextView value = row.findViewWithTag("finance-detail-stat:" + id);
            if (source == null || value == null) continue;
            String formatted = removeLeadingLabel(text(source));
            SpannableString styled = new SpannableString(formatted);
            int unitStart = formatted.lastIndexOf(" kg");
            if (unitStart < 0) unitStart = formatted.lastIndexOf(" XAF");
            if (unitStart >= 0) {
                styled.setSpan(new RelativeSizeSpan(0.65f), unitStart + 1, formatted.length(),
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                styled.setSpan(new StyleSpan(Typeface.BOLD), unitStart + 1, formatted.length(),
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            value.setText(styled);
        }
    }

    private static String removeLeadingLabel(String input) {
        return input.replaceFirst("(?i)^(quantity|price/kg|price / kg|total|amount|date|source)\\s*:?\\s*", "");
    }

    private static void styleSectionCards(ScrollView scroll) {
        if (scroll == null || scroll.getChildCount() == 0) return;
        styleCardTree(scroll.getChildAt(0));
    }

    private static void styleCardTree(View view) {
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        if (view instanceof CardView && !"finance-detail-hero".equals(view.getTag())) {
            CardView card = (CardView) view;
            card.setRadius(dp(view, 24));
            card.setCardBackgroundColor(Color.WHITE);
            card.setCardElevation(0);
            card.setPreventCornerOverlap(true);
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (!(child instanceof ViewGroup)) {
                    ViewGroup.LayoutParams lp = child.getLayoutParams();
                    if (lp != null && lp.width == ViewGroup.LayoutParams.MATCH_PARENT
                            && lp.height == ViewGroup.LayoutParams.MATCH_PARENT) child.setVisibility(View.GONE);
                } else if (child instanceof LinearLayout) {
                    child.setPadding(dp(child, 20), dp(child, 18), dp(child, 20), dp(child, 18));
                }
            }
            TextView heading = firstText(card);
            if (heading != null) {
                String value = heading.getText() == null ? "" : heading.getText().toString().trim();
                if ("Harvest Details".equalsIgnoreCase(value)) heading.setText("Batch");
                if ("Procurement Details".equalsIgnoreCase(value)) heading.setText("Supplier");
                heading.setTextSize(16);
                heading.setTextColor(0xFF1C1B1A);
                heading.setTypeface(font(heading.getContext(), R.font.manrope_extrabold));
            }
        }
        for (int i = 0; i < group.getChildCount(); i++) styleCardTree(group.getChildAt(i));
    }

    private static TextView firstText(View view) {
        if (view instanceof TextView && !(view instanceof Button)) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && text.length() > 0) return (TextView) view;
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

    private static void circle(ImageButton button) {
        if (button == null) return;
        button.setBackgroundResource(R.drawable.bg_farm_action_circle);
        button.setPadding(dp(button, 12), dp(button, 12), dp(button, 12), dp(button, 12));
    }

    private static void collectActions(View view, List<Button> result) {
        if (view instanceof Button && view.getVisibility() == View.VISIBLE && view.isEnabled()) result.add((Button) view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collectActions(group.getChildAt(i), result);
        }
    }

    private static LinearLayout content(Activity activity) {
        ScrollView scroll = findScroll(activity.findViewById(android.R.id.content));
        if (scroll == null || scroll.getChildCount() == 0 || !(scroll.getChildAt(0) instanceof LinearLayout)) return null;
        return (LinearLayout) scroll.getChildAt(0);
    }

    private static ScrollView findScroll(View view) {
        if (view instanceof ScrollView) return (ScrollView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ScrollView found = findScroll(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static String text(TextView view) {
        return view == null || view.getText() == null ? "" : view.getText().toString().trim();
    }

    private static Typeface font(Context context, int id) {
        return ResourcesCompat.getFont(context, id);
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }
    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
