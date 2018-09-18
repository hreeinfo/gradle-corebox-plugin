package corebox.plugin.gradle.vaadin8

import corebox.plugin.gradle.common.CBGs
import corebox.plugin.gradle.vaadin8.task.CBGVaadin8BuildClassPathJar
import corebox.plugin.gradle.vaadin8.task.CBGVaadin8CompileThemeTask
import groovy.transform.Memoized
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.util.VersionNumber

import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import java.util.jar.Manifest

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/31 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CBGVaadin8s {
    private static final String VAADIN = "VAADIN"

    private static final String JAR_EXTENSION = ".jar"
    private static final String GWT_MODULE_POSTFIX = '.gwt.xml'


    private static final String DEFAULT_WIDGETSET = 'com.vaadin.DefaultWidgetSet'
    private static final String DEFAULT_LEGACY_V6_WIDGETSET = 'com.vaadin.terminal.gwt.DefaultWidgetSet'
    private static final String DEFAULT_LEGACY_V7_WIDGETSET = 'com.vaadin.v7.Vaadin7WidgetSet'

    static final String CLIENT_PACKAGE_NAME = "client"
    static final String VAADIN_CLIENT_DENDENCY = "vaadin-client"
    static final String VAADIN_SHARED_DEPENDENCY = "vaadin-shared"

    static final String VAADIN_SERVER_DEPENDENCY = "vaadin-server"

    static final String VAADIN_COMP_SERVER_DEPENDENCY = 'vaadin-compatibility-server'
    static final String VAADIN_COMP_CLIENT_DEPENDENCY = 'vaadin-compatibility-client'
    static final String VAADIN_COMP_SHARED_DEPENDENCY = 'vaadin-compatibility-shared'
    static final String VALIDATION_API_DEPENDENCY = 'validation-api-1.0'


    static final String APP_WIDGETSET = 'AppWidgetset'


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

    static enum ProjectType {
        JAVA,
        GROOVY,
        KOTLIN
    }

    @Memoized
    static Properties getPluginProperties() {
        Properties properties = new Properties()
        properties.load(CBGVaadin8s.class.getResourceAsStream('/gradle-corebox-vaadin8-plugin.properties') as InputStream)
        properties
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
     * 项目的 widgetsets 目录
     * @param project
     * @return
     */
    @Memoized
    static File getWidgetsetDirectory(Project project) {
        File webAppDir = getWebAppDirectory(project)
        File vaadinDir = new File(webAppDir, VAADIN)
        File widgetsetsDir = new File(vaadinDir, 'widgetsets')
        widgetsetsDir
    }

    /**
     * 项目的 widgetsets cache 目录
     * @param project
     * @return
     */
    @Memoized
    static File getWidgetsetCacheDirectory(Project project) {
        File webAppDir = getWebAppDirectory(project)
        File vaadinDir = new File(webAppDir, VAADIN)
        File unitCacheDir = new File(vaadinDir, 'gwt-unitCache')
        unitCacheDir
    }

    /**
     * Vaadin Theme 目录
     * @param project
     * @return
     */
    @Memoized
    static File getThemesDirectory(Project project) {
        File themesDir
        CBGVaadin8CompileThemeTask compileThemeTask = project.tasks.getByName(CBGVaadin8CompileThemeTask.TASK_NAME_VAADIN_THEME)
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

    @Memoized
    static String getClientPackage(Project project) {
        String clientPackage
        getMainSourceSet(project).srcDirs.each { File srcDir ->
            project.fileTree(srcDir).visit { FileVisitDetails details ->
                if (details.name == CLIENT_PACKAGE_NAME && details.directory) {
                    details.stopVisiting()
                    clientPackage = details.file.canonicalPath - srcDir.canonicalPath
                    project.logger.info "Found client package $clientPackage"
                }
            }
        }
        clientPackage
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
            CBGVaadin8BuildClassPathJar pathJarTask = project.tasks.getByName(CBGVaadin8BuildClassPathJar.TASK_NAME_VAADIN_BCPJ)
            if (!pathJarTask.archivePath.exists()) throw new IllegalStateException("Classpath jar has not been created in project $pathJarTask.project")
            classpath = project.files(pathJarTask.archivePath)
        } else {
            classpath = getCompileClassPath(project)
        }
        classpath
    }

    /**
     * 返回项目首选的 widgetset TODO 需另外提供方法 用于快速检测是否包含 widgetsets （避免addon和client扫描过程）
     */
    @Memoized
    static String getWidgetset(Project project) {
        // TODO 引用错误
        if (project.vaadinCompile.widgetset) return project.vaadinCompile.widgetset

        // 未设置 widgetset 时 执行查找操作
        File widgetsetFile = resolveWidgetsetFile(project)
        if (widgetsetFile) {
            def sourceDirs = project.sourceSets.main.allSource
            File rootDir = sourceDirs.srcDirs.find { File directory ->
                project.fileTree(directory.absolutePath).contains(widgetsetFile)
            }
            if (rootDir) {
                File relativePath = new File(rootDir.toURI().relativize(widgetsetFile.toURI()).toString())
                String widgetset = CBGs.convertFilePathToFQN(relativePath.path, GWT_MODULE_POSTFIX)
                project.logger.quiet "检测到项目的 widgetset=${widgetset}"
                widgetset
            }
        }
    }

    /**
     * 查找源代码中所对应的 widgetset 此操作会扫描src并确定本项目所包含的 widgetset
     */
    @Memoized
    static File resolveWidgetsetFile(Project project) {

        // 在 sources 中查找 module XML
        def sourceDirs = project.sourceSets.main.allSource
        List modules = []
        sourceDirs.srcDirs.each {
            modules.addAll(project.fileTree(it.absolutePath).include('**/*/*.gwt.xml'))
        }
        if (!modules.isEmpty()) return (File) modules.first()

        // WidgetsetFile 已经定义但未创建 创建该文件 TODO 引用错误
        String widgetset = project.vaadinCompile.widgetset

        // 如果 client side classes 已存在，则使用 client side package 查找 widgetset
        if (!widgetset) {
            String clientPackage = getClientPackage(project)
            if (clientPackage) {
                String widgetsetPath = StringUtils.removeEnd(clientPackage, File.separator + CLIENT_PACKAGE_NAME)
                if (widgetsetPath.size() > 0) widgetsetPath = CBGs.convertFilePathToFQN(widgetsetPath, '') + '.'
                widgetset = widgetsetPath + APP_WIDGETSET
            }
        }

        // 如果 addons 存在 但 widgetset 未定义 使用默认值
        if (!widgetset && findAddonsInProject(project).size() > 0) {
            widgetset = APP_WIDGETSET
        }

        // 如果依赖项目具有 widgetsets 使用默认值
        if (!widgetset && findInheritsInDependencies(project).size() > 0) {
            widgetset = APP_WIDGETSET
        }

        if (widgetset) {
            // 没有检测到，则创建
            File resourceDir = project.sourceSets.main.resources.srcDirs.first()
            File widgetsetFile = new File(resourceDir, CBGs.convertFQNToFilePath(widgetset, GWT_MODULE_POSTFIX))
            widgetsetFile.parentFile.mkdirs()
            widgetsetFile.createNewFile()
            return widgetsetFile
        }

        null
    }


    static Set<String> findInheritsInDependencies(Project project, List<Project> scannedProjects = []) {
        Set<String> inherits = []

        if (scannedProjects.size() > 0) {
            inherits.addAll(findInheritsInProject(project))
        }

        scannedProjects << project

        // 扫描子项目以整理 addon 继承关系
        def attribute = new Attributes.Name('Vaadin-Widgetsets')
        project.configurations.all.each { Configuration conf ->
            conf.allDependencies.each { Dependency dependency ->
                if (dependency in ProjectDependency) {
                    Project dependentProject = ((ProjectDependency) dependency).dependencyProject
                    if (!(dependentProject in scannedProjects)) {
                        inherits.addAll(findInheritsInDependencies(dependentProject, scannedProjects))
                    }
                } else if (isResolvable(project, conf)) {
                    conf.files(dependency).each { File file ->
                        if (file.file && file.name.endsWith('.jar')) {
                            file.withInputStream { InputStream stream ->
                                def jarStream = new JarInputStream(stream)
                                jarStream.with {
                                    def mf = jarStream.getManifest()
                                    def attributes = mf?.mainAttributes
                                    def widgetsetsValue = attributes?.getValue(attribute)
                                    if (widgetsetsValue && !dependency.name.startsWith('vaadin-client')) {
                                        List<String> widgetsets = widgetsetsValue?.split(',')?.collect { it.trim() }
                                        widgetsets?.each { String widgetset ->
                                            if (widgetset != DEFAULT_WIDGETSET
                                                    && widgetset != DEFAULT_LEGACY_V6_WIDGETSET
                                                    && widgetset != DEFAULT_LEGACY_V7_WIDGETSET) {
                                                inherits.add(widgetset)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        inherits
    }

    private static Set<String> findInheritsInProject(Project project) {
        if (!project.hasProperty('vaadin')) { // TODO 需要修正属性
            return []
        }

        Set<String> inherits = []

        def scan = { File srcDir ->
            if (srcDir.exists()) {
                project.fileTree(srcDir.absolutePath)
                        .include("**/*/*$GWT_MODULE_POSTFIX")
                        .each { File file ->
                    if (file.exists() && file.isFile()) {
                        def path = file.absolutePath.substring(srcDir.absolutePath.size() + 1)
                        def widgetset = CBGs.convertFilePathToFQN(path, GWT_MODULE_POSTFIX)
                        inherits.add(widgetset)
                    }
                }
            }
        }

        getMainSourceSet(project).srcDirs.each(scan)

        project.sourceSets.main.resources.srcDirs.each(scan)

        inherits
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

    // TODO 需要实现
    @Memoized
    static FileCollection getClientCompilerClassPath(Project project) {
        FileCollection collection = project.sourceSets.main.runtimeClasspath
        collection += project.sourceSets.main.compileClasspath

        getMainSourceSet(project).srcDirs.each {
            collection += project.files(it)
        }

        project.sourceSets.main.java.srcDirs.each { File dir ->
            collection += project.files(dir)
        }

        // 过滤所需依赖
        collection = collection.filter { File file ->
            if (file.name.endsWith(JAR_EXTENSION)) {

                // 增加 GWT compiler + deps
                if (file.name.startsWith(VAADIN_SERVER_DEPENDENCY) ||
                        file.name.startsWith(VAADIN_CLIENT_DENDENCY) ||
                        file.name.startsWith(VAADIN_SHARED_DEPENDENCY) ||
                        file.name.startsWith(VAADIN_COMP_SERVER_DEPENDENCY) ||
                        file.name.startsWith(VAADIN_COMP_CLIENT_DEPENDENCY) ||
                        file.name.startsWith(VAADIN_COMP_SHARED_DEPENDENCY) ||
                        file.name.startsWith(VALIDATION_API_DEPENDENCY)) {
                    return true
                }

                // Addons: client side widgetset
                JarFile jar = new JarFile(file.absolutePath)
                if (!jar.manifest) {
                    return false
                }

                Attributes attributes = jar.manifest.mainAttributes
                return attributes.getValue('Vaadin-Widgetsets')
            }
            true
        }

        // 直接增加额外的依赖到 classpath
        collection += project.configurations[CBGVaadin8Plugin.CONFIGURATION_CLIENT_COMPILE]

        // 确保 gwt sdk libs 顺序 TODO 引用错误
        if (project.vaadinCompile.gwtSdkFirstInClasspath) collection = moveGwtSdkFirstInClasspath(project, collection)

        collection
    }

    // TODO 引用错误
    @Memoized
    static FileCollection moveGwtSdkFirstInClasspath(Project project, FileCollection collection) {
        if (project.vaadin.manageDependencies) { // TODO 引用错误
            FileCollection gwtCompilerClasspath = project.configurations[CBGVaadin8Plugin.CONFIGURATION_CLIENT]
            return gwtCompilerClasspath + (collection - gwtCompilerClasspath)
        } else if (project.appVaadinWidgetset.getGwtSdkFirstInClasspath()) {
            project.logger.log(LogLevel.WARN, 'Cannot move GWT SDK first in classpath since plugin does not manage ' +
                    'dependencies. You can set vaadinCompile.gwtSdkFirstInClasspath=false and ' +
                    'arrange the dependencies yourself if you need to.')
        }
        collection
    }

    // TODO 引用错误
    @Memoized
    static SourceDirectorySet getMainSourceSet(Project project, boolean forceDefaultJavaSourceset = false) {
        // TODO 引用错误
        if (project.vaadin.mainSourceSet) {
            project.vaadin.mainSourceSet
        } else if (forceDefaultJavaSourceset) {
            project.sourceSets.main.java
        } else {
            switch (getProjectType(project)) {
                case ProjectType.GROOVY:
                    return project.sourceSets.main.groovy
                case ProjectType.KOTLIN:
                    return project.sourceSets.main.kotlin
                case ProjectType.JAVA:
                    return project.sourceSets.main.java
            }
        }
    }

    @Memoized
    static ProjectType getProjectType(Project project) {
        if (project.plugins.findPlugin('groovy')) {
            ProjectType.GROOVY
        } else if (project.plugins.findPlugin('org.jetbrains.kotlin.jvm')) {
            ProjectType.KOTLIN
        } else {
            ProjectType.JAVA
        }
    }
}
