package corebox.plugin.gradle.common

import groovy.transform.Memoized
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project

import java.nio.charset.Charset
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/4/3 </p>
 * <p>版权所属：xingxiuyi </p>
 */
class CBGs {

    private static final String GRADLE_HOME = "org.gradle.java.home"
    private static final String JAVA_HOME = "java.home"
    private static final String JAVA_BIN_NAME = "java"

    private static final String PLUS = '+'
    private static final String DOT = '.'
    private static final String SPACE = ' '
    private static final String WARNING_LOG_MARKER = '[WARN]'
    private static final String ERROR_LOG_MARKER = '[ERROR]'
    private static final String INFO_LOG_MARKER = '[INFO]'
    private static final String STREAM_CLOSED_LOG_MESSAGE = 'Stream was closed'

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1)
    static final Executor THREAD_POOL = Executors.newCachedThreadPool({ Runnable r ->
        new Thread(r, "gradle-corebox-plugin-thread-${THREAD_COUNTER.getAndIncrement()}")
    })

    static final String ENV_COREBOX_JAVA_HOME = "COREBOX_JAVA_HOME"

    /**
     * 针对进程做日志记录
     * @param project
     * @param process
     * @param filename
     * @param monitor
     */
    static void logProcess(Project project, Process process, boolean logToConsole, String filename, Closure monitor) {
        logProcess(project, process, logToConsole, null, filename, monitor)
    }

    /**
     * 针对进程做日志记录
     * @param project
     * @param process
     * @param logToConsole
     * @param directPrint
     * @param filename
     * @param monitor
     */
    static File logProcess(Project project, Process process, boolean logToConsole,
                           String charset, String filename, Closure monitor) {
        if (!charset) charset = Charset.defaultCharset().name()
        if (logToConsole) {
            logProcessToConsole(project, process, charset, monitor)
        } else {
            return logProcessToFile(project, process, charset, filename, monitor)
        }
        return null
    }

    /**
     * 针对进程做日志记录 写入到文件
     * @param project
     * @param process
     * @param filename
     * @param monitor
     */
    static File logProcessToFile(final Project project, final Process process,
                                 final String charset, final String filename, Closure monitor = {}) {
        File logDir = project.file("$project.buildDir/logs/")
        logDir.mkdirs()

        final File LOGFILE = new File(logDir, filename)
        project.logger.info("记录日志到文件 $LOGFILE")

        THREAD_POOL.submit {
            LOGFILE.withWriterAppend { out ->
                try {
                    process.inputStream.eachLine(charset) { output ->
                        monitor.call(output)
                        out.println output
                        out.flush()
                        // 错误发生时 记录所有信息到控制台
                        if (output.contains(ERROR_LOG_MARKER)) project.logger.error(output.replace(ERROR_LOG_MARKER, '').trim())
                    }
                } catch (IOException e) {
                    project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
                }
            }
        }

        THREAD_POOL.submit {
            LOGFILE.withWriterAppend { out ->
                try {
                    process.errorStream.eachLine(charset) { output ->
                        monitor.call(output)
                        out.println output
                        out.flush()
                    }
                } catch (IOException e) {
                    project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
                }
            }
        }
        return LOGFILE
    }

    /**
     * 针对进程做日志记录 写入到控制台
     * @param project
     * @param process
     * @param monitor
     */
    static void logProcessToConsole(final Project project, final Process process,
                                    final String charset, final Closure monitor = {}) {
        project.logger.info("记录日志到控制台")

        THREAD_POOL.submit {
            try {
                process.inputStream.eachLine(charset) { output ->
                    monitor.call(output)

                    println output

                    if (output.contains(ERROR_LOG_MARKER)) project.logger.error(output.replace(ERROR_LOG_MARKER, '').trim())
                }
            } catch (IOException e) {
                project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
            }
        }

        THREAD_POOL.submit {
            try {
                process.errorStream.eachLine(charset) { String output ->
                    monitor.call(output)
                    project.logger.error(output.replace(ERROR_LOG_MARKER, '').trim())
                }
            } catch (IOException e) {
                project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
            }
        }
    }

    /**
     * 返回JavaBinary
     *
     * 可以定义目标全局环境变量 COREBOX_JAVA_HOME 指向独立java环境
     *
     * 如未定义 分别查找 org.gradle.java.home 和 java.home
     *
     * @return
     */
    @Memoized
    static String getJavaBinary(Project project) {
        String javaHome = System.getenv(ENV_COREBOX_JAVA_HOME)

        if (!javaHome && System.hasProperty(ENV_COREBOX_JAVA_HOME)) javaHome = System.getProperty(ENV_COREBOX_JAVA_HOME)
        if (!javaHome && project.hasProperty(GRADLE_HOME)) javaHome = project.properties[GRADLE_HOME]
        if (!javaHome) javaHome = System.getProperty(JAVA_HOME)

        if (javaHome) {
            File javaBin = new File(javaHome, "bin")
            File java = new File(javaBin, JAVA_BIN_NAME)
            return java.canonicalPath
        }

        // Fallback to Java on PATH with a warning
        project.logger.warn("没有检测到 Java JRE 请确认 JAVA_HOME 是否设置？")
        JAVA_BIN_NAME
    }

    static String convertFilePathToFQN(String path, String postfix) {
        StringUtils.removeEnd(path, postfix).replace(File.separator, DOT)
    }

    static String convertFQNToFilePath(String fqn, String postfix = '') {
        fqn.replace(DOT, File.separator) + postfix
    }

    /**
     * 返回系统变量 此方法不会返回空 如未定义系统变量则返回 []
     * @return
     */
    static List<String> getSystemEnvs() {
        List<String> senvs = []
        try {
            Map<String, String> envs = System.getenv()
            if (!envs) return senvs

            envs.each { k, v ->
                if (k) senvs.add("${k}=${v}")
            }
        } catch (Throwable ignored) {
        }

        return senvs
    }

    static List<String> runProcess(Project project, List process, String charset) {
        List<String> values = []
        if (!process) return values
        final ProcessResultReader stderr
        final ProcessResultReader stdout

        try {
            if (!charset) charset = Charset.defaultCharset().name()

            Process proc = process.execute(getSystemEnvs(), project.getBuildDir())
            if (!proc) return values


            stderr = new ProcessResultReader(proc.getErrorStream(), "STDERR", charset);
            stdout = new ProcessResultReader(proc.getInputStream(), "STDOUT", charset);

            stderr.start()
            stdout.start()

            proc.waitFor()

            values.addAll(stdout.output())
            values.addAll(stderr.output())
        } catch (Throwable e) {
            println("运行命令发生错误 ${process} 错误信息为 ${e.getMessage()}")
        } finally {
            if (stderr) {
                try {
                    if (stderr.isAlive()) stderr.interrupt()
                } catch (Throwable e) {
                }
            }

            if (stdout) {
                try {
                    if (stdout.isAlive()) stderr.interrupt()
                } catch (Throwable e) {
                }
            }
        }
        return values
    }

    private static class ProcessResultReader extends Thread {
        final InputStream is
        final String type
        final List<String> sb
        final String charset

        ProcessResultReader(final InputStream is, String type, String charset) {
            this.is = is
            this.type = type
            this.sb = []
            this.charset = charset ? charset : Charset.defaultCharset().name()
        }

        public void run() {
            try {
                final InputStreamReader isr = new InputStreamReader(is, charset)
                final BufferedReader br = new BufferedReader(isr)
                String line = null
                while ((line = br.readLine()) != null) {
                    this.sb.add(line)
                }
            } catch (final Throwable ioe) {
            }
        }

        public List<String> output() {
            return this.sb
        }
    }
}
