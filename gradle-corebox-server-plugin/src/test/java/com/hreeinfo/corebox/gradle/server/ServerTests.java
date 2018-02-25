package com.hreeinfo.corebox.gradle.server;

import com.hreeinfo.corebox.gradle.server.runner.ServerRunnerOpt;
import com.hreeinfo.corebox.gradle.server.runner.impl.JettyServerRunner;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/24 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class ServerTests {
    private static List<String> getTestClassPaths() {
        String[] acps = StringUtils.split(System.getProperty("java.class.path"), ":");
        return Arrays.asList(acps);
    }

    private static String getTestResourcePath() {
        try {
            String path = System.getProperty("user.dir") + "/gradle-corebox-server-plugin/src/test/resources";
            if (new File(path).exists()) return path;

            path = System.getProperty("user.dir") + "/src/test/resources";
            if (new File(path).exists()) return path;

        } catch (Throwable e) {
        }

        throw new IllegalArgumentException("未找到项目 resources 目录");
    }

    private static String getTestWebappPath() {
        try {
            String path = System.getProperty("user.dir") + "/gradle-corebox-server-plugin/src/test/web";
            if (new File(path).exists()) return path;

            path = System.getProperty("user.dir") + "/src/test/web";
            if (new File(path).exists()) return path;

        } catch (Throwable e) {
        }

        throw new IllegalArgumentException("未找到项目 resources 目录");
    }

    public static void main(String[] args) throws Exception {
        StringBuilder sb = new StringBuilder();

        sb.append("--webappdir=").append(getTestWebappPath()).append(" ");

        List<String> cps = getTestClassPaths();
        for (String s : cps) sb.append("--classesdir=").append(s).append(" ");

        sb.append("--resourcesdir=").append(getTestResourcePath()).append(" ");

        JettyServerRunner runner = new JettyServerRunner(ServerRunnerOpt.loadFromOpts(StringUtils.split(sb.toString(), " ")));
        runner.start();
    }
}
