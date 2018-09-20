package corebox.plugin.gradle.vaadin.flow.task

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig
import com.vaadin.flow.plugin.common.AnnotationValuesExtractor
import com.vaadin.flow.plugin.common.FrontendDataProvider
import com.vaadin.flow.plugin.common.FrontendToolsManager
import com.vaadin.flow.plugin.common.RunnerManager
import com.vaadin.flow.plugin.production.TranspilationStep
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.reflections.Reflections
import org.slf4j.Logger

/**
 *
 */
@Log('LOGGER')
class PackageForProductionTask extends DefaultTask {
    static final String TASK_NAME = "vaadinPackageForProduction"

    @Input
    File transpileEs6SourceDirectory
    @Input
    File transpileWorkingDirectory
    @Input
    File transpileOutputDirectory
    @Input
    @Optional
    String es6OutputDirectoryName
    @Input
    @Optional
    String es5OutputDirectoryName
    @Input
    @Optional
    Boolean skipEs5 = Boolean.FALSE
    @Input
    @Optional
    Boolean bundle = Boolean.TRUE
    @Input
    @Optional
    Boolean minify = Boolean.TRUE
    @Input
    @Optional
    Boolean hash = Boolean.TRUE
    @Input
    @Optional
    File bundleConfiguration
    @Input
    @Optional
    File nodePath
    @Input
    @Optional
    String nodeVersion
    @Input
    @Optional
    File yarnPath
    @Input
    @Optional
    String yarnVersion
    @Input
    @Optional
    Integer yarnNetworkConcurrency = -1

    @Input
    @Optional
    Set<File> fragments = []

    @Input
    Set<File> classpaths = []

    @TaskAction
    void exec() {
        File vtWorking = this.getTranspileWorkingDirectory()
        if (!vtWorking.exists()) vtWorking.mkdirs()

        File vtES6Dir = this.getTranspileEs6SourceDirectory()
        File vtBC = this.getBundleConfiguration()
        String vtES6Out = this.getEs6OutputDirectoryName()
        String vtES5Out = this.getEs5OutputDirectoryName()

        // 暂时禁用警告
        Logger rlog = Reflections.log
        Reflections.log = null
        AnnotationValuesExtractor vave = new AnnotationValuesExtractor(getProjectClassPathUrls())
        Reflections.log = rlog
        Map<String, Set<String>> vfrm = getFragmentsData(fragments)
        FrontendDataProvider fdp = new FrontendDataProvider(this.getBundle(), this.getMinify(), this.getHash(), vtES6Dir, vave, vtBC, vfrm)

        // 构建工具下载和配置
        RunnerManager rmgt = null

        boolean rmgtUseDWVersion = (this.getNodePath() == null || this.getYarnPath() == null)
        if (rmgtUseDWVersion) {
            this.resetNodeYarn(vtWorking)
            rmgt = new RunnerManager(this.getTranspileWorkingDirectory(), getProxyConfig(), this.getNodeVersion(), this.getYarnVersion())
        } else {
            rmgt = new RunnerManager(this.getTranspileWorkingDirectory(), getProxyConfig(), this.getNodePath(), this.getYarnPath());
        }

        FrontendToolsManager ftm = new FrontendToolsManager(vtWorking, vtES5Out, vtES6Out, fdp, rmgt)

        TranspilationStep step = new TranspilationStep(ftm, this.getYarnNetworkConcurrency())
        step.transpileFiles(vtES6Dir, this.getTranspileOutputDirectory(), this.getSkipEs5())

        // 缓存下载的工具
        if (rmgtUseDWVersion) this.cacheNodeYarn(vtWorking)
    }

    private void cacheNodeYarn(File vtWorking) {
        File f = project.file(".gradle/cache/cbvaddin/node_yarn/${this.getNodeVersion()}_${this.getYarnVersion()}")
        if (!f.exists()) {
            f.mkdirs()
            File ndir = new File(vtWorking, "node")
            if (!ndir.exists()) ndir.mkdirs()
            project.copy {
                from ndir
                into f
            }
        }
    }

    private void resetNodeYarn(File vtWorking) {
        File f = project.file(".gradle/cache/cbvaddin/node_yarn/${this.getNodeVersion()}_${this.getYarnVersion()}")
        if (new File(f, "node").exists()) {
            File ndir = new File(vtWorking, "node")
            File nfile = new File(ndir, "node")
            if (!nfile.exists()) {
                ndir.mkdirs()
                project.copy {
                    from f
                    into ndir
                }
            }

        }
    }

    private static Map<String, Set<String>> getFragmentsData(Set<File> fragments) {
        return [:]
        /*
        return Optional.ofNullable(mavenFragments)
                .orElse(Collections.emptyList()).stream()
                .peek(this::verifyFragment).collect(Collectors
                .toMap(Fragment::getName, Fragment::getFiles));
         */
    }

    private static void verifyFragment(VaadinFlowFragment fragment) {
        if (fragment.getName() == null || fragment.getFiles() == null
                || fragment.getFiles().isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Each fragment definition should have a name and list of files to include defined. Got incorrect definition: '%s'",
                    fragment))
        }
    }

    private URL[] getProjectClassPathUrls() {
        URL[] urls = []

        if (this.getClasspaths()) for (it in this.getClasspaths()) {
            if (!it.name.endsWith(".war")) urls += it.toURI().toURL()
        }

        urls
    }

    private static ProxyConfig getProxyConfig() {
        return new ProxyConfig(Collections.emptyList());
    }
}
