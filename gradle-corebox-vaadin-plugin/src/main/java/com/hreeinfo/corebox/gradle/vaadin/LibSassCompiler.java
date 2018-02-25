package com.hreeinfo.corebox.gradle.vaadin;


import io.bit3.jsass.CompilationException;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;


/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/19 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class LibSassCompiler {

    // Usage: 'LibSassCompiler [scss] [css] [unpackedThemes]
    public static void main(String[] args) throws Exception {
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }

        File sourceMapFile = new File(args[1]+".map");
        if(!sourceMapFile.exists()) {
            sourceMapFile.createNewFile();
        }

        File unpackedThemes = new File(args[2]);
        File unpackedInputFile = Paths.get(
                unpackedThemes.getCanonicalPath(),
                inputFile.getParentFile().getName(),
                inputFile.getName()).toFile();

        Compiler compiler = new Compiler();
        Options options = new Options();

        try {
            Output output = compiler.compileFile(unpackedInputFile.toURI(), outputFile.toURI(), options);
            FileUtils.write(outputFile, output.getCss(), StandardCharsets.UTF_8.name());
            FileUtils.write(sourceMapFile, output.getSourceMap(), StandardCharsets.UTF_8.name());
        } catch (CompilationException e) {
            outputFile.delete();
            sourceMapFile.delete();
            throw e;
        }
    }

}
