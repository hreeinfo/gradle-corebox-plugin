package corebox.plugin.gradle.server

import corebox.plugin.gradle.server.extension.CBGServerExtension
import groovy.transform.Memoized
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.plugins.WarPluginConvention

import java.security.MessageDigest

/**
 * CBG Server 的工具类
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/21 </p>
 * <p>版权所属：xingxiuyi </p>
 */
final class CBGServers {
    private CBGServers() {
    }

    @Memoized
    static Properties getPluginProperties() {
        Properties properties = new Properties()
        properties.load(CBGServers.class.getResourceAsStream('/gradle-corebox-server-plugin.properties') as InputStream)
        properties
    }

    /**
     * 获取 war plugin 实例，不存在则抛出异常
     * @param project
     * @return
     */
    static WarPlugin projectWarPlugin(Project project) {
        return project.plugins.getPlugin(WarPlugin)
    }

    /**
     * 获取 war plugin 配置
     * @param project
     * @return
     */
    static WarPluginConvention projectWarConvention(Project project) {
        return project.convention.getPlugin(WarPluginConvention)
    }

    /**
     * 生成 exploded webapp
     * @param project
     * @param spe
     */
    static void explodeWebApps(Project project, CBGServerExtension spe) {
        if (project == null || spe == null || !spe.getExplode()) return

        def explodedWebapp = (spe.getExplodePath()) ? spe.getExplodePath() : "${project.buildDir}/${CBGServerPlugin.TASK_EXPLODE_WAR_DIR}"
        def warArchivePath = project.tasks.getByName(WarPlugin.WAR_TASK_NAME).archivePath

        FileCollection weconfig = project.configurations.getByName(CBGServerPlugin.WEBAPP_EXTENSION_NAME)

        project.copy {
            from project.zipTree(warArchivePath)
            into explodedWebapp
        }

        if (!project.file("${explodedWebapp}/WEB-INF/lib")) project.mkdir("${explodedWebapp}/WEB-INF/lib")

        if (weconfig) weconfig.each { File f ->
            project.copy {
                from f
                into "${explodedWebapp}/WEB-INF/lib"
            }
        }
    }

    /**
     * 获取 目标目录
     *
     * explode = true 时 返回 ${project.buildDir}/explodedWar
     * explode = false 时 返回 ${project.webAppDir}*
     * @param project
     * @param spe
     * @return
     */
    static File appRunWebappDir(Project project, CBGServerExtension spe) {
        if (project == null || spe == null) return null

        if (spe.getExplode()) { // explode 模式
            return project.file((spe.getExplodePath()) ? spe.getExplodePath() : "${project.buildDir}/${CBGServerPlugin.TASK_EXPLODE_WAR_DIR}")
        } else { // 直接运行模式 对应 src/main/webapp
            return projectWarConvention(project).getWebAppDir();
        }
    }

    /**
     * 获取 目标目录
     *
     * explode = true 时 返回 ${project.buildDir}/explodedWar
     * explode = false 时 返回 ${project.webAppDir}*
     * @param project
     * @param spe
     * @return
     */
    static FileCollection appRunClasspath(Project project, CBGServerExtension spe) {
        if (project == null || spe == null) return null

        if (spe.getExplode()) { // TODO explode 模式
            return null
        } else { // TODO 直接运行模式 对应 src/main/webapp
            return null
        }
    }

    static URL[] runtimeEmbedServerClasspaths(Project project) {
        if (!project) return null

        if (!project.configurations) return null

        Configuration conf = project.configurations.getByName(CBGServerPlugin.SERVER_EXTENSION_NAME)

        if (conf) return conf.asFileTree.collect { file -> file.toURI().toURL() } as URL[]
        return null
    }

    static String sha1(String str) {
        if (!str) return ""

        try {
            def messageDigest = MessageDigest.getInstance("SHA1")
            messageDigest.update(str.getBytes("UTF-8"))

            return new BigInteger(1, messageDigest.digest()).toString(16).padLeft(40, '0')
        } catch (Throwable e) {
            throw e
        }
    }

    static File runningServerLockFile(Project project) {
        if (!project) return null

        File tmpDir = new File(project.getBuildDir(), "tmp")
        if (!tmpDir.exists()) tmpDir.mkdirs()

        return new File(tmpDir, ".cbserver.lock")
    }

    static File runningServerReloadLockFile(Project project) {
        if (!project) return null

        File tmpDir = new File(project.getBuildDir(), "tmp")
        if (!tmpDir.exists()) tmpDir.mkdirs()

        return new File(tmpDir, ".cbserver.reload.lock")
    }

    static void forceDeleteServerLockFile(Project project) {
        if (!project) return
        try {
            File lockfile = runningServerLockFile(project)
            if (lockfile == null) lockfile = new File(new File(project.getBuildDir(), "tmp"), ".cbserver.lock")
            if (lockfile.exists()) lockfile.delete()
        } catch (Throwable ignored) {
        }
    }

    static void forceDeleteServerReloadLockFile(Project project) {
        if (!project) return
        try {
            File lockfile = runningServerReloadLockFile(project)
            if (lockfile == null) lockfile = new File(new File(project.getBuildDir(), "tmp"), ".cbserver.reload.lock")
            if (lockfile.exists()) lockfile.delete()
        } catch (Throwable ignored) {
        }
    }

    static boolean isHotReloadTypeSprintLoaded(String hotReloadType) {
        if (!hotReloadType) return false
        hotReloadType = hotReloadType.trim()
        return StringUtils.equalsIgnoreCase(hotReloadType, CBGServerPlugin.HOT_RELOAD_TYPE_SPTRING_LOADED) || StringUtils.equalsIgnoreCase(hotReloadType, CBGServerPlugin.HOT_RELOAD_TYPE_SPTRING)
    }

    static boolean isHotReloadTypeHotSwap(String hotReloadType) {
        if (!hotReloadType) return false
        hotReloadType = hotReloadType.trim()
        return StringUtils.equalsIgnoreCase(hotReloadType, CBGServerPlugin.HOT_RELOAD_TYPE_DCEVM) || StringUtils.equalsIgnoreCase(hotReloadType, CBGServerPlugin.HOT_RELOAD_TYPE_HOTSWAP)
    }
}
