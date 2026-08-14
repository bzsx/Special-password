package com.bzsx.password;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private PasswordListFragment passwordListFragment;
    private SettingsFragment settingsFragment;
    private TextView tvToolbarTitle;

    // 标记当前展示的 Fragment 类型
    private boolean showingSettings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 应用用户自定义主题色
        applyThemeColor();

        // 设置 Toolbar
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tvToolbarTitle = findViewById(R.id.tv_toolbar_title);

        // 创建 Fragment 实例
        passwordListFragment = new PasswordListFragment();
        settingsFragment = new SettingsFragment();

        // 设置底部导航
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_passwords) {
                tvToolbarTitle.setText("神奇的密码");
                switchFragment(passwordListFragment, false);
                return true;
            } else if (itemId == R.id.nav_settings) {
                tvToolbarTitle.setText("关于");
                switchFragment(settingsFragment, true);
                return true;
            }
            return false;
        });

        // 默认显示密码列表
        if (savedInstanceState == null) {
            tvToolbarTitle.setText("神奇的密码");
            switchFragment(passwordListFragment, false);
            bottomNavigation.setSelectedItemId(R.id.nav_passwords);
        }

        // ===== 预测性返回：设置页 → 返回先切回密码列表 =====
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (showingSettings) {
                    BottomNavigationView bn = findViewById(R.id.bottom_navigation);
                    bn.setSelectedItemId(R.id.nav_passwords);
                    showingSettings = false;
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // 应用背景图片
        BackgroundUtil.applyBackground(this, findViewById(R.id.root_layout));
    }

    private void switchFragment(Fragment fragment, boolean isSettings) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
        showingSettings = isSettings;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add) {
            if (passwordListFragment != null && passwordListFragment.isAdded()) {
                passwordListFragment.onAddPassword();
            } else {
                Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
                startActivityForResult(intent, 1);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
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

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            android.content.res.ColorStateList iconTint = createColorStateList(
                    0xFF9E9E9E,
                    themeColor
            );
            bottomNav.setItemIconTintList(iconTint);
            bottomNav.setItemTextColor(iconTint);
        }

        TextView tvTitle = findViewById(R.id.tv_toolbar_title);
        if (tvTitle != null) {
            tvTitle.setTextColor(themeColor);
        }
    }

    private android.content.res.ColorStateList createColorStateList(int defaultColor, int selectedColor) {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
        };
        int[] colors = new int[] {
                selectedColor,
                defaultColor
        };
        return new android.content.res.ColorStateList(states, colors);
    }
}