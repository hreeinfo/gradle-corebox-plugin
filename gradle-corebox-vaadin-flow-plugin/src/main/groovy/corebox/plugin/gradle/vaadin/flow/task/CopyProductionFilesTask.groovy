package corebox.plugin.gradle.vaadin.flow.task

import com.vaadin.flow.plugin.common.ArtifactData
import com.vaadin.flow.plugin.common.JarContentsManager
import com.vaadin.flow.plugin.production.ProductionModeCopyStep
import corebox.plugin.gradle.vaadin.flow.CBGVaadinFlowPlugin
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 *
 */
@Log('LOGGER')
class CopyProductionFilesTask extends DefaultTask {
    static final String TASK_NAME = "vaadinCopyProductionFiles"

    static final String TEMPWORK_DIR = "vaadin-flow/tmp-frontend-unpack"

    static String FRONTEND_EXCLUDES = "**/LICENSE*,**/LICENCE*," +
            "**/demo/**,**/docs/**,**/test*/**,**/.*,**/*.md," +
            "**/bower.json,**/package.json,**/package-lock.json"

    @Input
    File copyOutputDirectory
    @Input
    String excludes
    @Input
    File frontendWorkingDirectory

    @TaskAction
    void exec() {
        File vcoDir = this.getCopyOutputDirectory()
        if (!vcoDir.exists()) vcoDir.mkdirs()

        Set<ResolvedArtifact> vallJars = projectAllJars(this.project)

        File workingDir = new File(project.buildDir, TEMPWORK_DIR)
        if (workingDir.exists()) project.delete(workingDir)
        workingDir.mkdirs()

        LOGGER.info("执行 ProductionModeCopyStep")
        // 解压 jar 中的 META-INF/resources/frontend/** 注意此处的jar必须在 cbvaddin 依赖配置中声明
        this.copyFrontendJars(workingDir)
        // 解压 war 中的 frontend/**
        this.copyFrontendWars(workingDir)
        // 复制 webapp 中的 frontend/**
        this.copyFrontendWebapp(workingDir)

        ProductionModeCopyStep step = new ProductionModeCopyStep(new JarContentsManager(), projectArtifactDatas(vallJars))
        step.copyWebApplicationFiles(this.getCopyOutputDirectory(), workingDir, this.getExcludes())
    }


    void copyFrontendJars(File workingDir) {
        Set<ResolvedArtifact> vfrontendWars = projectFrontendJars(this.project)
        File workingTempDir = new File(project.buildDir, "${TEMPWORK_DIR}-jars")
        if (workingTempDir.exists()) project.delete(workingTempDir)
        workingTempDir.mkdirs()

        // 复制 依赖 war 的 frontend 文件
        if (vfrontendWars) vfrontendWars.each {
            File f = it.file
            project.copy {
                from(project.zipTree(f)) { include "META-INF/resources/frontend/**" }
                into workingTempDir
            }
        }

        File warFiles = new File(workingTempDir, "META-INF/resources/frontend")
        if (warFiles.exists()) project.copy {
            from warFiles
            into workingDir
        }

        project.delete(workingTempDir)
    }

    void copyFrontendWars(File workingDir) {
        Set<ResolvedArtifact> vallWars = projectAllWars(this.project)
        File workingTempDir = new File(project.buildDir, "${TEMPWORK_DIR}-wars")
        if (workingTempDir.exists()) project.delete(workingTempDir)
        workingTempDir.mkdirs()

        // 复制 依赖 war 的 frontend 文件
        if (vallWars) vallWars.each {
            File f = it.file
            project.copy {
                from(project.zipTree(f)) { include "frontend/**" }
                into workingTempDir
            }
        }

        File warFiles = new File(workingTempDir, "frontend")
        if (warFiles.exists()) project.copy {
            from warFiles
            into workingDir
        }
        project.delete(workingTempDir)
    }

    void copyFrontendWebapp(File workingDir) {
        File vfwDir = this.getFrontendWorkingDirectory()

        // 复制 webapp 的 frontend 文件
        if (vfwDir.exists()) project.copy {
            from vfwDir
            into workingDir
        }
    }

    static Set<ResolvedArtifact> projectAllJars(Project project) {
        Set<ResolvedArtifact> cpartifacts = project.configurations.getByName("runtime").resolvedConfiguration.resolvedArtifacts
        cpartifacts += project.configurations.getByName("compile").resolvedConfiguration.resolvedArtifacts
        cpartifacts += project.configurations.getByName("compileOnly").resolvedConfiguration.resolvedArtifacts
        cpartifacts += project.configurations.getByName("providedCompile").resolvedConfiguration.resolvedArtifacts
        cpartifacts += project.configurations.getByName("providedRuntime").resolvedConfiguration.resolvedArtifacts
        cpartifacts += project.configurations.getByName(CBGVaadinFlowPlugin.WEBAPP_EXTENSION_NAME).resolvedConfiguration.resolvedArtifacts

        Set<ResolvedArtifact> ras = []
        cpartifacts.each {
            if ("jar" == it.type && !ras.contains(it)) ras += it
        }

        ras
    }

    static Set<ResolvedArtifact> projectFrontendJars(Project project) {
        Set<ResolvedArtifact> cpartifacts = project.configurations.getByName(CBGVaadinFlowPlugin.SERVER_EXTENSION_NAME).resolvedConfiguration.resolvedArtifacts

        Set<ResolvedArtifact> ras = []
        cpartifacts.each {
            if ("jar" == it.type && !ras.contains(it)) ras += it
        }

        ras
    }

    static Set<ResolvedArtifact> projectAllWars(Project project) {
        Set<ResolvedArtifact> cpartifacts = project.configurations.getByName("runtime").resolvedConfiguration.resolvedArtifacts
        cpartifacts += project.configurations.getByName("compile").resolvedConfiguration.resolvedArtifacts
        cpartifacts += project.configurations.getByName("compileOnly").resolvedConfiguration.resolvedArtifacts
        cpartifacts += project.configurations.getByName("providedCompile").resolvedConfiguration.resolvedArtifacts
        cpartifacts += project.configurations.getByName("providedRuntime").resolvedConfiguration.resolvedArtifacts
        cpartifacts += project.configurations.getByName(CBGVaadinFlowPlugin.WEBAPP_EXTENSION_NAME).resolvedConfiguration.resolvedArtifacts

        Set<ResolvedArtifact> ras = []
        cpartifacts.each {
            if ("war" == it.type && !ras.contains(it)) ras += it
        }

        ras
    }


    static Set<ArtifactData> projectArtifactDatas(Set<ResolvedArtifact> ras) {
        Set<ArtifactData> artifactDatas = []
        if (ras != null) ras.each {
            artifactDatas += new ArtifactData(it.file, it.name, it.moduleVersion.id.version)
        }
        artifactDatas
    }
}
