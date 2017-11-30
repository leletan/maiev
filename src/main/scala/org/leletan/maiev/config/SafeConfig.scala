package org.leletan.maiev.config

import scala.util.Try

import com.typesafe.config.Config

trait AbstractConfig {
  def config: Config
}

trait SafeConfig extends AbstractConfig {
  def getOption(keyName: String): Option[String] = {
    val value = Try({
      config.getString(keyName)
    })
    if (value.isSuccess) Some(value.get) else None
  }

  def safeGetConfig(keyName: String): String = {
    val value = getOption(keyName)

    require(
      value.isDefined,
      s"Cannot get $keyName in config file."
    )

    value.get
  }

  def safeGetConfigBoolean(keyName: String): Boolean = {
    val value = safeGetConfig(keyName)
    if (value.isEmpty) false else value.toBoolean
  }

  def safeGetConfigInt(keyName: String): Int = {
    val value = safeGetConfig(keyName)
    if (value.isEmpty) 0 else value.toInt
  }
}
