package org.leletan.maiev.lib

import java.sql.Timestamp

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.{JsonMappingException, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.reflect.{ClassTag, classTag}
import scala.util.{Failure, Success, Try}

/**
 * Created by jiale.tan on 12/5/17.
 */
object JsonHelper {
  private val timestampWithoutZone = "yyyy-MM-dd HH:mm:ss.SSS"

  private val jacksonMapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)
    .setSerializationInclusion(Include.NON_NULL)

  // country code mapping from alpha 3 to alpha 2 in uppercase
  lazy val countryCodeMap: Map[String, String] = java.util.Locale.getISOCountries.map(a2 =>
    new java.util.Locale("en", a2).getISO3Country.toUpperCase -> a2.toUpperCase
  ).toMap

  // give a country code in 3-letter representation, return a corresponding 2 letter country code,
  // or return the input country code if no match found
  def getAlpha2CountryCode(countryCode: String): String = {
    countryCodeMap.getOrElse(countryCode.toUpperCase, countryCode)
  }

  def toJson(value: Map[Symbol, Any]): String = {
    toJson(value map { case (k, v) => k.name -> v })
  }

  def toJson(value: Any): String = {
    jacksonMapper.writeValueAsString(value)
  }

  def fromJson[T: ClassTag](json: String): Try[T] = {
    try {
      val o = jacksonMapper.readValue[T](json, classTag[T].runtimeClass.asInstanceOf[Class[T]])
      Success(o)
    } catch {
      // To resolve issue: had a not serializable result: com.fasterxml.jackson.module.scala.deser.BuilderWrapper
      case e: Throwable => Failure(new JsonMappingException("%s at JSON %s".format(e.getMessage, json)))
    }
  }

  // Joda Time already supports this format by default: yyyy-MM-dd'T'HH:mm:ss'Z'
  def parseTimestampWithZone(ts: String): Timestamp = {
    new Timestamp(DateTime.parse(ts).getMillis)
  }

  def parseTimestampWithoutZone(ts: String): Timestamp = {
    new Timestamp(DateTimeFormat.forPattern(timestampWithoutZone).parseDateTime(ts).getMillis)
  }
}
