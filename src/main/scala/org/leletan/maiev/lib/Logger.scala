package org.leletan.maiev.lib

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

/**
 * Created by jiale.tan on 12/4/17.
 */
trait Logger {

  def info(msg: String): Unit = {
    val time = ISODateTimeFormat.dateTime().print(DateTime.now())
    println(s"[$time] INFO $msg")
  }

  def warn(msg: String): Unit = {
    val time = ISODateTimeFormat.dateTime().print(DateTime.now())
    println(s"[$time] WARN $msg")
  }

  def error(e: Throwable, msgOpt: Option[String] = None): Unit = {
    val time = ISODateTimeFormat.dateTime().print(DateTime.now())
    println(s"[$time] ERROR ${msgOpt.map(msg => s"$msg\n").getOrElse("")}$e")
  }
}
