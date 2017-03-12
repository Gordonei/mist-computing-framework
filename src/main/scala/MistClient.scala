package mist.client


import org.scalajs.dom
import org.scalajs.jquery.jQuery

import scala.scalajs.js
import scala.scalajs.js.JSApp
import js.Dynamic.{global => g}

// HTTP support
import fr.hmil.roshttp.Protocol.HTTP
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.Method.{PUT,POST}
import monix.execution.Scheduler.Implicits.global
import scala.util.{Failure, Success}
import fr.hmil.roshttp.response.SimpleHttpResponse
import fr.hmil.roshttp.body.{JSONBody, URLEncodedBody}
import fr.hmil.roshttp.body.JSONBody.JSONObject
import fr.hmil.roshttp.body.Implicits._

object MistClient extends JSApp{
  def redirect (redirectUrl: String): Unit = {
    //dom.window.location.href = redirectUrl
    jQuery("#textBox").append(s"<p> Redirected. </p>")
  }

  implicit class When[A](a: A) {
    def when(f: Boolean)(g: A => A) = if (f) g(a) else a
  }

  def formHttpRequest(query: String = "InterestingArticle",
                      path: String = "/.",
                      response: String = null,
                      responseId: String = null): HttpRequest = {

    val req = HttpRequest()
      .withProtocol(HTTP)
      .withHost(WorkServerAddress)
      .withPort(WorkServerPort)
      .when(path != null)(_.withPath(path))
      .when(query != null)(_.withQueryString(query))
      .when(responseId != null)(_.withQueryParameter("RequestId",responseId))
      .when(response != null)(_.withQueryParameter("Response",response))

    println(s"Request URL: ${req.url}")

    req
  }

  //val WorkServerAddress = "ec2-34-248-115-82.eu-west-1.compute.amazonaws.com" //"127.0.0.1"
  val WorkServerAddress = "127.0.0.1"
  val WorkServerPort = 8000

  def main(): Unit = {
    jQuery("#loader").hide()
    jQuery("#textBox").append(s"<p> Sending Request to server $WorkServerAddress </p>")

    val request = formHttpRequest().send

    request.onComplete({
      case res:Success[SimpleHttpResponse] =>

        val requestMessage = res.get.body.split("\n").take(1).mkString
        val requestID = res.get.body.split("\n").last

        jQuery("#textBox").append(s"<p> Request sent successfully. </p> " +
          s"<p> You've been asked to encode: </p> " +
          s"<p> $requestMessage ($requestID) </p>")
        jQuery("#loader").show()

        val hashCode = g.sha256(s"$requestMessage").asInstanceOf[String]

        jQuery("#textBox").append(s"<p> Sending response $hashCode </p>")

        val response = formHttpRequest(
          response = hashCode,
          responseId = requestID).send()

        /*
        val jsonData = JSONObject("some" -> "test")
        val response2 = formHttpRequest(null).put(jsonData)
        */

        response.onComplete({
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
