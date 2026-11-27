package com.afrinuts.farmos.ui.blocks;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.repository.BlockRepository;
import com.afrinuts.farmos.data.repository.PhotoRepository;
import com.afrinuts.farmos.ui.photos.PhotoDetailActivity;
import com.afrinuts.farmos.ui.photos.PhotoGalleryActivity;
import com.afrinuts.farmos.utils.SessionManager;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tablet detail-pane counterpart of BlockDetailActivity. */
public class BlockDetailFragment extends Fragment {
    private static final String ARG_BLOCK_ID = "block_id";
    public interface Listener { void onBlockDeleted(long blockId); }
    public static BlockDetailFragment newInstance(long id) { BlockDetailFragment f = new BlockDetailFragment(); Bundle b = new Bundle(); b.putLong(ARG_BLOCK_ID,id); f.setArguments(b); return f; }
    private long blockId, farmId=1;
    private BlockEntity block;
    private View root, edit, save, delete;
    private BlockRepository blocks;
    private PhotoRepository photos;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private SessionManager session;
    private TextInputEditText survival,replacements,notes;
    private TextView[] statValues;
    private boolean editing;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state) {
        root=inflater.inflate(R.layout.activity_block_detail,container,false);
        blockId=getArguments()==null?-1:getArguments().getLong(ARG_BLOCK_ID,-1); session=new SessionManager(requireContext());
        View chrome=root.findViewById(R.id.farmDetailHeader); if(chrome!=null) chrome.setVisibility(View.GONE);
        survival=root.findViewById(R.id.etSurvivalRate); replacements=root.findViewById(R.id.etReplacementCount); notes=root.findViewById(R.id.etNotes);
        edit=root.findViewById(R.id.btnEdit); save=root.findViewById(R.id.btnSave); delete=root.findViewById(R.id.btnDelete);
        edit.setOnClickListener(v->setEditing(true)); save.setOnClickListener(v->save()); delete.setOnClickListener(v->confirmDelete());
        root.findViewById(R.id.btnRecordHarvest).setOnClickListener(v->showHarvest()); root.findViewById(R.id.btnRecordHarvestBottom).setOnClickListener(v->showHarvest());
        root.findViewById(R.id.btnPhotoGallery).setOnClickListener(v->{Intent i=new Intent(requireContext(),PhotoGalleryActivity.class);i.putExtra("block_id",blockId);startActivity(i);});
        addStatsRow();
        hideTimelineIcon();
        root.findViewById(R.id.statusBadge).setOnClickListener(v -> { if (block != null && editing) showStatusUpdate(); });
        styleAsSingleDetailCard();
        styleTabletActions();
        blocks=new BlockRepository(requireContext(),farmId); photos=new PhotoRepository(requireContext(),farmId); loadBlock(); return root;
    }
    private void loadBlock(){ if(blockId<0)return; blocks.getBlockById(blockId).observe(getViewLifecycleOwner(),b->{if(b==null||root==null)return;block=b;farmId=b.getFarmId();
        text(R.id.blockNameText,"Block "+b.getBlockName());text(R.id.statusBadge,b.getStatus().getDisplayName());text(R.id.farmInfoText,String.format(Locale.getDefault(),"AfriNuts Odienné · Size: %.1f ha",b.getHectareSize()));
        edit.setVisibility(session.isInvestor()?View.GONE:View.VISIBLE);delete.setVisibility(session.isOwner()?View.VISIBLE:View.GONE);View h=root.findViewById(R.id.farmDetailEdit);if(h!=null)h.setVisibility(View.GONE);
        display();loadFarmName(b.getFarmId());photos.getPhotosForBlock(blockId).observe(getViewLifecycleOwner(),this::displayPhotos);
    }); }
    private void display(){if(block==null||root==null)return;
        text(R.id.statusBadge,block.getStatus().getDisplayName());text(R.id.aliveTreesText,String.format(Locale.getDefault(),"%,d",block.getAliveTrees()));text(R.id.deadTreesText,String.format(Locale.getDefault(),"%,d",block.getDeadTrees()));
        survival.setText(new DecimalFormat("0.#").format(block.getSurvivalRate())+"%");replacements.setText(String.valueOf(block.getReplacementCount()));root.findViewById(R.id.plantingDetailsSection).setVisibility(block.isPlanted()?View.VISIBLE:View.GONE);
        if (statValues != null) {
            statValues[0].setText(String.format(Locale.getDefault(), "%,d", block.getAliveTrees()));
            statValues[1].setText(String.format(Locale.getDefault(), "%.0f%%", block.getSurvivalRate()));
            statValues[2].setText(block.getHarvestCount() > 0 ? block.getFormattedTotalHarvest() : "—");
        }
        boolean harvested=block.getHarvestCount()>0||block.getStatus()==BlockEntity.BlockStatus.HARVESTED;root.findViewById(R.id.harvestSection).setVisibility(block.isPlanted()||harvested?View.VISIBLE:View.GONE);
        text(R.id.tvTotalHarvest,harvested?block.getFormattedTotalHarvest():"Not harvested yet");text(R.id.tvLastHarvestDate,block.getFormattedLastHarvestDate());text(R.id.tvLastHarvestQuantity,block.getLastHarvestQuantity()>0?String.format(Locale.getDefault(),"%,.0f kg",block.getLastHarvestQuantity()):"—");text(R.id.tvHarvestCount,String.valueOf(block.getHarvestCount()));text(R.id.tvYieldPerHectare,String.format(Locale.getDefault(),"%,.0f kg/ha",block.getYieldPerHectare()));
        text(R.id.clearedDateText,date(block.getClearedDate()));text(R.id.plowedDateText,date(block.getPlowedDate()));text(R.id.plantedDateText,date(block.getPlantedDate()));
        root.findViewById(R.id.clearedSection).setVisibility(block.getClearedDate()!=null||block.getStatus().ordinal()>=1?View.VISIBLE:View.GONE);root.findViewById(R.id.plowedSection).setVisibility(block.getPlowedDate()!=null||block.getStatus().ordinal()>=2?View.VISIBLE:View.GONE);root.findViewById(R.id.plantedSection).setVisibility(block.getPlantedDate()!=null||block.isPlanted()?View.VISIBLE:View.GONE);
        notes.setText(block.getNotes());((android.widget.ProgressBar)root.findViewById(R.id.survivalProgress)).setProgress((int)block.getSurvivalRate()); if(session.isInvestor()){edit.setVisibility(View.GONE);delete.setVisibility(View.GONE);}
    }
    private void text(int id,String value){((android.widget.TextView)root.findViewById(id)).setText(value);}
    private String date(Long value){return value==null||value<=0?"Not recorded":new SimpleDateFormat("MMM dd, yyyy",Locale.getDefault()).format(new Date(value));}
    private void loadFarmName(long id){executor.execute(()->{FarmEntity f=AppDatabase.getInstance(requireContext()).farmDao().getFarmById(id);if(!isAdded()||f==null)return;requireActivity().runOnUiThread(()->{if(root!=null)text(R.id.farmInfoText,String.format(Locale.getDefault(),"%s · Size: %.1f ha",f.getName(),block.getHectareSize()));});});}
    private void setEditing(boolean value){if(!session.canEditData()){Snackbar.make(root,"You don't have permission to edit",Snackbar.LENGTH_SHORT).show();return;}editing=value;edit.setVisibility(value?View.GONE:View.VISIBLE);save.setVisibility(value?View.VISIBLE:View.GONE);survival.setEnabled(value);replacements.setEnabled(value);notes.setEnabled(value);}
    private void showStatusUpdate() {
        StatusUpdateDialog dialog = StatusUpdateDialog.newInstance(block);
        dialog.setOnStatusUpdatedListener(updated -> blocks.updateBlock(updated).thenRun(() -> {
            if (isAdded()) requireActivity().runOnUiThread(() -> { block = updated; display(); });
        }));
        dialog.show(getParentFragmentManager(), "StatusUpdateDialog");
    }
    private void addStatsRow() {
        ViewGroup content = null;
        for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
            View child = ((ViewGroup) root).getChildAt(i);
            if (child instanceof ScrollView) { content = (ViewGroup) ((ScrollView) child).getChildAt(0); break; }
        }
        if (content == null || content.getChildCount() == 0) return;
        LinearLayout row = new LinearLayout(requireContext()); row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(12));
        String[] labels = {"Alive trees", "Survival rate", "Total harvest"};
        statValues = new TextView[3];
        for (int i = 0; i < labels.length; i++) {
            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(requireContext());
            card.setTag("tablet-stat-card");
            card.setRadius(dp(20)); card.setCardElevation(0); card.setCardBackgroundColor(i == 0 ? 0xFF0E6B3C : 0xFFF6F1E7);
            LinearLayout inner = new LinearLayout(requireContext()); inner.setOrientation(LinearLayout.VERTICAL); inner.setPadding(dp(16),dp(14),dp(16),dp(14));
            TextView label = new TextView(requireContext()); label.setText(labels[i]); label.setTextSize(13); label.setTextColor(i == 0 ? 0xFFB8DAC6 : 0xFF59544C);
            TextView value = new TextView(requireContext()); value.setText("—"); value.setTextSize(22); value.setTextColor(i == 0 ? 0xFFFFFFFF : 0xFF1C1B1A); value.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); statValues[i] = value;
            inner.addView(label); inner.addView(value); card.addView(inner);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1); lp.setMarginEnd(i == 2 ? 0 : dp(10)); row.addView(card,lp);
        }
        content.addView(row, 1);
    }
    private void styleAsSingleDetailCard() {
        ScrollView scroll = null;
        for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
            View child = ((ViewGroup) root).getChildAt(i);
            if (child instanceof ScrollView) { scroll = (ScrollView) child; break; }
        }
        if (scroll == null) return;
        android.graphics.drawable.GradientDrawable surface = new android.graphics.drawable.GradientDrawable();
        surface.setColor(android.graphics.Color.WHITE);
        surface.setCornerRadius(dp(24));
        surface.setStroke(dp(1), 0xFFEFE8DA);
        scroll.setBackground(surface);
        scroll.setClipToOutline(true);
        scroll.setPadding(dp(16), dp(16), dp(16), dp(24));
        ViewGroup content = (ViewGroup) scroll.getChildAt(0);
        content.setBackgroundColor(android.graphics.Color.WHITE);
        flattenSectionCards(content);
    }
    private void flattenSectionCards(View view) {
        if (view instanceof com.google.android.material.card.MaterialCardView && "tablet-stat-card".equals(view.getTag())) return;
        if (view instanceof androidx.cardview.widget.CardView) {
            androidx.cardview.widget.CardView card = (androidx.cardview.widget.CardView) view;
            card.setCardElevation(0);
            card.setRadius(0);
            card.setCardBackgroundColor(android.graphics.Color.WHITE);
            if (card instanceof com.google.android.material.card.MaterialCardView) ((com.google.android.material.card.MaterialCardView) card).setStrokeWidth(0);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) flattenSectionCards(group.getChildAt(i));
        }
    }
    private void save(){if(block==null)return;try{if(block.isPlanted()){block.setSurvivalRate(Double.parseDouble(survival.getText().toString().replace("%","").trim()));block.setReplacementCount(Integer.parseInt(replacements.getText().toString().trim()));}}catch(Exception e){Toast.makeText(requireContext(),"Enter valid planting details",Toast.LENGTH_SHORT).show();return;}String n=notes.getText()==null?"":notes.getText().toString().trim();block.setNotes(n.isEmpty()?null:n);blocks.updateBlock(block).thenRun(()->{if(isAdded())requireActivity().runOnUiThread(()->{setEditing(false);display();Toast.makeText(requireContext(),"Changes saved successfully!",Toast.LENGTH_SHORT).show();});});}
    private void showHarvest(){if(block==null)return;RecordHarvestDialog d=RecordHarvestDialog.newInstance(blockId);d.setOnHarvestRecordedListener(b->{block=b;display();});d.show(getParentFragmentManager(),"RecordHarvestDialog");}
    private void confirmDelete(){if(!session.isOwner()||block==null)return;new MaterialAlertDialogBuilder(requireContext()).setTitle("Delete block").setMessage("Are you sure you want to delete "+block.getBlockName()+"? This action cannot be undone.").setNegativeButton("Cancel",null).setPositiveButton("Delete block",(d,w)->blocks.deleteBlock(block).thenRun(()->{if(isAdded())requireActivity().runOnUiThread(()->{if(requireActivity() instanceof Listener)((Listener)requireActivity()).onBlockDeleted(blockId);});})).show();}
    private void displayPhotos(List<PhotoEntity> items){if(root==null)return;android.widget.LinearLayout strip=root.findViewById(R.id.photoThumbnailStrip);strip.removeAllViews();int n=items==null?0:items.size();text(R.id.tvPhotoCount,n+(n==1?" photo":" photos"));root.findViewById(R.id.photoThumbnailScroller).setVisibility(n==0?View.GONE:View.VISIBLE);if(items==null)return;for(PhotoEntity p:items){android.widget.ImageView image=new android.widget.ImageView(requireContext());int size=dp(88);android.widget.LinearLayout.LayoutParams lp=new android.widget.LinearLayout.LayoutParams(size,size);lp.setMarginEnd(dp(10));image.setLayoutParams(lp);image.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();background.setColor(0xFFF5F1E8);background.setCornerRadius(dp(14));background.setStroke(dp(1),0xFFE9E1CF);image.setBackground(background);image.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);image.setClipToOutline(true);String path=p.getThumbnailPath();if(TextUtils.isEmpty(path))path=p.getFilePath();Glide.with(this).load(path).placeholder(R.drawable.ic_photo_placeholder).error(R.drawable.ic_photo_placeholder).centerCrop().into(image);image.setOnClickListener(v->com.afrinuts.farmos.ui.photos.PhotoNavigation.openPhoto(requireContext(),p.getId(),blockId,-1,-1));strip.addView(image);}}
    private void styleTabletActions(){
        com.google.android.material.button.MaterialButton editButton=(com.google.android.material.button.MaterialButton)edit;
        com.google.android.material.button.MaterialButton deleteButton=(com.google.android.material.button.MaterialButton)delete;
        styleTabletAction(editButton,0xFFF3EAD8,0xFF1C1B1A);
        styleTabletAction(deleteButton,0xFFF9E4DE,0xFFB3402A);
        ViewGroup row=(ViewGroup)editButton.getParent().getParent();
        ViewGroup oldParent=(ViewGroup)deleteButton.getParent();
        if(row==null||oldParent==row)return;
        oldParent.removeView(deleteButton);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(52),1f);
        lp.setMarginStart(dp(6));
        deleteButton.setLayoutParams(lp);
        row.addView(deleteButton);
    }
    private void styleTabletAction(com.google.android.material.button.MaterialButton button,int backgroundColor,int textColor){
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(backgroundColor));
        button.setStrokeWidth(0);
        button.setTextColor(textColor);
        button.setCornerRadius(dp(999));
        button.setElevation(0);
        button.setStateListAnimator(null);
    }
    private void hideTimelineIcon(){
        java.util.ArrayList<View> labels=new java.util.ArrayList<>();
        root.findViewsWithText(labels,"Timeline",View.FIND_VIEWS_WITH_TEXT);
        for(View label:labels){
            if(label instanceof TextView && "Timeline".contentEquals(((TextView)label).getText())){
                ViewParent parent=label.getParent();
                if(parent instanceof ViewGroup && ((ViewGroup)parent).getChildCount()>1)
                    ((ViewGroup)parent).getChildAt(0).setVisibility(View.GONE);
                return;
            }
        }
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public void onDestroyView(){super.onDestroyView();root=null;}
    @Override public void onDestroy(){super.onDestroy();if(blocks!=null)blocks.shutdown();if(photos!=null)photos.shutdown();executor.shutdown();}
}
