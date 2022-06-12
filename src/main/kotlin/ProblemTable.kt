import org.jetbrains.exposed.sql.Table

object ProblemTable : Table() {
    val id = integer("ProblemId").autoIncrement().primaryKey()
}