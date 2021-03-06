package corebox.plugin.gradle.server.extension

import corebox.plugin.gradle.server.CBGServerPlugin
import corebox.plugin.gradle.server.CBGServers

import java.nio.charset.Charset

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/20 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CBGServerExtension {
    String type = CBGServerPlugin.EMBED_SERVER_DEFAULT_TYPE
    String version
    Integer port = 8080
    Boolean daemon = Boolean.FALSE

    String context = "/"
    File webapp
    File workingdir
    File configfile

    List<String> classesdirs = []
    List<String> resourcesdirs = []
    List<String> jars = []
    List<String> serverClasspaths = []

    Map<String, String> options = [:]

    Map<String, String> runtimeConfigs = [:]

    Boolean explode = Boolean.TRUE
    String explodePath = null

    String loglevel = "INFO"
    Boolean logToConsole = Boolean.TRUE
    String logCharset = Charset.defaultCharset().name()

    Integer debugPort = 5005
    String debugConfig = null

    List<String> jvmArgs = []
    List<String> envs = []

    List<String> blacklistJars = []

    Boolean hot = Boolean.FALSE
    String hottype = CBGServerPlugin.HOT_RELOAD_TYPE_DEFAULT

    /**
     * 所需服务的类型 可选值： JETTY TOMCAT PAYARA
     *
     * 其中 PAYARA 仅用于调试完整 war，不支持 classesdirs 和 resourcesdirs
     * @param type
     */
    void type(String type) {
        if (type) this.type = type
        else this.type = CBGServerPlugin.EMBED_SERVER_DEFAULT_TYPE
    }

    void version(String version) {
        this.version = version
    }

    /**
     * Server监听端口号
     * @param port
     */
    void port(Integer port) {
        if (port != null && port > 0) this.port = port
    }

    /**
     * 是否以后台 daemon 方式运行 默认在前台运行
     * @param daemon
     */
    void daemon(Boolean daemon) {
        this.daemon = daemon
    }

    /**
     * hot 模式 监测 classesdir 中的类 实时动态载入变更的类内容 （需要IDE在目录中生成类或者单个项目进行rebuild）
     *
     * 如果使用 gradle 生成目标类，可以考虑开启该项目的 增量编译 模式以加快调试速度 此时需要注意的是不要构建web项目
     *
     * @param hot
     */
    void hot(Boolean hot) {
        this.hot = hot
        if (this.hot) this.hottype = CBGServerPlugin.HOT_RELOAD_TYPE_DEFAULT
        else this.hottype = null
    }

    /**
     * hot 模式 当值为有效值时 设置 hot=true 且 hottype 为给定类型
     *
     * 否则打印警告信息
     *
     * @param hottype
     */
    void hot(String hottype) {
        if (hottype) {
            if (CBGServers.isHotReloadTypeSprintLoaded(hottype) || CBGServers.isHotReloadTypeHotSwap(hottype)) {
                this.hot = true
                this.hottype = hottype
            } else {
                println "错误：您设置的 hottype 值无效，可选值为：[springloaded|hotswap]"
                this.hot = false
                this.hottype = null
            }
        } else {
            this.hot = false
            this.hottype = null
        }
    }

    /**
     * Webapp 的应用路径 ContextPath
     * @param context
     */
    void context(String context) {
        this.context = context
    }

    /**
     * 当前webapp目录，一般情况下不需要指定，按下列规则来获取
     *
     * 执行任务 appRun 时，此路径默认为 war 任务目标 的 explodedWar 目录
     *
     * 执行任务 appRunWar 时，此路径默认为 war 任务目标 的 目标 war 包
     *
     * 当指定了此属性后，忽略上述规则，直接使用给定的目标目录
     * @param webapp
     */
    void webapp(File webapp) {
        this.webapp = webapp
    }

    /**
     * 工作目录（包含Server生成的tmp等目录） 如果未设置则使用临时目录作为工作目录
     *
     * 一般情况下不需要设置此配置
     *
     * @param workingdir
     */
    void workingdir(File workingdir) {
        this.workingdir = workingdir
    }

    /**
     * 应用对应的webapp context配置文件 例如 tomcat webapp 所需的 context.xml
     *
     * 每种服务对应的配置文件有所不同，通过此参数来指定所需生效的context配置文件
     *
     * @param configfile
     */
    void configfile(File configfile) {
        this.configfile = configfile
    }

    /**
     * 日志级别
     * @param level
     */
    void loglevel(String level) {
        this.loglevel = level
    }

    /**
     * 附加的类路径 （当前ClassLoader以外的类路径）
     * @param classesdir
     */
    void classesdir(String... classesdir) {
        if (this.classesdirs == null) this.classesdirs = []
        if (classesdir) this.classesdirs.addAll(Arrays.asList(classesdir))
    }

    /**
     * 附加的类路径 （当前ClassLoader以外的类路径）
     * @param classesdir
     */
    void classesdir(File... classesdir) {
        if (this.classesdirs == null) this.classesdirs = []
        if (classesdir != null) classesdir.each { File f ->
            this.classesdirs.add(f.getAbsolutePath())
        }
    }

    /**
     * 附加的资源路径 （当前Webapp以外的资源路径）
     *
     * 例如在调试环境下，设置 project/src/main/webapp 后可以实时调试该目录下的资源
     *
     * @param resourcedir
     */
    void resourcedir(String... resourcedir) {
        if (this.resourcesdirs == null) this.resourcesdirs = []
        if (resourcedir) this.resourcesdirs.addAll(Arrays.asList(resourcedir))
    }

    /**
     * 附加的资源路径 （当前Webapp以外的资源路径）
     *
     * 例如在调试环境下，设置 project/src/main/webapp 后可以实时调试该目录下的资源
     *
     * @param resourcedir
     */
    void resourcedir(File... resourcedir) {
        if (this.resourcesdirs == null) this.resourcesdirs = []
        if (resourcedir != null) resourcedir.each { File f ->
            this.resourcesdirs.add(f.getCanonicalPath())
        }
    }

    /**
     * 附加的jar包 （当前Webapp以外的资源路径）
     *
     * 用于直接映射到 /WEB-INF/lib 下的jar文件
     *
     * @param resourcedir
     */
    void jar(String... jar) {
        if (this.jars == null) this.jars = []
        if (jar) this.jars.addAll(Arrays.asList(jar))
    }

    /**
     * 附加的jar包 （当前Webapp以外的资源路径）
     *
     * 用于直接映射到 /WEB-INF/lib 下的jar文件
     *
     * @param resourcedir
     */
    void jar(File... jar) {
        if (this.jars == null) this.jars = []
        if (jar != null) jar.each { File f ->
            this.jars.add(f.getCanonicalPath())
        }
    }

    /**
     * 服务器层级的 classpath
     * @param serverClasspath
     */
    void serverClasspath(String... serverClasspath) {
        if (this.serverClasspaths == null) this.serverClasspaths = []
        if (serverClasspath) this.serverClasspaths.addAll(Arrays.asList(serverClasspath))
    }

    /**
     * 服务器层级的 classpath
     * @param serverClasspath
     */
    void serverClasspath(File... serverClasspath) {
        if (this.serverClasspaths == null) this.serverClasspaths = []
        if (serverClasspath != null) serverClasspath.each { File f ->
            this.serverClasspaths.add(f.getAbsolutePath())
        }
    }

    /**
     * 附加给 embed server 的选项，对应 embed server 的 options 属性
     *
     * 不同server对应不同的配置，请看到具体server中的变量说明
     *
     * @param options
     */
    void options(Map<String, String> options) {
        if (this.options == null) this.options = [:]
        if (options != null) this.options.putAll(options)
    }

    /**
     * 附加给 embed server 的选项，对应 embed server 的 options 属性
     *
     * 不同server对应不同的配置，请看到具体server中的变量说明
     *
     * @param key
     * @param value
     */
    void option(String key, String value) {
        if (this.options == null) this.options = [:]
        if (key != null) this.options.put(key, value)
    }

    /**
     * 是否执行 explode webapp 操作
     *
     * 当值为true时使用默认路径 ${buildDir}/explodedWar
     * @param exp
     */
    void explode(Boolean exp) {
        if (exp != null) this.explode = exp
        else this.explode = Boolean.TRUE
    }

    /**
     * explode webapp 当值不为空时使用给定路径 当值为空时禁用 explode
     * @param explodePath
     */
    void explode(String explodePath) {
        if (explodePath != null) {
            this.explode = Boolean.TRUE
            this.explodePath = explodePath
        } else {
            this.explode = Boolean.FALSE
            this.explodePath = null
        }
    }

    /**
     * log日志是否输出到控制台
     *
     * 当值为false时，输出内容至build/log目录中
     * @param logToConsole
     */
    void logToConsole(Boolean logToConsole) {
        if (logToConsole != null) this.logToConsole = logToConsole
        else this.logToConsole = Boolean.TRUE
    }

    /**
     * 日志文件的 charset 默认为系统编码
     *
     * 针对于 windows 系统，需要根据实际情况调整此编码以便输出正确的文件
     * @param charset
     */
    void logCharset(String charset) {
        if (charset) this.logCharset = charset
        else this.logCharset = Charset.defaultCharset().name()
    }

    /**
     * 调试器端口 用于外部IDE调试器的连接
     * @param debugPort
     */
    void debugPort(Integer debugPort) {
        if (debugPort != null && debugPort > 0) this.debugPort = debugPort
    }

    /**
     * 调试器配合 此项设置后将会替换默认的调试器启动参数
     *
     * 默认值为
     *
     * <pre>
     *  -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9999
     * </pre>
     *
     * 当值以 -Xrunjdwp 开头则会以该值作为完整debug配置，否则仅增加参数
     * @param debugConfig
     */
    void debugConfig(String debugConfig) {
        this.debugConfig = debugConfig
    }

    /**
     * gradle 运行时的特殊配置选项 仅在 gradle 任务中有效
     *
     * 目前支持下列配置
     *
     * <ul>
     *     <li> version_springloaded=版本号 - 在hot模式下所使用的 springloaded agent jar版本 </li>
     *     <li> version_hotswap=版本号 - 在hot模式下所使用的 hotswap agent jar版本 </li>
     *     <li> dcevm=DCEVM_JVM_PATH - 在hot模式下所使用的 dcevm 路径 必须指向最终执行文件 </li>
     *     <li> dcevm_args=ARG_VALUE  - 在hot模式下所使用的 dcevm args 系统默认值为 -XXaltjvm=dcevm 如果不需要此参数需要强制设置为 none </li>
     *     <li> hotswap_config=CONFIG_FILE - 在hot模式下所使用的 hotswap 配置文件 </li>
     *     <li> hotswap_disable_plugins= - 在hot模式下所使用的 hotswap 配置项 </li>
     *     <li> hotswap_auto= - 在hot模式下所使用的 hotswap  配置项 </li>
     *     <li> hotswap_logger= - 在hot模式下所使用的 hotswap  配置项 </li>
     * </ul>
     * @param key
     * @param value
     */
    void runtimeConfig(String key, String value) {
        if (this.runtimeConfigs == null) this.runtimeConfigs = [:]
        if (key != null) this.runtimeConfigs.put(key, value)
    }

    /**
     * java 运行 server 时所使用的 args 参数，需符合java args定义
     *
     * 可多次调用 每次调用给定的参数均附加至 args 中
     * @param jvmArg
     */
    void jvmArg(String... jvmArg) {
        if (this.jvmArgs == null) this.jvmArgs = []
        if (jvmArg) this.jvmArgs.addAll(Arrays.asList(jvmArg))
    }

    /**
     * 设置 server 运行时对应的 环境变量
     *
     * 可多次调用 每次调用给定的变量均附加至 envs 中
     *
     * 注意 环境变量定义格式应为 key=value
     * @param env
     */
    void env(String... env) {
        if (this.envs == null) this.envs = []
        if (env) this.envs.addAll(Arrays.asList(env))
    }

    /**
     * 运行环境忽略的jar包 忽略的文件将不会出现在 classpath 中 （ 仅处理 classpath 在 WEB-INF/lib 下的文件仍然存在 ）
     * @param jarPattern
     */
    void blacklistJar(String... jarPatterns) {
        if (this.blacklistJars == null) this.blacklistJars = []
        if (jarPatterns) this.blacklistJars.addAll(Arrays.asList(jarPatterns))
    }
}
