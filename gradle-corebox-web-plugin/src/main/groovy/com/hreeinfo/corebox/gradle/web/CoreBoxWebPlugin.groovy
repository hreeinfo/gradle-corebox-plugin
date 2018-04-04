package com.hreeinfo.corebox.gradle.web

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.WarPlugin

/**
 * CoreBoxWebPlugin - 提供 embed server 和 vaadin ui 相关的任务
 *
 * TODO 需要实现
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/18 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CoreBoxWebPlugin implements Plugin<Project> {
    static final String SERVER_EXTENSION_NAME = 'cbweb'

    @Override
    void apply(Project project) {
        if (project.plugins.findPlugin(WarPlugin) == null) project.plugins.apply(WarPlugin)

        CoreBoxWebExtension spe = project.extensions.findByType(CoreBoxWebExtension.class)

        if (spe == null) spe = project.extensions.create(SERVER_EXTENSION_NAME, CoreBoxWebExtension.class)
    }
}
