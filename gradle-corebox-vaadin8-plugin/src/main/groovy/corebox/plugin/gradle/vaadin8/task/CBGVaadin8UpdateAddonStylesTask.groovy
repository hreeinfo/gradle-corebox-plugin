package corebox.plugin.gradle.vaadin8.task

import corebox.plugin.gradle.common.CBGs
import corebox.plugin.gradle.vaadin8.CBGVaadin8s
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/31 </p>
 * <p>版权所属：xingxiuyi </p>
 */
@CacheableTask
class CBGVaadin8UpdateAddonStylesTask extends DefaultTask {
    static final String TASK_NAME_VAADIN_ADDON_STYLE = "appVaadinUpdateAddonStyles"
    static final String ADDONS_SCSS_FILE = "addons.scss"

    @Input
    Boolean  enableTask = Boolean.TRUE

    @Input
    @Optional
    Boolean useClasspathJar = Os.isFamily(Os.FAMILY_WINDOWS)

    @Input
    @Optional
    Boolean logToConsole = Boolean.FALSE

    CBGVaadin8UpdateAddonStylesTask() {
        super()

        project.afterEvaluate {
            if (!getEnableTask()) return

            def themesDir = CBGVaadin8s.getThemesDirectory(project)
            if (themesDir && themesDir.exists()) {
                themesDir.eachDir {
                    inputs.dir it
                    outputs.file new File(it, ADDONS_SCSS_FILE)
                }
            }

            if (this.getUseClasspathJar()) {
                CBGVaadin8BuildClassPathJar pathJarTask = project.getTasksByName(CBGVaadin8BuildClassPathJar.TASK_NAME_VAADIN_BCPJ, true).first()
                inputs.file(pathJarTask.archivePath)
            }
        }
    }

    @TaskAction
    void run() {
        if (!getEnableTask()) return

        File themesDir = CBGVaadin8s.getThemesDirectory(project)
        themesDir.mkdirs()
        themesDir.eachDir {

            File addonsScss = new File(it, ADDONS_SCSS_FILE)

            project.logger.info "更新 $addonsScss"

            FileCollection classpath = CBGVaadin8s.getCompileClassPathOrJar(project, getUseClasspathJar())

            if (this.getUseClasspathJar()) {
                CBGVaadin8s.findAddonsInProject(project, 'Vaadin-Stylesheets', true).each {
                    classpath += project.files(it.file)
                }
            }

            List<String> importer = [CBGs.getJavaBinary(project)]
            importer.add('-cp')
            importer.add(classpath.asPath)
            importer.add('com.vaadin.server.themeutils.SASSAddonImportFileCreator')
            importer.add(it.canonicalPath)

            Process process = importer.execute(CBGs.getSystemEnvs(), project.buildDir)

            CBGs.logProcess(project, process, getLogToConsole(), 'addon-style-updater.log') { true }

            int result = process.waitFor()
            if (result != 0) {
                project.logger.error "更新 $addonsScss 失败。 SASS importer 返回错误代码 $result"
            }
        }
    }
}
