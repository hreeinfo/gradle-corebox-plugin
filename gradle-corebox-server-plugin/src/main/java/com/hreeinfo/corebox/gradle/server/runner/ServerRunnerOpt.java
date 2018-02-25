package com.hreeinfo.corebox.gradle.server.runner;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * ServerRunner 载入参数
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/24 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class ServerRunnerOpt implements Serializable {
    private String type = "TOMCAT";// 默认 tomcat jetty payara 忽略大小写
    private int port = 8080;
    private String context = "";
    private String webappdir = "";
    private List<String> classesdirs = new ArrayList<>();
    private List<String> resourcesdirs = new ArrayList<>();
    private String configfile = "";
    private String workdir = "";
    private String loglevel = "DEBUG";

    /**
     * 根据命令行参数构建配置对象
     *
     * @param args
     * @return
     */
    public static final ServerRunnerOpt loadFromOpts(String[] args) {
        OptionParser parser = new OptionParser();
        parser.accepts("type").withOptionalArg();
        parser.accepts("port").withOptionalArg().ofType(Integer.class);
        parser.accepts("context").withOptionalArg();
        parser.accepts("webappdir").withOptionalArg();
        parser.accepts("classesdir").withOptionalArg();
        parser.accepts("resourcesdir").withOptionalArg();
        parser.accepts("configfile").withOptionalArg();
        parser.accepts("workdir").withOptionalArg();
        parser.accepts("loglevel").withOptionalArg();


        ServerRunnerOpt sro = new ServerRunnerOpt();

        OptionSet options = parser.parse((args != null) ? args : new String[]{});
        if (options == null) return sro;

        sro.type = StringUtils.upperCase(optString(options, "type", "TOMCAT"));
        sro.port = optInteger(options, "port", 8080);
        sro.context = optString(options, "context", "");
        sro.webappdir = optString(options, "webappdir", "");
        sro.classesdirs.addAll(optString(options, "classesdir"));
        sro.resourcesdirs.addAll(optString(options, "resourcesdir"));
        sro.configfile = optString(options, "configfile", "");
        sro.workdir = optString(options, "workdir", "");
        sro.loglevel = StringUtils.upperCase(optString(options, "loglevel", "INFO"));

        return sro;
    }

    private static final String optString(OptionSet options, String name, String defaultValue) {
        if (options == null) return defaultValue;
        if (options.has(name)) {
            Object o = options.valueOf(name);
            if (o == null) return defaultValue;
            String s = o.toString();
            if (StringUtils.isBlank(s)) return defaultValue;
            return s;
        }
        return defaultValue;
    }

    private static final int optInteger(OptionSet options, String name, int defaultValue) {
        if (options == null) return defaultValue;
        if (options.has(name)) {
            Object o = options.valueOf(name);
            if (o == null) return defaultValue;

            int i = 0;
            if (o instanceof Number) i = ((Number) o).intValue();

            return (i == 0) ? defaultValue : i;
        }
        return defaultValue;
    }

    private static final List<String> optString(OptionSet options, String name) {
        List<String> list = new ArrayList<>();
        if (options == null) return list;
        if (options.has(name)) {
            List<?> objects = options.valuesOf(name);
            if (objects != null) for (Object o : objects) {
                if (o == null) continue;
                String s = o.toString();
                if (StringUtils.isBlank(s)) continue;
                list.add(StringUtils.trim(s));
            }
        }
        return list;
    }

    public String toParams() {
        StringBuilder sb = new StringBuilder();

        if (StringUtils.isNotBlank(this.type)) sb.append("--type=").append(this.type).append(" ");
        if (this.port > 0) sb.append("--port=").append(this.port).append(" ");
        if (StringUtils.isNotBlank(this.context)) sb.append("--context=").append(this.context).append(" ");
        if (StringUtils.isNotBlank(this.webappdir)) sb.append("--webappdir=").append(this.webappdir).append(" ");
        for (String s : this.classesdirs) {
            if (StringUtils.isNotBlank(s)) sb.append("--classesdir=").append(s).append(" ");
        }
        for (String s : this.classesdirs) {
            if (StringUtils.isNotBlank(s)) sb.append("--resourcesdir=").append(s).append(" ");
        }
        if (StringUtils.isNotBlank(this.configfile)) sb.append("--configfile=").append(this.configfile).append(" ");
        if (StringUtils.isNotBlank(this.workdir)) sb.append("--workdir=").append(this.workdir).append(" ");
        if (StringUtils.isNotBlank(this.loglevel)) sb.append("--loglevel=").append(this.loglevel).append(" ");

        return sb.toString();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getWebappdir() {
        return webappdir;
    }

    public void setWebappdir(String webappdir) {
        this.webappdir = webappdir;
    }

    public List<String> getClassesdirs() {
        return classesdirs;
    }

    public void setClassesdirs(List<String> classesdirs) {
        this.classesdirs = classesdirs;
    }

    public List<String> getResourcesdirs() {
        return resourcesdirs;
    }

    public void setResourcesdirs(List<String> resourcesdirs) {
        this.resourcesdirs = resourcesdirs;
    }

    public String getConfigfile() {
        return configfile;
    }

    public void setConfigfile(String configfile) {
        this.configfile = configfile;
    }

    public String getWorkdir() {
        return workdir;
    }

    public void setWorkdir(String workdir) {
        this.workdir = workdir;
    }

    public String getLoglevel() {
        return loglevel;
    }

    public void setLoglevel(String loglevel) {
        this.loglevel = loglevel;
    }

    @Override
    public String toString() {
        return "ServerRunnerOpt{" +
                "type='" + type + '\'' +
                ", port=" + port +
                ", context='" + context + '\'' +
                ", webappdir='" + webappdir + '\'' +
                ", classesdirs=" + classesdirs +
                ", resourcesdirs=" + resourcesdirs +
                ", configfile='" + configfile + '\'' +
                ", workdir='" + workdir + '\'' +
                ", loglevel='" + loglevel + '\'' +
                '}';
    }

    public static void main(String[] args) {
        ServerRunnerOpt opt = loadFromOpts(StringUtils.split(
                "--type=aaa --port=222 --context=sss --loglevel=sssss --resourcesdir=a --resourcesdir=bbbb/sss",
                " "));
        System.out.println(opt);

        System.out.println(opt.toParams());
    }
}
