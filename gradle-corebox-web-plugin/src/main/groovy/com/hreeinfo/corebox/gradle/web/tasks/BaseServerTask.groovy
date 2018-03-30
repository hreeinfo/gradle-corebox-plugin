package com.hreeinfo.corebox.gradle.web.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.*

import java.util.concurrent.CountDownLatch

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/14 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class BaseServerTask extends DefaultTask {
    private Thread shutdownHook


    @Input
    @Optional
    String contextPath


    @Input
    Integer httpPort = 8080


    @InputFiles
    Iterable<File> additionalRuntimeResources = []


    @InputFiles
    Iterable<File> additionalWebResources = []

    @InputFiles
    FileCollection serverClasspath

    @Input
    String URIEncoding = 'UTF-8'


    @Input
    Boolean daemon = Boolean.FALSE


    @InputFile
    @Optional
    File configFile

    @OutputFile
    @Optional
    File outputFile


    @Input
    @Optional
    Integer cacheSize = 0


    @Internal
    def server


    BaseServerTask() {
        group = WarPlugin.WEB_APP_GROUP

        outputs.upToDateWhen { false }
    }

    @TaskAction
    protected void start() {
        logger.info "配置 Server 实例 ${getProject()}"

        // TODO 实际配置运行
        this.startServerRunner()
    }

    void startServerRunner() {
        try {
            logger.debug 'Starting Tomcat Server ...'


            final CountDownLatch startupBarrier = new CountDownLatch(1)
            server.addStartUpLifecycleListener(startupBarrier, getDaemon())

            // Start server
            server.start()

            addShutdownHook()

            //Thread shutdownMonitor = new ShutdownMonitor(this.httpPort + 1, '', server, getDaemon())
            //shutdownMonitor.start()

            startupBarrier.await()

        } catch (Exception e) {
            stopServerRunner()
            throw new GradleException('An error occurred starting the Tomcat server.', e)
        } finally {
            removeShutdownHook()
        }
    }


    private void addShutdownHook() {
        shutdownHook = new Thread(new Runnable() {
            @Override
            void run() {
                stopServerRunner()
            }
        })

        Runtime.getRuntime().addShutdownHook(shutdownHook)
    }

    private void removeShutdownHook() {
        if (shutdownHook) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook)
        }
    }

    private void stopServerRunner() {
        if (server && !server.stopped) {
            server.stop()
        }
    }
}
