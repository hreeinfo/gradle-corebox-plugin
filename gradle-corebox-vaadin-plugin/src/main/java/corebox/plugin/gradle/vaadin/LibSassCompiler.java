package corebox.plugin.gradle.vaadin;


import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/18 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class LibSassCompiler {
    private static final Logger LOG = Logger.getLogger(LibSassCompiler.class.getName());

    // 使用方法: 'LibSassCompiler [scss] [css] [unpackedThemes]
    public static void main(String[] args) throws Exception {
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        if (!outputFile.exists() && !outputFile.createNewFile()) {
            throw new IllegalStateException("无法创建输出文件 " + outputFile.getAbsolutePath());
        }

        File sourceMapFile = new File(args[1] + ".map");
        if (!sourceMapFile.exists() && !sourceMapFile.createNewFile()) {
            throw new IllegalStateException("无法创建Map文件 " + sourceMapFile.getAbsolutePath());
        }

        File unpackedThemes = new File(args[2]);
        File unpackedInputFile = Paths.get(unpackedThemes.getCanonicalPath(), inputFile.getParentFile().getName(), inputFile.getName()).toFile();

        Compiler compiler = new Compiler();
        Options options = new Options();

        try {
            Output output = compiler.compileFile(unpackedInputFile.toURI(), outputFile.toURI(), options);
            FileUtils.write(outputFile, output.getCss(), StandardCharsets.UTF_8.name());
            FileUtils.write(sourceMapFile, output.getSourceMap(), StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            if (!outputFile.delete()) LOG.warning("无法删除文件 " + outputFile.getAbsolutePath());
            if (!sourceMapFile.delete()) LOG.warning("无法删除文件 " + sourceMapFile.getAbsolutePath());
            LOG.severe("执行过程错误 " + e.getMessage());
            throw e;
        }
    }

}
