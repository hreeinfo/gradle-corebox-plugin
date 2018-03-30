package corebox.plugin.gradle.server.task

import corebox.plugin.gradle.server.CBGServerInstance
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/20 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CBGServerRunTask extends CBGServerBaseTask {
    static final String TASK_NAME = "appRun"


    @InputDirectory
    File webapp

    @InputFiles
    FileCollection webAppClasspath

    @InputDirectory
    @Optional
    File classesDirectory

    @Override
    protected void configWebapp(CBGServerInstance instance) {

        if (this.getWebAppClasspath()) this.getWebAppClasspath().each { file ->
            instance.callBuilder("classesdir", file.getAbsolutePath())
        }

        if (this.getClassesDirectory()) {
            instance.callBuilder("classesdir", this.getClassesDirectory().getAbsolutePath())
            instance.callBuilder("option", "classes_dir", this.getClassesDirectory().getAbsolutePath())
        }

        instance.callBuilder("webapp", this.getWebapp().absolutePath)
    }
}
