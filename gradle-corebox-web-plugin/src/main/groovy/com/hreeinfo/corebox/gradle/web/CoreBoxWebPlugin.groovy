package com.hreeinfo.corebox.gradle.web

import com.hreeinfo.corebox.gradle.web.tasks.DemoTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.WarPlugin

/**
 * CoreBoxWebPlugin - 提供 embed server 和 vaadin ui 相关的任务
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/18 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CoreBoxWebPlugin implements Plugin<Project> {
    static final String SERVER_EXTENSION_NAME = 'cbweb'
    static final String SERVER_RUN_TASK_NAME = 'serverRun'
    static final String SERVER_RUNWAR_TASK_NAME = 'serverRunWar'
    static final String SERVER_STOP_TASK_NAME = 'serverStop'

    @Override
    void apply(Project project) {
        if (project.plugins.findPlugin(WarPlugin) == null) project.plugins.apply(WarPlugin)

        CoreBoxWebExtension spe = project.extensions.findByType(CoreBoxWebExtension.class)

        if (spe == null) spe = project.extensions.create(SERVER_EXTENSION_NAME, CoreBoxWebExtension.class)

        println "Apply 测试测试 ${spe.dump()}"

        // 配置 Server
        this.configServerTasks(project, spe)

        // 配置 Vaadin
        this.configVaadinTasks(project, spe)
    }

    /**
     * 配置 Server 相关的任务
     * @param project
     * @param spe
     */
    private void configServerTasks(Project project, CoreBoxWebExtension spe) {
        project.tasks.withType(DemoTask) {
            println "Config 测试测试 ${spe.dump()}"
            conventionMapping.map("serverEnabled") { spe.serverEnabled }
        }

        project.tasks.create("demo", DemoTask) {

            println "Config 测试测试 ${spe.dump()}"
            conventionMapping.map("serverEnabled") { spe.serverEnabled }
        }
    }

    /**
     * 配置 vaadin 相关的任务
     * @param project
     * @param spe
     */
    private void configVaadinTasks(Project project, CoreBoxWebExtension spe) {
    }
}
