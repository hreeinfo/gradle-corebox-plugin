package corebox.plugin.gradle.server.task

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
    static final String NAME = "appRun"


    @InputDirectory
    File webapp

    @InputFiles
    FileCollection webAppClasspath

    @InputDirectory
    @Optional
    File classesDirectory

    @Override
    protected String getProcessWebapp() {
        return this.getWebapp().canonicalPath
    }

    @Override
    protected Set<String> getProcessServerClasspaths() {
        if (!this.getWebAppClasspath()) return []

        Set<String> os = new LinkedHashSet<>()
        this.getWebAppClasspath().each { File f ->
            if (f) os.add(f.canonicalPath)
        }

        return os
    }

    @Override
    protected Set<String> getProcessClassesdirs() {
        Set<String> os = super.getProcessClassesdirs()
        if (this.getClassesDirectory()) {
            if (!os) os = new LinkedHashSet<>()
            os.add(this.getClassesDirectory().canonicalPath)
        }
        return os
    }

    @Override
    protected Set<String> getProcessOptions() {
        Set<String> os = super.getProcessOptions()

        if (this.getClassesDirectory()) {
            if (!os) os = new LinkedHashSet<>()
            os.add("classes_dir:${this.getClassesDirectory().canonicalPath}")
        }

        return os
    }
}
