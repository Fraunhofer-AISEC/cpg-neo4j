package de.fraunhofer.aisec.cpg.ptn4j

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Path
import java.util.*

class DockerUtils {
    private val runtime = Runtime.getRuntime()
    private var process: Process? = null

    private fun setProcess(process: Process) {
        this.process = process
    }

    @Throws(InterruptedException::class)
    fun startDocker() {
        val isWin = os.contains("win")
        try {
            setProcess(runtime.exec((if (isWin) openWin else "") + "docker -v"))
            setProcess(
                    runtime.exec(
                            (if (isWin) comEnv else "")
                                    + "docker run --name neo4j -p7474:7474 -p7687:7687 -v "
                                    + neo4JPath
                                    + ":"
                                    + fileSep
                                    + "data -d --rm neo4j:latest"
                    )
            )
            println(getResult(process))
        } catch (ex: IOException) {
            System.err.println(ex.message)
        }
    }

    @Throws(InterruptedException::class)
    fun killDocker() {
        val isWin = os.contains("win")
        try {
            setProcess(
                    runtime.exec((if (isWin) comEnv else "")
                            + "docker ps -a -q --filter name=neo4j | ForEach-Object -Process {docker stop  \$_ }")
            )
            println(getResult(process))
        } catch (e: IOException) {
            System.err.println(e.message)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun getResult(process: Process?): List<String> {
        val response = ArrayList<String>()
        BufferedReader(InputStreamReader(process!!.inputStream)).use { reader ->
            var line = reader.readLine()
            var counter = 0
            while (line != null && counter < 20) {
                response.add(line)
                line = reader.readLine()
                Thread.sleep(50)
                counter++
            }
        }
        return response
    }

    companion object {
        private val fileSep = File.separator
        private val os = System.getProperty("os.name").toLowerCase()
        private val neo4JPath = Path.of(".")
                .resolve("..")
                .resolve("..")
                .resolve("neo4j")
                .resolve("data")
                .toAbsolutePath()
                .normalize()
        private const val openWin = "cmd.exe /c\r\n"
        private const val comEnv = "powershell "
    }

}