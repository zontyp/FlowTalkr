package db

import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource

object PostgresDataSource {

    fun create(): DataSource {
        val ds = PGSimpleDataSource()

        // ✅ MUST be full JDBC URL
        ds.setURL(
            "jdbc:postgresql://ep-quiet-cloud-ah40ft50-pooler.c-3.us-east-1.aws.neon.tech:5432/neondb?sslmode=require"
        )

        ds.user = "neondb_owner"
        ds.password = "npg_IUFX7jgEJl9m"

        return ds
    }
}
