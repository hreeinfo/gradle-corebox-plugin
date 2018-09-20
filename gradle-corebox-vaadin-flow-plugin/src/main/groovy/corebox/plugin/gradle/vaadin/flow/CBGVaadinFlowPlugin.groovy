package corebox.plugin.gradle.vaadin.flow

import corebox.plugin.gradle.common.CBGs
import corebox.plugin.gradle.vaadin.flow.task.CopyProductionFilesTask
import corebox.plugin.gradle.vaadin.flow.task.PackageForProductionTask
import groovy.transform.Memoized
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.bundling.War

/**
 *
 */
class CBGVaadinFlowPlugin implements Plugin<Project> {
    static final String SERVER_EXTENSION_NAME = "cbvaadin"
    static final String WEBAPP_EXTENSION_NAME = 'cbweb'
    static final String TASK_GROUP = "corebox"

    static String FRONTEND_WORKING_DIRECTORY = "src/main/webapp/frontend"
    static String DEFAULT_VERSION_PRODUCTION_MODE = "1.0.5"
    static String DEFAULT_VERSION_NODEJS = "v8.11.1"
    static String DEFAULT_VERSION_YARN = "v1.6.0"

    @Override
    void apply(Project project) {
        CBGVaadinFlowExtension vfe = project.extensions.findByType(CBGVaadinFlowExtension.class)
        if (vfe == null) vfe = project.extensions.create(SERVER_EXTENSION_NAME, CBGVaadinFlowExtension.class)

        vfe.productionMode(CBGs.enableByProps(project, "product", "production", "productionMode"))

        project.configurations.create(SERVER_EXTENSION_NAME).setVisible(false).setTransitive(true)
                .setDescription("用于 cbvaadin 额外 frontend 的库依赖 仅在 productionMode 模式时可见")

        project.afterEvaluate { Project p ->
            if (vfe.enable && vfe.productionMode) {
                PackageForProductionTask vpt = project.tasks.findByName(PackageForProductionTask.TASK_NAME)
                War war = project.tasks.findByName(WarPlugin.WAR_TASK_NAME)
                if (war && vpt) {
                    war.dependsOn vpt
                    war.from(vpt.getTranspileOutputDirectory())
                }

                String pmVersion = getProdModeVersion(vfe)
                if (pmVersion) {
                    project.dependencies.add("compile", "com.vaadin:flow-server-production-mode:${pmVersion}")
                }
            }
        }

        project.tasks.create(name: CopyProductionFilesTask.TASK_NAME, group: TASK_GROUP, type: CopyProductionFilesTask) {
            description = "Vaadin 产品模式 复制frontend源文件 任务"
            dependsOn 'classes'

            onlyIf {
                vfe.enable && vfe.productionMode
            }

            conventionMapping.map("copyOutputDirectory") {
                (vfe.copyOutputDirectory == null) ? getFrontendBuildDirectory(project) : vfe.copyOutputDirectory
            }
            conventionMapping.map("frontendWorkingDirectory") {
                (vfe.frontendWorkingDirectory == null) ? getFrontendWorkingDirectory(project) : vfe.frontendWorkingDirectory
            }
            conventionMapping.map("excludes") {
                (vfe.excludes == null || vfe.excludes.isEmpty()) ? CopyProductionFilesTask.FRONTEND_EXCLUDES : vfe.excludes
            }
        }


        project.tasks.create(name: PackageForProductionTask.TASK_NAME, group: TASK_GROUP, type: PackageForProductionTask) {
            description = "Vaadin 产品模式 生成发行文件 任务"
            dependsOn CopyProductionFilesTask.TASK_NAME

            onlyIf {
                vfe.enable && vfe.productionMode
            }

            conventionMapping.map("transpileEs6SourceDirectory") {
                (vfe.copyOutputDirectory == null) ? getFrontendBuildDirectory(project) : vfe.copyOutputDirectory
            }
            conventionMapping.map("transpileWorkingDirectory") {
                (vfe.transpileWorkingDirectory == null) ? new File(project.buildDir, "vaadin-flow") : vfe.transpileWorkingDirectory
            }
            conventionMapping.map("transpileOutputDirectory") {
                File vdir = vfe.transpileOutputDirectory
                if (vdir == null) {
                    War war = project.tasks.findByName(WarPlugin.WAR_TASK_NAME)
                    if (war) {
                        vdir = new File(project.buildDir, "vaadin-flow/transpileOutput")
                    } else if (project.getArtifactMap().containsKey("com.vaadin:vaadin-spring-boot-starter")) {
                        vdir = new File(project.buildDir, "META-INF/resources")
                    }
                }

                if (!vdir.exists()) vdir.mkdirs()

                vdir
            }
            conventionMapping.map("es6OutputDirectoryName") {
                (vfe.es6OutputDirectoryName == null || vfe.es6OutputDirectoryName.isEmpty()) ? "frontend-es6" : vfe.es6OutputDirectoryName
            }
            conventionMapping.map("es5OutputDirectoryName") {
                (vfe.es5OutputDirectoryName == null || vfe.es5OutputDirectoryName.isEmpty()) ? "frontend-es5" : vfe.es5OutputDirectoryName
            }
            conventionMapping.map("skipEs5") { vfe.skipEs5 }
            conventionMapping.map("bundle") { vfe.bundle }
            conventionMapping.map("minify") { vfe.minify }
            conventionMapping.map("hash") { vfe.hash }
            conventionMapping.map("bundleConfiguration") { vfe.bundleConfiguration }
            conventionMapping.map("nodePath") { vfe.nodePath }
            conventionMapping.map("nodeVersion") {
                getNodeJSVersion(vfe)
            }
            conventionMapping.map("yarnPath") { vfe.yarnPath }
            conventionMapping.map("yarnVersion") {
                getYarnVersion(vfe)
            }
            conventionMapping.map("yarnNetworkConcurrency") { vfe.yarnNetworkConcurrency }
            conventionMapping.map("fragments") { vfe.fragments }
            conventionMapping.map("classpaths") {
                Set<File> files = []


                War war = project.tasks.findByName(WarPlugin.WAR_TASK_NAME)
                if (war) {
                    files += war.classpath.files
                } else {
                    Set<ResolvedArtifact> cpartifacts = project.configurations.getByName("runtime").resolvedConfiguration.resolvedArtifacts
                    cpartifacts += project.configurations.getByName("providedCompile").resolvedConfiguration.resolvedArtifacts
                    cpartifacts += project.configurations.getByName("providedRuntime").resolvedConfiguration.resolvedArtifacts
                    cpartifacts += project.configurations.getByName(WEBAPP_EXTENSION_NAME).resolvedConfiguration.resolvedArtifacts

                    for (fitem in cpartifacts) {
                        if (fitem.file != null && fitem.file.exists()) files += fitem.file
                    }
                }

                files
            }
        }
    }


