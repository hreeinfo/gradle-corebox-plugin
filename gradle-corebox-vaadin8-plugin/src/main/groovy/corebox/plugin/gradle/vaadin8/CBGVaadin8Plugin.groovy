package corebox.plugin.gradle.vaadin8


import corebox.plugin.gradle.vaadin8.extension.CBGVaadin8Extension
import corebox.plugin.gradle.vaadin8.task.CBGVaadin8BuildClassPathJar
import corebox.plugin.gradle.vaadin8.task.CBGVaadin8CompileThemeTask
import corebox.plugin.gradle.vaadin8.task.CBGVaadin8CompressCssTask
import corebox.plugin.gradle.vaadin8.task.CBGVaadin8UpdateAddonStylesTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.bundling.War

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/20 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CBGVaadin8Plugin implements Plugin<Project> {
    static final String TASK_GROUP = "corebox"
    static final String EXTENSION_NAME = "cbvaadin8"

    static final String CONFIGURATION_THEME = "cbvaadin-theme-compiler"
    static final String CONFIGURATION_SERVER = "cbvaadin-server"
    static final String CONFIGURATION_CLIENT = 'cbvaadin-client'
    static final String CONFIGURATION_CLIENT_COMPILE = 'cbvaadin-client-compile'
    // TODO 此处对应 vaadinCompile 需确定是否需要此名称与Task一致


    @Override
    void apply(Project project) {
        project.buildDir.mkdirs()

        if (!project.plugins.findPlugin(WarPlugin)) project.plugins.apply(WarPlugin)

        CBGVaadin8Extension vde = project.extensions.create(EXTENSION_NAME, CBGVaadin8Extension)

        configVaadinWarTasks(project, vde)

        configVaadinRepositories(project, vde)
        configVaadinDependencies(project, vde)

        configVaadinTasks(project, vde)
    }

    private static configVaadinWarTasks(Project project, CBGVaadin8Extension vde) {
        project.afterEvaluate { Project p ->
            if (vde.enable) {
                War war = project.tasks.findByName(WarPlugin.WAR_TASK_NAME)
                CBGVaadin8CompileThemeTask themeTask = project.tasks.findByName(CBGVaadin8CompileThemeTask.TASK_NAME_VAADIN_THEME)
                if (war && themeTask) {
                    war.dependsOn themeTask
                    // TODO 增加打包的逻辑
                }
            }
        }
    }

    /**
     * 增加源，附加 vaadin 的 addons
     * @param project
     * @param vde
     */
    private static configVaadinRepositories(Project project, CBGVaadin8Extension vde) {
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

    private static configVaadinDependencies(Project project, CBGVaadin8Extension vde) {
        ConfigurationContainer configurations = project.configurations
        DependencyHandler projectDependencies = project.dependencies
        def sources = project.sourceSets.main
        def testSources = project.sourceSets.test

        configurations.create(CONFIGURATION_SERVER) { conf ->
            conf.description = 'Vaadin Server 运行所需依赖'
            conf.defaultDependencies { dependencies ->
                if (vde.enable) {
                    String vaadinVersion = CBGVaadin8s.getVaadinVersion(project)

                    if (vaadinVersion) {
                        Dependency vaadinServer = projectDependencies.create("com.vaadin:vaadin-server:${vaadinVersion}")
                        dependencies.add(vaadinServer)

                        Dependency vaadinThemes = projectDependencies.create("com.vaadin:vaadin-themes:${vaadinVersion}")
                        dependencies.add(vaadinThemes)

                        Dependency widgetsetCompiled = projectDependencies.create("com.vaadin:vaadin-client-compiled:${vaadinVersion}")
                        dependencies.add(widgetsetCompiled)
                    }

                    String servletAPIVersion = CBGVaadin8s.getServletVersion(project)
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
                    if (!themeCompilerName) themeCompilerName = CBGVaadin8CompileThemeTask.VAADIN_COMPILER
                    switch (themeCompilerName) {
                        case CBGVaadin8CompileThemeTask.VAADIN_COMPILER:
                            // 当前的 vaadin-server 已经包含了所需的 scss编译器 使用其提供的编译器而不是直接使用 vaadin-sass-compiler
                            //String vaadinSassVersion = CBGVaadin8s.getVaadinSassVersion(project)
                            //if (!vaadinSassVersion) vaadinSassVersion = "+"
                            //Dependency themeCompiler = projectDependencies.create("com.vaadin:vaadin-sass-compiler:${vaadinSassVersion}")
                            //dependencies.add(themeCompiler)

                            String vaadinVersion = CBGVaadin8s.getVaadinVersion(project)
                            Dependency vaadinServer = projectDependencies.create("com.vaadin:vaadin-server:${vaadinVersion}")
                            dependencies.add(vaadinServer)
                            break
                        case CBGVaadin8CompileThemeTask.LIBSASS_COMPILER:
                            String jsassVersion = CBGVaadin8s.getJSassVersion(project)
                            if (!jsassVersion) jsassVersion = "+"
                            Dependency libsass = projectDependencies.create("io.bit3:jsass:${jsassVersion}")
                            dependencies.add(libsass)
                            Dependency libsassPlugin = projectDependencies.create("com.hreeinfo.plugins:gradle-corebox-vaadin-plugin:${CBGVaadin8s.getPluginVersion(project)}")
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

    private static configVaadinTasks(Project project, CBGVaadin8Extension vde) {
        project.tasks.create(name: CBGVaadin8BuildClassPathJar.TASK_NAME_VAADIN_BCPJ, group: TASK_GROUP, type: CBGVaadin8BuildClassPathJar) {
            description = "执行 Vaadin 工具任务 Build ClassPath Jar"

            classifier = 'classpath'
            dependsOn 'classes'

            onlyIf {
                vde.enable && vde.useClasspathJar
            }

            conventionMapping.map("enableTask") { vde.enable }
            conventionMapping.map("useClasspathJar") { vde.useClasspathJar }
        }

        project.tasks.create(name: CBGVaadin8CompressCssTask.TASK_NAME_VAADIN_CSS, group: TASK_GROUP, type: CBGVaadin8CompressCssTask) {
            description = "执行 Vaadin Theme CSS 压缩"
            dependsOn CBGVaadin8CompileThemeTask.TASK_NAME_VAADIN_THEME

            onlyIf = {
                vde.enable && vde.themeCompress
            }
            conventionMapping.map("enableTask") { vde.enable }
        }

        project.tasks.create(name: CBGVaadin8UpdateAddonStylesTask.TASK_NAME_VAADIN_ADDON_STYLE, group: TASK_GROUP, type: CBGVaadin8UpdateAddonStylesTask) {
            description = "执行 Vaadin Addon Theme Style 更新"
            dependsOn 'classes', CBGVaadin8BuildClassPathJar.TASK_NAME_VAADIN_BCPJ
            onlyIf = {
                vde.enable
            }
            conventionMapping.map("enableTask") { vde.enable }
            conventionMapping.map("useClasspathJar") { vde.useClasspathJar }
            conventionMapping.map("logToConsole") { vde.logToConsole }
        }

        project.tasks.create(name: CBGVaadin8CompileThemeTask.TASK_NAME_VAADIN_THEME, group: TASK_GROUP, type: CBGVaadin8CompileThemeTask) {
            description = "执行 Vaadin Theme 编译过程，将 scss 编译为 css"
            dependsOn('classes', CBGVaadin8BuildClassPathJar.TASK_NAME_VAADIN_BCPJ, CBGVaadin8UpdateAddonStylesTask.TASK_NAME_VAADIN_ADDON_STYLE)

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
