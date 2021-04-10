package io.renren.modules.test.jmeter;

import io.renren.modules.test.entity.StressTestEntity;
import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.entity.StressTestReportsEntity;
import io.renren.modules.test.service.StressTestFileService;
import io.renren.modules.test.service.StressTestReportsService;
import io.renren.modules.test.service.StressTestService;
import io.renren.modules.test.utils.LogUtils;
import io.renren.modules.test.utils.PicUtil;
import io.renren.modules.test.utils.StressTestUtils;
import io.renren.modules.test.utils.WeChatUtils;
import org.apache.jmeter.engine.ClientJMeterEngine;
import org.apache.jmeter.engine.JMeterEngine;
import org.apache.jmeter.report.dashboard.GenerationException;
import org.apache.jmeter.report.dashboard.ReportGenerator;
import org.apache.jmeter.samplers.Remoteable;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 核心代价借鉴了Jmeter源码JMeter.java，没有继承覆盖是因为源码是私有子类。
 * 所以其中部分注释和基本的代码内容保留，以免造成bug。
 * 作用是创建一个listener，用来监控测试执行结束，用其来执行回调后的操作。
 * 如修改当前脚本运行的状态。
 * <p>
 * Created by zyanycall@gmail.com on 2018/10/8 14:28.
 */
public class JmeterListenToTest implements TestStateListener, Runnable, Remoteable {

    Logger log = LoggerFactory.getLogger(getClass());

    private final AtomicInteger started = new AtomicInteger(0); // keep track of remote tests

    private final List<JMeterEngine> engines;

    private final ReportGenerator reportGenerator;

    private final StressTestFileService stressTestFileService;

    private final StressTestReportsService stressTestReportsService;

    private final StressTestService stressTestService;

    private final Long fileId;

    /**
     * @param engines         List<JMeterEngine>
     * @param reportGenerator {@link ReportGenerator}
     */
    public JmeterListenToTest(List<JMeterEngine> engines, ReportGenerator reportGenerator,
                              StressTestFileService stressTestFileService, Long fileId, StressTestService stressTestService, StressTestReportsService stressTestReportsService) {
        this.engines = engines;
        this.reportGenerator = reportGenerator;
        this.stressTestFileService = stressTestFileService;
        this.fileId = fileId;
        this.stressTestReportsService = stressTestReportsService;
        this.stressTestService = stressTestService;
    }

    @Override
    // N.B. this is called by a daemon RMI thread from the remote host
    public void testEnded(String host) {
        final long now = System.currentTimeMillis();
        log.info("Finished remote host: {} ({})", host, now);
        if (started.decrementAndGet() <= 0) {
            Thread stopSoon = new Thread(this);
            // the calling thread is a daemon; this thread must not be
            // see Bug 59391
            stopSoon.setDaemon(false);
            stopSoon.start();
        }
        updateEndStatus();
        log.error("... end of run");
    }

    @Override
    public void testEnded() {
        long now = System.currentTimeMillis();
        log.error("Tidying up ...    @ " + new Date(now) + " (" + now + ")");
        try {
            generateReport();
        } catch (Exception e) {
            log.error("Error generating the report", e);
        }
        checkForRemainingThreads();
        //JmeterRunEntity jmeterRunEntity = StressTestUtils.jMeterEntity4file.get(fileId);
        updateEndStatus();
        log.error("... end of run");
    }

    @Override
    public void testStarted(String host) {
        started.incrementAndGet();
        final long now = System.currentTimeMillis();
        log.info("Started remote host:  {} ({})", host, now);
    }

    @Override
    public void testStarted() {
        JmeterRunEntity jmeterRunEntity = StressTestUtils.jMeterEntity4file.get(fileId);
        jmeterRunEntity.setTestStartTime(System.currentTimeMillis());
        if (log.isInfoEnabled()) {
            final long now = System.currentTimeMillis();
            log.info("{} ({})", JMeterUtils.getResString("running_test"), now);//$NON-NLS-1$
        }
    }

    /**
     * This is a hack to allow listeners a chance to close their files. Must
     * implement a queue for sample responses tied to the engine, and the
     * engine won't deliver testEnded signal till all sample responses have
     * been delivered. Should also improve performance of remote JMeter
     * testing.
     */
    @Override
    public void run() {
        long now = System.currentTimeMillis();
        log.error("Tidying up remote @ " + new Date(now) + " (" + now + ")");
        if (engines != null) { // it will be null unless remoteStop = true
            log.error("Exiting remote servers");
            for (JMeterEngine e : engines) {
                e.exit();
            }
        }
        try {
            TimeUnit.SECONDS.sleep(5); // Allow listeners to close files
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        ClientJMeterEngine.tidyRMI(log);
        try {
            generateReport();
        } catch (Exception e) {
            System.err.println("Error generating the report: " + e);//NOSONAR
            log.error("Error generating the report", e);
        }
        checkForRemainingThreads();
        log.error("... end of run");
    }

    /**
     * Generate report
     * 当前程序没有测试完成后直接生成测试报告的要求。
     * 所以此方法仅作为保留。
     */
    private void generateReport() {
        if (reportGenerator != null) {
            try {
                log.info("Generating Dashboard");
                reportGenerator.generate();
                log.info("Dashboard generated");
            } catch (GenerationException ex) {
                log.error("Error generating dashboard: {}", ex, ex);
            }
        }
    }

    /**
     * Runs daemon thread which waits a short while;
     * if JVM does not exit, lists remaining non-daemon threads on stdout.
     */
    private void checkForRemainingThreads() {
        // This cannot be a JMeter class variable, because properties
        // are not initialised until later.
        // 由于系统集成了Jmeter的配置文件架构，所以此处可以这么引用。
        // 未来如果引用新的配置文件，此处需要修改。
        final int pauseToCheckForRemainingThreads =
                JMeterUtils.getPropDefault("jmeter.exit.check.pause", 2000); // $NON-NLS-1$

        if (pauseToCheckForRemainingThreads > 0) {
            Thread daemon = new Thread(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(pauseToCheckForRemainingThreads); // Allow enough time for JVM to exit
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                // This is a daemon thread, which should only reach here if there are other
                // non-daemon threads still active
//                log.debug("The JVM should have exited but did not.");//NOSONAR
//                log.debug("The following non-daemon threads are still running (DestroyJavaVM is OK):");//NOSONAR
//                JOrphanUtils.displayThreads(false);
            });
            daemon.setDaemon(true);
            daemon.start();
        } else {
            log.debug("jmeter.exit.check.pause is <= 0, JMeter won't check for unterminated non-daemon threads");
        }
    }

