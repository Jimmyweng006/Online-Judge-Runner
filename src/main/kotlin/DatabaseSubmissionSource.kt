import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import redis.clients.jedis.Jedis

const val SUPPORTED_LANGUAGE = "kotlin"

object DatabaseSubmissionSource: ISubmissionSource {
    private val supportedLanguages = listOf("kotlin", "c", "java", "python", "c++")
    var jedis: Jedis?

    init {
        val config = HikariConfig("/hikari.properties")
        config.schema = "public"
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(ProblemTable, TestCaseTable, SubmissionTable)
        }

        jedis = Jedis()
    }

    override fun getNextSubmissionData(): SubmissionData? {
        // 整個改成從 Jedis 拉資料出來
        try {
            jedis = jedis.getConnection()
            // no need ?
//            if (jedis == null) return null

            val currentJedisConnection = jedis!!
            for (language in supportedLanguages) {
                val isDataAvailable = currentJedisConnection.exists(language)
                if (!isDataAvailable) continue

                val data = currentJedisConnection.lpop(language)
                return jacksonObjectMapper().readValue(data, SubmissionData::class.java)
            }
        } catch(e: Exception) {
            jedis?.disconnect()
            jedis = null
            println(e)
            return null
        }

        return null
    }

    override fun setResult(id: Int, result: Judger.Result, executedTime: Double, score: Int) {
        transaction {
            SubmissionTable.update({
                SubmissionTable.id.eq(id)
            }) {
                it[SubmissionTable.result] = "$result ($score)"
                it[SubmissionTable.executedTime] = executedTime
            }
        }

        println("Submission $id: $result - Score: $score ($executedTime)")
    }
}