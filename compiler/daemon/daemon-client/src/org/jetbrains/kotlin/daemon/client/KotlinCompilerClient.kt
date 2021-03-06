/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon.client

import net.rubygrapefruit.platform.Native
import net.rubygrapefruit.platform.ProcessLauncher
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.rmi.server.UnicastRemoteObject
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.comparisons.compareByDescending
import kotlin.concurrent.thread

class CompilationServices(
        val incrementalCompilationComponents: IncrementalCompilationComponents? = null,
        val compilationCanceledStatus: CompilationCanceledStatus? = null
)


object KotlinCompilerClient {

    val DAEMON_DEFAULT_STARTUP_TIMEOUT_MS = 10000L
    val DAEMON_CONNECT_CYCLE_ATTEMPTS = 3

    val verboseReporting = System.getProperty(COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY) != null


    fun connectToCompileService(compilerId: CompilerId,
                                daemonJVMOptions: DaemonJVMOptions,
                                daemonOptions: DaemonOptions,
                                reportingTargets: DaemonReportingTargets,
                                autostart: Boolean = true,
                                checkId: Boolean = true
    ): CompileService? {
        val flagFile = System.getProperty(COMPILE_DAEMON_CLIENT_ALIVE_PATH_PROPERTY)
                     ?.let(String::trimQuotes)
                     ?.check { !it.isBlank() }
                     ?.let(::File)
                     ?.check(File::exists)
                     ?: makeAutodeletingFlagFile()
        return connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, reportingTargets, autostart)
    }

    fun connectToCompileService(compilerId: CompilerId,
                                clientAliveFlagFile: File,
                                daemonJVMOptions: DaemonJVMOptions,
                                daemonOptions: DaemonOptions,
                                reportingTargets: DaemonReportingTargets,
                                autostart: Boolean = true
    ): CompileService? {

        var attempts = 0
        try {
            while (attempts++ < DAEMON_CONNECT_CYCLE_ATTEMPTS) {
                val (service, newJVMOptions) = tryFindSuitableDaemonOrNewOpts(File(daemonOptions.runFilesPath), compilerId, daemonJVMOptions, { cat, msg -> reportingTargets.report(cat, msg) })
                if (service != null) {
                    // the newJVMOptions could be checked here for additional parameters, if needed
                    service.registerClient(clientAliveFlagFile.absolutePath)
                    reportingTargets.report(DaemonReportCategory.DEBUG, "connected to the daemon")
                    return service
                }
                reportingTargets.report(DaemonReportCategory.DEBUG, "no suitable daemon found")
                if (autostart) {
                    startDaemon(compilerId, newJVMOptions, daemonOptions, reportingTargets)
                    reportingTargets.report(DaemonReportCategory.DEBUG, "new daemon started, trying to find it")
                }
            }
        }
        catch (e: Throwable) {
            reportingTargets.report(DaemonReportCategory.EXCEPTION, e.toString())
        }
        return null
    }


    fun shutdownCompileService(compilerId: CompilerId, daemonOptions: DaemonOptions): Unit {
        connectToCompileService(compilerId, DaemonJVMOptions(), daemonOptions, DaemonReportingTargets(out = System.out), autostart = false, checkId = false)
                ?.shutdown()
    }


    fun shutdownCompileService(compilerId: CompilerId): Unit {
        shutdownCompileService(compilerId, DaemonOptions())
    }


    fun compile(compilerService: CompileService,
                sessionId: Int,
                targetPlatform: CompileService.TargetPlatform,
                args: Array<out String>,
                out: OutputStream,
                port: Int = SOCKET_ANY_FREE_PORT,
                operationsTracer: RemoteOperationsTracer? = null
    ): Int {
        val outStrm = RemoteOutputStreamServer(out, port = port)
        return compilerService.remoteCompile(sessionId, targetPlatform, args, CompilerCallbackServicesFacadeServer(port = port), outStrm, CompileService.OutputFormat.PLAIN, outStrm, operationsTracer).get()
    }


    fun incrementalCompile(compileService: CompileService,
                           sessionId: Int,
                           targetPlatform: CompileService.TargetPlatform,
                           args: Array<out String>,
                           callbackServices: CompilationServices,
                           compilerOut: OutputStream,
                           daemonOut: OutputStream,
                           port: Int = SOCKET_ANY_FREE_PORT,
                           profiler: Profiler = DummyProfiler(),
                           operationsTracer: RemoteOperationsTracer? = null
    ): Int = profiler.withMeasure(this) {
            compileService.remoteIncrementalCompile(
                    sessionId,
                    targetPlatform,
                    args,
                    CompilerCallbackServicesFacadeServer(incrementalCompilationComponents = callbackServices.incrementalCompilationComponents,
                                                         compilationCanceledStatus = callbackServices.compilationCanceledStatus,
                                                         port = port),
                    RemoteOutputStreamServer(compilerOut, port),
                    CompileService.OutputFormat.XML,
                    RemoteOutputStreamServer(daemonOut, port),
                    operationsTracer).get()
    }

    fun compile(compilerService: CompileService,
                sessionId: Int,
                targetPlatform: CompileService.TargetPlatform,
                args: Array<out String>,
                messageCollector: MessageCollector,
                outputsCollector: ((File, List<File>) -> Unit)? = null,
                compilerMode: CompilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
                reportSeverity: ReportSeverity = ReportSeverity.INFO,
                port: Int = SOCKET_ANY_FREE_PORT,
                profiler: Profiler = DummyProfiler()
    ): Int = profiler.withMeasure(this) {
        val services = BasicCompilerServicesWithResultsFacadeServer(messageCollector, outputsCollector, port)
        compilerService.compile(
                sessionId,
                args,
                CompilationOptions(
                        compilerMode,
                        targetPlatform,
                        arrayOf(ReportCategory.COMPILER_MESSAGE.code, ReportCategory.DAEMON_MESSAGE.code, ReportCategory.EXCEPTION.code, ReportCategory.OUTPUT_MESSAGE.code),
                        reportSeverity.code,
                        emptyArray()),
                services,
                null
        ).get()
    }

    val COMPILE_DAEMON_CLIENT_OPTIONS_PROPERTY: String = "kotlin.daemon.client.options"
    data class ClientOptions(
            var stop: Boolean = false
    ) : OptionsGroup {
        override val mappers: List<PropMapper<*, *, *>>
            get() = listOf(BoolPropMapper(this, ClientOptions::stop))
    }

    private fun configureClientOptions(opts: ClientOptions): ClientOptions {
        System.getProperty(COMPILE_DAEMON_CLIENT_OPTIONS_PROPERTY)?.let {
            val unrecognized = it.trimQuotes().split(",").filterExtractProps(opts.mappers, "")
            if (unrecognized.any())
                throw IllegalArgumentException(
                        "Unrecognized client options passed via property ${COMPILE_DAEMON_OPTIONS_PROPERTY}: " + unrecognized.joinToString(" ") +
                        "\nSupported options: " + opts.mappers.joinToString(", ", transform = { it.names.first() }))
        }
        return opts
    }

    private fun configureClientOptions(): ClientOptions = configureClientOptions(ClientOptions())


    @JvmStatic
    fun main(vararg args: String) {
        val compilerId = CompilerId()
        val daemonOptions = configureDaemonOptions()
        val daemonLaunchingOptions = configureDaemonJVMOptions(inheritMemoryLimits = true, inheritAdditionalProperties = true)
        val clientOptions = configureClientOptions()
        val filteredArgs = args.asIterable().filterExtractProps(compilerId, daemonOptions, daemonLaunchingOptions, clientOptions, prefix = COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX)

        if (!clientOptions.stop) {
            if (compilerId.compilerClasspath.none()) {
                // attempt to find compiler to use
                System.err.println("compiler wasn't explicitly specified, attempt to find appropriate jar")
                detectCompilerClasspath()
                        ?.let { compilerId.compilerClasspath = it }
            }
            if (compilerId.compilerClasspath.none())
                throw IllegalArgumentException("Cannot find compiler jar")
            else
                println("desired compiler classpath: " + compilerId.compilerClasspath.joinToString(File.pathSeparator))
        }

        val daemon = connectToCompileService(compilerId, daemonLaunchingOptions, daemonOptions, DaemonReportingTargets(out = System.out), autostart = !clientOptions.stop, checkId = !clientOptions.stop)

        if (daemon == null) {
            if (clientOptions.stop) {
                System.err.println("No daemon found to shut down")
            }
            else throw Exception("Unable to connect to daemon")
        }
        else when {
            clientOptions.stop -> {
                println("Shutdown the daemon")
                daemon.shutdown()
                println("Daemon shut down successfully")
            }
            filteredArgs.none() -> {
                // so far used only in tests
                println("Warning: empty arguments list, only daemon check is performed: checkCompilerId() returns ${daemon.checkCompilerId(compilerId)}")
            }
            else -> {
                println("Executing daemon compilation with args: " + filteredArgs.joinToString(" "))
                val outStrm = RemoteOutputStreamServer(System.out)
                val servicesFacade = CompilerCallbackServicesFacadeServer()
                try {
                    val memBefore = daemon.getUsedMemory().get() / 1024
                    val startTime = System.nanoTime()

                    val res = daemon.remoteCompile(CompileService.NO_SESSION, CompileService.TargetPlatform.JVM, filteredArgs.toList().toTypedArray(), servicesFacade, outStrm, CompileService.OutputFormat.PLAIN, outStrm, null)

                    val endTime = System.nanoTime()
                    println("Compilation ${if (res.isGood) "succeeded" else "failed"}, result code: ${res.get()}")
                    val memAfter = daemon.getUsedMemory().get() / 1024
                    println("Compilation time: " + TimeUnit.NANOSECONDS.toMillis(endTime - startTime) + " ms")
                    println("Used memory $memAfter (${"%+d".format(memAfter - memBefore)} kb)")
                }
                finally {
                    // forcing RMI to unregister all objects and stop
                    UnicastRemoteObject.unexportObject(servicesFacade, true)
                    UnicastRemoteObject.unexportObject(outStrm, true)
                }
            }
        }
    }

    fun detectCompilerClasspath(): List<String>? =
            System.getProperty("java.class.path")
            ?.split(File.pathSeparator)
            ?.map { File(it).parentFile }
            ?.distinct()
            ?.mapNotNull {
                it?.walk()
                        ?.firstOrNull { it.name.equals(COMPILER_JAR_NAME, ignoreCase = true) }
            }
            ?.firstOrNull()
            ?.let { listOf(it.absolutePath) }

    // --- Implementation ---------------------------------------

    private fun DaemonReportingTargets.report(category: DaemonReportCategory, message: String, source: String = "daemon client") {
        if (category == DaemonReportCategory.DEBUG && !verboseReporting) return
        out?.println("[$source] ${category.name}: $message")
        messages?.add(DaemonReportMessage(category, "[$source] $message"))
        messageCollector?.let {
            when (category) {
                DaemonReportCategory.DEBUG -> it.report(CompilerMessageSeverity.LOGGING, message, CompilerMessageLocation.NO_LOCATION)
                DaemonReportCategory.INFO -> it.report(CompilerMessageSeverity.INFO, message, CompilerMessageLocation.NO_LOCATION)
                DaemonReportCategory.EXCEPTION -> it.report(CompilerMessageSeverity.EXCEPTION, message, CompilerMessageLocation.NO_LOCATION)
            }
        }
        compilerServices?.let {
                when (category) {
                    DaemonReportCategory.DEBUG -> it.report(ReportCategory.DAEMON_MESSAGE, ReportSeverity.DEBUG, message, source)
                    DaemonReportCategory.INFO -> it.report(ReportCategory.DAEMON_MESSAGE, ReportSeverity.INFO, message, source)
                    DaemonReportCategory.EXCEPTION -> it.report(ReportCategory.EXCEPTION, ReportSeverity.ERROR, message, source)
                }
        }
    }

    private fun tryFindSuitableDaemonOrNewOpts(registryDir: File, compilerId: CompilerId, daemonJVMOptions: DaemonJVMOptions, report: (DaemonReportCategory, String) -> Unit): Pair<CompileService?, DaemonJVMOptions> {
        val aliveWithOpts = walkDaemons(registryDir, compilerId, report = report)
                .map { Pair(it, it.getDaemonJVMOptions()) }
                .filter { it.second.isGood }
                .sortedWith(compareByDescending(DaemonJVMOptionsMemoryComparator(), { it.second.get() }))
        val optsCopy = daemonJVMOptions.copy()
        // if required options fit into fattest running daemon - return the daemon and required options with memory params set to actual ones in the daemon
        return aliveWithOpts.firstOrNull()?.check { daemonJVMOptions memorywiseFitsInto it.second.get() }?.let {
                Pair(it.first, optsCopy.updateMemoryUpperBounds(it.second.get()))
            }
            // else combine all options from running daemon to get fattest option for a new daemon to run
            ?: Pair(null, aliveWithOpts.fold(optsCopy, { opts, d -> opts.updateMemoryUpperBounds(d.second.get()) }))
    }


    private fun startDaemon(compilerId: CompilerId, daemonJVMOptions: DaemonJVMOptions, daemonOptions: DaemonOptions, reportingTargets: DaemonReportingTargets) {
        val javaExecutable = File(File(System.getProperty("java.home"), "bin"), "java")
        val platformSpecificOptions = listOf("-Djava.awt.headless=true") // hide daemon window
        val args = listOf(
                   javaExecutable.absolutePath, "-cp", compilerId.compilerClasspath.joinToString(File.pathSeparator)) +
                   platformSpecificOptions +
                   daemonJVMOptions.mappers.flatMap { it.toArgs("-") } +
                   COMPILER_DAEMON_CLASS_FQN +
                   daemonOptions.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) } +
                   compilerId.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) }
        reportingTargets.report(DaemonReportCategory.DEBUG, "starting the daemon as: " + args.joinToString(" "))
        val processBuilder = ProcessBuilder(args)
        processBuilder.redirectErrorStream(true)
        // assuming daemon process is deaf and (mostly) silent, so do not handle streams
        val daemonLauncher = Native.get(ProcessLauncher::class.java)
        val daemon = daemonLauncher.start(processBuilder)

        val isEchoRead = Semaphore(1)
        isEchoRead.acquire()

        val stdoutThread =
                thread {
                    try {
                        daemon.inputStream
                                .reader()
                                .forEachLine {
                                    if (daemonOptions.runFilesPath.isNotEmpty() && it.contains(daemonOptions.runFilesPath)) {
                                        isEchoRead.release()
                                        return@forEachLine
                                    }
                                    reportingTargets.report(DaemonReportCategory.DEBUG, it, "daemon")
                                }
                    }
                    finally {
                        daemon.inputStream.close()
                        daemon.outputStream.close()
                        daemon.errorStream.close()
                    }
                }
        try {
            // trying to wait for process
            val daemonStartupTimeout = System.getProperty(COMPILE_DAEMON_STARTUP_TIMEOUT_PROPERTY)?.let {
                try {
                    it.toLong()
                }
                catch (e: Exception) {
                    reportingTargets.report(DaemonReportCategory.INFO, "unable to interpret $COMPILE_DAEMON_STARTUP_TIMEOUT_PROPERTY property ('$it'); using default timeout $DAEMON_DEFAULT_STARTUP_TIMEOUT_MS ms")
                    null
                }
            } ?: DAEMON_DEFAULT_STARTUP_TIMEOUT_MS
            if (daemonOptions.runFilesPath.isNotEmpty()) {
                val succeeded = isEchoRead.tryAcquire(daemonStartupTimeout, TimeUnit.MILLISECONDS)
                if (!isProcessAlive(daemon))
                    throw Exception("Daemon terminated unexpectedly")
                if (!succeeded)
                    throw Exception("Unable to get response from daemon in $daemonStartupTimeout ms")
            }
            else
            // without startEcho defined waiting for max timeout
                Thread.sleep(daemonStartupTimeout)
        }
        finally {
            // assuming that all important output is already done, the rest should be routed to the log by the daemon itself
            if (stdoutThread.isAlive) {
                // TODO: find better method to stop the thread, but seems it will require asynchronous consuming of the stream
                stdoutThread.stop()
            }
        }
    }
}


data class DaemonReportMessage(val category: DaemonReportCategory, val message: String)

class DaemonReportingTargets(val out: PrintStream? = null,
                             val messages: MutableCollection<DaemonReportMessage>? = null,
                             val messageCollector: MessageCollector? = null,
                             val compilerServices: CompilerServicesFacadeBase? = null)


internal fun isProcessAlive(process: Process) =
        try {
            process.exitValue()
            false
        }
        catch (e: IllegalThreadStateException) {
            true
        }
