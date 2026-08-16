package com.bzsx.password;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PasswordListFragment extends Fragment {

    private RecyclerView recyclerView;
    private PasswordAdapter adapter;
    private List<PasswordEntry> entryList;
    private List<PasswordEntry> allEntries;
    private EditText searchEditText;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyIcon;
    private TextView tvEmptyText;
    private TextView tvTotalCount;

    // 批量选择栏控件
    private LinearLayout layoutBatchBar;
    private TextView tvSelectCount;
    private MaterialButton btnSelectAll;
    private MaterialButton btnDeleteSelected;
    private MaterialButton btnCancelSelect;

    private DatabaseHelper dbHelper;

    // 导入导出按钮
    private TextView tvImport;
    private TextView tvExport;

    // 文件选择 Launcher
    private ActivityResultLauncher<String> exportLauncher;
    private ActivityResultLauncher<String[]> importLauncher;

    private static final int REQUEST_CODE_ADD_EDIT = 1;
    private static final int REQUEST_CODE_DETAIL = 2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_password_list, container, false);

        recyclerView = view.findViewById(R.id.rv_passwords);
        searchEditText = view.findViewById(R.id.et_search);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        tvEmptyIcon = view.findViewById(R.id.tv_empty_icon);
        tvEmptyText = view.findViewById(R.id.tv_empty_text);
        tvTotalCount = view.findViewById(R.id.tv_total_count);

        // 绑定批量选择栏控件
        layoutBatchBar = view.findViewById(R.id.layout_batch_bar);
        tvSelectCount = view.findViewById(R.id.tv_select_count);
        btnSelectAll = view.findViewById(R.id.btn_select_all);
        btnDeleteSelected = view.findViewById(R.id.btn_delete_selected);
        btnCancelSelect = view.findViewById(R.id.btn_cancel_select);

        dbHelper = new DatabaseHelper(getActivity());

        entryList = new ArrayList<>();
        allEntries = new ArrayList<>();
        adapter = new PasswordAdapter(entryList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        // 注册所有监听器
        setupAdapterListeners();
        setupBatchBarListeners();

        // 设置 ItemTouchHelper 实现拖动排序
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new DragSortCallback());
        itemTouchHelper.attachToRecyclerView(recyclerView);
        adapter.setItemTouchHelper(itemTouchHelper);

        // 初始化导入导出按钮
        setupImportExportButtons(view);

        loadPasswords();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchPasswords(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // 初始化文件选择 Launcher（导出：创建新文件）
        exportLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
            if (uri != null) {
                exportPasswords(uri);
            }
        });

        // 初始化文件选择 Launcher（导入：选择文件）
        importLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                importPasswords(uri);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPasswords();
    }

    // ======================== 设置 Adapter 监听器 ========================

    private void setupAdapterListeners() {
        adapter.setOnItemClickListener(new PasswordAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                PasswordEntry entry = entryList.get(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class);
                intent.putExtra("password_id", entry.getId());
                startActivity(intent);
            }
        });

        adapter.setOnItemLongClickListener(new PasswordAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(int position) {
                enterSelectMode();
                adapter.toggleSelection(position);
                updateSelectCount();
            }
        });

        adapter.setOnThreeDotClickListener(new PasswordAdapter.OnThreeDotClickListener() {
            @Override
            public void onThreeDotClick(int position, View anchorView) {
                showThreeDotMenu(position, anchorView);
            }
        });

        adapter.setOnSelectionChangedListener(new PasswordAdapter.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(int selectedCount) {
                updateSelectCount();
            }
        });
    }

    // ======================== 批量选择栏监听器 ========================

    private void setupBatchBarListeners() {
        btnSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int totalVisible = entryList.size();
                int selectedCount = adapter.getSelectedPositions().size();
                if (selectedCount >= totalVisible) {
                    adapter.clearSelection();
                } else {
                    adapter.selectAll();
                }
                updateSelectCount();
            }
        });

        btnDeleteSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedCount = adapter.getSelectedPositions().size();
                if (selectedCount == 0) {
                    Toast.makeText(getActivity(), "请先选择要删除的密码", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle("确认删除")
                        .setMessage("确定要删除选中的 " + selectedCount + " 个密码吗？")
                        .setPositiveButton("删除", (dialog, which) -> PasswordListFragment.this.deleteSelectedPasswords())
                        .setNegativeButton("取消", null)
                        .show();
            }
        });

        btnCancelSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSelectMode();
            }
        });
    }

    // ======================== 进入/退出批量选择模式 ========================

    private void enterSelectMode() {
        adapter.setSelectMode(true);
        layoutBatchBar.setVisibility(View.VISIBLE);
        adapter.clearSelection();
        updateSelectCount();
    }

    private void exitSelectMode() {
        adapter.setSelectMode(false);
        layoutBatchBar.setVisibility(View.GONE);
        adapter.clearSelection();
        adapter.notifyDataSetChanged();
    }

    private void updateSelectCount() {
        int count = adapter.getSelectedPositions().size();
        int total = entryList.size();
        tvSelectCount.setText("已选择 " + count + " / " + total + " 项");
        if (count >= total) {
            btnSelectAll.setText("取消全选");
        } else {
            btnSelectAll.setText("全选");
        }
    }

    // ======================== 删除选中项 ========================

    private void deleteSelectedPasswords() {
        List<Integer> positions = new ArrayList<>(adapter.getSelectedPositions());

        List<Long> idsToDelete = new ArrayList<>();
        List<PasswordEntry> entriesToRemove = new ArrayList<>();

        for (int pos : positions) {
            if (pos >= 0 && pos < entryList.size()) {
                PasswordEntry entry = entryList.get(pos);
                idsToDelete.add(entry.getId());
                entriesToRemove.add(entry);
            }
        }

        for (long id : idsToDelete) {
            dbHelper.deletePassword(id);
        }

        entryList.removeAll(entriesToRemove);
        allEntries.removeAll(entriesToRemove);

        adapter.clearSelection();
        adapter.notifyDataSetChanged();
        updateSelectCount();
        exitSelectMode();
        updateEmptyView();
        updateTotalCount();
        Toast.makeText(getActivity(), "删除成功", Toast.LENGTH_SHORT).show();
    }

    // ======================== 三点菜单 ========================

    private void showThreeDotMenu(int position, View anchorView) {
        PasswordEntry entry = entryList.get(position);
        PopupMenu popup = new PopupMenu(getActivity(), anchorView);
        popup.getMenu().add(0, 1, 0, "编辑");
        popup.getMenu().add(0, 2, 0, "删除");
        if (entry.isPinned()) {
            popup.getMenu().add(0, 3, 0, "取消置顶");
        } else {
            popup.getMenu().add(0, 3, 0, "置顶");
        }

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: // 编辑
                    Intent editIntent = new Intent(getActivity(), AddEditActivity.class);
                    editIntent.putExtra("edit_id", (int) entry.getId());
                    startActivityForResult(editIntent, REQUEST_CODE_ADD_EDIT);
                    return true;
                case 2: // 删除
                    new AlertDialog.Builder(requireContext())
                            .setTitle("确认删除")
                            .setMessage("确定要删除「" + entry.getName() + "」吗？")
                            .setPositiveButton("删除", (dialog, which) -> {
                                dbHelper.deletePassword(entry.getId());
                                entryList.remove(position);
                                allEntries.remove(entry);
                                adapter.notifyDataSetChanged();
                                updateEmptyView();
                                Toast.makeText(getActivity(), "已删除", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                    return true;
                case 3: // 置顶/取消置顶
                    if (entry.isPinned()) {
                        dbHelper.updateSortOrder(entry.getId(), 0);
                        entry.setSortOrder(0);
                    } else {
                        long sortOrder = System.currentTimeMillis();
                        dbHelper.updateSortOrder(entry.getId(), sortOrder);
                        entry.setSortOrder(sortOrder);
                    }
                    loadPasswords();
                    return true;
                default:
                    return false;
            }
        });

        popup.show();
    }

    // ======================== 拖动排序回调 ========================

    private class DragSortCallback extends ItemTouchHelper.SimpleCallback {

        public DragSortCallback() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            int fromPos = viewHolder.getAdapterPosition();
            int toPos = target.getAdapterPosition();

            PasswordEntry fromEntry = entryList.get(fromPos);
            PasswordEntry toEntry = entryList.get(toPos);

            boolean bothPinned = fromEntry.isPinned() && toEntry.isPinned();
            boolean bothUnpinned = !fromEntry.isPinned() && !toEntry.isPinned();

            if (!bothPinned && !bothUnpinned) {
                return false;
            }

            java.util.Collections.swap(entryList, fromPos, toPos);
            adapter.notifyItemMoved(fromPos, toPos);

            updateSortOrdersForGroup(fromEntry.isPinned());

            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        private void updateSortOrdersForGroup(boolean pinnedGroup) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            try {
                if (pinnedGroup) {
                    long baseTime = System.currentTimeMillis();
                    int index = 0;
                    for (int i = 0; i < entryList.size(); i++) {
                        PasswordEntry entry = entryList.get(i);
                        if (entry.isPinned()) {
                            long newSortOrder = baseTime - index;
                            entry.setSortOrder(newSortOrder);
                            android.content.ContentValues values = new android.content.ContentValues();
                            values.put("sort_order", newSortOrder);
                            db.update("passwords", values, "id=?", new String[]{String.valueOf(entry.getId())});
                            index++;
                        }
                    }
                } else {
                    for (int i = 0; i < entryList.size(); i++) {
                        PasswordEntry entry = entryList.get(i);
                        if (!entry.isPinned()) {
                            entry.setSortOrder(0);
                            android.content.ContentValues values = new android.content.ContentValues();
                            values.put("sort_order", 0);
                            db.update("passwords", values, "id=?", new String[]{String.valueOf(entry.getId())});
                        }
                    }
                }
            } finally {
                db.close();
            }
        }
    }

    // ======================== 数据加载与搜索 ========================

    private void loadPasswords() {
        if (adapter.isSelectMode()) {
            exitSelectMode();
        }

        allEntries.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("passwords", null, null, null, null, null, "sort_order DESC, updated_at DESC");
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String account = cursor.getString(cursor.getColumnIndexOrThrow("account"));
            String encryptedPassword = cursor.getString(cursor.getColumnIndexOrThrow("encrypted_password"));
            long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
            long updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
            long sortOrder = cursor.getLong(cursor.getColumnIndexOrThrow("sort_order"));

            PasswordEntry entry = new PasswordEntry(name, account, encryptedPassword);
            entry.setId(id);
            entry.setCreatedAt(createdAt);
            entry.setUpdatedAt(updatedAt);
            entry.setSortOrder(sortOrder);
            allEntries.add(entry);
        }
        cursor.close();
        db.close();

        String query = searchEditText.getText().toString();
        searchPasswords(query);
    }

    private void updateTotalCount() {
        if (entryList.isEmpty()) {
            tvTotalCount.setVisibility(View.GONE);
            return;
        }
        String query = searchEditText.getText().toString().trim();
        if (query.isEmpty()) {
            tvTotalCount.setText("共 " + entryList.size() + " 条密码");
        } else {
            tvTotalCount.setText("搜索结果：共 " + entryList.size() + " 条");
        }
        tvTotalCount.setVisibility(View.VISIBLE);
    }

    private void searchPasswords(String query) {
        entryList.clear();
        if (query.isEmpty()) {
            entryList.addAll(allEntries);
            adapter.setSearchQuery("");
        } else {
            String lowerQuery = query.toLowerCase();
            for (PasswordEntry entry : allEntries) {
                if (entry.getName().toLowerCase().contains(lowerQuery) ||
                        entry.getAccount().toLowerCase().contains(lowerQuery)) {
                    entryList.add(entry);
                }
            }
            adapter.setSearchQuery(query);
        }
        adapter.notifyDataSetChanged();
        updateEmptyView();
        updateTotalCount();
    }

    private void updateEmptyView() {
        if (allEntries.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            tvEmptyIcon.setText("\uD83D\uDD12");
            tvEmptyText.setText("还没有密码\n点击右上角 + 添加");
            recyclerView.setVisibility(View.GONE);
        } else if (entryList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            tvEmptyIcon.setText("\uD83D\uDD0D");
            tvEmptyText.setText("没有匹配的密码\n试试换个关键词搜索");
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    public void onAddPassword() {
        Intent intent = new Intent(getActivity(), AddEditActivity.class);
        startActivityForResult(intent, REQUEST_CODE_ADD_EDIT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_EDIT && resultCode == getActivity().RESULT_OK) {
            loadPasswords();
        }
    }

    // ======================== 导入/导出按钮初始化 ========================

    private void setupImportExportButtons(View view) {
        tvImport = view.findViewById(R.id.tv_import);
        tvExport = view.findViewById(R.id.tv_export);

        int colorPrimary = MaterialColors.getColor(requireContext(), R.attr.colorPrimary, 0);

        GradientDrawable importBg = (GradientDrawable) tvImport.getBackground();
        importBg.setColor(colorPrimary);

        GradientDrawable exportBg = (GradientDrawable) tvExport.getBackground();
        exportBg.setColor(colorPrimary);

        tvImport.setTextColor(0xFFFFFFFF);
        tvExport.setTextColor(0xFFFFFFFF);

        tvImport.setOnClickListener(v -> {
            importLauncher.launch(new String[]{"application/json", "*/*"});
        });

        tvExport.setOnClickListener(v -> {
            int totalCount = allEntries.size();
            if (totalCount == 0) {
                Toast.makeText(getActivity(), "没有可导出的密码", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(requireContext())
                    .setTitle("导出密码")
                    .setMessage("将导出全部 " + totalCount + " 条密码到加密JSON文件\n\n导出的文件将使用主密码加密，请妥善保管。")
                    .setPositiveButton("导出", (dialog, which) -> {
                        exportLauncher.launch("passwords_backup.json");
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    // ======================== 导出密码 ========================

    private void exportPasswords(Uri uri) {
        String masterPassword = SessionManager.getInstance().getMasterPassword(getActivity());
        if (masterPassword == null || masterPassword.isEmpty()) {
            Toast.makeText(getActivity(), "主密码丢失，无法导出", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query("passwords", null, null, null, null, null, "sort_order DESC, updated_at DESC");
            JSONArray jsonArray = new JSONArray();
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String account = cursor.getString(cursor.getColumnIndexOrThrow("account"));
                String encryptedPassword = cursor.getString(cursor.getColumnIndexOrThrow("encrypted_password"));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
                long updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
                long sortOrder = cursor.getLong(cursor.getColumnIndexOrThrow("sort_order"));

                JSONObject entryJson = new JSONObject();
                entryJson.put("name", name);
                entryJson.put("account", account);
                entryJson.put("encrypted_password", encryptedPassword);
                entryJson.put("is_pinned", sortOrder > 0);
                entryJson.put("sort_order", sortOrder);
                entryJson.put("created_at", createdAt);
                entryJson.put("updated_at", updatedAt);
                jsonArray.put(entryJson);
            }
            cursor.close();
            db.close();

            JSONObject exportJson = new JSONObject();
            exportJson.put("version", 1);
            exportJson.put("entries", jsonArray);

            String plaintext = exportJson.toString();
            String encryptedExport = CryptoUtil.encrypt(plaintext, masterPassword);

            try (OutputStream outputStream = getActivity().getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(encryptedExport.getBytes("UTF-8"));
                    outputStream.flush();
                    Toast.makeText(getActivity(), "导出成功！共导出 " + jsonArray.length() + " 条密码", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ======================== 导入密码 ========================

    private void importPasswords(Uri uri) {
        String masterPassword = SessionManager.getInstance().getMasterPassword(getActivity());
        if (masterPassword == null || masterPassword.isEmpty()) {
            Toast.makeText(getActivity(), "主密码丢失，无法导入", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            StringBuilder stringBuilder = new StringBuilder();
            try (InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            }

            String encryptedContent = stringBuilder.toString();

            String decryptedJson;
            try {
                decryptedJson = CryptoUtil.decrypt(encryptedContent, masterPassword);
            } catch (Exception e) {
                Toast.makeText(getActivity(), "文件格式错误或主密码不正确", Toast.LENGTH_LONG).show();
                return;
            }

            JSONObject exportJson = new JSONObject(decryptedJson);
            int version = exportJson.getInt("version");
            if (version != 1) {
                Toast.makeText(getActivity(), "不支持的文件版本", Toast.LENGTH_LONG).show();
                return;
            }

            JSONArray jsonArray = exportJson.getJSONArray("entries");
            if (jsonArray.length() == 0) {
                Toast.makeText(getActivity(), "文件中没有密码数据", Toast.LENGTH_SHORT).show();
                return;
            }

            List<PasswordEntry> importedEntries = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject entryJson = jsonArray.getJSONObject(i);
                String name = entryJson.getString("name");
                String account = entryJson.optString("account", "");
                String encryptedPassword = entryJson.getString("encrypted_password");
                long createdAt = entryJson.optLong("created_at", System.currentTimeMillis());
                long updatedAt = entryJson.optLong("updated_at", System.currentTimeMillis());
                long sortOrder = entryJson.optLong("sort_order", 0);

                PasswordEntry entry = new PasswordEntry(name, account, encryptedPassword);
                entry.setCreatedAt(createdAt);
                entry.setUpdatedAt(updatedAt);
                entry.setSortOrder(sortOrder);
                importedEntries.add(entry);
            }

            showImportOptionDialog(importedEntries);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "导入失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showImportOptionDialog(List<PasswordEntry> importedEntries) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 8, 48, 8);

        android.widget.CheckBox checkBox = new android.widget.CheckBox(requireContext());
        checkBox.setText("导入到置顶区（排在最前面）");
        checkBox.setTextSize(15);
        checkBox.setTextColor(ThemeColorManager.getThemeColor(requireContext()));
        layout.addView(checkBox);

        new AlertDialog.Builder(requireContext())
                .setTitle("确认导入")
                .setMessage("将导入 " + importedEntries.size() + " 条密码")
                .setView(layout)
                .setPositiveButton("确定", (dialog, which) -> {
                    if (checkBox.isChecked()) {
                        performImportPinned(importedEntries);
                    } else {
                        performImportUnpinned(importedEntries);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void performImportPinned(List<PasswordEntry> importedEntries) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long maxSortOrder = 0;
        Cursor cursor = db.rawQuery("SELECT MAX(sort_order) FROM passwords", null);
        if (cursor.moveToFirst()) {
            maxSortOrder = cursor.getLong(0);
        }
        cursor.close();

        long currentSortOrder = Math.max(maxSortOrder, System.currentTimeMillis()) + 1;
        int successCount = 0;

        for (PasswordEntry entry : importedEntries) {
            entry.setSortOrder(currentSortOrder);
            currentSortOrder++;

            long newId = dbHelper.insertPassword(entry);
            if (newId != -1) {
                successCount++;
            }
        }
        db.close();

        Toast.makeText(getActivity(), "成功导入 " + successCount + " 条密码（置顶）", Toast.LENGTH_LONG).show();
        loadPasswords();
    }

    private void performImportUnpinned(List<PasswordEntry> importedEntries) {
        int successCount = 0;
        for (PasswordEntry entry : importedEntries) {
            entry.setSortOrder(0);
            long newId = dbHelper.insertPassword(entry);
            if (newId != -1) {
                successCount++;
            }
        }

        Toast.makeText(getActivity(), "成功导入 " + successCount + " 条密码（非置顶）", Toast.LENGTH_LONG).show();
        loadPasswords();
    }
}