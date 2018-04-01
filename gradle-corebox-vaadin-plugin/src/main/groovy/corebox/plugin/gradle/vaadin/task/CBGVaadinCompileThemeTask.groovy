package corebox.plugin.gradle.vaadin.task

import corebox.plugin.gradle.vaadin.CBGVaadins
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.BuildActionFailureException

import java.nio.file.Paths
import java.util.jar.Attributes
import java.util.jar.JarInputStream

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/20 </p>
 * <p>版权所属：xingxiuyi </p>
 */
@CacheableTask
class CBGVaadinCompileThemeTask extends DefaultTask {
    static final String NAME = "appVaadinTheme"

    /**
     * 主 CSS 文件
     */
    static final String STYLES_CSS = "styles.css"
    static final String STYLES_SCSS = "styles.scss"
    static final String STYLES_SCSS_PATTERN = "**/styles.scss"

    private static final String CLASSPATH_SWITCH = "-cp"
    private static final String TEMPDIR_SWITCH = "-Djava.io.tmpdir"

    static final String LIBSASS_COMPILER = "libsass"
    static final String VAADIN_COMPILER = "vaadin"

    @Input
    Boolean  enableTask = Boolean.TRUE

    @Input
    @Optional
    String themesDirectory = null

    @Input
    @Optional
    String compiler = VAADIN_COMPILER

    @Input
    @Optional
    Boolean compress = Boolean.TRUE

    @Input
    @Optional
    Boolean useClasspathJar = Os.isFamily(Os.FAMILY_WINDOWS)

    @Input
    @Optional
    Boolean logToConsole = Boolean.FALSE

    @Input
    @Optional
    List<String> jvmArgs = []

    CBGVaadinCompileThemeTask() {
        super()
        project.afterEvaluate {
            if (!getEnableTask()) return

            File themesDir = CBGVaadins.getThemesDirectory(project)
            inputs.dir themesDir
            inputs.files(project.fileTree(dir: themesDir, include: '**/*.scss').collect())
            outputs.files(project.fileTree(dir: themesDir, include: STYLES_SCSS_PATTERN).collect {
                File theme -> new File(new File(theme.parent), STYLES_CSS)
            })

            if (getUseClasspathJar()) {
                CBGVaadinBuildClassPathJar pathJarTask = project.tasks.getByName(CBGVaadinBuildClassPathJar.NAME)
                inputs.file(pathJarTask.archivePath)
            }

            finalizedBy project.tasks[CBGVaadinCompressCssTask.NAME]
        }
    }

    @TaskAction
    void exec() {
        if (!getEnableTask()) return
        this.compile(this.project)
    }

    /**
     * 编译项目
     * @param project
     * @param isRecompile
     * @return
     */
    void compile(Project project, boolean isRecompile = false) {
        File themesDir = CBGVaadins.getThemesDirectory(project)
        logger.info "开始编译 Theme 所在目录为" + themesDir

        FileTree themes = project.fileTree(dir: themesDir, include: STYLES_SCSS_PATTERN)

        logger.info "找到主题 ${themes.files.size()} 项"

        File unpackedThemesDir
        if (this.getCompiler() in [LIBSASS_COMPILER]) {
            unpackedThemesDir = unpackThemes(project)
        } else if (this.getThemesDirectory()) {
            // Must unpack themes for Valo to work when using custom directory
            unpackedThemesDir = unpackThemes(project)
        }

        themes.each { File theme ->
            File dir = new File(theme.parent)

            if (isRecompile) {
                logger.lifecycle "重新编译 ${theme.canonicalPath}..."
            } else {
                logger.info "开始编译 ${theme.canonicalPath}..."
            }

            def processStartTime = System.currentTimeMillis()


            Process process

            String cptype = this.getCompiler()
            if (!cptype) cptype = VAADIN_COMPILER

            switch (cptype) {
                case VAADIN_COMPILER:
                    File targetCss = new File(dir, STYLES_CSS)
                    if (this.getThemesDirectory()) {
                        File sourceScss = Paths.get(unpackedThemesDir.canonicalPath, dir.name, theme.name).toFile()
                        process = this.executeVaadinSassCompiler(sourceScss, targetCss)
                    } else {
                        process = this.executeVaadinSassCompiler(theme, targetCss)
                    }
                    break
                case LIBSASS_COMPILER:
                    process = this.executeLibSassCompiler(dir, unpackedThemesDir)
                    break
                default:
                    throw new BuildActionFailureException("Theme 编译器类型 \"${cptype}\" 无效", null)
            }

            boolean failed = false
            // TODO 此处需要判断是否记录到控制台
            CBGVaadins.logProcess(project, process, this.getLogToConsole(), "theme-compile.log") { String line ->
                if (line.contains("error")) {
                    logger.error(line)
                    failed = true
                    return false
                }
                true
            }

            int result = (process) ? process.waitFor() : -1

            def processUsedTime = (System.currentTimeMillis() - processStartTime) / 1000

            if (result != 0 || failed) {
                // 清理失败的CSS文件
                new File(dir, STYLES_CSS).delete()
                throw new BuildActionFailureException("Theme 编译失败，请检查 log 以获取详细信息", null)
            } else if (isRecompile) {
                logger.lifecycle "Theme 重新编译完成 用时 ${processUsedTime}s"
            } else {
                logger.info "Theme 编译完成 用时 ${processUsedTime}s"
            }
        }
    }

