package corebox.plugin.gradle.vaadin.flow

/**
 *
 */
class CBGVaadinFlowExtension {
    Boolean enable = Boolean.FALSE
    Boolean productionMode = Boolean.FALSE

    String productionModeVersion = "1.0.5"

    File copyOutputDirectory
    String excludes
    File frontendWorkingDirectory
    File transpileWorkingDirectory
    File transpileOutputDirectory
    String es6OutputDirectoryName
    String es5OutputDirectoryName
    Boolean skipEs5 = Boolean.FALSE
    Set<File> fragments = []
    Boolean bundle = Boolean.TRUE
    Boolean minify = Boolean.TRUE
    Boolean hash = Boolean.TRUE
    File bundleConfiguration
    File nodePath
    String nodeVersion
    File yarnPath
    String yarnVersion
    Integer yarnNetworkConcurrency = -1

    void enable(boolean enable) {
        this.enable = enable
    }

    void productionMode(boolean productionMode) {
        this.productionMode = productionMode
    }


    void productionModeVersion(String productionModeVersion) {
        this.productionModeVersion = productionModeVersion
    }

    void copyOutputDirectory(File copyOutputDirectory) { this.copyOutputDirectory = copyOutputDirectory }

    void excludes(String excludes) { this.excludes = excludes }

    void frontendWorkingDirectory(File frontendWorkingDirectory) { this.frontendWorkingDirectory = frontendWorkingDirectory }

    void transpileWorkingDirectory(File transpileWorkingDirectory) { this.transpileWorkingDirectory = transpileWorkingDirectory }

    void transpileOutputDirectory(File transpileOutputDirectory) { this.transpileOutputDirectory = transpileOutputDirectory }

    void es6OutputDirectoryName(String es6OutputDirectoryName) { this.es6OutputDirectoryName = es6OutputDirectoryName }

    void es5OutputDirectoryName(String es5OutputDirectoryName) { this.es5OutputDirectoryName = es5OutputDirectoryName }

    void skipEs5(boolean skipEs5) { this.skipEs5 = skipEs5 }

    void fragments(File fragment) { this.fragments += fragment }

    void bundle(boolean bundle) { this.bundle = bundle }

    void minify(boolean minify) { this.minify = minify }

    void hash(boolean hash) { this.hash = hash }

    void bundleConfiguration(File bundleConfiguration) { this.bundleConfiguration = bundleConfiguration }

    void nodePath(File nodePath) { this.nodePath = nodePath }

    void nodeVersion(String nodeVersion) { this.nodeVersion = nodeVersion }

    void yarnPath(File yarnPath) { this.yarnPath = yarnPath }

    void yarnVersion(String yarnVersion) { this.yarnVersion = yarnVersion }

    void yarnNetworkConcurrency(Integer yarnNetworkConcurrency) { this.yarnNetworkConcurrency = yarnNetworkConcurrency }
}
