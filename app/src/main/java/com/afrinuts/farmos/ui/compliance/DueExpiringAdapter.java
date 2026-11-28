package com.afrinuts.farmos.ui.compliance;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;

import java.util.ArrayList;
import java.util.List;

public class DueExpiringAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_SECTION = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_EMPTY = 2;

    public interface OnItemClickListener {
        void onItemClick(Row row);
    }

    public static class Row {
        public final int viewType;
        public final String title;
        public final String subtitle;
        public final String dateText;
        public final String daysText;
        public final String recordId;
        public final String recordType;
        public final long sortDate;
        public final boolean overdue;
        public final String alertKey;

        private Row(int viewType, String title, String subtitle, String dateText, String daysText,
                    String recordId, String recordType, long sortDate, boolean overdue, String alertKey) {
            this.viewType = viewType;
            this.title = title;
            this.subtitle = subtitle;
            this.dateText = dateText;
            this.daysText = daysText;
            this.recordId = recordId;
            this.recordType = recordType;
            this.sortDate = sortDate;
            this.overdue = overdue;
            this.alertKey = alertKey;
        }

        public static Row section(String title) {
            return new Row(TYPE_SECTION, title, null, null, null, null, null, 0, false, null);
        }

        public static Row empty(String message) {
            return new Row(TYPE_EMPTY, message, null, null, null, null, null, 0, false, null);
        }

        public static Row item(String title, String subtitle, String dateText, String daysText,
                               String recordId, String recordType, long sortDate, boolean overdue, String alertKey) {
            return new Row(TYPE_ITEM, title, subtitle, dateText, daysText, recordId, recordType, sortDate, overdue, alertKey);
        }
    }

    private final Context context;
    private final OnItemClickListener listener;
    private final List<Row> rows = new ArrayList<>();
    private String selectedRecordType;
    private String selectedRecordId;

    public DueExpiringAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void submitRows(List<Row> updatedRows) {
        rows.clear();
        if (updatedRows != null) rows.addAll(updatedRows);
        notifyDataSetChanged();
    }

    public void setSelectedDue(String recordType, String recordId) {
        selectedRecordType = recordType;
        selectedRecordId = recordId;
        notifyDataSetChanged();
    }

    public String getSelectedRecordType() {
        return selectedRecordType;
    }

    public String getSelectedRecordId() {
        return selectedRecordId;
    }

    public Row findItem(String recordType, String recordId) {
        if (recordType == null || recordId == null) return null;
        for (Row row : rows) {
            if (row.viewType == TYPE_ITEM && recordType.equals(row.recordType)
                    && recordId.equals(row.recordId)) return row;
        }
        return null;
    }

    public Row firstItem() {
        for (Row row : rows) {
            if (row.viewType == TYPE_ITEM) return row;
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SECTION) {
            TextView title = new TextView(context);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            title.setTypeface(context.getResources().getFont(R.font.manrope_extrabold));
            title.setTextColor(0xFF0E6B3C);
            title.setPadding(0, dp(12), 0, dp(10));
            return new TextHolder(title);
        }
        if (viewType == TYPE_EMPTY) {
            TextView message = new TextView(context);
            message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            message.setTypeface(context.getResources().getFont(R.font.manrope_semibold));
            message.setTextColor(0xFF59544C);
            message.setPadding(0, 0, 0, dp(8));
            message.setLineSpacing(0, 1.18f);
            return new TextHolder(message);
        }

        CardView card = new CardView(context);
        card.setRadius(dp(24));
        card.setCardElevation(0);
        card.setCardBackgroundColor(0xFFFFFFFF);
        RecyclerView.LayoutParams cardParams = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(8);
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView title = textView(16, true, 0xFF1C1B1A);
        title.setTypeface(context.getResources().getFont(R.font.manrope_extrabold));
        title.setLineSpacing(0, 1.31f);
        TextView subtitle = textView(13, true, 0xFF59544C);
        subtitle.setTypeface(context.getResources().getFont(R.font.manrope_semibold));
        LinearLayout meta = new LinearLayout(context);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        TextView date = textView(13, true, 0xFF1C1B1A);
        date.setTypeface(context.getResources().getFont(R.font.manrope_extrabold));
        TextView days = textView(11.5f, true, 0xFF8A6414);
        days.setTypeface(context.getResources().getFont(R.font.manrope_extrabold));
        days.setPadding(dp(10), dp(4), dp(10), dp(4));

        content.addView(title);
        content.addView(subtitle, margins(0, dp(4), 0, 0));
        View divider = new View(context);
        divider.setBackgroundColor(0xFFE9E1CF);
        content.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)) {{ topMargin = dp(12); }});
        meta.addView(date, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        meta.addView(days, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(meta, margins(0, dp(12), 0, 0));
        card.addView(content);
        return new ItemHolder(card, title, subtitle, date, days);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof ItemHolder) {
            ItemHolder item = (ItemHolder) holder;
            item.title.setText(row.title);
            item.subtitle.setText(row.subtitle);
            item.date.setText(row.dateText);
            item.days.setText(row.daysText);
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(row.overdue ? 0xFFF8E4DD : 0xFFF6ECD2);
            pill.setCornerRadius(dp(999));
            item.days.setBackground(pill);
            boolean tablet = context.getResources().getConfiguration().screenWidthDp >= 600;
            boolean selected = tablet && row.recordType.equals(selectedRecordType)
                    && row.recordId.equals(selectedRecordId);
            item.card.setCardBackgroundColor(selected ? 0xFFE3F0E7 : 0xFFFFFFFF);
            item.card.setOnClickListener(view -> {
                if (listener != null) listener.onItemClick(row);
            });
        } else {
            TextView text = ((TextHolder) holder).text;
            text.setText(row.title);
            if (row.viewType == TYPE_SECTION) {
                text.setPadding(0, dp("Overdue".equals(row.title) ? 16 : 12), 0, dp(10));
            }
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private TextView textView(float sizeSp, boolean bold, int color) {
        TextView view = new TextView(context);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        if (bold) view.setTypeface(context.getResources().getFont(R.font.manrope_bold));
        return view;
    }

    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private int dp(float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static class TextHolder extends RecyclerView.ViewHolder {
        final TextView text;
        TextHolder(TextView text) {
            super(text);
            this.text = text;
        }
    }

    private static class ItemHolder extends RecyclerView.ViewHolder {
        final CardView card;
        final TextView title;
        final TextView subtitle;
        final TextView date;
        final TextView days;

        ItemHolder(CardView card, TextView title, TextView subtitle, TextView date, TextView days) {
            super(card);
            this.card = card;
            this.title = title;
            this.subtitle = subtitle;
            this.date = date;
            this.days = days;
        }
    }
}
