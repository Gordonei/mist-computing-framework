package mist.client

import org.scalajs.dom
import org.scalajs.jquery.jQuery
import scala.scalajs.js

import scala.scalajs.js.JSApp
import js.Dynamic.{global => g}

// HTTP support
import fr.hmil.roshttp.Protocol.HTTP
import fr.hmil.roshttp.HttpRequest
import monix.execution.Scheduler.Implicits.global
import scala.util.{Failure, Success}
import fr.hmil.roshttp.response.SimpleHttpResponse

object MistClient extends JSApp{
  def redirect (redirectUrl: String): Unit = {
    dom.window.location.href = redirectUrl
  }

  val ServerAddress = "127.0.0.1"
  val ServerPort = 8000

  def main(): Unit = {
    jQuery("#loader").hide()
    jQuery("#textBox").append(s"<p> Sending Request to server $ServerAddress </p>")

    val request = HttpRequest()
      .withProtocol(HTTP)
      .withHost(ServerAddress)
      .withPort(ServerPort)
      .withPath("/.")
      .withQueryString("InterestingArticle")

    request.send().onComplete({
      case res:Success[SimpleHttpResponse] =>
        val requestMessage = res.get.body.split("\n").take(1).mkString
        val requestID = res.get.body.split("\n").last
        jQuery("#textBox").append(s"<p> Request sent successfully. </p> " +
          s"<p> You've been asked to encode: </p> " +
          s"<p> $requestMessage ($requestID) </p>")
        jQuery("#loader").show()

        val hashCode = g.sha256(s"$requestMessage").asInstanceOf[String]
        jQuery("#textBox").append(s"<p> Sending response $hashCode </p>")

        val response = HttpRequest()
          .withProtocol(HTTP)
          .withHost(ServerAddress)
          .withPort(ServerPort)
          .withPath("/.")
          .withQueryString("InterestingArticle")
          .withQueryParameter("RequestId",requestID)
          .withQueryParameter("Response", hashCode)

        response.send().onComplete({
          case res:Success[SimpleHttpResponse] =>
            jQuery("#loader").hide()
            jQuery("#textBox").append("<p> Response accepted </p> <p> Redirecting... </p>")
            redirect(res.get.body)
          case e: Failure[SimpleHttpResponse] =>
            jQuery("#textBox").append("<p> Response not accepted </p>")
        })

      case e: Failure[SimpleHttpResponse] =>
        jQuery("#textBox").append("<p> Request failed </p>")
    })
  }
}
