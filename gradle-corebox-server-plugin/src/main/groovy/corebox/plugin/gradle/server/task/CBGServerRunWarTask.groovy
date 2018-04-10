package corebox.plugin.gradle.server.task
/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/20 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CBGServerRunWarTask extends CBGServerBaseTask {
    static final String TASK_NAME = "appRunWar"

    @Override
    protected String getProcessWebapp() {
        return null
    }

    @Override
    protected Set<String> getProcessServerClasspaths(String pWebappDir) {
        return null
    }
}
