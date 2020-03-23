package slack.models

import akka.http.scaladsl.model.HttpResponse
import slack.api.InvalidResponseError

import scala.compat.java8.OptionConverters._
import scala.concurrent.duration._
import scala.util.Try


case class ResponseMetadata(next_cursor: Option[String])

// Retry after time in seconds returned in a rate limited response
// https://api.slack.com/docs/rate-limits
case class RetryAfter(seconds: Int) {
  def finiteDuration: FiniteDuration = seconds.seconds
  def invalidResponseError: InvalidResponseError = {
    InvalidResponseError(RetryAfter.responseCode, RetryAfter.message)
  }
}

object RetryAfter {
  val header       = "Retry-After"
  val defaultRetry = "3"
  val message      = "Too Many Requests"
  val responseCode = 429

  def responseToInt(response: HttpResponse): Int = {
    response.getHeader(header).asScala match {
      case Some(header) =>
        Try(header.value().toInt).getOrElse(defaultRetry.toInt)
      case None =>
        defaultRetry.toInt
    }
  }
}