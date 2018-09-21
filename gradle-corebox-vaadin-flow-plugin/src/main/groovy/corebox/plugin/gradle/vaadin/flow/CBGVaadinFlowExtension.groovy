package corebox.plugin.gradle.vaadin.flow

import corebox.plugin.gradle.vaadin.flow.task.VaadinFlowFragment

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
    Set<VaadinFlowFragment> fragments = []
    Boolean bundle = Boolean.TRUE
    Boolean minify = Boolean.TRUE
    Boolean hash = Boolean.TRUE
    File bundleConfiguration
    File nodePath
    String nodeVersion
    File yarnPath
    String yarnVersion
    Integer yarnNetworkConcurrency = -1

    Set<String> persistFrontend = []

    void enable(Boolean enable) {
        this.enable = enable
    }

    void productionMode(Boolean productionMode) {
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

    void skipEs5(Boolean skipEs5) { this.skipEs5 = skipEs5 }

    void bundle(Boolean bundle) { this.bundle = bundle }

    void minify(Boolean minify) { this.minify = minify }

    void hash(Boolean hash) { this.hash = hash }

    void bundleConfiguration(File bundleConfiguration) { this.bundleConfiguration = bundleConfiguration }

    void nodePath(File nodePath) { this.nodePath = nodePath }

    void nodeVersion(String nodeVersion) { this.nodeVersion = nodeVersion }

    void yarnPath(File yarnPath) { this.yarnPath = yarnPath }

    void yarnVersion(String yarnVersion) { this.yarnVersion = yarnVersion }

    void yarnNetworkConcurrency(Integer yarnNetworkConcurrency) { this.yarnNetworkConcurrency = yarnNetworkConcurrency }

    /**
     * 与 vaadin production mode 所要求的 fragments.json  相同
     *
     * 此方法与 bundleConfiguration 作用相同，是 bundleConfiguration 的别名
     * @param fragment
     */
    void fragments(File fragmentFile) {
        if (!fragmentFile || !fragmentFile.exists()) return
        this.bundleConfiguration = fragmentFile
    }

    /**
     * 按项增加 vaadin production mode  fragment
     * @param name
     * @param files
     */
    void fragment(String name, List<String> files) {
        if (name && files && !name.trim().isEmpty() && !files.isEmpty()) this.fragments.add(new VaadinFlowFragment(name, files))
    }

    /**
     * 特殊配置 当需要在编译后的 es 目录中保留源 frontend 文件 可以在此声明 如有多个值可以多次调用本方法
     *
     * 注意 file 是相对于 frontend:// 的路径名（文件名）ANT 模式匹配表达式 例如：
     *
     * persistFrontend "textConnector.js"
     *
     * persistFrontend "src/views/**"
     *
     * @param file
     */
    void persistFrontend(String file) { if (file) this.persistFrontend += file }
}
