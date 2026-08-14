package com.bzsx.password;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.ViewHolder> {

    private List<PasswordEntry> entryList;
    private String searchQuery = "";
    private boolean isSelectMode = false;
    private java.util.Set<Integer> selectedPositions = new java.util.HashSet<>();
    private ItemTouchHelper itemTouchHelper;

    // 设置 ItemTouchHelper，用于拖动手柄直接触发拖动
    public void setItemTouchHelper(ItemTouchHelper helper) {
        this.itemTouchHelper = helper;
    }

    // 接口定义
    public interface OnItemClickListener {
        void onItemClick(int position);
    }
    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }
    public interface OnThreeDotClickListener {
        void onThreeDotClick(int position, View anchor);
    }
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnThreeDotClickListener onThreeDotClickListener;
    private OnSelectionChangedListener onSelectionChangedListener;

    public PasswordAdapter(List<PasswordEntry> entryList) {
        this.entryList = entryList;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query;
    }

    public void setSelectMode(boolean selectMode) {
        this.isSelectMode = selectMode;
        if (!selectMode) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isSelectMode() {
        return isSelectMode;
    }

    public java.util.Set<Integer> getSelectedPositions() {
        return selectedPositions;
    }

    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged(selectedPositions.size());
        }
    }

    public void selectAll() {
        selectedPositions.clear();
        for (int i = 0; i < entryList.size(); i++) {
            selectedPositions.add(i);
        }
        notifyDataSetChanged();
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged(selectedPositions.size());
        }
    }

    public void clearSelection() {
        selectedPositions.clear();
        notifyDataSetChanged();
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged(0);
        }
    }

    public boolean isAllSelected() {
        return selectedPositions.size() == entryList.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }

    public void setOnThreeDotClickListener(OnThreeDotClickListener listener) {
        this.onThreeDotClickListener = listener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.onSelectionChangedListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_password, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PasswordEntry entry = entryList.get(position);
        Context context = holder.itemView.getContext();

        // 设置名称和账号（支持搜索高亮）
        if (searchQuery.isEmpty()) {
            holder.tvName.setText(entry.getName());
            holder.tvAccount.setText(entry.getAccount());
        } else {
            holder.tvName.setText(highlightText(entry.getName(), searchQuery, context));
            holder.tvAccount.setText(highlightText(entry.getAccount(), searchQuery, context));
        }

        // 设置复选框状态
        holder.cbSelect.setVisibility(isSelectMode ? View.VISIBLE : View.GONE);
        holder.cbSelect.setChecked(selectedPositions.contains(position));

        // 设置拖动手柄
        holder.ivDragHandle.setVisibility(isSelectMode ? View.VISIBLE : View.GONE);
        // 触摸拖动手柄直接开始拖动，无需长按
        holder.ivDragHandle.setOnTouchListener((v, event) -> {
            if (isSelectMode && itemTouchHelper != null && event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                itemTouchHelper.startDrag(holder);
            }
            return false;
        });

        // 设置三点菜单可见性（批量模式下隐藏三点，显示复选框）
        holder.ivThreeDots.setVisibility(isSelectMode ? View.GONE : View.VISIBLE);

        // 置顶标签
        if (entry.isPinned()) {
            holder.flPinBadge.setVisibility(View.VISIBLE);
        } else {
            holder.flPinBadge.setVisibility(View.GONE);
        }

        // 显示最后修改时间
        holder.tvTime.setText(formatTime(context, entry.getCreatedAt(), entry.getUpdatedAt()));

        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (isSelectMode) {
                toggleSelection(position);
            } else if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position);
            }
        });

        // 长按事件
        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectMode && onItemLongClickListener != null) {
                onItemLongClickListener.onItemLongClick(position);
            }
            return true;
        });

        // 三点菜单点击
        holder.ivThreeDots.setOnClickListener(v -> {
            if (onThreeDotClickListener != null) {
                onThreeDotClickListener.onThreeDotClick(position, holder.ivThreeDots);
            }
        });
    }

    /**
     * 格式化时间显示：今天/昨天/具体日期，创建与修改时间相同时显示"创建于"
     */
    private String formatTime(Context context, long createdAt, long updatedAt) {
        long timeToShow;
        String prefix;
        if (createdAt == updatedAt) {
            timeToShow = createdAt;
            prefix = "创建于 ";
        } else {
            timeToShow = updatedAt;
            prefix = "修改于 ";
        }

        // 获取今天的开始时间（毫秒）
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);
        long todayStart = todayCal.getTimeInMillis();

        // 获取昨天的开始时间
        long yesterdayStart = todayStart - 24 * 60 * 60 * 1000L;
        // 前天的开始时间（用于判断昨天范围）
        long dayBeforeYesterdayStart = yesterdayStart - 24 * 60 * 60 * 1000L;

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeStr = timeFormat.format(new Date(timeToShow));

        if (timeToShow >= todayStart) {
            // 今天
            return prefix + "今天 " + timeStr;
        } else if (timeToShow >= yesterdayStart) {
            // 昨天
            return prefix + "昨天 " + timeStr;
        } else {
            // 更早
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            return prefix + dateTimeFormat.format(new Date(timeToShow));
        }
    }

    @Override
    public int getItemCount() {
        return entryList.size();
    }

    private SpannableString highlightText(String text, String query, Context context) {
        SpannableString spannable = new SpannableString(text);
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        // 从 M3 主题获取主色作为高亮色
        int highlightColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, android.graphics.Color.CYAN);
        int index = 0;
        while ((index = lowerText.indexOf(lowerQuery, index)) != -1) {
            spannable.setSpan(new ForegroundColorSpan(highlightColor),
                    index, index + query.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            index += query.length();
        }
        return spannable;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvAccount;
        TextView tvTime;
        ImageView ivDragHandle;
        CheckBox cbSelect;
        ImageView ivThreeDots;
        FrameLayout flPinBadge;
        View viewPinBadge;
        TextView tvPinLabel;

        public ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_name);
            tvAccount = itemView.findViewById(R.id.tv_item_account);
            tvTime = itemView.findViewById(R.id.tv_item_time);
            ivDragHandle = itemView.findViewById(R.id.iv_drag_handle);
            cbSelect = itemView.findViewById(R.id.cb_select);
            ivThreeDots = itemView.findViewById(R.id.iv_three_dots);
            flPinBadge = itemView.findViewById(R.id.fl_pin_badge);
            viewPinBadge = itemView.findViewById(R.id.view_pin_badge);
            tvPinLabel = itemView.findViewById(R.id.tv_pin_label);
        }
    }
}