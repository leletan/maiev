package org.leletan.maiev.lib

import java.sql.{Connection, DriverManager, Statement}

/**
 * Created by jiale.tan on 12/23/17.
 */
object JDBCConnectionFactory {
  Class.forName("org.postgresql.Driver")
  val connection: Connection =
    DriverManager
    .getConnection("jdbc:postgresql://cockroachdb-public:26257/bank?sslmode=disable", "leletan", "leletan")
}
