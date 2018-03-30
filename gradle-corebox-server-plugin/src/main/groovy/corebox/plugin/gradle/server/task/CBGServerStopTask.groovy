package corebox.plugin.gradle.server.task

import corebox.plugin.gradle.server.CBGServers
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/28 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CBGServerStopTask extends DefaultTask {
    static final String TASK_NAME = "appStop"

    CBGServerStopTask() {
        outputs.upToDateWhen { false }
    }


    @TaskAction
    void stop() {
        try {
            Project project = this.getProject()
            if (![project]) return

            final File lockFile = CBGServers.runningServerLockFile(project)
            if (lockFile == null) return

            if (lockFile.exists()) lockFile.delete()

            logger.quiet "服务器停止命令已提交"
        } catch (Exception e) {
            logger.error "停止服务发生错误" + e.getMessage(), e
        }
    }
}
