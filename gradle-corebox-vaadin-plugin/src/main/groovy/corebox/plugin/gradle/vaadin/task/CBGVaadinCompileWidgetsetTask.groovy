package corebox.plugin.gradle.vaadin.task

import corebox.plugin.gradle.common.CBGs
import corebox.plugin.gradle.vaadin.CBGVaadins
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 *
 * <p创建作者：xingxiuyi </p
 * <p创建日期：2018/4/2 </p
 * <p版权所属：xingxiuyi </p
 */
@CacheableTask
class CBGVaadinCompileWidgetsetTask extends DefaultTask {
    static final String NAME = "appVaadinWidgetset"

    @Input
    @Optional
    String style = null

    @Input
    @Optional
    Integer optimize = 0

    @Input
    @Optional
    Boolean logEnabled = Boolean.TRUE


    @Input
    @Optional
    Boolean logToConsole = Boolean.FALSE

    @Input
    @Optional
    String logLevel = null

    @Input
    @Optional
    Integer localWorkers = null

    @Input
    @Optional
    Boolean draftCompile = Boolean.TRUE

    @Input
    @Optional
    Boolean strict = Boolean.TRUE

    @Input
    @Optional
    String userAgent = null

    @Input
    @Optional
    List<String> jvmArgs = []

    @Input
    @Optional
    List<String> extraArgs = []

    @Input
    @Optional
    List<String> sourcePaths = []

    @Input
    @Optional
    Boolean collapsePermutations = Boolean.TRUE

    @Input
    @Optional
    List<String> extraInherits = []

    @Input
    @Optional
    Boolean gwtSdkFirstInClasspath = Boolean.TRUE

    @Input
    @Optional
    String outputDirectory = null

    @Input
    @Optional
    Boolean profiler = Boolean.FALSE

    @Input
    @Optional
    Boolean manageWidgetset = Boolean.TRUE

    @Input
    @Optional
    String widgetset = null

    @Input
    @Optional
    String widgetsetGenerator = null

    @TaskAction
    void exec() {
        String widgetset = CBGVaadins.getWidgetset(project)
        if (widgetset) compileLocally(widgetset)
    }

    private void compileLocally(String widgetset = CBGVaadins.getWidgetset(project)) {

        // 重新创建目录
        CBGVaadins.getWidgetsetDirectory(project).mkdirs()

        // 增加 client 依赖： 缺少的 classpath jar
        FileCollection classpath = CBGVaadins.getClientCompilerClassPath(project)

        List widgetsetCompileProcess = [CBGs.getJavaBinary(project)]

        if (getJvmArgs()) {
            widgetsetCompileProcess += getJvmArgs() as List
        }

        widgetsetCompileProcess += ['-cp', classpath.asPath]

        widgetsetCompileProcess += ["-Dgwt.persistentunitcachedir=${project.buildDir.canonicalPath}"]
        widgetsetCompileProcess += ["-Dgwt.forceVersionCheckURL=foobar"]

        widgetsetCompileProcess += ["-Djava.io.tmpdir=${temporaryDir.canonicalPath}"]

        widgetsetCompileProcess += 'com.google.gwt.dev.Compiler'

        widgetsetCompileProcess += ['-style', getStyle()]
        widgetsetCompileProcess += ['-optimize', getOptimize()]
        widgetsetCompileProcess += ['-war', CBGVaadins.getWidgetsetDirectory(project).canonicalPath]
        widgetsetCompileProcess += ['-logLevel', getLogLevel()]
        widgetsetCompileProcess += ['-localWorkers', getLocalWorkers()]
        widgetsetCompileProcess += ['-workDir', project.buildDir.canonicalPath + File.separator + 'tmp']

        if (getDraftCompile()) widgetsetCompileProcess += '-draftCompile'

        if (getStrict()) widgetsetCompileProcess += '-strict'

        if (getExtraArgs()) widgetsetCompileProcess += getExtraArgs() as List

        widgetsetCompileProcess += widgetset

        Process process = widgetsetCompileProcess.execute([], project.buildDir)
        boolean failed = false
        CBGs.logProcess(project, process, getLogToConsole(), 'widgetset-compile.log') { String output ->
            // Monitor 错误日志
            if (output.trim().startsWith('[ERROR]')) {
                failed = true
                return false
            }
            true
        }

        // 等待编译进程
        int result = process.waitFor()

        // 编译器会在 widgetsets 目录中 生成额外的 WEB-INF ，完成后清理掉
        new File(CBGVaadins.getWidgetsetDirectory(project), 'WEB-INF').deleteDir()

        if (failed || result != 0) throw new GradleException('Widgetset 编译失败，请检查日志')
    }
}
