package com.afrinuts.farmos.ui.users;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.afrinuts.farmos.R;

import java.util.List;

public class RoleAdapter extends ArrayAdapter<RoleItem> {

    private LayoutInflater inflater;
    private List<RoleItem> roles;

    public RoleAdapter(@NonNull Context context, @NonNull List<RoleItem> roles) {
        super(context, 0, roles);
        this.inflater = LayoutInflater.from(context);
        this.roles = roles;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_role_dropdown, parent, false);
        }

        RoleItem roleItem = getItem(position);

        ImageView iconView = convertView.findViewById(R.id.roleIcon);
        TextView nameView = convertView.findViewById(R.id.roleName);

        if (roleItem != null) {
            iconView.setImageResource(roleItem.getIconResId());
            iconView.setColorFilter(ContextCompat.getColor(getContext(), R.color.primary));
            nameView.setText(roleItem.getDisplayName());
        }

        return convertView;
    }
}