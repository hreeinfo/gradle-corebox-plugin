package corebox.plugin.gradle.vaadin8.task

import corebox.plugin.gradle.vaadin.CBGVaadin8s
import corebox.plugin.gradle.vaadin8.CBGVaadin8s
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/31 </p>
 * <p>版权所属：xingxiuyi </p>
 */

@CacheableTask
class CBGVaadin8CompressCssTask extends DefaultTask {
    static final String TASK_NAME_VAADIN_CSS = "appVaadinCompressCss"

    @Input
    Boolean  enableTask = Boolean.TRUE

    CBGVaadin8CompressCssTask() {
        project.afterEvaluate {
            if (!getEnableTask()) return
            File themesDir = CBGVaadin8s.getThemesDirectory(project)
            FileTree themes = project.fileTree(dir: themesDir, include: CBGVaadin8CompileThemeTask.STYLES_SCSS_PATTERN)
            themes.each { File theme ->
                File dir = new File(theme.parent)
                inputs.file new File(dir, CBGVaadin8CompileThemeTask.STYLES_CSS)
                outputs.file new File(dir, 'styles.css.gz')
            }
        }
    }

    @TaskAction
    void exec() {
        if (!getEnableTask()) return
        compress()
    }

    void compress(boolean isRecompress = false) {
        File themesDir = CBGVaadin8s.getThemesDirectory(project)
        FileTree themes = project.fileTree(dir: themesDir, include: CBGVaadin8CompileThemeTask.STYLES_SCSS_PATTERN)
        themes.each { File theme ->
            File dir = new File(theme.parent)
            File stylesCss = new File(dir, CBGVaadin8CompileThemeTask.STYLES_CSS)
            if (stylesCss.exists()) {
                if (isRecompress) {
                    project.logger.lifecycle("重新压缩 $stylesCss.canonicalPath...")
                } else {
                    project.logger.info("压缩 $stylesCss.canonicalPath...")
                }

                def processStartTime = System.currentTimeMillis()

                project.ant.gzip(src: stylesCss.canonicalPath, destfile: "${stylesCss.canonicalPath}.gz")

                def processUsedTime = (System.currentTimeMillis() - processStartTime) / 1000
                if (isRecompress) {
                    project.logger.lifecycle("Theme 重新压缩完成 用时 ${processUsedTime}s")
                } else {
                    project.logger.info("Theme 压缩完成 用时 ${processUsedTime}s")
                }
            } else {
                project.logger.warn("查找 $theme 下的 styles.css 文件出错")
            }
        }
    }
}
