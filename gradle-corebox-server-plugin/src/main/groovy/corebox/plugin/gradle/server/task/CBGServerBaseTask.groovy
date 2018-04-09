package corebox.plugin.gradle.server.task

import corebox.plugin.gradle.common.CBGs
import corebox.plugin.gradle.server.CBGServerPlugin
import corebox.plugin.gradle.server.CBGServers
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.file.FileCollection
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
    String hotReloadType

    @Input
    @Optional
    String hotReloadDCEVM

    @Input
    @Optional
    String hotReloadDCEVMArgs

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

    @Input
    @Optional
    Map<String, String> runtimeConfigs = [:]

    @Internal
    protected String innerHotReloadType

    CBGServerBaseTask() {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    protected void startServer() {
        Process process

        process = this.executeServerProcess()

        if (!process) {
            this.project.logger.error("运行应用错误")
            return
        }

        Charset lccs = null
        try {
            lccs = Charset.forName(this.getLogCharset())
        } catch (Throwable ignored) {
            def dfcs = Charset.defaultCharset()
            project.logger.error("设置的 log charset: ${this.getLogCharset()} 无效，将使用系统默认值 ${dfcs}")
        }

        if (lccs == null) lccs = Charset.defaultCharset()

        if (this.getDaemon()) {
            File dlf = CBGs.logProcess(project, process, false, lccs.name(), "embed-server-${this.getType()}.log") { String line ->
                true
            }

            this.onProcessReady()

            logger.quiet "${this.getType()} 启动完成并在后台运行 如需关闭服务请执行 appStop 命令"
            if (dlf) logger.quiet "${this.getType()} 日志记录文件 ${project.relativePath(dlf)}"
        } else {
            File dlf = CBGs.logProcess(project, process, this.getLogToConsole(), lccs.name(), "embed-server-${this.getType()}.log") { String line ->
                true
            }

            addShutdownHook(process)

            this.onProcessReady()
            if (dlf) logger.quiet "${this.getType()} 日志记录文件 ${project.relativePath(dlf)}"
            process.waitFor()
        }
    }


    protected Process executeServerProcess() {
        String javaBinary = getJavaBinary()


        JavaVersion javaVersion = getJavaVersion(javaBinary)

        boolean javaEnableDCE = this.validJavaDCEVM(javaBinary)

        List compileProcess = [javaBinary]

        String tc = this.getTypeClass()
        if (!tc) tc = CBGServerPlugin.EMBED_SERVER_DEFAULT_TYPE_CLASS

        Set<String> alljvmargs = new LinkedHashSet<>()

        Set<String> pjargs = this.getProcessJvmArgs(javaBinary, javaVersion)

        if (pjargs) alljvmargs.addAll(pjargs)

        if (this.getHotReload()) {
            Set<String> hotargs = this.getHotReloadAgentJvmArgs(javaEnableDCE)
            if (hotargs) alljvmargs.addAll(hotargs)
        }

        if (alljvmargs) compileProcess += alljvmargs

        compileProcess += ["$TEMPDIR_SWITCH=${this.temporaryDir.canonicalPath}"]

        String javaCPs = this.getCPPaths(javaEnableDCE)
        compileProcess += [CLASSPATH_SWITCH, javaCPs]

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

    protected void onProcessReady() {
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

    protected Set<String> getProcessJvmArgs(String javaBinary, JavaVersion javaVersion) {
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

    /**
     * 获取 -cp 参数值
     *
     * 仅对已启动 DCE 的扩展有效
     *
     * @param javaEnableDCE
     * @return
     */
    protected String getCPPaths(boolean javaEnableDCE) {
        FileCollection secps = this.project.configurations[CBGServerPlugin.SERVER_EXTENSION_NAME]

        if (javaEnableDCE && CBGServers.isHotReloadTypeHotSwap(this.getHotReloadType())) {
            try {
                // 此时增加临时路径的 hotswap-agent.properties
                File hapDir = this.generateHotswapConfigFile()
                if (hapDir) {
                    if (hapDir.isDirectory()) secps = this.project.files(hapDir, secps)
                    else secps = this.project.files(hapDir.parentFile, secps)
                }
            } catch (Throwable e) {
                this.project.logger.error("无法设置cp - ${e.message}", e)
            }
        }
        return secps.asPath
    }


    protected Set<String> getHotReloadAgentJvmArgs(boolean validJavaDCEVM) {
        Set<String> os = new LinkedHashSet<>()

        if (!this.getHotReload()) return os

        File sfjar = null

        this.innerHotReloadType = ""
        if (CBGServers.isHotReloadTypeHotSwap(this.getHotReloadType())) {
            if (!validJavaDCEVM) return os

            this.project.configurations.getByName(CBGServerPlugin.SERVER_EXTENSION_NAME).files.each { File f ->
                if (sfjar == null && StringUtils.startsWith(f.getName(), "hotswap")) sfjar = f
            }

            if (sfjar) {
                String da = this.getRuntimeHotReloadDCEVMArgs()
                if (da) os.add(da)

                os.add("-javaagent:${sfjar.getCanonicalPath()}=autoHotswap=true")

                this.innerHotReloadType = "HotSwap Agent (with DCEVM)"
            }
        } else if (CBGServers.isHotReloadTypeSprintLoaded(this.getHotReloadType())) {
            this.project.configurations.getByName(CBGServerPlugin.SERVER_EXTENSION_NAME).files.each { File f ->
                if (sfjar == null && StringUtils.startsWith(f.getName(), "springloaded")) sfjar = f
            }

            if (sfjar) {
                os.add("-javaagent:${sfjar.getCanonicalPath()}")
                os.add("-noverify")

                this.innerHotReloadType = "SpringLoaded"
            }
        }
        return os
    }

    /**
     * 获取 hotswap 配置文件
     * @return
     */
    protected File generateHotswapConfigFile() {
        try {
            String rhscf = this.getRuntimeConfigs().get(CBGServerPlugin.RUNTIME_CONFIG_HOTSWAP_CONFIG)

            if (rhscf) {
                File pasdir = new File(this.temporaryDir, "hotswap_config")
                if (!pasdir.exists()) pasdir.mkdirs()

                File pasfile = new File(pasdir, "hotswap-agent.properties")
                if (pasfile.exists()) pasfile.delete()

                File hscf = this.project.file(rhscf)
                if (hscf && hscf.isFile() && hscf.exists()) {
                    FileUtils.copyFile(hscf, pasfile)
                }

                return pasfile
            }
        } catch (Throwable e) {
            this.project.logger.error("无法生成 hotswap 配置文件 - ${e.message}", e)
        }

        return null
    }

    /**
     * 此处给出的 JavaBinary Home 有额外的处理
     *
     * <p>
     * 如果当前server支持 hot reload 且方式为 hotswap -> 读取  dcevm java
     * <p>
     *
     * 如果当前server支持 hot reload 且方式为 springloaded -> 读取默认java
     * <p>
     *
     * 如果不支持 hot reload -> 直接读取默认 java binary
     * <p>
     *
     * @return
     */
    protected final String getJavaBinary() {
        if (!this.getHotReload()) return CBGs.getJavaBinary(this.project)

        if (CBGServers.isHotReloadTypeHotSwap(this.getHotReloadType())) {
            String javaHome = this.getHotReloadDCEVM()

            if (!javaHome) javaHome = System.getenv(CBGServerPlugin.HOT_RELOAD_DCEVM_ENV)
            if (!javaHome && System.hasProperty(CBGServerPlugin.HOT_RELOAD_DCEVM_ENV)) javaHome = System.getProperty(CBGServerPlugin.HOT_RELOAD_DCEVM_ENV)
            if (!javaHome && project.hasProperty(CBGServerPlugin.HOT_RELOAD_DCEVM_PROP)) javaHome = project.properties[CBGServerPlugin.HOT_RELOAD_DCEVM_PROP]
            if (!javaHome && project.hasProperty(CBGServerPlugin.HOT_RELOAD_DCEVM_PROP2)) javaHome = project.properties[CBGServerPlugin.HOT_RELOAD_DCEVM_PROP2]

            if (javaHome) return javaHome
            else CBGs.getJavaBinary(this.project)
        } else if (CBGServers.isHotReloadTypeSprintLoaded(this.getHotReloadType())) {
            return CBGs.getJavaBinary(this.project)
        }

        return CBGs.getJavaBinary(this.project)
    }

    protected final JavaVersion getJavaVersion(String javaBinary) {
        JavaVersion jv = null

        if (javaBinary) { // TODO 判断java版本 需要详细验证 java 版本字符串的解析 是否准确
            List<String> vsp = CBGs.runProcess(this.project, ["${javaBinary}", "-version"], this.getLogCharset())
            vsp.each { String output ->
                if (StringUtils.startsWithIgnoreCase(output, "java version")) {
                    if (StringUtils.containsIgnoreCase(output, ' "1.8') || StringUtils.containsIgnoreCase(output, ' "8')) {
                        jv = JavaVersion.VERSION_1_8
                    } else if (StringUtils.containsIgnoreCase(output, ' "1.9') || StringUtils.containsIgnoreCase(output, ' "9')) {
                        jv = JavaVersion.VERSION_1_9
                    } else if (StringUtils.containsIgnoreCase(output, ' "1.10') || StringUtils.containsIgnoreCase(output, ' "10')) {
                        jv = JavaVersion.VERSION_1_10
                    }
                }
            }
        }

        //if (jv) println "计算得到的 java 版本号为 ${jv.toString()}"
        //else println "计算未得到 java 版本号"

        if (!jv) JavaVersion.current()
        return jv
    }

    private String getRuntimeHotReloadDCEVMArgs() {
        String da = this.getHotReloadDCEVMArgs()

        if (!da) da = System.getenv(CBGServerPlugin.HOT_RELOAD_DCEVM_ARGS_ENV)
        if (!da && System.hasProperty(CBGServerPlugin.HOT_RELOAD_DCEVM_ARGS_ENV)) da = System.getProperty(CBGServerPlugin.HOT_RELOAD_DCEVM_ARGS_ENV)
        if (!da && project.hasProperty(CBGServerPlugin.HOT_RELOAD_DCEVM_ARGS_PROP)) da = project.properties[CBGServerPlugin.HOT_RELOAD_DCEVM_ARGS_PROP]
        if (!da && project.hasProperty(CBGServerPlugin.HOT_RELOAD_DCEVM_ARGS_PROP2)) da = project.properties[CBGServerPlugin.HOT_RELOAD_DCEVM_ARGS_PROP2]
        if (!da) da = CBGServerPlugin.HOT_RELOAD_DCEVM_ARGS

        if (StringUtils.equalsIgnoreCase(da, "none") || StringUtils.equalsIgnoreCase(da, "no")) {
            project.logger.debug("忽略了 dcevm 的 altjvm 参数")
            return null
        }

        return da
    }

    /**
     * 测试 DCEVM
     * @param jvm
     * @return
     */
    private boolean validJavaDCEVM(String javaBinary) {
        if (!javaBinary) return false
        if (!this.getHotReload()) return false

        if (CBGServers.isHotReloadTypeHotSwap(this.getHotReloadType())) {
            try {
                boolean valid = false
                List cmds

                String da = this.getRuntimeHotReloadDCEVMArgs()

                if (da) cmds = ["${javaBinary}", "${da}", "-version"]
                else cmds = ["${javaBinary}", "-version"]

                List<String> vsp = CBGs.runProcess(this.project, cmds, this.getLogCharset())
                vsp.each { String output ->
                    if (StringUtils.containsIgnoreCase(output, "Dynamic Code")) {
                        valid = true
                        this.logger.info "检测到您已运行 DCEVM 打开 HotSwap 模式"
                    }
                }
                return valid
            } catch (Throwable ignored) {
            }
        }

        return false
    }
}
