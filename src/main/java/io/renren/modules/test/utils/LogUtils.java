package io.renren.modules.test.utils;

import java.io.IOException;

public class LogUtils {

    public static void getSpecifiedLog(String startTime, String endTime, String logPath, String newLogPath) {
        try {
            Runtime.getRuntime().exec("sed -n '/" + startTime + "/,/" + endTime + ":/p'" + logPath + " > " + newLogPath );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
