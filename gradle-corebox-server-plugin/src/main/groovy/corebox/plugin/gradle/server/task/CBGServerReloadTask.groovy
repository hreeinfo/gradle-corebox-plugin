package corebox.plugin.gradle.server.task

import corebox.plugin.gradle.server.CBGServers
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * EmbedServer 重新载入 运行中的 webapp
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/4/6 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CBGServerReloadTask extends DefaultTask {
    static final String NAME = "appReload"

    CBGServerReloadTask() {
        outputs.upToDateWhen { false }
    }


    @TaskAction
    void execReload() {
        CBGServers.forceDeleteServerReloadLockFile(this.project)
        logger.quiet "服务器 Webapp Context 重载命令已提交"
    }
}
