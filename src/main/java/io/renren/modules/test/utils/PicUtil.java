package io.renren.modules.test.utils;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import javax.annotation.Resources;
import java.io.*;
import java.util.concurrent.TimeUnit;

public class PicUtil {
    public static void transferHtmlToPic(String htmlFilePath, String picPath) {
        //设置必要参数
        DesiredCapabilities dcaps = new DesiredCapabilities();
        //ssl证书支持
        dcaps.setCapability("acceptSslCerts", true);
        //截屏支持
        dcaps.setCapability("takesScreenshot", true);
        //css搜索支持
        dcaps.setCapability("cssSelectorsEnabled", true);
        //js支持
        dcaps.setJavascriptEnabled(true);
        //驱动支持（第二参数表明的是你的phantomjs引擎所在的路径）
        //dcaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
        //"E:\\phantomjs-2.1.1-windows\\bin\\phantomjs.exe");
        //创建无界面浏览器对象
        PhantomJSDriver driver = new PhantomJSDriver(dcaps);

        //设置隐性等待（作用于全局）createReport
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        driver.manage().window().maximize();
        long start = System.currentTimeMillis();
        //打开页面

        driver.get(htmlFilePath);
        JavascriptExecutor js = driver;

        File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        //利用FileUtils工具类的copyFile()方法保存getScreenshotAs()返回的文件对象
        try {
            System.out.println("图片路径" + picPath);
            FileUtils.copyFile(srcFile, new File(picPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!new File(picPath).exists()) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("耗时：" + (System.currentTimeMillis() - start) + " 毫秒");
    }
}
