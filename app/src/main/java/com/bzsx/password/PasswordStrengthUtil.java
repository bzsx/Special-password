package com.bzsx.password;

/**
 * 密码强度判定工具类
 * 规则覆盖1位到100位以上，共六个等级：极弱、弱、中、强、超强、离谱强
 */
public class PasswordStrengthUtil {

    /** 六个等级对应的颜色值 */
    public static final int COLOR_EXTREME_WEAK   = 0xFFE53935; // 红色
    public static final int COLOR_WEAK           = 0xFFFF7043; // 橙色
    public static final int COLOR_MEDIUM         = 0xFFFFC107; // 黄色
    public static final int COLOR_STRONG         = 0xFF43A047; // 绿色
    public static final int COLOR_VERY_STRONG    = 0xFF1B5E20; // 深绿
    public static final int COLOR_RIDICULOUS     = 0xFF7B1FA2; // 紫色

    public static class StrengthResult {
        public final String label;      // 等级名称
        public final String message;    // 显示文本
        public final int color;         // 颜色值

        public StrengthResult(String label, String message, int color) {
            this.label = label;
            this.message = message;
            this.color = color;
        }
    }

    /**
     * 判定密码强度
     */
    public static StrengthResult evaluate(String password) {
        if (password == null) password = "";

        int len = password.length();

        // ===== 长度底线：小于6位 → 极弱 =====
        if (len < 6) {
            return new StrengthResult("极弱", "这也太短了，随便猜都能中", COLOR_EXTREME_WEAK);
        }

        // ===== 100位以上 → 离谱强 =====
        if (len > 100) {
            return new StrengthResult("离谱强", "无人能破解你这猎奇的密码", COLOR_RIDICULOUS);
        }

        // ===== 51~100位 → 任何组合都离谱强 =====
        if (len >= 51) {
            return new StrengthResult("离谱强", "无人能破解你这猎奇的密码", COLOR_RIDICULOUS);
        }

        // ===== 分析密码构成 =====
        boolean hasDigit = false;
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasSymbol = false;

        for (int i = 0; i < len; i++) {
            char c = password.charAt(i);
            if (c >= '0' && c <= '9') {
                hasDigit = true;
            } else if (c >= 'a' && c <= 'z') {
                hasLower = true;
            } else if (c >= 'A' && c <= 'Z') {
                hasUpper = true;
            } else {
                hasSymbol = true; // 非数字非字母即符号
            }
        }

        boolean hasLetter = hasLower || hasUpper;
        boolean hasCase = hasLower && hasUpper;

        // 构成类别
        int type;
        // 1=纯数字 2=纯小写 3=纯大写 4=纯符号 5=数字+字母 6=数字+符号 7=字母+符号 8=数字+字母+符号 9=大小写+数字+符号
        if (hasDigit && !hasLetter && !hasSymbol) {
            type = 1; // 纯数字
        } else if (!hasDigit && hasLower && !hasUpper && !hasSymbol) {
            type = 2; // 纯小写
        } else if (!hasDigit && !hasLower && hasUpper && !hasSymbol) {
            type = 3; // 纯大写
        } else if (!hasDigit && !hasLetter && hasSymbol) {
            type = 4; // 纯符号
        } else if (hasDigit && hasLetter && !hasSymbol) {
            type = 5; // 数字+字母
        } else if (hasDigit && !hasLetter && hasSymbol) {
            type = 6; // 数字+符号
        } else if (!hasDigit && hasLetter && hasSymbol) {
            type = 7; // 字母+符号
        } else if (hasDigit && hasLetter && hasSymbol && !hasCase) {
            type = 8; // 数字+字母+符号（无大小写）
        } else {
            type = 9; // 大小写+数字+符号
        }

        // ===== 按长度分组判定 =====
        if (len >= 6 && len <= 8)   return evaluate6_8(type);
        if (len >= 9 && len <= 12)  return evaluate9_12(type);
        if (len >= 13 && len <= 16) return evaluate13_16(type);
        if (len >= 17 && len <= 20) return evaluate17_20(type);
        if (len >= 21 && len <= 25) return evaluate21_25(type);
        if (len >= 26 && len <= 50) return evaluate26_50(type);

        // fallback（理论上不会到这里）
        return new StrengthResult("中", "密码强度中等", COLOR_MEDIUM);
    }

