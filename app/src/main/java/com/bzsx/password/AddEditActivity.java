package com.bzsx.password;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bzsx.password.PasswordStrengthUtil.StrengthResult;

public class AddEditActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etAccount;
    private EditText etPassword;
    private EditText etWebsite;
    private TextView tvPasswordStrength;
    private Button btnSave;

    private DatabaseHelper dbHelper;
    private int editId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiThemeManager.applyNightMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        // 应用用户自定义主题色
        applyThemeColor();

        etName = findViewById(R.id.et_name);
        etAccount = findViewById(R.id.et_account);
        etPassword = findViewById(R.id.et_password);
        etWebsite = findViewById(R.id.et_website);
        tvPasswordStrength = findViewById(R.id.tv_password_strength);
        btnSave = findViewById(R.id.btn_save);

        dbHelper = new DatabaseHelper(this);

        editId = getIntent().getIntExtra("edit_id", -1);
        if (editId != -1) {
            setTitle("编辑密码");
            loadEntry(editId);
        } else {
            setTitle("添加密码");
        }

        Button btnCancel = findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePassword();
            }
        });

        // 延迟200ms让名称输入框自动获取焦点并弹出软键盘
        etName.postDelayed(new Runnable() {
            @Override
            public void run() {
                etName.requestFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etName, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 200);

        // 实时监听密码输入变化，更新强度指示
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updatePasswordStrength(s.toString());
            }
        });
    }

    private void loadEntry(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        android.database.Cursor cursor = db.query("passwords", null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        if (cursor.moveToFirst()) {
            etName.setText(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            etAccount.setText(cursor.getString(cursor.getColumnIndexOrThrow("account")));
            try {
                int websiteIdx = cursor.getColumnIndexOrThrow("website");
                String websiteVal = cursor.getString(websiteIdx);
                if (websiteVal != null) etWebsite.setText(websiteVal);
            } catch (IllegalArgumentException ignored) {}
            String encryptedPassword = cursor.getString(cursor.getColumnIndexOrThrow("encrypted_password"));
            // 解密后显示明文
            String masterPassword = SessionManager.getInstance().getMasterPassword(this);
            if (masterPassword != null && encryptedPassword != null && !encryptedPassword.isEmpty()) {
                try {
                    String decrypted = CryptoUtil.decrypt(encryptedPassword, masterPassword);
                    etPassword.setText(decrypted);
                    updatePasswordStrength(decrypted);
                } catch (Exception e) {
                    etPassword.setText(encryptedPassword);
                    Toast.makeText(this, "密码解密失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                etPassword.setText(encryptedPassword);
            }
        }
        cursor.close();
        db.close();
    }

    /**
     * 更新密码强度显示
     */
    private void updatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            tvPasswordStrength.setVisibility(View.GONE);
            return;
        }
        StrengthResult result = PasswordStrengthUtil.evaluate(password);
        tvPasswordStrength.setText(result.label + "：" + result.message);
        tvPasswordStrength.setTextColor(result.color);
        tvPasswordStrength.setVisibility(View.VISIBLE);
    }

    private void savePassword() {
        String name = etName.getText().toString().trim();
        String account = etAccount.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "请输入名称", Toast.LENGTH_SHORT).show();
            etName.requestFocus();
            return;
        }
        if (account.isEmpty()) {
            Toast.makeText(this, "请输入账号", Toast.LENGTH_SHORT).show();
            etAccount.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            etPassword.requestFocus();
            return;
        }

        // 通过 SessionManager 获取主密码
        String masterPassword = SessionManager.getInstance().getMasterPassword(this);
        String encryptedPassword;
        if (masterPassword != null && !masterPassword.isEmpty()) {
            encryptedPassword = CryptoUtil.encrypt(password, masterPassword);
        } else {
            encryptedPassword = password;
        }

        PasswordEntry entry = new PasswordEntry(name, account, encryptedPassword);
        String websiteText = etWebsite.getText().toString().trim();
        entry.setWebsite(websiteText);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            if (editId != -1) {
                entry.setId(editId);
                dbHelper.updatePassword(entry);
                Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.insertPassword(entry);
                Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
            }
            setResult(RESULT_OK);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.close();
        }
    }

    /**
     * 应用用户自定义主题色
     */
    private void applyThemeColor() {
        int themeColor = ThemeColorManager.getThemeColor(this);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        getWindow().setStatusBarColor(themeColor);

        if (btnSave != null) {
            btnSave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(themeColor));
            btnSave.setTextColor(android.graphics.Color.WHITE);
        }
    }
}