    /**
     * 更新状态
     * 程序到这里engine已经停止结束了。
     * 分布式处理会复杂，先考虑单机
     */
    private void updateEndStatus() {
        // 延时两秒，是为了给前端监控返回完整的数据。
        // 要不然直接停止设置停止状态后，前端监控就会立即停止更新
        // 有可能丢掉一次内容数据
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            log.error("Thread.sleep meet error!", e);
            Thread.currentThread().interrupt();
        }

        JmeterRunEntity jmeterRunEntity = StressTestUtils.jMeterEntity4file.get(fileId);

        //实际上已经完全停止，则使用立即停止的方式，会打断Jmeter执行的线程

        //发送邮件通知
        sendReport(jmeterRunEntity);

        //发送企业微信通知
        String msg = "您发起的性能测试 " + jmeterRunEntity.getStressTestFile().getCaseName() + " 已完成，请前往平台查看结果";
        WeChatUtils weChatUtils = new WeChatUtils();
        weChatUtils.sendMessage(jmeterRunEntity.getStressTestFile().getAddBy(), msg);
        stressTestFileService.stopLocal(fileId, jmeterRunEntity, true);
        //生成执行日志
        jmeterRunEntity.setTestEndTime(System.currentTimeMillis());
        //generateRunLog(jmeterRunEntity);
    }

    private void sendReport(JmeterRunEntity jmeterRunEntity) {
        StressTestFileEntity stressTestFileEntity = jmeterRunEntity.getStressTestFile();
        StressTestEntity stressTestEntity = stressTestService.queryObject(stressTestFileEntity.getCaseId());
        String owner = stressTestEntity.getAddBy();
        String emailTile = stressTestEntity.getCaseName() + "性能测试结果";
        String[] receiverList = stressTestEntity.getEmailListStr().replaceAll(" ", "").split(",");

        Long reportId = jmeterRunEntity.getStressTestReports().getReportId();

        try {
            stressTestReportsService.createReport(new Long[]{reportId});
        } catch (Exception e) {
            log.error("create report failed");
            e.printStackTrace();
            return;
        }

        String reportFilePath = jmeterRunEntity.getStressTestReports().getReportName();

        StressTestUtils stressTestUtils = new StressTestUtils();
        String basePath = stressTestUtils.getCasePath()  + "/" + new File(reportFilePath).getPath().replace(".csv", "/");

        Long start = System.currentTimeMillis();
        while (!new File(basePath).exists() && System.currentTimeMillis() - start < 300000) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("基本路径为：" + basePath);
        if (!new File(basePath).exists()) {
            log.error("生成报告失败");
            return;
        }

        String picPath = basePath + System.currentTimeMillis() + ".png";
        System.out.println(basePath.replace("\\", "/") + "index.html");

        String baseFilePath = basePath.replace("\\", "/");
        if (System.getProperty("os.name").contains("Windows")) {
            baseFilePath = "file:///" + basePath.replace("\\", "/");
        }

        try {
            PicUtil.transferHtmlToPic(baseFilePath + "index.html", picPath);
            stressTestReportsService.sendMailWithPic(picPath, receiverList, emailTile, owner, "");
        } catch (Exception e) {
            log.error("html 报告转化成图片失败");
            e.printStackTrace();
        }
    }

//    private void generateRunLog(JmeterRunEntity jmeterRunEntity) {
//        String reportFilePath = jmeterRunEntity.getStressTestReports().getReportName();
//        StressTestUtils stressTestUtils = new StressTestUtils();
//        String basePath = stressTestUtils.getCasePath()  + "\\" + new File(reportFilePath).getPath().replace(".csv", "\\");
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
//        String startTime = format.format(jmeterRunEntity.getTestStartTime());
//        String endTime = format.format(jmeterRunEntity.getTestEndTime());
//        String logPath = "/home/seven.chen/pts/renren-fast.log";
//        String newLogPath = basePath.replace("\\", "/") + "run.log";
//        System.out.println("日志路径为" + newLogPath);
//        LogUtils.getSpecifiedLog(startTime, endTime, logPath, newLogPath);
//
//        StressTestReportsEntity stressTestReportsEntity = jmeterRunEntity.getStressTestReports();
//        stressTestReportsEntity.setLogPath(newLogPath);
//    }
}
