package com.hreeinfo.corebox.gradle.web

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/18 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CoreBoxWebExtension {
    Boolean serverEnabled

    Boolean vaadinEnabled

    /**
     * 是否启用server相关配置
     * @param enabled
     */
    void server(boolean enabled) {
        println "设置了 server=${enabled}"
        this.serverEnabled = enabled
    }

    /**
     * 是否启用vaadin相关配置
     * @param enabled
     */
    void vaadin(boolean enabled) {
        println "设置了 vaadin=${enabled}"
        this.vaadinEnabled = enabled
    }
}
