package com.hreeinfo.corebox.gradle.web

import corebox.plugin.gradle.common.CBGs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.bundling.War

/**
 * CoreBoxWebPlugin
 *
 * 应用此插件后 如果 runtime 和 cbweb 依赖配置中存在 war 包，将会执行 war overlay 功能。
 * 其中 cbweb 配置的war将会覆盖 runtime 配置的同名文件。
 *
 *
 * 如果 warjars = true 将会把所有 cbweb 的jar压缩到 war 中，否则将仅作为 compileOnly
 *
 * 命令行属性 warjars 或 production 设置为true时与 warjars=true 一致
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/18 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CoreBoxWebPlugin implements Plugin<Project> {
    static final String WEBAPP_EXTENSION_NAME = 'cbweb'

    @Override
    void apply(Project project) {
        if (project.plugins.findPlugin(WarPlugin) == null) project.plugins.apply(WarPlugin)

        CoreBoxWebExtension spe = project.extensions.findByType(CoreBoxWebExtension.class)

        if (spe == null) spe = project.extensions.create(WEBAPP_EXTENSION_NAME, CoreBoxWebExtension.class)

        spe.warjars(CBGs.enableByProp(project, "warjars")
                || (CBGs.enableByProps(project, "product", "production", "productionMode") && !CBGs.diableByProp(project, "warjars")))


        if (!project.configurations.findByName(WEBAPP_EXTENSION_NAME)) {
            project.configurations.create(WEBAPP_EXTENSION_NAME).setVisible(false).setTransitive(true)
                    .setDescription("用于 cbweb 额外 的war依赖 用于 war overlay 功能")
        }

        project.tasks.withType(War, { War war ->
            war.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            war.doFirst {
                war.classpath = war.classpath.filter { !it.name.endsWith(".war") }
                war.project.configurations.findByName("runtime").each {
                    if (it.name.endsWith(".war")) {
                        def fileList = war.project.zipTree(it)
                        if (spe.warjars) {
                            war.from fileList
                        } else {
                            war.from fileList.matching { exclude "**/*.jar" }
                        }
                    }
                }
                war.project.configurations.findByName(WEBAPP_EXTENSION_NAME).each {
                    if (it.name.endsWith(".war")) {
                        def fileList = war.project.zipTree(it)
                        if (spe.warjars) {
                            war.from fileList
                        } else {
                            war.from fileList.matching { exclude "**/*.jar" }
                        }
                    }
                }
            }
        })

        project.afterEvaluate {
            if (project.configurations.findByName(WEBAPP_EXTENSION_NAME)) {

                project.sourceSets.main.compileClasspath += project.configurations.findByName(WEBAPP_EXTENSION_NAME).filter {
                    !it.name.endsWith(".war")
                }


                if (spe.warjars) project.tasks.withType(War, { War war ->
                    war.classpath += project.configurations.findByName(WEBAPP_EXTENSION_NAME).filter {
                        !it.name.endsWith(".war")
                    }
                })
            }

            if (spe.archiveName && !spe.archiveName.isEmpty()) project.tasks.withType(War, { War war ->
                war.setArchiveName(spe.archiveName)

                if (spe.classpaths) war.classpath.add(project.files(spe.classpaths))
            })

        }
    }
}