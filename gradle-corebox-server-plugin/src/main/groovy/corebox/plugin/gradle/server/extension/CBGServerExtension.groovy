package corebox.plugin.gradle.server.extension

import corebox.plugin.gradle.server.CBGServerPlugin

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/20 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CBGServerExtension {

    String type = CBGServerPlugin.EMBED_SERVER_DEFAULT_TYPE
    String version = CBGServerPlugin.EMBED_SERVER_DEFAULT_VERSION
    Integer port = 8080
    Boolean daemon = Boolean.FALSE
    String context = "/"
    File webapp
    File workingdir
    File configfile
    String loglevel = "INFO"

    List<String> classesdirs = []
    List<String> resourcesdirs = []

    Map<String, String> options = [:]

    Boolean explode = Boolean.TRUE
    String explodePath = null

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
        if (version) this.version = version
        else this.version = CBGServerPlugin.EMBED_SERVER_DEFAULT_VERSION
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
        this.classesdirs.add(classesdir)
    }

    /**
     * 附加的类路径 （当前ClassLoader以外的类路径）
     * @param classesdir
     */
    void classesdir(File classesdir) {
        if (this.classesdirs == null) this.classesdirs = []
        this.classesdirs.add(classesdir.getAbsolutePath())
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
        this.resourcesdirs.add(resourcedir)
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
        this.resourcesdirs.add(resourcedir.getAbsolutePath())
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
        else explode = Boolean.TRUE
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
}
