package com.afrinuts.farmos.ui.compliance;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
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

        private Row(int viewType, String title, String subtitle, String dateText, String daysText,
                    String recordId, String recordType, long sortDate) {
            this.viewType = viewType;
            this.title = title;
            this.subtitle = subtitle;
            this.dateText = dateText;
            this.daysText = daysText;
            this.recordId = recordId;
            this.recordType = recordType;
            this.sortDate = sortDate;
        }

        public static Row section(String title) {
            return new Row(TYPE_SECTION, title, null, null, null, null, null, 0);
        }

        public static Row empty(String message) {
            return new Row(TYPE_EMPTY, message, null, null, null, null, null, 0);
        }

        public static Row item(String title, String subtitle, String dateText, String daysText,
                               String recordId, String recordType, long sortDate) {
            return new Row(TYPE_ITEM, title, subtitle, dateText, daysText, recordId, recordType, sortDate);
        }
    }

    private final Context context;
    private final OnItemClickListener listener;
    private final List<Row> rows = new ArrayList<>();

    public DueExpiringAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void submitRows(List<Row> updatedRows) {
        rows.clear();
        if (updatedRows != null) rows.addAll(updatedRows);
        notifyDataSetChanged();
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
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            title.setTextColor(ContextCompat.getColor(context, R.color.primary));
            title.setPadding(0, dp(18), 0, dp(8));
            return new TextHolder(title);
        }
        if (viewType == TYPE_EMPTY) {
            TextView message = new TextView(context);
            message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            message.setTextColor(0xFF777777);
            message.setPadding(dp(4), dp(4), dp(4), dp(12));
            return new TextHolder(message);
        }

        CardView card = new CardView(context);
        card.setRadius(dp(12));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(0xFFFFFFFF);
        RecyclerView.LayoutParams cardParams = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(13), dp(16), dp(13));

        TextView title = textView(16, true, 0xFF222222);
        TextView subtitle = textView(13, false, 0xFF666666);
        LinearLayout meta = new LinearLayout(context);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        TextView date = textView(12, false, 0xFF666666);
        TextView days = textView(12, true, ContextCompat.getColor(context, R.color.primary));

        content.addView(title);
        content.addView(subtitle, margins(0, dp(4), 0, 0));
        meta.addView(date, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        meta.addView(days, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(meta, margins(0, dp(8), 0, 0));
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
            item.card.setOnClickListener(view -> {
                if (listener != null) listener.onItemClick(row);
            });
        } else {
            ((TextHolder) holder).text.setText(row.title);
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
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
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
