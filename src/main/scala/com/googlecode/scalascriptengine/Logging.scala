package com.googlecode.scalascriptengine

import org.slf4j.{Logger, LoggerFactory}

/**
 * logging is done via slf4j
 *
 * @author kostantinos.kougios
 *
 *         25 Dec 2011
 */
protected trait Logging
{
	private val logger: Logger = LoggerFactory.getLogger(getClass)

	protected def debug(msg: => String): Unit = if (logger.isDebugEnabled) logger.debug(msg)

	protected def info(msg: => String): Unit = if (logger.isInfoEnabled) logger.info(msg)

	protected def warn(msg: => String): Unit = if (logger.isWarnEnabled()) logger.warn(msg)

	protected def error(msg: => String): Unit = if (logger.isErrorEnabled) logger.error(msg)

	protected def error(msg: => String, e: Throwable): Unit = if (logger.isErrorEnabled) logger.error(msg, e)
}