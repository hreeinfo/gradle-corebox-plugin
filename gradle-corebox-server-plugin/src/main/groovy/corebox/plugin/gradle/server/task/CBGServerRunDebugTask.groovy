package corebox.plugin.gradle.server.task

import org.apache.commons.lang3.StringUtils
import org.gradle.api.JavaVersion
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/4/5 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CBGServerRunDebugTask extends CBGServerRunTask {
    static final String TASK_NAME_APPRUN_DEBUG = "appRunDebug"

    static final String DAD_FLAG = "-Xdebug"

    // -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9999
    static final String DAC_FLAG = "-Xrunjdwp"


    @Input
    @Optional
    Integer debugPort = 5005

    @Input
    @Optional
    String debugConfig = null

    protected void onProcessReady() {
        super.onProcessReady()
        project.logger.quiet(" ")
        project.logger.quiet("已经开启调试模式 远程调试端口 ${this.getDebugPort()} 请在IDE中配置 Remote Debug 选项")
    }

    @Override
    protected Set<String> getProcessJvmArgs(String javaBinary, JavaVersion javaVersion) {
        Set<String> os = super.getProcessJvmArgs(javaBinary, javaVersion)

        if (!os) os = new LinkedHashSet<>()

        boolean addXdebug = true
        boolean addXrunjdwp = true

        os.each { String s ->
            if (s) {
                if (StringUtils.equals(s.trim(), DAD_FLAG)) {
                    addXdebug = false
                } else if (StringUtils.startsWith(s, DAC_FLAG)) {
                    addXrunjdwp = false
                }
                os.add(s)
            }
        }

        if (addXdebug) os.add(DAD_FLAG)
        if (addXrunjdwp) os.add(this.generateXrunjdwp(javaBinary, javaVersion))

        return os
    }

    private String generateXrunjdwp(String javaBinary, JavaVersion javaVersion) {
        String xrun = "${DAC_FLAG}:"
        Map<String, String> map = [:]

        map.put("transport", "dt_socket")
        map.put("server", "y")
        map.put("suspend", "n")

        String socs = this.getDebugConfig()
        if (socs) {
            if (StringUtils.startsWith(socs, "${DAC_FLAG}:")) socs = StringUtils.substringAfter(socs, "${DAC_FLAG}:")
            String[] ocs = StringUtils.split(socs, ",")
            if (ocs) ocs.each { String o ->
                if (o && StringUtils.contains(o, "=")) {
                    String[] pkv = StringUtils.split(o, "=")
                    if (pkv && pkv.size() > 1) map.put(StringUtils.trimToEmpty(pkv[0]), StringUtils.trimToEmpty(pkv[1]))
                }
            }
        }

        map.each { String k, String v ->
            xrun = "${xrun}${k}=${v},"
        }

        String rdport = this.generateDebugPortStr(javaVersion)

        xrun = "${xrun}${rdport}"

        return xrun
    }

    /**
     * 规则：
     *
     * 对于9-10+ 使用*:port
     *
     * 对于8及以下使用 port
     *
     * @return
     */
    private String generateDebugPortStr(JavaVersion javaVersion) {
        JavaVersion jv = javaVersion

        if (!jv) jv = JavaVersion.current()

        try {
            if (jv.isJava9Compatible() || jv.isJava10Compatible()) return "address=*:${this.getDebugPort()}"
        } catch (Throwable ignored) {
        }

        return "address=${this.getDebugPort()}"
    }
}
