package com.afrinuts.farmos.ui.theme;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.afrinuts.farmos.R;

public class GradientCardView extends FrameLayout {

    private CardView cardView;
    private View gradientOverlay;
    private View edgeEffect;
    private View glassOverlay;
    private View innerShadow;
    private ImageView iconView;
    private TextView titleText;
    private TextView subtitleText;
    private FrameLayout contentContainer;
    private View ctaContainer;

    private String title;
    private String subtitle;
    private int iconResource;
    private int variant; // 0=primary, 1=accent, 2=secondary
    private boolean showInfoBadge;
    private String ctaText;

    public GradientCardView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public GradientCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public GradientCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_gradient_card, this, true);

        // Find views
        cardView = findViewById(R.id.cardView);
        gradientOverlay = findViewById(R.id.gradientOverlay);
        edgeEffect = findViewById(R.id.edgeEffect);
        glassOverlay = findViewById(R.id.glassOverlay);
        innerShadow = findViewById(R.id.innerShadow);
        iconView = findViewById(R.id.iconView);
        titleText = findViewById(R.id.titleText);
        subtitleText = findViewById(R.id.subtitleText);
        contentContainer = findViewById(R.id.contentContainer);
        ctaContainer = findViewById(R.id.ctaContainer);

        // Load attributes
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.GradientCardView,
                    0, 0);

            try {
                title = a.getString(R.styleable.GradientCardView_cardTitle);
                subtitle = a.getString(R.styleable.GradientCardView_cardSubtitle);
                iconResource = a.getResourceId(R.styleable.GradientCardView_cardIcon, 0);
                variant = a.getInt(R.styleable.GradientCardView_cardVariant, 0);
                showInfoBadge = a.getBoolean(R.styleable.GradientCardView_showInfoBadge, true);
                ctaText = a.getString(R.styleable.GradientCardView_ctaText);
            } finally {
                a.recycle();
            }
        }

        // Apply attributes
        setTitle(title);
        setSubtitle(subtitle);
        setIcon(iconResource);
        setVariant(variant);
        setShowInfoBadge(showInfoBadge);
        setCtaText(ctaText);

        // Setup hover effect (will be handled in activity/fragment)
    }

    public void setTitle(String title) {
        this.title = title;
        if (titleText != null) {
            titleText.setText(title);
        }
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        if (subtitleText != null) {
            if (subtitle != null && !subtitle.isEmpty()) {
                subtitleText.setText(subtitle);
                subtitleText.setVisibility(View.VISIBLE);
            } else {
                subtitleText.setVisibility(View.GONE);
            }
        }
    }

    public void setIcon(int iconRes) {
        this.iconResource = iconRes;
        if (iconView != null) {
            if (iconRes != 0) {
                iconView.setImageResource(iconRes);
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }
        }
    }

    public void setVariant(int variant) {
        this.variant = variant;
        if (gradientOverlay != null) {
            GradientDrawable gradient = (GradientDrawable) gradientOverlay.getBackground();
            if (gradient != null) {
                switch (variant) {
                    case 0: // primary
                        gradient.setColors(new int[]{
                                0x0D5A7411, 0x0DEA580C, 0x0DA0C800
                        });
                        break;
                    case 1: // accent
                        gradient.setColors(new int[]{
                                0x0DEA580C, 0x0D5A7411, 0x0DA0C800
                        });
                        break;
                    case 2: // secondary
                        gradient.setColors(new int[]{
                                0x0DA0C800, 0x0D5A7411, 0x0DEA580C
                        });
                        break;
                }
            }
        }
    }

    public void setShowInfoBadge(boolean show) {
        this.showInfoBadge = show;
        // Info badge implementation will be in layout
        findViewById(R.id.infoBadge).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void setCtaText(String text) {
        this.ctaText = text;
        if (ctaContainer != null) {
            TextView ctaText = findViewById(R.id.ctaText);
            if (text != null && !text.isEmpty()) {
                ctaText.setText(text);
                ctaContainer.setVisibility(View.VISIBLE);
            } else {
                ctaContainer.setVisibility(View.GONE);
            }
        }
    }

    public void setOnCtaClickListener(OnClickListener listener) {
        if (ctaContainer != null) {
            ctaContainer.setOnClickListener(listener);
        }
    }

    public FrameLayout getContentContainer() {
        return contentContainer;
    }

    public void addContent(View content) {
        contentContainer.addView(content);
    }
}