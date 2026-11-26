package com.afrinuts.farmos.ui.expenses;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.ui.photos.PhotoDetailActivity;
import com.afrinuts.farmos.ui.photos.PhotoGalleryActivity;
import com.bumptech.glide.Glide;
import com.afrinuts.farmos.data.repository.ExpenseRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tablet master-detail presentation for Finance expenses. */
public class ExpenseDetailFragment extends Fragment {
    private static final String ARG_ID = "expense_id";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout content;
    private ExpenseEntity expense;

    public static ExpenseDetailFragment newInstance(long id) {
        ExpenseDetailFragment f = new ExpenseDetailFragment();
        Bundle b = new Bundle(); b.putLong(ARG_ID, id); f.setArguments(b); return f;
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(Color.WHITE);
        scroll.setBackground(background(Color.WHITE, 28));
        scroll.setClipToOutline(true);
        content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(24), dp(24), dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        load();
        return scroll;
    }

    private void load() {
        long id = requireArguments().getLong(ARG_ID);
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            ExpenseEntity e = db.expenseDao().getExpenseById(id);
            BlockEntity block = e == null || e.getBlockId() == null ? null : db.blockDao().getBlockById(e.getBlockId());
            QualityBatchEntity batch = e == null || e.getBatchId() == null ? null : db.qualityBatchDao().getBatchById(e.getBatchId());
            SupplierEntity supplier = e == null || e.getSupplierId() == null ? null : db.supplierDao().getSupplierById(e.getSupplierId());
            FarmEntity farm = e == null ? null : db.farmDao().getFarmById(e.getFarmId());
            int photoCount = e == null ? 0 : db.photoDao().getPhotoCountForExpense(e.getId());
            java.util.List<PhotoEntity> photos = e == null ? java.util.Collections.emptyList() : db.photoDao().getPhotosByExpenseId(e.getId());
            int supplierBatchCount = supplier == null ? 0 : db.qualityBatchDao().getBatchCountBySupplierId(supplier.getId());
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> { if (isAdded() && e != null) bind(e, block, batch, supplier, farm, photoCount, supplierBatchCount, photos); });
        });
    }

    private void bind(ExpenseEntity e, BlockEntity block, QualityBatchEntity batch, SupplierEntity supplier, FarmEntity farm, int photoCount, int supplierBatchCount, java.util.List<PhotoEntity> photos) {
        expense = e;
        String category = e.getCategory() == null ? "Expense" : e.getCategory().getDisplayName();
        String source = e.isProcurementRelated()
                ? join(e.getBatchNumber() == null ? null : "Batch " + e.getBatchNumber(), e.getSupplierName() == null ? null : e.getSupplierName())
                : e.isFarmWide() ? "Farm-Wide" : "Block " + (block == null ? "" : block.getBlockName());
        if (farm != null && !blank(farm.getLocation())) source = join(source, farm.getLocation());
        TextView title = text(category, 26, 0xFF1C1B1A, R.font.manrope_extrabold);
        content.addView(title);
        TextView subtitle = text(source, 16, 0xFF59544C, R.font.manrope_semibold);
        margin(subtitle, 0, 2, 0, 18); content.addView(subtitle);

        LinearLayout stats = new LinearLayout(requireContext()); stats.setOrientation(LinearLayout.HORIZONTAL);
        String date = e.getDate() <= 0 ? "Not recorded" : new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date(e.getDate()));
        addStat(stats, "Amount", String.format(Locale.getDefault(), "%,.0f FCFA", e.getAmount()), true);
        addStat(stats, "Date", date, false);
        addStat(stats, "Source", category, false);
        content.addView(stats, params(-1, -2));
        heading("Details", 20);
        row("Amount", String.format(Locale.getDefault(), "%,.0f FCFA", e.getAmount()));
        row("Date", date); row("Category", category);
        if (e.getBlockId() != null || !e.isProcurementRelated()) row("Block", block == null ? "Farm-Wide" : block.getBlockName());
        if (!blank(e.getDescription())) {
            sectionHeading("Notes"); content.addView(text(e.getDescription(), 16, 0xFF393632, R.font.manrope_medium));
        }
        LinearLayout photoHeader = new LinearLayout(requireContext());
        photoHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView photoTitle = text("Photos", 18, 0xFF1C1B1A, R.font.manrope_extrabold);
        photoHeader.addView(photoTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView photoLabel = text(photoCount + (photoCount == 1 ? " photo" : " photos"), 15, 0xFF59544C, R.font.manrope_semibold);
        photoHeader.addView(photoLabel);
        margin(photoHeader, 0, 18, 0, 0);
        content.addView(photoHeader);
        if (!photos.isEmpty()) {
            HorizontalScrollView photoScroll = new HorizontalScrollView(requireContext());
            photoScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout strip = new LinearLayout(requireContext());
            strip.setOrientation(LinearLayout.HORIZONTAL);
            for (PhotoEntity photo : photos) {
                ImageView image = new ImageView(requireContext());
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                GradientDrawable photoBg = background(0xFFF5F0E6, 18);
                photoBg.setStroke(dp(1), 0xFFE9E1CF);
                image.setBackground(photoBg);
                image.setClipToOutline(true);
                String path = blank(photo.getThumbnailPath()) ? photo.getFilePath() : photo.getThumbnailPath();
                Glide.with(this).load(path).placeholder(R.drawable.ic_photo_placeholder).error(R.drawable.ic_photo_placeholder).into(image);
                LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(112), dp(112));
                imageParams.setMargins(0, dp(10), dp(10), 0);
                strip.addView(image, imageParams);
                image.setOnClickListener(v -> {
                    Intent i = new Intent(requireContext(), PhotoDetailActivity.class);
                    i.putExtra(PhotoDetailActivity.EXTRA_PHOTO_ID, photo.getId());
                    startActivity(i);
                });
            }
            photoScroll.addView(strip);
            content.addView(photoScroll);
        }
        LinearLayout galleryRow = new LinearLayout(requireContext());
        galleryRow.setGravity(Gravity.END);
        LinearLayout gallery = new LinearLayout(requireContext());
        gallery.setGravity(Gravity.CENTER);
        gallery.setPadding(dp(16), dp(10), dp(16), dp(10));
        gallery.setBackground(background(0xFFE2F1E8, 999));
        ImageView galleryIcon = new ImageView(requireContext());
        galleryIcon.setImageResource(R.drawable.ic_photo_placeholder);
        galleryIcon.setColorFilter(0xFF075C37);
        gallery.addView(galleryIcon, new LinearLayout.LayoutParams(dp(18), dp(18)));
        TextView galleryText = text("View gallery", 14, 0xFF075C37, R.font.manrope_bold);
        LinearLayout.LayoutParams galleryTextParams = new LinearLayout.LayoutParams(-2, -2);
        galleryTextParams.setMarginStart(dp(8));
        gallery.addView(galleryText, galleryTextParams);
        gallery.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), PhotoGalleryActivity.class);
            i.putExtra("expense_id", e.getId());
            startActivity(i);
        });
        galleryRow.addView(gallery);
        LinearLayout.LayoutParams galleryParams = new LinearLayout.LayoutParams(-1, -2);
        galleryParams.topMargin = dp(12);
        content.addView(galleryRow, galleryParams);
        if (e.isProcurementRelated()) {
            heading("Procurement Details", 20);
            row("Supplier", supplier == null ? safe(e.getSupplierName()) : supplier.getName());
            row("Invoice", "Not recorded"); row("Receipt", "Not recorded");
            if (batch != null) { row("Batch", safe(batch.getBatchNumber())); row("KOR", batch.getKorGPerKg() > 0 ? String.format(Locale.getDefault(), "%.0f g/kg", batch.getKorGPerKg()) : "Not recorded"); row("Received", batch.getProcessingDate() > 0 ? new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date(batch.getProcessingDate())) : "Not recorded"); }
            if (supplier != null) { row("Village", safe(supplier.getLocation())); row("Batches supplied", String.valueOf(supplierBatchCount)); }
        }
        View spacer = new View(requireContext()); content.addView(spacer, new LinearLayout.LayoutParams(1, dp(20)));
        action("Edit", 0xFFF1E8D7, 0xFF1C1B1A, () -> { Intent i = new Intent(requireContext(), ExpenseDetailActivity.class); i.putExtra(ExpenseDetailActivity.EXTRA_EXPENSE_ID, e.getId()); startActivity(i); });
        action("Delete", 0xFFF9E4DE, 0xFFB3402A, this::confirmDelete);
    }

    private void addStat(LinearLayout row, String label, String value, boolean primary) {
        LinearLayout card = new LinearLayout(requireContext()); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(18), dp(18), dp(12), dp(18)); card.setMinimumHeight(dp(96));
        card.setBackground(background(primary ? 0xFF0E6B3C : 0xFFF5F0E6, 24));
        TextView l = text(label, 14, primary ? 0xFFD7E9DD : 0xFF59544C, R.font.manrope_bold); card.addView(l);
        TextView v = text(value, 22, primary ? Color.WHITE : 0xFF1C1B1A, R.font.manrope_extrabold); margin(v, 0, 4, 0, 0); v.setMaxLines(1); v.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM); card.addView(v);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1); p.setMargins(dp(4), 0, dp(4), 0); row.addView(card, p);
    }
    private void heading(String value, int top) { TextView t=text(value,18,0xFF1C1B1A,R.font.manrope_extrabold); margin(t,0,top,0,0); content.addView(t); }
    private void sectionHeading(String value) { TextView t=text(value,15,0xFF59544C,R.font.manrope_bold); margin(t,0,18,0,5); content.addView(t); }
    private void row(String label, String value) { LinearLayout r=new LinearLayout(requireContext()); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(0,dp(13),0,dp(13)); TextView l=text(label,15,0xFF59544C,R.font.manrope_semibold); r.addView(l,new LinearLayout.LayoutParams(0,-2,1)); TextView v=text(value,16,0xFF1C1B1A,R.font.manrope_bold); v.setGravity(Gravity.END); v.setMaxWidth(dp(480)); r.addView(v); View line=new View(requireContext()); line.setBackgroundColor(0xFFE9E1CF); content.addView(r); content.addView(line,new LinearLayout.LayoutParams(-1,dp(1))); }
    private void action(String title,int bg,int fg,Runnable run) { TextView b=text(title,16,fg,R.font.manrope_extrabold); b.setGravity(Gravity.CENTER); b.setBackground(background(bg,999)); b.setClickable(true); b.setFocusable(true); content.addView(b,new LinearLayout.LayoutParams(-1,dp(58))); margin(b,0,0,0,12); b.setOnClickListener(v->run.run()); }
    private void confirmDelete() { new MaterialAlertDialogBuilder(requireContext()).setTitle("Delete expense").setMessage("Delete this expense record?").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{ ExpenseRepository repository=new ExpenseRepository(requireContext(),expense.getFarmId()); repository.deleteExpense(expense,new com.afrinuts.farmos.data.repository.BaseRepository.RepositoryCallback<Boolean>() { public void onSuccess(Boolean ok) { if(isAdded() && getActivity() instanceof Listener) ((Listener)getActivity()).onExpenseDeleted(expense.getId()); repository.shutdown(); } public void onError(String error) { repository.shutdown(); } }); }).show(); }
    public interface Listener { void onExpenseDeleted(long id); }
    private TextView text(String s,int size,int color,int font) { TextView t=new TextView(requireContext()); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(),font)); return t; }
    private GradientDrawable background(int color,int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private LinearLayout.LayoutParams params(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private void margin(View v,int l,int t,int r,int b){ ViewGroup.LayoutParams raw=v.getLayoutParams(); if(raw instanceof ViewGroup.MarginLayoutParams){((ViewGroup.MarginLayoutParams)raw).setMargins(dp(l),dp(t),dp(r),dp(b));v.setLayoutParams(raw);} else {int width=raw==null?-1:raw.width; int height=raw==null?-2:raw.height; LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(width,height);p.setMargins(dp(l),dp(t),dp(r),dp(b));v.setLayoutParams(p);} }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private String safe(String s){return blank(s)?"Not recorded":s;}
    private boolean blank(String s){return s==null||s.trim().isEmpty();}
    private String join(String... xs){StringBuilder b=new StringBuilder();for(String x:xs)if(!blank(x)){if(b.length()>0)b.append(" · ");b.append(x);}return b.toString();}
    @Override public void onDestroy(){executor.shutdown();super.onDestroy();}
}
