package com.omsent.addon.GliemtUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

/**
 * 调试模式检测工具类
 */
public final class DebugUtils {

    private DebugUtils() {
        // 私有构造器，防止实例化
    }

    // 检测当前是否为调试模式
    public static boolean isDebugMode() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMXBean.getInputArguments();
        for (String arg : arguments) {
            if (arg.contains("-agentlib:jdwp") ||
                arg.contains("-Xdebug") ||
                arg.contains("-Xrunjdwp")) {
                return true;
            }
        }
        return false;
    }
}
