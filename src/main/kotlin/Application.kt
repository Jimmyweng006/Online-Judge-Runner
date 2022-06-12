const val DOCKER_WORKSPACE = "workspace"

fun main() {
    val submissionSource: ISubmissionSource = DatabaseSubmissionSource

    while (true) {
        var submission = submissionSource.getNextSubmissionData()
        while (submission != null) {
            val judger = Judger(KotlinCompiler(DOCKER_WORKSPACE), JVMExecutor(DOCKER_WORKSPACE))

            val result = judger.judge(submission)
            submissionSource.setResult(submission.id,
                result.result, result.executedTime, result.totalScore)
            submission = submissionSource.getNextSubmissionData()
        }

        Thread.sleep(5000);
    }
}
