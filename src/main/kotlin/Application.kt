const val DOCKER_WORKSPACE = "workspace"

fun main() {
    val submissionSource: ISubmissionSource = DatabaseSubmissionSource

    while (true) {
        var submission = submissionSource.getNextSubmissionData()
        while (submission != null) {
            // 輸入程式語言進函式去生出相對應的 Judger
            val judger = getJudger(submission.language)

            val result = judger.judge(submission)
            submissionSource.setResult(submission.id,
                result.result, result.executedTime, result.totalScore)
            submission = submissionSource.getNextSubmissionData()
        }

        Thread.sleep(5000);
    }
}

// 根據各個語言選擇正確的 Judger 去進行編譯與執行的動作
fun getJudger(language: String): Judger =
    when (language) {
        "kotlin" -> Judger(KotlinCompiler(DOCKER_WORKSPACE), JVMExecutor(DOCKER_WORKSPACE))
        "c" -> Judger(GCCCompiler(DOCKER_WORKSPACE), GCCExecutor(DOCKER_WORKSPACE))
        "c++" -> Judger(GCCCompiler(DOCKER_WORKSPACE), GCCExecutor(DOCKER_WORKSPACE))
        "java" -> Judger(JavaCompiler(DOCKER_WORKSPACE), JVMExecutor(DOCKER_WORKSPACE))
        "python" -> Judger(PassThroughCompiler(DOCKER_WORKSPACE), PythonExecutor(DOCKER_WORKSPACE))
        else -> throw NotImplementedError()
    }
