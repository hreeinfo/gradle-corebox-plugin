package corebox.plugin.gradle.server

import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.InvokerHelper

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/27 </p>
 * <p>版权所属：xingxiuyi </p>
 */
@Singleton
@Slf4j
class CBGServerInstance {
    public static final String SERVER_BUILDER_CLASS_NAME = 'com.hreeinfo.commons.embed.server.EmbedServer$Builder'
    private final Object lock = new Object()
    private final def builder = createServerBuilder()
    private volatile def server

    private static Class loadClass(String className) {
        ClassLoader classLoader = Thread.currentThread().contextClassLoader
        classLoader.loadClass(className)
    }

    private static Object createServerBuilder() throws RuntimeException {
        Class serverClass = loadClass(SERVER_BUILDER_CLASS_NAME)
        if (!serverClass) throw new IllegalArgumentException("未找到目标类 ${SERVER_BUILDER_CLASS_NAME}")
        try {
            log.debug "生成了类 ${serverClass}"
            Object o = InvokerHelper.invokeMethod(serverClass, "builder", new Object[0])
            if (o != null) return o
        } catch (Throwable e) {
            throw new IllegalStateException("无法生成目标对象 ${SERVER_BUILDER_CLASS_NAME} " + e.getMessage(), e)
        }
        throw new IllegalStateException("无法生成目标对象 ${SERVER_BUILDER_CLASS_NAME}")
    }

    Object callBuilder(String method, Object... params) {
        Object o = InvokerHelper.invokeMethod(this.builder, method, params)
        log.debug "调用 builder 方法 ${method} 获取返回 ${o}"
        return o
    }

    private Object callServer(String method, Object... params) {
        if (this.server == null) throw new IllegalStateException("server实例不存在")
        Object o = InvokerHelper.invokeMethod(this.server, method, params)
        log.debug "调用 server 方法 ${method} 获取返回 ${o}"
        return o
    }

    def initServer(String type, String typeClass) { // TODO 此处的实例通过type构建时会发生错误
        if (this.server == null) {
            synchronized (lock) {
                if (this.server == null) {
                    def initServer = null

                    if (typeClass) {
                        try {
                            Class edserverClass = loadClass(typeClass)
                            if (edserverClass) initServer = this.callBuilder("build", edserverClass)
                        } catch (Throwable e) {
                            log.error "initServer 构建指定目标类的服务 ${typeClass} 错误 " + e.getMessage(), e
                        }
                        if (!initServer) log.warn "initServer 指定目标类 ${typeClass} 无法构建服务"
                    }

                    if (!initServer) {
                        try {
                            initServer = this.callBuilder("build", type)
                        } catch (Throwable e) {
                            log.error "initServer 构建指定类型 ${type} 错误 " + e.getMessage(), e
                        }
                    }

                    this.server = initServer
                }
            }
        }

        return this.server
    }

    void start(ClassLoader loader, boolean daemon) {
        synchronized (lock) {
            if (this.server != null) this.callServer("start", loader, daemon)
        }
    }

    void stop() {
        if (this.server != null) this.callServer("stop")
    }

    boolean isRunning() {
        if (this.server != null) return this.callServer("isRunning")
        return false
    }
}
