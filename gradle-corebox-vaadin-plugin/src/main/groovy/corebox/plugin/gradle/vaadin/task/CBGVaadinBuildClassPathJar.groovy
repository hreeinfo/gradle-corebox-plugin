package corebox.plugin.gradle.vaadin.task

import corebox.plugin.gradle.vaadin.CBGVaadinPlugin
import corebox.plugin.gradle.vaadin.CBGVaadins
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
class CBGVaadinBuildClassPathJar extends Jar {
    static final String TASK_NAME_VAADIN_BCPJ = "appVaadinBuildClassPathJar"

    @Input
    Boolean enableTask = Boolean.TRUE

    @Input
    Boolean useClasspathJar = Os.isFamily(Os.FAMILY_WINDOWS)

    CBGVaadinBuildClassPathJar() {
        super()

        inputs.files project.configurations[CBGVaadinPlugin.CONFIGURATION_THEME]
        inputs.files project.configurations[CBGVaadinPlugin.CONFIGURATION_SERVER]
        inputs.files CBGVaadins.getCompileClassPath(project)
    }

    @Override
    protected void copy() {
        if (getEnableTask()) {
            FileCollection files = (CBGVaadins.getWarClasspath(project) + project.configurations[CBGVaadinPlugin.CONFIGURATION_THEME]).filter {
                it.file && it.canonicalFile.name.endsWith('.jar')
            }
            manifest {
                it.attributes('Class-Path': files.collect { File file -> file.toURI().toString() }.join(' '))
            }
        }
        super.copy()
    }
}
