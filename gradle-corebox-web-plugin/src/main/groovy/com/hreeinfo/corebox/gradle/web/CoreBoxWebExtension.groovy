package com.hreeinfo.corebox.gradle.web

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/18 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CoreBoxWebExtension {
    Boolean warjars = Boolean.FALSE
    String archiveName
    Set<File> classpaths = []

    public warjars(boolean warjars) {
        this.warjars = warjars
    }

    public archiveName(String archiveName) { this.archiveName = archiveName }

    public classpath(File classpath) { this.classpaths.add(classpath) }
}
