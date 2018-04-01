package corebox.plugin.gradle.vaadin

import corebox.plugin.gradle.vaadin.extension.CBGVaadinExtension
import corebox.plugin.gradle.vaadin.task.CBGVaadinBuildClassPathJar
import corebox.plugin.gradle.vaadin.task.CBGVaadinCompileThemeTask
import corebox.plugin.gradle.vaadin.task.CBGVaadinCompressCssTask
import corebox.plugin.gradle.vaadin.task.CBGVaadinUpdateAddonStylesTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.bundling.War

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/20 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CBGVaadinPlugin implements Plugin<Project> {
    static final String TASK_GROUP = "corebox"
    static final String EXTENSION_NAME = "cbvaadin"

    static final String CONFIGURATION_THEME = "cbvaadin-theme-compiler"
    static final String CONFIGURATION_SERVER = "cbvaadin-server"

    static final Executor THREAD_POOL = Executors.newCachedThreadPool({ Runnable r ->
        new Thread(r, "gradle-corebox-vaadin-plugin-thread-${THREAD_COUNTER.getAndIncrement()}")
    })


    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1)

    @Override
    void apply(Project project) {
        project.buildDir.mkdirs()

        if (!project.plugins.findPlugin(WarPlugin)) project.plugins.apply(WarPlugin)
        War war = project.tasks.findByName(WarPlugin.WAR_TASK_NAME)
        if (war) war.dependsOn CBGVaadinCompileThemeTask.NAME

        CBGVaadinExtension vde = project.extensions.create(EXTENSION_NAME, CBGVaadinExtension)

        configVaadinRepositories(project, vde)
        configVaadinDependencies(project, vde)

        configVaadinTasks(project, vde)
    }

    /**
     * 增加源，附加 vaadin 的 addons
     * @param project
     * @param vde
     */
    private static configVaadinRepositories(Project project, CBGVaadinExtension vde) {
        project.afterEvaluate { Project p ->
            if (vde.enable && vde.manageRepositories) {
                RepositoryHandler repositories = p.repositories
                repositories.maven { repository ->
                    repository.name = "Vaadin addons"
                    repository.url = "http://maven.vaadin.com/vaadin-addons"
                }
            }
        }
    }

    private static configVaadinDependencies(Project project, CBGVaadinExtension vde) {
        ConfigurationContainer configurations = project.configurations
        DependencyHandler projectDependencies = project.dependencies
        def sources = project.sourceSets.main
        def testSources = project.sourceSets.test

        configurations.create(CONFIGURATION_SERVER) { conf ->
            conf.description = 'Vaadin Server 运行所需依赖'
            conf.defaultDependencies { dependencies ->
                if (vde.enable) {
                    String vaadinVersion = CBGVaadins.getVaadinVersion(project)

                    if (vaadinVersion) {
                        Dependency vaadinServer = projectDependencies.create("com.vaadin:vaadin-server:${vaadinVersion}")
                        dependencies.add(vaadinServer)

                        Dependency vaadinThemes = projectDependencies.create("com.vaadin:vaadin-themes:${vaadinVersion}")
                        dependencies.add(vaadinThemes)

                        Dependency widgetsetCompiled = projectDependencies.create("com.vaadin:vaadin-client-compiled:${vaadinVersion}")
                        dependencies.add(widgetsetCompiled)
                    }

                    String servletAPIVersion = CBGVaadins.getServletVersion(project)
                    if (servletAPIVersion) {
                        Dependency servletAPI = projectDependencies.create("javax.servlet:javax.servlet-api:${servletAPIVersion}")
                        dependencies.add(servletAPI)
                    }
                }
            }

            sources.compileClasspath += conf
            testSources.compileClasspath += conf
        }

        configurations.create(CONFIGURATION_THEME) { conf ->
            conf.description = 'Vaadin SASS theme 编译所需依赖'
            conf.defaultDependencies { dependencies ->
                if (vde.enable) {
                    String themeCompilerName = vde.themeCompiler
                    if (!themeCompilerName) themeCompilerName = CBGVaadinCompileThemeTask.VAADIN_COMPILER
                    switch (themeCompilerName) {
                        case CBGVaadinCompileThemeTask.VAADIN_COMPILER:
                            // 当前的 vaadin-server 已经包含了所需的 scss编译器 使用其提供的编译器而不是直接使用 vaadin-sass-compiler
                            //String vaadinSassVersion = CBGVaadins.getVaadinSassVersion(project)
                            //if (!vaadinSassVersion) vaadinSassVersion = "+"
                            //Dependency themeCompiler = projectDependencies.create("com.vaadin:vaadin-sass-compiler:${vaadinSassVersion}")
                            //dependencies.add(themeCompiler)

                            String vaadinVersion = CBGVaadins.getVaadinVersion(project)
                            Dependency vaadinServer = projectDependencies.create("com.vaadin:vaadin-server:${vaadinVersion}")
                            dependencies.add(vaadinServer)
                            break
                        case CBGVaadinCompileThemeTask.LIBSASS_COMPILER:
                            String jsassVersion = CBGVaadins.getJSassVersion(project)
                            if (!jsassVersion) jsassVersion = "+"
                            Dependency libsass = projectDependencies.create("io.bit3:jsass:${jsassVersion}")
                            dependencies.add(libsass)
                            Dependency libsassPlugin = projectDependencies.create("com.hreeinfo.plugins:gradle-corebox-vaadin-plugin:${CBGVaadins.getPluginVersion(project)}")
                            dependencies.add(libsassPlugin)
                            break
                        default:
                            throw new GradleException("设置的 Theme 编译器 \"${themeCompilerName}\" 无效")
                    }
                }
            }

            sources.compileClasspath += conf
            testSources.compileClasspath += conf
        }
    }

    private static configVaadinTasks(Project project, CBGVaadinExtension vde) {
        project.tasks.create(name: CBGVaadinBuildClassPathJar.NAME, group: TASK_GROUP, type: CBGVaadinBuildClassPathJar) {
            description = "执行 Vaadin 工具任务 Build ClassPath Jar"

            classifier = 'classpath'
            dependsOn 'classes'

            onlyIf {
                vde.enable && vde.useClasspathJar
            }

            conventionMapping.map("enableTask") { vde.enable }
            conventionMapping.map("useClasspathJar") { vde.useClasspathJar }
        }

        project.tasks.create(name: CBGVaadinCompressCssTask.NAME, group: TASK_GROUP, type: CBGVaadinCompressCssTask) {
            description = "执行 Vaadin Theme CSS 压缩"
            dependsOn CBGVaadinCompileThemeTask.NAME

            onlyIf = {
                vde.enable && vde.themeCompress
            }
            conventionMapping.map("enableTask") { vde.enable }
        }

        project.tasks.create(name: CBGVaadinUpdateAddonStylesTask.NAME, group: TASK_GROUP, type: CBGVaadinUpdateAddonStylesTask) {
            description = "执行 Vaadin Addon Theme Style 更新"
            dependsOn 'classes', CBGVaadinBuildClassPathJar.NAME
            onlyIf = {
                vde.enable
            }
            conventionMapping.map("enableTask") { vde.enable }
            conventionMapping.map("useClasspathJar") { vde.useClasspathJar }
            conventionMapping.map("logToConsole") { vde.logToConsole }
        }

        project.tasks.create(name: CBGVaadinCompileThemeTask.NAME, group: TASK_GROUP, type: CBGVaadinCompileThemeTask) {
            description = "执行 Vaadin Theme 编译过程，将 scss 编译为 css"
            dependsOn('classes', CBGVaadinBuildClassPathJar.NAME, CBGVaadinUpdateAddonStylesTask.NAME)

            onlyIf {
                vde.enable
            }

            conventionMapping.map("enableTask") { vde.enable }
            conventionMapping.map("compiler") { vde.themeCompiler }
            conventionMapping.map("compress") { vde.themeCompress }
            conventionMapping.map("useClasspathJar") { vde.useClasspathJar }
            conventionMapping.map("logToConsole") { vde.logToConsole }
            conventionMapping.map("jvmArgs") { vde.themeJvmArgs }
        }
    }
}
