package io.renren.modules.test.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class LogUtils {

    private static final Logger log = LoggerFactory.getLogger("LogUtils.class");

    public static String getSpecifiedLog(String startTime, String endTime, String logPath) {
        String log = "";
        try {
            System.out.println("日志执行命令为" + "sed -n '/" + startTime + ":*/,/" + endTime + ":*/p' " + logPath);

            String[] command = {"sed", "-n", "'/" + startTime + ":*/,/" + endTime + ":*/p'", logPath};
            Process ps = Runtime.getRuntime().exec(command);

            BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            log = sb.toString();
            System.out.println(log);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return log;
    }

    public static byte[] read(InputStream inStream) throws Exception{
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while( (len = inStream.read(buffer)) != -1){
            outputStream.write(buffer, 0, len);
            System.out.println("输出内容为： " + outputStream.toByteArray());
        }
        inStream.close();
        return outputStream.toByteArray();
    }

    public static String findSpecifiedContentLine(String content, String logPath) {
        String specifiedContentLine = "";
        try {
            InputStream inputStream = Runtime.getRuntime().exec("cat " + logPath + " | grep " + content)
                    .getInputStream();
            specifiedContentLine = new String(read(inputStream));
        } catch (Exception e) {
            log.error("获取指定行数失败");
        }

        String[] specifiedContentLines = specifiedContentLine.split("\n");
        return specifiedContentLines[specifiedContentLines.length -1];
    }
}
