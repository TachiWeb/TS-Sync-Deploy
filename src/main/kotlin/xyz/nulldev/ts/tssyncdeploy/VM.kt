package xyz.nulldev.ts.tssyncdeploy

import sun.tools.jar.resources.jar
import java.io.File
import java.util.Scanner
import java.util.concurrent.TimeUnit

class VM(val account: String) {
    var process: Process? = null
    var port: Int? = null
    var lastUsedTime = System.currentTimeMillis()

    fun shutdown() {
        println("Destroying VM for $account")
        AccountManager.lockAcc(account) {
            //Destroy process and wait for 3s
            process?.destroy()
            process?.waitFor(3000, TimeUnit.SECONDS)

            //Forcibly destroy process if it refuses to die
            if(process?.isAlive == true) {
                process?.destroyForcibly()
                process?.waitFor()
            }

            process = null

            //Release port
            port?.let { AccountManager.releasePort(it) }

            println("VM for $account destroyed!")
        }
    }

    fun startup() {
        println("Booting VM for $account")
        AccountManager.lockAcc(account) {
            //Write port
            val newPort = AccountManager.reservePort()
            port = newPort
            val configFile = File(it.configFolder, "ip.config")
            configFile.writeText("ts.server.port = $newPort")

            //Find Java
            val javaBinFolder = File(File(System.getProperty("java.home")), "bin")
            val windowsExec = File(javaBinFolder, "java.exe")
            val unixExec = File(javaBinFolder, "java")

            val java = when {
                windowsExec.exists() -> windowsExec.absolutePath
                unixExec.exists() -> unixExec.absolutePath
                else -> throw RuntimeException("Cannot find JVM binary!")
            }

            //Launch process
            val newP = ProcessBuilder()
                    //Skip patches, 50MB heap
                    .command(java, "-Dts.bootstrap.active=true",
//                            "-Xmx50m",
//                            "-Xss1m",
//                            "-XX:MaxHeapFreeRatio=10",
//                            "-XX:MinHeapFreeRatio=10",
//                            "-XX:MaxMetaspaceSize=25m",
                            "-jar", SERVER_FILE)
                    .directory(it.folder)
                    //Log
                    .redirectError(ProcessBuilder.Redirect.appendTo(File(it.folder, "vm.log")))
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .start()
            process = newP

            //Wait for init message
            Scanner(newP.inputStream.bufferedReader()).use { scanner ->
                while(newP.isAlive) {
                    if(scanner.hasNextLine()) {
                        val line = scanner.nextLine().trim()

                        if(line == AccountManager.INIT_MESSAGE) {
                            println("VM for $account started!")
                            break
                        }
                    }
                }
            }
        }
    }

    companion object {
        val SERVER_FILE = File("TachiServer.jar").absolutePath
    }
}