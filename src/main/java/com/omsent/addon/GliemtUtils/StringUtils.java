package com.omsent.addon.GliemtUtils;


public final class StringUtils {

    private StringUtils() {
        //ignore
    }

    /**
     * 获取指定字符串后面的内容
     * @param text 原始字符串
     * @param target 要查找的字符串
     * @return 目标字符串后面的内容，如果找不到返回 null
     */
    public static String getAfter(String text, String target) {
        if (text == null || target == null) {
            return null;
        }

        int index = text.indexOf(target);
        if (index == -1) {
            return null;
        }

        return text.substring(index + target.length());
    }

    /**
     * 获取指定字符串后面的内容（带默认值）
     * @param text 原始字符串
     * @param target 要查找的字符串
     * @param defaultValue 找不到时的默认值
     * @return 目标字符串后面的内容
     */
    public static String getAfter(String text, String target, String defaultValue) {
        String result = getAfter(text, target);
        return result != null ? result : defaultValue;
    }

    /**
     * 获取第一个匹配后面的内容
     */
    public static String getAfterFirst(String text, String target) {
        return getAfter(text, target);
    }

    /**
     * 获取最后一个匹配后面的内容
     */
    public static String getAfterLast(String text, String target) {
        if (text == null || target == null) {
            return null;
        }

        int index = text.lastIndexOf(target);
        if (index == -1) {
            return null;
        }

        return text.substring(index + target.length());
    }
}
