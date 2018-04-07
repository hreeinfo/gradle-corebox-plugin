package corebox.plugin.gradle.server.extension

import corebox.plugin.gradle.server.CBGServerPlugin

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
    Boolean hot = Boolean.FALSE
    String context = "/"
    File webapp
    File workingdir
    File configfile

    List<String> classesdirs = []
    List<String> resourcesdirs = []

    Map<String, String> options = [:]

    Map<String, String> runtimeConfigs = [:]

    Boolean explode = Boolean.TRUE
    String explodePath = null

    String loglevel = "INFO"
    Boolean logToConsole = Boolean.TRUE
    String logCharset = Charset.defaultCharset().name()

    Integer debugPort = 9999
    String debugConfig = null

    List<String> jvmArgs = []
    List<String> envs = []

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
    void classesdir(String classesdir) {
        if (this.classesdirs == null) this.classesdirs = []
        if (classesdir) this.classesdirs.add(classesdir)
    }

    /**
     * 附加的类路径 （当前ClassLoader以外的类路径）
     * @param classesdir
     */
    void classesdir(File classesdir) {
        if (this.classesdirs == null) this.classesdirs = []
        if (classesdir != null) this.classesdirs.add(classesdir.getAbsolutePath())
    }

    /**
     * 附加的资源路径 （当前Webapp以外的资源路径）
     *
     * 例如在调试环境下，设置 project/src/main/webapp 后可以实时调试该目录下的资源
     *
     * @param resourcedir
     */
    void resourcedir(String resourcedir) {
        if (this.resourcesdirs == null) this.resourcesdirs = []
        if (resourcedir) this.resourcesdirs.add(resourcedir)
    }

    /**
     * 附加的资源路径 （当前Webapp以外的资源路径）
     *
     * 例如在调试环境下，设置 project/src/main/webapp 后可以实时调试该目录下的资源
     *
     * @param resourcedir
     */
    void resourcedir(File resourcedir) {
        if (this.resourcesdirs == null) this.resourcesdirs = []
        if (resourcedir != null) this.resourcesdirs.add(resourcedir.getAbsolutePath())
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
     *     <li> version_springloaded=版本号  - 在hot模式下所使用的agent jar版本 </li>
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
    void jvmArg(String jvmArg) {
        if (this.jvmArgs == null) this.jvmArgs = []
        if (jvmArg) this.jvmArgs.add(jvmArg)
    }

    /**
     * 设置 server 运行时对应的 环境变量
     *
     * 可多次调用 每次调用给定的变量均附加至 envs 中
     *
     * 注意 环境变量定义格式应为 key=value
     * @param env
     */
    void env(String env) {
        if (this.envs == null) this.envs = []
        if (env) this.envs.add(env)
    }
}
