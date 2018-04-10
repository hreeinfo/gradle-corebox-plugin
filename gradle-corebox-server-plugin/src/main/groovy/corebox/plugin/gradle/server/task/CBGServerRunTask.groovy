package corebox.plugin.gradle.server.task

import org.apache.commons.io.filefilter.WildcardFileFilter
import org.apache.commons.lang3.StringUtils
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional

import java.nio.file.Paths

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

    @InputFiles
    @Optional
    FileCollection classesDirectories

    protected void onProcessReady() {
        String ctxsp = this.getContext()

        String fullctxsp = (StringUtils.startsWith(ctxsp, "/")) ? "${ctxsp}" : "/${ctxsp}"
        project.logger.quiet("服务器 ${this.getType()} 已启动 访问地址 http://127.0.0.1:${this.getPort()}${fullctxsp}")

        if (this.getHotReload() && this.innerHotReloadType) {
            project.logger.quiet("已开启热重载模式 ${this.innerHotReloadType}")
            if (this.getClassesdirs()) {
                project.logger.quiet("下列路径的 .class 文件变化将热重载：")
                this.getClassesdirs().each { String s ->
                    project.logger.quiet("\t> ${project.relativePath(s)}")
                }
            }
        }

        if (this.getResourcesdirs()) {
            project.logger.quiet("下列路径的 webapp 静态资源文件支持实时修改：")
            this.getResourcesdirs().each { String s ->
                project.logger.quiet("\t> ${project.relativePath(s)}")
            }
        }
    }

    @Override
    protected String getProcessWebapp() {
        return this.getWebapp().canonicalPath
    }

    @Override
    protected Set<String> getProcessServerClasspaths(String pWebappDir) {
        if (!this.getWebAppClasspath()) return []

        Set<String> os = new LinkedHashSet<>()

        if (this.getServerClasspaths()) os.addAll(this.getServerClasspaths())

        // 额外处理 获取的 classpath 应该做去重处理

        Map<String, File> files = new LinkedHashMap<>()

        this.getWebAppClasspath().each { File f ->
            if (f) files.put(f.name, f)
        }

        // blacklist: 当存在 WEB-INF/lib 时，server classpaths 不应包含对应 jar
        Set<WildcardFileFilter> blacklist = []
        if (this.getBlacklistJars()) this.getBlacklistJars().each { String fp ->
            blacklist.add(new WildcardFileFilter(fp))
        }

        Set<String> blkjars = []
        files.each { String k, File f ->
            for (WildcardFileFilter wf : blacklist) {
                if (wf.accept(f)) blkjars.add(k)
            }
        }

        if (blkjars) blkjars.each { String k ->
            files.remove(k)
            project.logger.info "classpath 去掉了与 blacklistJars 匹配的项 ${k}  "
        }

        File webinfLib = project.file(Paths.get(pWebappDir, "WEB-INF", "lib"))
        if (webinfLib && webinfLib.exists() && webinfLib.isDirectory()) {
            File[] libfiles = webinfLib.listFiles()
            if (libfiles) libfiles.each { File f ->
                if (files.containsKey(f.getName())) {
                    files.remove(f.getName())
                    project.logger.info "classpath 去掉了与 WEB-INF/lib/ 已存在的项 ${f.getName()}"
                }
            }
        }

        files.each { String k, File v ->
            if (v) os.add(v.canonicalPath)
        }

        return os
    }

    @Override
    protected Set<String> getProcessClassesdirs() {
        Set<String> os = super.getProcessClassesdirs()
        if (this.getClassesDirectories()) {
            if (!os) os = new LinkedHashSet<>()
            this.getClassesDirectories().each { File f ->
                os.add(f.canonicalPath)
            }
        }
        return os
    }

    @Override
    protected Set<String> getProcessOptions() {
        Set<String> os = super.getProcessOptions()

        if (this.getClassesDirectories()) {
            if (!os) os = new LinkedHashSet<>()

            String cds = ""

            this.getClassesDirectories().each { File f ->
                if (f) cds = (StringUtils.isNotBlank(cds)) ? "${cds};${f.canonicalPath}" : "${f.canonicalPath}"
            }

            os.add("classes_dir:${cds}")
        }
        return os
    }
}
