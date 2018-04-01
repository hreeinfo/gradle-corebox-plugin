package corebox.plugin.gradle.vaadin

import corebox.plugin.gradle.vaadin.task.CBGVaadinBuildClassPathJar
import corebox.plugin.gradle.vaadin.task.CBGVaadinCompileThemeTask
import groovy.transform.Memoized
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.util.VersionNumber

import java.util.jar.Attributes
import java.util.jar.JarInputStream
import java.util.jar.Manifest

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/31 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CBGVaadins {
    private static final String VAADIN = "VAADIN"

    private static final String GRADLE_HOME = "org.gradle.java.home"
    private static final String JAVA_HOME = "java.home"
    private static final String JAVA_BIN_NAME = "java"
    private static final String JAR_EXTENSION = ".jar"

    private static final String PLUS = '+'
    private static final String SPACE = ' '
    private static final String WARNING_LOG_MARKER = '[WARN]'
    private static final String ERROR_LOG_MARKER = '[ERROR]'
    private static final String INFO_LOG_MARKER = '[INFO]'
    private static final String STREAM_CLOSED_LOG_MESSAGE = 'Stream was closed'

    static final String CLIENT_PACKAGE_NAME = "client"
    static final String VAADIN_CLIENT_DENDENCY = "vaadin-client"
    static final String VAADIN_SHARED_DEPENDENCY = "vaadin-shared"

    static final String VAADIN_SERVER_DEPENDENCY = "vaadin-server"


    static String getVaadinVersion(Project project) {
        project.cbvaadin.version ?: getPluginProperties().getProperty('vaadin.defaultVersion')
    }

    static String getServletVersion(Project project) {
        project.cbvaadin.servletVersion ?: getPluginProperties().getProperty('servlet.defaultVersion')
    }

    static String getVaadinSassVersion(Project project) {
        project.cbvaadin.vaadinSassVersion ?: getPluginProperties().getProperty('vaadin_sass.defaultVersion')
    }

    static String getJSassVersion(Project project) {
        project.cbvaadin.libsassVersion ?: getPluginProperties().getProperty('jsass.defaultVersion')
    }

    static String getPluginVersion(Project project) {
        getPluginProperties().getProperty('version')
    }

    @Memoized
    static Properties getPluginProperties() {
        Properties properties = new Properties()
        properties.load(CBGVaadins.class.getResourceAsStream('/gradle-corebox-vaadin-plugin.properties') as InputStream)
        properties
    }

    /**
     * Vaadin Theme 目录
     * @param project
     * @return
     */
    @Memoized
    static File getThemesDirectory(Project project) {
        File themesDir
        CBGVaadinCompileThemeTask compileThemeTask = project.tasks.getByName(CBGVaadinCompileThemeTask.NAME)
        if (compileThemeTask.themesDirectory) {
            String customDir = compileThemeTask.themesDirectory
            themesDir = new File(customDir)
            if (!themesDir.absolute) {
                themesDir = project.file(project.rootDir.canonicalPath + File.separator + customDir)
            }
        } else {
            File webAppDir = getWebAppDirectory(project)
            File vaadinDir = new File(webAppDir, VAADIN)
            themesDir = new File(vaadinDir, "themes")
        }
        themesDir
    }

    /**
     * 项目的 webapp 目录
     * @param project
     * @return
     */
    @Memoized
    static File getWebAppDirectory(Project project) {
        String outputDir = project.cbvaadin.outputDir
        if (outputDir) {
            project.file(outputDir)
        } else if (project.convention.findPlugin(WarPluginConvention)) {
            project.convention.getPlugin(WarPluginConvention).webAppDir
        } else {
            project.file("src/main/webapp")
        }
    }

    /**
     * 返回JavaBinary
     *
     * @return
     */
    @Memoized
    static String getJavaBinary(Project project) {
        String javaHome
        if (project.hasProperty(GRADLE_HOME)) {
            javaHome = project.properties[GRADLE_HOME]
        } else if (System.getProperty(JAVA_HOME)) {
            javaHome = System.getProperty(JAVA_HOME)
        }

        if (javaHome) {
            File javaBin = new File(javaHome, "bin")
            File java = new File(javaBin, JAVA_BIN_NAME)
            return java.canonicalPath
        }

        // Fallback to Java on PATH with a warning
        project.logger.warn("没有检测到 Java JRE 请确认 JAVA_HOME 是否设置？")
        JAVA_BIN_NAME
    }

    @Memoized
    static boolean isResolvable(Project project, Configuration configuration) {
        hasNonResolvableConfigurations(project) ? configuration.canBeResolved : true
    }

    /**
     * 是否包含 NonResolvable Configurations
     * @param project
     * @return
     */
    static boolean hasNonResolvableConfigurations(Project project) {
        VersionNumber gradleVersion = VersionNumber.parse(project.gradle.gradleVersion)
        VersionNumber gradleVersionWithUnresolvableDeps = new VersionNumber(3, 3, 0, null)
        gradleVersion >= gradleVersionWithUnresolvableDeps
    }

    @Memoized
    static FileCollection getCompileClassPath(Project project) {
        project.sourceSets.main.compileClasspath
    }

    /**
     * TODO
     * @param project
     * @return
     */
    @Memoized
    static FileCollection getCompileClassPathOrJar(Project project, boolean useClasspathJar) {
        FileCollection classpath
        if (useClasspathJar) {
            // Add dependencies using the classpath jar
            CBGVaadinBuildClassPathJar pathJarTask = project.tasks.getByName(CBGVaadinBuildClassPathJar.NAME)
            if (!pathJarTask.archivePath.exists()) throw new IllegalStateException("Classpath jar has not been created in project $pathJarTask.project")
            classpath = project.files(pathJarTask.archivePath)
        } else {
            classpath = getCompileClassPath(project)
        }
        classpath
    }

    /**
     * 针对进程做日志记录
     * @param project
     * @param process
     * @param filename
     * @param monitor
     */
    static void logProcess(Project project, Process process, boolean logToConsole, String filename, Closure monitor) {
        if (logToConsole) {
            logProcessToConsole(project, process, monitor)
        } else {
            logProcessToFile(project, process, filename, monitor)
        }
    }

    /**
     * 针对进程做日志记录 写入到文件
     * @param project
     * @param process
     * @param filename
     * @param monitor
     */
    static void logProcessToFile(
            final Project project, final Process process, final String filename, Closure monitor = {}) {
        File logDir = project.file("$project.buildDir/logs/")
        logDir.mkdirs()

        final File LOGFILE = new File(logDir, filename)
        project.logger.info("记录日志到文件 $LOGFILE")

        CBGVaadinPlugin.THREAD_POOL.submit {
            LOGFILE.withWriterAppend { out ->
                try {
                    boolean errorOccurred = false

                    process.inputStream.eachLine { output ->
                        monitor.call(output)
                        if (output.contains(WARNING_LOG_MARKER)) out.println WARNING_LOG_MARKER + SPACE + output.replace(WARNING_LOG_MARKER, '').trim()
                        else if (output.contains(ERROR_LOG_MARKER)) {
                            errorOccurred = true
                            out.println ERROR_LOG_MARKER + SPACE + output.replace(ERROR_LOG_MARKER, '').trim()
                        } else out.println INFO_LOG_MARKER + SPACE + output.trim()
                        out.flush()

                        // 错误发生时 记录所有信息到控制台
                        if (errorOccurred) project.logger.error(output.replace(ERROR_LOG_MARKER, '').trim())
                    }
                } catch (IOException e) {
                    project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
                }
            }
        }

        CBGVaadinPlugin.THREAD_POOL.submit {
            LOGFILE.withWriterAppend { out ->
                try {
                    process.errorStream.eachLine { output ->
                        monitor.call(output)
                        out.println ERROR_LOG_MARKER + SPACE + output.replace(ERROR_LOG_MARKER, '').trim()
                        out.flush()
                    }
                } catch (IOException e) {
                    project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
                }
            }
        }
    }

    /**
     * 针对进程做日志记录 写入到控制台
     * @param project
     * @param process
     * @param monitor
     */
    static void logProcessToConsole(final Project project, final Process process, Closure monitor = {}) {
        project.logger.info("记录日志到控制台")

        CBGVaadinPlugin.THREAD_POOL.submit {
            try {
                boolean errorOccurred = false
                process.inputStream.eachLine { output ->
                    monitor.call(output)
                    if (output.contains(WARNING_LOG_MARKER)) project.logger.warn(output.replace(WARNING_LOG_MARKER, '').trim())
                    else if (output.contains(ERROR_LOG_MARKER)) errorOccurred = true
                    else project.logger.info(output.trim())

                    // 错误发生时 记录所有信息到控制台
                    if (errorOccurred) project.logger.error(output.replace(ERROR_LOG_MARKER, '').trim())
                }
            } catch (IOException e) {
                project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
            }
        }

        CBGVaadinPlugin.THREAD_POOL.submit {
            try {
                process.errorStream.eachLine { String output ->
                    monitor.call(output)
                    project.logger.error(output.replace(ERROR_LOG_MARKER, '').trim())
                }
            } catch (IOException e) {
                project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
            }
        }
    }

    @Memoized
    static FileCollection getWarClasspath(Project project) {
        // 包含 project classes 和 resources
        JavaPluginConvention java = project.convention.getPlugin(JavaPluginConvention)
        SourceSet mainSourceSet = java.sourceSets.getByName('main')
        FileCollection classpath = mainSourceSet.output.classesDirs
        classpath += project.files(mainSourceSet.output.resourcesDir)

        // 包含 runtime 依赖
        classpath += project.configurations.runtime

        // 移除 provided 依赖
        if (project.configurations.findByName('providedCompile')) {
            classpath -= project.configurations.providedCompile
        }

        if (project.configurations.findByName('providedRuntime')) {
            classpath -= project.configurations.providedRuntime
        }

        // 确保没有重复
        classpath = project.files(classpath.files)

        classpath
    }

    @Memoized
    static Set findAddonsInProject(Project project,
                                   String byAttribute = 'Vaadin-Widgetsets',
                                   Boolean includeFile = false,
                                   List<Project> scannedProjects = []) {
        Set addons = []
        scannedProjects << project
        Attributes.Name attribute = new Attributes.Name(byAttribute)

        project.configurations.all.each { Configuration conf ->
            // TODO 忽略 client 包
            conf.allDependencies.each { Dependency dependency ->
                if (dependency in ProjectDependency) {
                    Project dependentProject = ((ProjectDependency) dependency).dependencyProject
                    if (!(dependentProject in scannedProjects)) {
                        addons.addAll(findAddonsInProject(dependentProject, byAttribute, includeFile, scannedProjects))
                    }
                } else if (isResolvable(project, conf)) {
                    conf.files(dependency).each { File file ->
                        if (file.file && file.name.endsWith(JAR_EXTENSION)) {
                            file.withInputStream { InputStream stream ->
                                JarInputStream jarStream = new JarInputStream(stream)
                                Manifest mf = jarStream.getManifest()
                                Attributes attributes = mf?.mainAttributes
                                if (attributes?.getValue(attribute)) {
                                    if (!dependency.name.startsWith(VAADIN_CLIENT_DENDENCY)) {
                                        if (includeFile) {
                                            addons << [
                                                    groupId   : dependency.group,
                                                    artifactId: dependency.name,
                                                    version   : getResolvedArtifactVersion(project, conf,
                                                            dependency.name, dependency.version),
                                                    file      : file
                                            ]
                                        } else {
                                            addons << [
                                                    groupId   : dependency.group,
                                                    artifactId: dependency.name,
                                                    version   : getResolvedArtifactVersion(project, conf,
                                                            dependency.name, dependency.version)
                                            ]
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        addons
    }

    @Memoized
    static String getResolvedArtifactVersion(Project project, Configuration conf, String artifactName, String defaultVersion = null) {
        String version = defaultVersion
        if (isResolvable(project, conf)) {
            version = conf.resolvedConfiguration.resolvedArtifacts.find {
                it.name == artifactName
            }?.moduleVersion?.id?.version
        } else project.logger.warn("Failed to get artifact version from non-resolvable configuration $conf")

        version
    }
}
