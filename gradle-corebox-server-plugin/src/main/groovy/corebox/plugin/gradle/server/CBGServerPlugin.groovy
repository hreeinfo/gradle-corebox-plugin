package corebox.plugin.gradle.server

import corebox.plugin.gradle.server.extension.CBGServerExtension
import corebox.plugin.gradle.server.task.CBGServerBaseTask
import corebox.plugin.gradle.server.task.CBGServerRunTask
import corebox.plugin.gradle.server.task.CBGServerStopTask
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.SourceSetOutput

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/20 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CBGServerPlugin implements Plugin<Project> {
    static final String SERVER_EXTENSION_NAME = "cbserver"
    static final String TASK_GROUP = "corebox"

    static final String TASK_EXPLODE_WAR_NAME = "explodedWar"
    static final String TASK_EXPLODE_WAR_DIR = "explodedWar"

    static final String EMBED_SERVER_GROUP = "com.hreeinfo.commons"
    static final String EMBED_SERVER_MODULE = "commons-embed-server"
    static final String EMBED_SERVER_MODULE_JETTY = "commons-embed-server-jetty"
    static final String EMBED_SERVER_MODULE_JETTY_CLASS = "com.hreeinfo.commons.embed.server.support.EmbedJettyServer"
    static final String EMBED_SERVER_MODULE_TOMCAT = "commons-embed-server-tomcat"
    static final String EMBED_SERVER_MODULE_TOMCAT_CLASS = "com.hreeinfo.commons.embed.server.support.EmbedTomcatServer"
    static final String EMBED_SERVER_MODULE_PAYARA = "commons-embed-server-payara"
    static final String EMBED_SERVER_MODULE_PAYARA_CLASS = "com.hreeinfo.commons.embed.server.support.EmbedPayaraServer"

    static final String EMBED_SERVER_DEFAULT_VERSION = "0.1.1"

    static final String EMBED_SERVER_DEFAULT_TYPE = "JETTY"
    static final String EMBED_SERVER_DEFAULT_TYPE_CLASS = EMBED_SERVER_MODULE_JETTY_CLASS


    @Override
    void apply(Project project) {
        if (project.plugins.findPlugin(WarPlugin) == null) project.plugins.apply(WarPlugin)

        project.configurations.create(SERVER_EXTENSION_NAME).setVisible(false).setTransitive(true)
                .setDescription("用于 cbserver 运行的库依赖")


        CBGServerExtension spe = project.extensions.create(SERVER_EXTENSION_NAME, CBGServerExtension.class)

        // 配置 Server
        configServerTasks(project, spe)

        configServerDependency(project, spe)
    }

    private static void configServerTasks(Project project, CBGServerExtension spe) {
        project.tasks.create(name: TASK_EXPLODE_WAR_NAME, group: TASK_GROUP) {
            description = "解压webapp war包（含上级所有的war包） 至目标目录 \${project.buildDir}/${TASK_EXPLODE_WAR_DIR}"
            dependsOn WarPlugin.WAR_TASK_NAME

            doLast {
                CBGServers.explodeWebApps(project, spe)
            }
        }

        project.tasks.withType(CBGServerBaseTask) {
            conventionMapping.map("type") { spe.type }
            conventionMapping.map("typeClass") { findEmbedServerTypeClass(spe.type) }
            conventionMapping.map("port") { spe.port }
            conventionMapping.map("daemon") { spe.daemon }
            conventionMapping.map("context") { spe.context }
            conventionMapping.map("workingdir") { spe.workingdir }
            conventionMapping.map("configfile") { spe.configfile }
            conventionMapping.map("loglevel") { spe.loglevel }

            conventionMapping.map("logToConsole") { spe.logToConsole }
            conventionMapping.map("jvmArgs") { spe.jvmArgs }

            conventionMapping.map("classesdirs") { spe.classesdirs }
            conventionMapping.map("resourcesdirs") { spe.resourcesdirs }
            conventionMapping.map("options") { findEmbedServerTypeOptions(spe) }
        }


        project.tasks.create(name: CBGServerRunTask.NAME, group: TASK_GROUP, type: CBGServerRunTask) {
            description = "运行appserver"
            dependsOn TASK_EXPLODE_WAR_NAME


            conventionMapping.map("webapp") { CBGServers.appRunWebappDir(project, spe) }
            conventionMapping.map("webAppClasspath") {
                if (spe.explode) {
                    FileCollection runtimeClasspath = project.files()
                    return runtimeClasspath
                } else {
                    return project.tasks.getByName(WarPlugin.WAR_TASK_NAME).classpath
                }
            }
            conventionMapping.map("classesDirectories") { getMainSourceSetOutputClassesDir(project) }
        }


        project.tasks.create(name: CBGServerStopTask.NAME, group: TASK_GROUP, type: CBGServerStopTask) {
            description = "停止appserver"
        }
    }

    private static void configServerDependency(Project project, CBGServerExtension spe) {
        project.afterEvaluate { proj ->

            String embedServerType = spe.type
            if (!embedServerType) embedServerType = EMBED_SERVER_DEFAULT_TYPE

            String embedServerVersion = spe.version
            if (!embedServerVersion) {
                embedServerVersion = CBGServers.getPluginProperties().getProperty('embed_server.defaultVersion')
                if (!embedServerVersion) embedServerVersion = EMBED_SERVER_DEFAULT_VERSION
            }

            project.logger.info "启动服务器 ${embedServerType}=${embedServerVersion}"

            project.dependencies.add SERVER_EXTENSION_NAME, "${EMBED_SERVER_GROUP}:${EMBED_SERVER_MODULE}:${embedServerVersion}"
            switch (embedServerType.toUpperCase()) {
                case "JETTY":
                    project.dependencies.add SERVER_EXTENSION_NAME, "${EMBED_SERVER_GROUP}:${EMBED_SERVER_MODULE_JETTY}:${embedServerVersion}"
                    break
                case "TOMCAT":
                    project.dependencies.add SERVER_EXTENSION_NAME, "${EMBED_SERVER_GROUP}:${EMBED_SERVER_MODULE_TOMCAT}:${embedServerVersion}"
                    break
                case "PAYARA":
                    project.dependencies.add SERVER_EXTENSION_NAME, "${EMBED_SERVER_GROUP}:${EMBED_SERVER_MODULE_PAYARA}:${embedServerVersion}"
                    break
                default:
                    project.logger.error "指定的类型 ${spe.type} 无效"
                    break
            }

        }

        project.beforeEvaluate { proj ->
            // TODO 增加依赖解析机制
        }
    }

    @SuppressWarnings("GrDeprecatedAPIUsage")
    private static FileCollection getMainSourceSetOutputClassesDir(Project project) {
        if (!project) return null
        SourceSetOutput output = project.sourceSets.main.output
        if (!output) return null
        String version = project.gradle.gradleVersion
        String majorVersion = StringUtils.substringBefore(version, '.')

        int mver = 0
        try {
            mver = Integer.parseInt(majorVersion)
        } catch (Throwable ignored) {
        }
        if (mver > 0 && mver < 4) {
            return project.files(output.classesDir)
        } else {
            return output.classesDirs.any { it.exists() } ? output.classesDirs : null
        }
    }

    private static String findEmbedServerTypeClass(String type) {
        if (!type) EMBED_SERVER_DEFAULT_TYPE_CLASS

        switch (type.toUpperCase()) {
            case "JETTY":
                return EMBED_SERVER_MODULE_JETTY_CLASS
                break
            case "TOMCAT":
                return EMBED_SERVER_MODULE_TOMCAT_CLASS
                break
            case "PAYARA":
                return EMBED_SERVER_MODULE_PAYARA_CLASS
                break
            default:
                break
        }

        return EMBED_SERVER_DEFAULT_TYPE_CLASS
    }

    private static Map<String, String> findEmbedServerTypeOptions(CBGServerExtension spe) {
        if (!spe) return [:]

        String type = spe.type
        if (!type) return [:]

        Map<String, String> etoptions = [:]
        if (spe.options) etoptions.putAll(spe.options)


        switch (type.toUpperCase()) {
            case "JETTY":
                // TODO 增加专用参数
                break
            case "TOMCAT":
                // TODO 增加专用参数
                break
            case "PAYARA":
                // TODO 增加专用参数
                break
            default:
                break
        }

        return etoptions
    }
}
