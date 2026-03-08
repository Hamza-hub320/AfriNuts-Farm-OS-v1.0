package com.afrinuts.farmos.ui.users;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.ui.users.UserDetailActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            userName = itemView.findViewById(R.id.userName);
            userRole = itemView.findViewById(R.id.userRole);
            userStatus = itemView.findViewById(R.id.userStatus);
            userPhone = itemView.findViewById(R.id.userPhone);
            userEmail = itemView.findViewById(R.id.userEmail);
            cardView = (CardView) itemView;
        }

        void bind(UserEntity user, OnUserClickListener listener) {
            // Set avatar with initials
            userAvatar.setImageResource(R.drawable.ic_person);
            userAvatar.setImageTintList(null); // Reset tint for avatar

            userName.setText(user.getName());
            userRole.setText(user.getDisplayRole());

            // Set status indicator
            if (user.isActive()) {
                userStatus.setImageResource(R.drawable.ic_check_circle);
                userStatus.setImageTintList(
                        android.content.res.ColorStateList.valueOf(
                                itemView.getContext().getColor(R.color.primary)));
            } else {
                userStatus.setImageResource(R.drawable.ic_cancel);
                userStatus.setImageTintList(
                        android.content.res.ColorStateList.valueOf(
                                itemView.getContext().getColor(android.R.color.holo_red_dark)));
            }

            // Set contact info
            userPhone.setText(user.getPhone() != null ? user.getPhone() : "No phone");

            if (user.getAuthMethod() == UserEntity.AuthMethod.PASSWORD) {
                userEmail.setText(user.getEmail() != null ? user.getEmail() : "No email");
            } else {
                userEmail.setText("🔢 PIN User");
            }

            // Click listener
            cardView.setOnClickListener(v -> {
                android.content.Context context = itemView.getContext();
                android.content.Intent intent = new android.content.Intent(context, UserDetailActivity.class);
                intent.putExtra(UserDetailActivity.EXTRA_USER_ID, user.getId());
                context.startActivity(intent);
            });
        }
    }
}