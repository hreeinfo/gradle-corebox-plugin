package corebox.plugin.gradle.vaadin.extension

import org.apache.tools.ant.taskdefs.condition.Os

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/20 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CBGVaadinExtension {
    Boolean enable = Boolean.TRUE
    String version = null

    String outputDir = null
    String themeCompiler = null
    Boolean themeCompress = Boolean.TRUE
    List<String> themeJvmArgs = []

    Boolean logToConsole = Boolean.FALSE
    Boolean useClasspathJar = Os.isFamily(Os.FAMILY_WINDOWS)
    Boolean manageRepositories = Boolean.TRUE

    String servletVersion = null
    String vaadinSassVersion = null
    String libsassVersion = null

    String widgetset = null

    public void enable(Boolean enable) {
        this.enable = enable
    }

    public void version(String version) {
        this.version = version
    }

    public void outputDir(String outputDir) {
        this.outputDir = outputDir
    }

    public void themeCompiler(String themeCompiler) {
        this.themeCompiler = themeCompiler
    }

    public void themeCompress(Boolean themeCompress) {
        this.themeCompress = themeCompress
    }


    public void themeJvmArgs(List<String> themeJvmArgs) {
        this.themeJvmArgs = themeJvmArgs
    }


    public void logToConsole(Boolean logToConsole) {
        this.logToConsole = logToConsole
    }

    public void useClasspathJar(Boolean useClasspathJar) {
        this.useClasspathJar = useClasspathJar
    }

    public void manageRepositories(Boolean manageRepositories) {
        this.manageRepositories = manageRepositories
    }

    public void servletVersion(String servletVersion) {
        this.servletVersion = servletVersion
    }

    public void vaadinSassVersion(String vaadinSassVersion) {
        this.vaadinSassVersion = vaadinSassVersion
    }

    public void libsassVersion(String libsassVersion) {
        this.libsassVersion = libsassVersion
    }

    public void widgetset(String widgetset) {
        this.widgetset = widgetset
    }
}
