package com.hreeinfo.corebox.gradle.web.tasks

import com.hreeinfo.corebox.gradle.web.CoreBoxWebExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/19 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class DemoTask extends DefaultTask{
    @Input
    boolean serverEnabled = false

    @TaskAction
    protected void start() {
        CoreBoxWebExtension spe = project.getExtensions().findByType(CoreBoxWebExtension.class)
        println "当前定变量为 serverEnabled=${getServerEnabled()}"
        println "当前定变量为 spe=${spe.dump()}"
    }
}
