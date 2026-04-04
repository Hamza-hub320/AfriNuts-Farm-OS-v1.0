package com.afrinuts.farmos.ui.users;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.UserEntity;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<UserEntity> users;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(UserEntity user);
    }

    public UserAdapter(List<UserEntity> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserEntity user = users.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {

        private ImageView userAvatar;
        private TextView userName;
        private TextView userRole;
        private ImageView userStatus;
        private TextView userPhone;
        private TextView userEmail;
        private CardView cardView;
        private ImageView roleIcon;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            userName = itemView.findViewById(R.id.userName);
            userRole = itemView.findViewById(R.id.userRole);
            userStatus = itemView.findViewById(R.id.userStatus);
            userPhone = itemView.findViewById(R.id.userPhone);
            userEmail = itemView.findViewById(R.id.userEmail);
            cardView = (CardView) itemView;
            roleIcon = itemView.findViewById(R.id.roleIcon);
        }

        void bind(UserEntity user, OnUserClickListener listener) {
            String initials = user.getInitials();
            if (initials != null && !initials.isEmpty()) {
                userAvatar.setImageResource(R.drawable.ic_person);
                userAvatar.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.primary));
            }

            userName.setText(user.getName());

            // Set role with vector icon
            UserEntity.UserRole role = user.getRole();
            int roleIconRes = getRoleIcon(role);
            String roleText = role.getDisplayName();

            // Set role icon and text
            if (roleIcon != null) {
                roleIcon.setImageResource(roleIconRes);
                roleIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.primary));
                roleIcon.setVisibility(View.VISIBLE);
            }
            userRole.setText(roleText);

            // Set status indicator
            if (user.isActive()) {
                userStatus.setImageResource(R.drawable.ic_check_circle);
                userStatus.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.primary));
                userStatus.setVisibility(View.VISIBLE);
            } else {
                userStatus.setImageResource(R.drawable.ic_cancel);
                userStatus.setColorFilter(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark));
                userStatus.setVisibility(View.VISIBLE);
            }

            // Set contact info
            String phone = user.getPhone();
            userPhone.setText(phone != null && !phone.isEmpty() ? phone : "No phone");

            // Set email based on auth method
            if (user.getAuthMethod() == UserEntity.AuthMethod.PASSWORD) {
                String email = user.getEmail();
                userEmail.setText(email != null && !email.isEmpty() ? email : "No email");
            } else {
                userEmail.setText("PIN User");
            }

            // Click listener
            cardView.setOnClickListener(v -> listener.onUserClick(user));
        }

        private int getRoleIcon(UserEntity.UserRole role) {
            if (role == null) return R.drawable.ic_person;

            switch (role) {
                case OWNER:
                    return R.drawable.ic_star;
                case MANAGER:
                    return R.drawable.ic_assignment;
                case WORKER:
                    return R.drawable.ic_person;
                case INVESTOR:
                    return R.drawable.ic_trending_up;
                default:
                    return R.drawable.ic_person;
            }
        }
    }
}