    static getFrontendWorkingDirectory(Project project) {
        File frontendWorkingDirectory = project.file(FRONTEND_WORKING_DIRECTORY)
        if (frontendWorkingDirectory == null || !frontendWorkingDirectory.exists()) {
            final List<String> potentialFrontEndDirectories = Arrays.asList(
                    "src/main/webapp/frontend",
                    "src/main/resources/META-INF/resources/frontend",
                    "src/main/resources/public/frontend",
                    "src/main/resources/static/frontend",
                    "src/main/resources/resources/frontend")

            for (String dir : potentialFrontEndDirectories) {
                File directory = project.file(dir)
                if (directory.exists()) return directory
            }
        } else return frontendWorkingDirectory

        return null
    }

    static getFrontendBuildDirectory(Project project) {
        File frontendBuildDirectory = project.file("${project.buildDir}/vaadin-flow/frontend")
        if (!frontendBuildDirectory.exists()) frontendBuildDirectory.mkdirs()

        return frontendBuildDirectory
    }


    @Memoized
    static Properties getPluginProperties() {
        Properties properties = new Properties()
        properties.load(CBGVaadinFlowPlugin.class.getResourceAsStream('/gradle-corebox-vaadin-flow-plugin.properties') as InputStream)
        properties
    }

    static String getProdModeVersion(CBGVaadinFlowExtension vfe) {
        String productionModeVersion = vfe.productionModeVersion
        if (!productionModeVersion) {
            productionModeVersion = getPluginProperties().getProperty('vaadin.productionModelVersion')
            if (!productionModeVersion) productionModeVersion = DEFAULT_VERSION_PRODUCTION_MODE
        }
        productionModeVersion
    }


    static String getNodeJSVersion(CBGVaadinFlowExtension vfe) {
        String ver = vfe.nodeVersion
        if (!ver) {
            ver = getPluginProperties().getProperty('nodejs.defaultVersion')
            if (!ver) ver = DEFAULT_VERSION_NODEJS
        }
        ver
    }

    static String getYarnVersion(CBGVaadinFlowExtension vfe) {
        String ver = vfe.yarnVersion
        if (!ver) {
            ver = getPluginProperties().getProperty('yarn.defaultVersion')
            if (!ver) ver = DEFAULT_VERSION_YARN
        }
        ver
    }
}
