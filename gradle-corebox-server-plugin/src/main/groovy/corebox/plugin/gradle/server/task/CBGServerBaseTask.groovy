package corebox.plugin.gradle.server.task

import corebox.plugin.gradle.common.CBGs
import corebox.plugin.gradle.server.CBGServerPlugin
import corebox.plugin.gradle.server.CBGServers
import org.apache.commons.lang3.StringUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

import java.nio.charset.Charset

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/20 </p>
 * <p>版权所属：xingxiuyi </p>
 */
abstract class CBGServerBaseTask extends DefaultTask {
    private static final String CLASSPATH_SWITCH = "-cp"
    private static final String TEMPDIR_SWITCH = "-Djava.io.tmpdir"

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
    Boolean hotReload = Boolean.FALSE

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

    @Input
    @Optional
    Boolean logToConsole = Boolean.TRUE

    @Input
    @Optional
    String logCharset = Charset.defaultCharset().name()

    @Input
    @Optional
    List<String> jvmArgs = []

    @Input
    @Optional
    List<String> processEnvs = []

    CBGServerBaseTask() {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    protected void startServer() {
        Process process

        process = this.executeServerProcess()

        Charset lccs = null
        try {
            lccs = Charset.forName(this.getLogCharset())
        } catch (Throwable ignored) {
            def dfcs = Charset.defaultCharset()
            project.logger.error("设置的 log charset: ${this.getLogCharset()} 无效，将使用系统默认值 ${dfcs}")
        }

        if (lccs == null) lccs = Charset.defaultCharset()

        if (this.getDaemon()) {
            CBGs.logProcess(project, process, false, lccs.name(), "embed-server-${this.getType()}.log") { String line ->
                true
            }

            logger.quiet "${this.getType()} 启动完成并在后台运行 如需关闭服务请执行 appStop 命令"
        } else {
            CBGs.logProcess(project, process, this.getLogToConsole(), lccs.name(), "embed-server-${this.getType()}.log") { String line ->
                true
            }

            addShutdownHook(process)

            process.waitFor()
        }
    }


    protected Process executeServerProcess() {
        List compileProcess = [CBGs.getJavaBinary(this.project)]

        String tc = this.getTypeClass()
        if (!tc) tc = CBGServerPlugin.EMBED_SERVER_DEFAULT_TYPE_CLASS

        Set<String> alljvmargs = new LinkedHashSet<>()

        Set<String> pjargs = this.getProcessJvmArgs()

        if (pjargs) alljvmargs.addAll(pjargs)

        if (this.getHotReload()) {
            Set<String> hotargs = this.getHotReloadAgentJvmArgs()
            if (hotargs) alljvmargs.addAll(hotargs)
        }

        if (alljvmargs) compileProcess += alljvmargs

        compileProcess += ["$TEMPDIR_SWITCH=${this.temporaryDir.canonicalPath}"]
        compileProcess += [CLASSPATH_SWITCH, this.project.configurations[CBGServerPlugin.SERVER_EXTENSION_NAME].asPath]

        compileProcess += [tc]

        if (this.getPort()) compileProcess += ["--port=${this.getPort()}"]
        if (this.getContext()) compileProcess += ["--context=${this.getContext()}"]

        if (this.getWorkingdir()) compileProcess += ["--workingdir=${this.getWorkingdir()}"]
        if (this.getConfigfile()) compileProcess += ["--configfile=${this.getConfigfile()}"]
        if (this.getLoglevel()) compileProcess += ["--loglevel=${this.getLoglevel()}"]

        String pWebapp = this.getProcessWebapp()
        if (!pWebapp) pWebapp = this.getWebapp().canonicalPath
        compileProcess += ["--webapp=${pWebapp}"]

        File pLockfile = CBGServers.runningServerLockFile(project)
        if (pLockfile == null) pLockfile = new File(new File(project.getBuildDir(), "tmp"), ".cbserver.lock")

        compileProcess += ["--lockfile=${pLockfile.canonicalPath}"]


        File pReloadLockfile = CBGServers.forceDeleteServerReloadLockFile(project)
        if (pReloadLockfile == null) pReloadLockfile = new File(new File(project.getBuildDir(), "tmp"), ".cbserver.reload.lock")

        compileProcess += ["--reloadLockfile=${pReloadLockfile.canonicalPath}"]


        this.getProcessClassesdirs().each { String s ->
            compileProcess += ["--classesdir=${s}"]
        }

        this.getProcessResourcedirs().each { String s ->
            compileProcess += ["--resourcesdir=${s}"]
        }

        this.getProcessServerClasspaths().each { String s ->
            compileProcess += ["--serverClasspath=${s}"]
        }

        this.getProcessOptions().each { String o ->
            compileProcess += ["--option=${o}"]
        }


        List<String> sysenvs = []

        if (this.getProcessEnvs()) sysenvs += this.getProcessEnvs()
        List<String> dsenvs = CBGs.getSystemEnvs()
        if (dsenvs) sysenvs += dsenvs

        project.logger.info "Server 执行命令 ${compileProcess}"
        project.logger.debug "Server 环境变量 ${sysenvs}"

        return compileProcess.execute(sysenvs, this.project.buildDir)
    }

    /**
     * 强制退出实例
     * @param process
     */
    private void addShutdownHook(Process process) {
        this.shutdownHook = new Thread(new Runnable() {
            @Override
            void run() {
                try {
                    CBGServers.forceDeleteServerLockFile(project)
                    // 等待超时后销毁进程
                    for (int i = 0; i < 300; i++) {
                        if (!process.isAlive()) break

                        if (i > 290) process.destroy()
                        Thread.sleep(1000)
                    }
                } catch (Throwable ignored) {
                }
            }
        })

        Runtime.getRuntime().addShutdownHook(this.shutdownHook)
    }

    /**
     * 下级类需实现方法 获取 webapp 目录
     * @return
     */
    protected abstract String getProcessWebapp()

    /**
     * 下级类需事先方法 获取 server 环境运行的 classpath
     *
     * 说明：
     *
     * 对于 war 任务（webapp目标中已包含lib且已生成所有依赖的jar）时，不应当包含任务依赖
     *
     * 对于 webapp 任务 （webapp目标中不存在lib，需要手工将依赖的类路径以及lib包含到此项中）
     * @return
     */
    protected abstract Set<String> getProcessServerClasspaths()

    protected Set<String> getProcessJvmArgs() {
        if (!this.getJvmArgs()) return []

        Set<String> os = new LinkedHashSet<>()

        this.getJvmArgs().each { String s ->
            if (s) os.add(s)
        }

        return os
    }

    protected Set<String> getProcessClassesdirs() {
        if (!this.getClassesdirs()) return []

        Set<String> os = new LinkedHashSet<>()
        this.getClassesdirs().each { String s ->
            if (s) os.add(s)
        }

        return os
    }

    protected Set<String> getProcessResourcedirs() {
        if (!this.getResourcesdirs()) return []

        Set<String> os = new LinkedHashSet<>()
        this.getResourcesdirs().each { String s ->
            if (s) os.add(s)
        }

        return os
    }


    protected Set<String> getProcessOptions() {
        if (!this.getOptions()) return []

        Set<String> os = new LinkedHashSet<>()
        this.getOptions().each { String k, String v ->
            if (k) {
                if (v) os.add("${k}:${v}")
                else os.add("${k}:")
            }
        }

        return os
    }

    protected Set<String> getHotReloadAgentJvmArgs() {
        Set<String> os = new LinkedHashSet<>()

        File sfjar

        this.project.configurations.getByName(CBGServerPlugin.SERVER_EXTENSION_NAME).files.each { File f ->
            if (StringUtils.startsWith(f.getName(), "springloaded")) sfjar = f
        }

        if (sfjar) {
            os.add("-javaagent:${sfjar.getCanonicalPath()}")
            os.add("-noverify")
        }
        return os
    }
}
