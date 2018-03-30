package corebox.plugin.gradle.server.task

import corebox.plugin.gradle.server.CBGServerPlugin
import corebox.plugin.gradle.server.CBGServers
import corebox.plugin.gradle.server.CBGServerInstance
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.*

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/20 </p>
 * <p>版权所属：xingxiuyi </p>
 */
abstract class CBGServerBaseTask extends DefaultTask {
    private final ExecutorService executor = Executors.newSingleThreadExecutor()
    private Thread shutdownHook

    @Input
    String type = CBGServerPlugin.EMBED_SERVER_DEFAULT_TYPE

    @Input
    String typeClass = CBGServerPlugin.EMBED_SERVER_DEFAULT_TYPE_CLASS

    @Input
    Integer port = 8080

    @Input
    Boolean daemon = Boolean.FALSE

    @Input
    @Optional
    String context = "/"

    @InputDirectory
    @Optional
    File workingdir

    @InputFile
    @Optional
    File configfile

    @Input
    @Optional
    String loglevel = "INFO"

    @Input
    @Optional
    List<String> classesdirs = []

    @Input
    @Optional
    List<String> resourcesdirs = []

    @Input
    @Optional
    Map<String, String> options = [:]

    CBGServerBaseTask() {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    protected void startServer() {
        final CountDownLatch startupBarrier = new CountDownLatch(1)

        Future<?> future = this.executor.submit(new Runnable() {
            @Override
            void run() {
                ClassLoader originLoader = Thread.currentThread().contextClassLoader
                try {
                    ClassLoader ncl = createRuntimeClassLoader()

                    Thread.currentThread().setContextClassLoader(ncl)


                    CBGServerInstance instance = CBGServerInstance.instance

                    instance.callBuilder("port", getPort())
                    instance.callBuilder("loglevel", getLoglevel())
                    instance.callBuilder("context", getContext())

                    if (getWorkingdir()) instance.callBuilder("workingdir", getWorkingdir().getAbsolutePath())
                    if (getConfigfile()) instance.callBuilder("configfile", getConfigfile().getAbsolutePath())

                    if (getClassesdirs()) for (String c : getClassesdirs()) {
                        if (c) instance.callBuilder("classesdir", c)
                    }
                    if (getResourcesdirs()) for (String r : getResourcesdirs()) {
                        if (r) instance.callBuilder("resourcedir", r)
                    }

                    innerLifeCycleCmds(startupBarrier).each { k, v -> instance.callBuilder("listener", k, v) }

                    configWebapp(instance)

                    instance.callBuilder("options", getOptions())

                    instance.initServer(getType(), getTypeClass())

                    try {
                        logger.debug "开始启动 server..."

                        // 此时始终以后台方式运行tomcat，最终由 startupBarrier 决定是否锁定
                        if (instance) instance.start(ClassLoader.systemClassLoader.parent, true)

                        addShutdownHook(instance, startupBarrier)

                        waitRunningServerLock(instance, startupBarrier, getDaemon())
                    } catch (Exception e) {
                        stopServerRunner(instance, startupBarrier)
                        throw new GradleException("启动server发生错误", e)
                    } finally {
                        removeShutdownHook()
                    }
                } catch (Throwable e) {
                    logger.error e.getMessage()
                } finally {
                    if (originLoader) Thread.currentThread().setContextClassLoader(originLoader)
                }
            }
        })

        startupBarrier.await()
    }

    protected ClassLoader createRuntimeClassLoader(ClassLoader originLoader) {
        if (originLoader == null) originLoader = Thread.currentThread().contextClassLoader
        if (originLoader == null) originLoader = CBGServerBaseTask.class.getClassLoader()

        URL[] urls = CBGServers.runtimeEmbedServerClasspaths(this.project)
        if (urls) return new URLClassLoader(urls, originLoader)
        else return originLoader
    }

    /**
     * 实现配置的具体逻辑
     */
    protected abstract void configWebapp(CBGServerInstance instance)

    private void waitRunningServerLock(CBGServerInstance instance, CountDownLatch startupBarrier, boolean daemonMode) {
        if (daemonMode) startupBarrier.countDown()

        Project project = this.getProject()
        if (![project]) return

        final File lockFile = CBGServers.runningServerLockFile(project)
        if (lockFile == null) {
            logger.error "无法找到锁文件"
            return
        }

        if (lockFile.exists()) lockFile.delete()

        lockFile.createNewFile()

        while (lockFile.exists()) {
            try {
                Thread.sleep(2000)
            } catch (Throwable ignored) {
            }
        }

        stopServerRunner(instance, startupBarrier)
    }

    private void interruptRunningServerLock() {
        Project project = this.getProject()
        if (!project) return

        try {
            File lockFile = CBGServers.runningServerLockFile(project)
            if (lockFile == null || !lockFile.exists()) return

            lockFile.delete()
        } catch (Throwable ignored) {
        }
    }


    private void addShutdownHook(CBGServerInstance instance, final CountDownLatch startupBarrier) {
        this.shutdownHook = new Thread(new Runnable() {
            @Override
            void run() {
                try {
                    interruptRunningServerLock()
                    stopServerRunner(instance, startupBarrier)
                } catch (Throwable ignored) {
                }
            }
        })

        Runtime.getRuntime().addShutdownHook(this.shutdownHook)
    }

    private void removeShutdownHook() {
        if (this.shutdownHook) {
            Runtime.getRuntime().removeShutdownHook(this.shutdownHook)
        }
    }

    private void stopServerRunner(CBGServerInstance instance, final CountDownLatch startupBarrier) {
        try {
            if (instance) instance.stop()
        } catch (Throwable ignored) {
        }

        try {
            if (executor) executor.shutdownNow()
        } catch (Throwable ignored) {
        }


        finishLatch(startupBarrier)

        if (this.getDaemon()) System.exit(0) // TODO daemon模式下 停止服务存在问题 使用强制退出的办法
    }

    private Map<String, Runnable> innerLifeCycleCmds(final CountDownLatch startupBarrier) {
        Map<String, Runnable> map = [:]

        map.put("onStarting", new Runnable() {
            @Override
            void run() {
                logger.quiet "开始启动服务..."
                if (startupBarrier && getDaemon()) {
                    startupBarrier.countDown()
                }
            }
        })

        map.put("onStarted", new Runnable() {
            @Override
            void run() {
                logger.quiet "服务已经启动"
            }
        })

        map.put("onFailure", new Runnable() {
            @Override
            void run() {
                logger.quiet "服务启动失败!"
            }
        })

        map.put("onStopping", new Runnable() {
            @Override
            void run() {
                logger.quiet "开始停止服务..."
            }
        })

        map.put("onStopped", new Runnable() {
            @Override
            void run() {
                logger.quiet "服务已经停止"
                finishLatch(startupBarrier)
            }
        })
        return map
    }

    public static void finishLatch(final CountDownLatch startupBarrier) {
        while (startupBarrier && startupBarrier.count > 0) startupBarrier.countDown()
    }
}