    /**
     * Creates a process that runs the Vaadin SASS compiler
     *
     * @param project
     *      the project to compile the SASS themes for
     * @param themePath
     *      the path of the theme
     * @param targetCSSFile
     *      the CSS file to compile into
     * @return
     * the process that runs the compiler
     */
    protected Process executeVaadinSassCompiler(File themeDir, File targetCSSFile) {
        def compileProcess = [CBGVaadins.getJavaBinary(project)]
        if (this.getJvmArgs()) {
            compileProcess += this.getJvmArgs() as List
        }

        compileProcess += ["$TEMPDIR_SWITCH=${this.temporaryDir.canonicalPath}"]
        compileProcess += [CLASSPATH_SWITCH, CBGVaadins.getCompileClassPathOrJar(project, this.getUseClasspathJar()).asPath]
        compileProcess += "com.vaadin.sass.SassCompiler"
        compileProcess += [themeDir.canonicalPath, targetCSSFile.canonicalPath]
        compileProcess.execute([], project.buildDir)
    }

    protected Process executeLibSassCompiler(File themeDir, File unpackedThemesDir) {
        File stylesScss = new File(themeDir, STYLES_SCSS)
        File stylesCss = new File(themeDir, STYLES_CSS)

        logger.info "使用编译器 libsass 编译 ${themeDir}"

        List compileProcess = [CBGVaadins.getJavaBinary(project)]
        if (this.getJvmArgs()) {
            compileProcess += this.getJvmArgs() as List
        }

        compileProcess += ["$TEMPDIR_SWITCH=${this.temporaryDir.canonicalPath}"]
        compileProcess += [CLASSPATH_SWITCH, CBGVaadins.getCompileClassPathOrJar(project, this.getUseClasspathJar()).asPath]
        compileProcess += "corebox.plugin.gradle.vaadin.LibSassCompiler"
        compileProcess += [stylesScss.canonicalPath, stylesCss.canonicalPath, unpackedThemesDir.canonicalPath]

        logger.debug compileProcess.toString()

        compileProcess.execute([], project.buildDir)
    }

    /**
     * 解压类路径下找到的原始 Theme 并解压到临时目录
     *
     * @param project
     * @return 解压后包含主题的临时目录
     */
    protected static File unpackThemes(Project project) {
        File unpackedVaadinDir = project.file("$project.buildDir/VAADIN")
        File unpackedThemesDir = project.file("$unpackedVaadinDir/themes")
        File unpackedAddonsThemesDir = project.file("$unpackedVaadinDir/addons")

        unpackedThemesDir.mkdirs()
        unpackedAddonsThemesDir.mkdirs()

        project.logger.info "Unpacking themes to ${unpackedThemesDir}"

        def themesAttribute = new Attributes.Name("Vaadin-Stylesheets")
        def bundleName = new Attributes.Name("Bundle-Name")
        project.configurations.all.each { Configuration conf ->
            conf.allDependencies.each { Dependency dependency ->
                if (dependency in ProjectDependency) {
                    def dependentProject = dependency.dependencyProject
                    if (dependentProject.hasProperty(VAADIN_COMPILER)) {
                        dependentProject.copy {
                            from CBGVaadins.getThemesDirectory(project)
                            into unpackedThemesDir
                        }
                    }
                } else if (CBGVaadins.isResolvable(project, conf)) {
                    conf.files(dependency).each { File file ->
                        file.withInputStream { InputStream stream ->
                            def jarStream = new JarInputStream(stream)
                            jarStream.withStream {
                                def mf = jarStream.manifest
                                def attributes = mf?.mainAttributes
                                String value = attributes?.getValue(themesAttribute)
                                Boolean themesValue = attributes?.getValue(bundleName) in ["vaadin-themes", "Vaadin Themes"]
                                if (value || themesValue) {
                                    project.logger.info "解压文件 ${file}"
                                    project.copy {
                                        includeEmptyDirs = false
                                        from project.zipTree(file)
                                        into unpackedVaadinDir
                                        include "VAADIN/themes/**/*", "VAADIN/addons/**/*"
                                        eachFile { details ->
                                            details.path -= "VAADIN"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 复制项目主题到解压目录中
        project.logger.info "Copying project theme into ${unpackedThemesDir}"
        project.copy {
            from CBGVaadins.getThemesDirectory(project)
            into unpackedThemesDir
        }

        return unpackedThemesDir
    }

}
