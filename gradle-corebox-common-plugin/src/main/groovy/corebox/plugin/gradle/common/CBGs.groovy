package corebox.plugin.gradle.common

import groovy.transform.Memoized
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project

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

    /**
     * 针对进程做日志记录
     * @param project
     * @param process
     * @param filename
     * @param monitor
     */
    static void logProcess(Project project, Process process, boolean logToConsole, String filename, Closure monitor) {
        logProcess(project, process, logToConsole, false, filename, monitor)
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
    static void logProcess(Project project, Process process, boolean logToConsole, boolean directPrint, String filename, Closure monitor) {
        if (logToConsole) {
            logProcessToConsole(project, process, directPrint, monitor)
        } else {
            logProcessToFile(project, process, directPrint, filename, monitor)
        }
    }

    /**
     * 针对进程做日志记录 写入到文件
     * @param project
     * @param process
     * @param filename
     * @param monitor
     */
    static void logProcessToFile(final Project project, final Process process, final boolean directPrint,
                                 final String filename, Closure monitor = {}) {
        File logDir = project.file("$project.buildDir/logs/")
        logDir.mkdirs()

        final File LOGFILE = new File(logDir, filename)
        project.logger.info("记录日志到文件 $LOGFILE")

        THREAD_POOL.submit {
            LOGFILE.withWriterAppend { out ->
                try {
                    boolean errorOccurred = false

                    process.inputStream.eachLine { output ->
                        monitor.call(output)

                        if (directPrint) {
                            out.println output
                            out.flush()
                        } else {
                            if (output.contains(WARNING_LOG_MARKER)) out.println WARNING_LOG_MARKER + SPACE + output.replace(WARNING_LOG_MARKER, '').trim()
                            else if (output.contains(ERROR_LOG_MARKER)) {
                                errorOccurred = true
                                out.println ERROR_LOG_MARKER + SPACE + output.replace(ERROR_LOG_MARKER, '').trim()
                            } else out.println INFO_LOG_MARKER + SPACE + output.trim()


                            out.flush()
                            // 错误发生时 记录所有信息到控制台
                            if (errorOccurred) project.logger.error(output.replace(ERROR_LOG_MARKER, '').trim())
                        }


                    }
                } catch (IOException e) {
                    project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
                }
            }
        }

        THREAD_POOL.submit {
            LOGFILE.withWriterAppend { out ->
                try {
                    process.errorStream.eachLine { output ->
                        monitor.call(output)
                        out.println ERROR_LOG_MARKER + SPACE + output.replace(ERROR_LOG_MARKER, '').trim()
                        out.flush()
                    }
                } catch (IOException e) {
                    project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
                }
            }
        }
    }

    /**
     * 针对进程做日志记录 写入到控制台
     * @param project
     * @param process
     * @param monitor
     */
    static void logProcessToConsole(final Project project, final Process process, final boolean directPrint,
                                    final Closure monitor = {}) {
        project.logger.info("记录日志到控制台")

        THREAD_POOL.submit {
            try {
                boolean errorOccurred = false
                process.inputStream.eachLine { output ->
                    monitor.call(output)

                    if (directPrint) {
                        println output
                    } else {
                        if (output.contains(WARNING_LOG_MARKER)) project.logger.warn(output.replace(WARNING_LOG_MARKER, '').trim())
                        else if (output.contains(ERROR_LOG_MARKER)) errorOccurred = true
                        else project.logger.info(output.trim())
                        // 错误发生时 记录所有信息到控制台
                        if (errorOccurred) project.logger.error(output.replace(ERROR_LOG_MARKER, '').trim())
                    }

                }
            } catch (IOException e) {
                project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
            }
        }

        THREAD_POOL.submit {
            try {
                process.errorStream.eachLine { String output ->
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
     * @return
     */
    @Memoized
    static String getJavaBinary(Project project) {
        String javaHome
        if (project.hasProperty(GRADLE_HOME)) {
            javaHome = project.properties[GRADLE_HOME]
        } else if (System.getProperty(JAVA_HOME)) {
            javaHome = System.getProperty(JAVA_HOME)
        }

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
}
