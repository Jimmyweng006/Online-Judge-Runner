import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

const val DOCKER_CONTAINER_NAME = "jvm-docker"
const val JVM_INPUT_FILENAME = "input.txt"
const val JVM_OUTPUT_FILENAME = "output.txt"

class JVMExecutor(val workspace: String): IExecutor {
    init {
        Files.createDirectories(Paths.get(workspace))
    }

    override fun execute(executableFilename: String, input: String, timeOutSeconds: Double): IExecutor.Result {
        /* 增加 workspace 資料夾於檔案前 */
        val inputFilePath = workspace.appendPath(JVM_INPUT_FILENAME)
        val outputFilePath = workspace.appendPath(JVM_OUTPUT_FILENAME)
        val inputFile = input.writeToFile(inputFilePath)

        val startTime = System.currentTimeMillis()
        /* 使用 Docker 來執行程式 */
        val executeProcess = ProcessBuilder(
            "docker",
            "run",
            "--rm",
            "--name",
            DOCKER_CONTAINER_NAME,
            "-v",
            "${System.getProperty("user.dir").appendPath(workspace)}:/$workspace",
            "dyninka/kotlin:dyninka",
            "sh",
            "-c",
            "java -jar /$executableFilename < /$inputFilePath > /$outputFilePath")
        executeProcess.redirectError(ProcessBuilder.Redirect.INHERIT)
        val process = executeProcess.start()
        val isFinished = process.waitFor(
            (timeOutSeconds * 1000).toLong(),
            TimeUnit.MILLISECONDS
        )

        /* 如果 TLE 的話，除了砍掉執行的指令，還要讓 Docker 去砍掉該 Container 才行。 */
        if (!isFinished) {
            ProcessBuilder("docker", "kill", DOCKER_CONTAINER_NAME).start().waitFor()
        }
        // use to terminate executeProcess
        process.destroy()
        // wait for process terminated
        process.waitFor()

        val isCorrupted = process.exitValue() != 0
        val executedTime = System.currentTimeMillis() - startTime
        /* 改使用指令型式的輸入輸出導向後，檔案不見得會存在，所以 output 從 String 變成了 String? */
        val outputFile = File(outputFilePath)
        var output: String? = null
        if (outputFile.exists()) {
            output = outputFile.readText()
        }
        inputFile.delete()
        outputFile.delete()

        return IExecutor.Result(
            !isFinished,
            isCorrupted,
            executedTime.toDouble() / 1000.0,
            output
        )
    }
}