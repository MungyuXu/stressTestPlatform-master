package io.renren.modules.test.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

public class LogAppender {
    private FileAppender fileAppender;

    private LoggerContext lc;
    private PatternLayoutEncoder ple;

    public LogAppender(String filePath) {
        lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        ple = new PatternLayoutEncoder();
        ple.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %level [%15.15t] %-40.40logger{39} : %m%n");
        ple.setCharset(Charset.forName("utf8"));
        ple.setContext(lc);
        Filter filter = new LogFilter();

        fileAppender = new FileAppender();
        fileAppender.setEncoder(ple);
        fileAppender.setContext(lc);
        fileAppender.addFilter(filter);
        fileAppender.setFile(filePath);
        lc.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(fileAppender);
    }


    public void start() {
        ple.start();
        fileAppender.start();
    }

    public void stop() {
        fileAppender.stop();
    }
}
