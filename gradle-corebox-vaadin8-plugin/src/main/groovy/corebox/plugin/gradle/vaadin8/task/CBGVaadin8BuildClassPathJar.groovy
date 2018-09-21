package corebox.plugin.gradle.vaadin8.task

import corebox.plugin.gradle.vaadin8.CBGVaadin8Plugin
import corebox.plugin.gradle.vaadin8.CBGVaadin8s
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.jvm.tasks.Jar

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/31 </p>
 * <p>版权所属：xingxiuyi </p>
 */
@CacheableTask
class CBGVaadin8BuildClassPathJar extends Jar {
    static final String TASK_NAME_VAADIN_BCPJ = "appVaadinBuildClassPathJar"

    @Input
    Boolean enableTask = Boolean.TRUE

    @Input
    Boolean useClasspathJar = Os.isFamily(Os.FAMILY_WINDOWS)

    CBGVaadin8BuildClassPathJar() {
        super()

        inputs.files project.configurations[CBGVaadin8Plugin.CONFIGURATION_THEME]
        inputs.files project.configurations[CBGVaadin8Plugin.CONFIGURATION_SERVER]
        inputs.files CBGVaadin8s.getCompileClassPath(project)
    }

    @Override
    protected void copy() {
        if (getEnableTask()) {
            FileCollection files = (CBGVaadin8s.getWarClasspath(project) + project.configurations[CBGVaadin8Plugin.CONFIGURATION_THEME]).filter {
                it.file && it.canonicalFile.name.endsWith('.jar')
            }
            manifest {
                it.attributes('Class-Path': files.collect { File file -> file.toURI().toString() }.join(' '))
            }
        }
        super.copy()
    }
}
