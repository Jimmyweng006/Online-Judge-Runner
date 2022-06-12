import java.nio.file.Files
import java.nio.file.Paths

const val KOTLIN_CODE_FILENAME = "_code.kt"
const val KOTLIN_CODE_EXECUTABLE_FILENAME = "_code.jar"

class KotlinCompiler(val workspace: String): ICompiler {
    init {
        Files.createDirectories(Paths.get(workspace))
    }

    override fun compile(code: String): String {
        // 檔案前面加上 workspace 的資料夾路徑
        val codeFilePath = workspace.appendPath(KOTLIN_CODE_FILENAME)
        val executableFilePath = workspace.appendPath(KOTLIN_CODE_EXECUTABLE_FILENAME)
        val codeFile = code.writeToFile(codeFilePath)

        // 使用 Docker 指令進行編譯
        val compileProcess = ProcessBuilder(
            "docker",
            "run",
            "--rm",
            "-v",
            "${System.getProperty("user.dir").appendPath(workspace)}:/$workspace",
            "dyninka/kotlin:dyninka",
            "kotlinc",
            "/$codeFilePath",
            "-include-runtime",
            "-d",
            "/$executableFilePath")

        // 將指令輸出錯誤的方式導向到主控台上
        compileProcess.redirectError(ProcessBuilder.Redirect.INHERIT)
        compileProcess.start().waitFor()

        codeFile.delete()
        return executableFilePath
    }
}