    // ==================== 各长度分组判定 ====================

    /**
     * 6~8位
     */
    private static StrengthResult evaluate6_8(int type) {
        switch (type) {
            case 1: case 2: case 3: case 4:
                return result("弱", "密码强度弱", COLOR_WEAK);
            case 5: case 6: case 7:
                return result("中", "密码强度中等", COLOR_MEDIUM);
            case 8: case 9:
                return result("强", "密码强度强", COLOR_STRONG);
            default:
                return result("中", "密码强度中等", COLOR_MEDIUM);
        }
    }

    /**
     * 9~12位
     */
    private static StrengthResult evaluate9_12(int type) {
        switch (type) {
            case 1: case 2: case 3: case 4:
                return result("弱", "密码强度弱", COLOR_WEAK);
            case 5: case 6: case 7:
                return result("中", "密码强度中等", COLOR_MEDIUM);
            case 8: case 9:
                return result("强", "密码强度强", COLOR_STRONG);
            default:
                return result("中", "密码强度中等", COLOR_MEDIUM);
        }
    }

    /**
     * 13~16位
     */
    private static StrengthResult evaluate13_16(int type) {
        switch (type) {
            case 1: case 2: case 3: case 4:
                return result("弱", "密码强度弱", COLOR_WEAK);
            case 5: case 6: case 7:
                return result("中", "密码强度中等", COLOR_MEDIUM);
            case 8:
                return result("强", "密码强度强", COLOR_STRONG);
            case 9:
                return result("超强", "密码强度很高", COLOR_VERY_STRONG);
            default:
                return result("中", "密码强度中等", COLOR_MEDIUM);
        }
    }

    /**
     * 17~20位
     */
    private static StrengthResult evaluate17_20(int type) {
        switch (type) {
            case 1: case 2: case 3: case 4:
                return result("中", "密码强度中等", COLOR_MEDIUM);
            case 5: case 6: case 7:
                return result("强", "密码强度强", COLOR_STRONG);
            case 8:
                return result("超强", "密码强度很高", COLOR_VERY_STRONG);
            case 9:
                return result("离谱强", "无人能破解你这猎奇的密码", COLOR_RIDICULOUS);
            default:
                return result("强", "密码强度强", COLOR_STRONG);
        }
    }

    /**
     * 21~25位
     */
    private static StrengthResult evaluate21_25(int type) {
        switch (type) {
            case 1: case 2: case 3: case 4:
                return result("强", "密码强度强", COLOR_STRONG);
            case 5: case 6: case 7:
                return result("超强", "密码强度很高", COLOR_VERY_STRONG);
            case 8: case 9:
                return result("离谱强", "无人能破解你这猎奇的密码", COLOR_RIDICULOUS);
            default:
                return result("超强", "密码强度很高", COLOR_VERY_STRONG);
        }
    }

    /**
     * 26~50位
     */
    private static StrengthResult evaluate26_50(int type) {
        switch (type) {
            case 1: case 2: case 3: case 4:
                return result("强", "密码强度强", COLOR_STRONG);
            case 5: case 6: case 7:
                return result("超强", "密码强度很高", COLOR_VERY_STRONG);
            case 8: case 9:
                return result("离谱强", "无人能破解你这猎奇的密码", COLOR_RIDICULOUS);
            default:
                return result("超强", "密码强度很高", COLOR_VERY_STRONG);
        }
    }

    private static StrengthResult result(String label, String message, int color) {
        return new StrengthResult(label, message, color);
    }
}
