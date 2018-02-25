package com.hreeinfo.corebox.gradle.server.runner.impl;

import com.hreeinfo.corebox.gradle.server.runner.ServerRunner;
import com.hreeinfo.corebox.gradle.server.runner.ServerRunnerOpt;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/19 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public abstract class BaseServerRunner implements ServerRunner {
    protected final ServerRunnerOpt opt;

    public BaseServerRunner(ServerRunnerOpt opt) {
        this.opt = opt;
    }
}
