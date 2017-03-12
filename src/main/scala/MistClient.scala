package mist.client


import org.scalajs.dom
import org.scalajs.jquery.jQuery

import scala.scalajs.js
import scala.scalajs.js.JSApp
import js.Dynamic.{global => g}
import scala.concurrent.Future

// HTTP support
import fr.hmil.roshttp.Protocol.HTTP
import fr.hmil.roshttp.HttpRequest
import monix.execution.Scheduler.Implicits.global
import scala.util.{Failure, Success}
import fr.hmil.roshttp.response.SimpleHttpResponse
import fr.hmil.roshttp.body.JSONBody.JSONObject
import fr.hmil.roshttp.body.Implicits._

object MistClient extends JSApp{

  //val WorkServerAddress = "ec2-34-248-115-82.eu-west-1.compute.amazonaws.com"
  val WorkServerAddress = "127.0.0.1"
  val WorkServerPort = 8000

  implicit class When[A](a: A) {
    def when(f: Boolean)(g: A => A) = if (f) g(a) else a
  }

  def formHttpRequest(query: String = "InterestingArticle",
                      path: String = "/."): HttpRequest = {
    val req = HttpRequest()
      .withProtocol(HTTP)
      .withHost(WorkServerAddress)
      .withPort(WorkServerPort)
      .when(path != null)(_.withPath(path))
      .when(query != null)(_.withQueryString(query))

    println(s"Request URL: ${req.url}")

    req
  }

  val WorkRequest = formHttpRequest()

  def askForWork(): Unit = {
    val req = WorkRequest.send()

    req.onComplete({
      case res: Success[SimpleHttpResponse] =>
        jQuery("#textBox").append(s"<p> Request sent successfully. </p>")

        val responseBody = res.get.body
        doTheWork(responseBody)

      case e: Failure[SimpleHttpResponse] =>
        jQuery("#textBox").append("<p> Request for work failed </p>")
        throw new Exception("Couldn't get any work")
    })
  }

  def redirect (redirectUrl: String): Unit = {
    dom.window.location.href = redirectUrl
    println(s"Redirect URL: $redirectUrl")
    jQuery("#textBox").append(s"<p> Redirected. </p>")
  }

  def doTheWork(responseBody: String): Unit = {
    val params = g.JSON.parse(responseBody)
    /*
    payload, the workload parameters
    workload id
    client id
    application id
     */
    val payload = params.payload.asInstanceOf[String]
    val wid = params.wid.asInstanceOf[String]
    val cid = params.cid.asInstanceOf[String]
    val aid = params.aid.asInstanceOf[String]

    jQuery("#textBox").append(s"<p> You've been asked to encode: </p> <p> ${payload} (${wid}) </p>")
    jQuery("#loader").show()

    val hashCode = g.sha256(payload).asInstanceOf[String]

    //Sending the response
    val jsonData = JSONObject("payload" -> hashCode,"wid" -> wid,"cid" -> cid,"aid" -> aid)
    val req = WorkRequest.put(jsonData)
    req.onComplete({
      case res:Success[SimpleHttpResponse] =>
        jQuery("#loader").hide()
        jQuery("#textBox").append("<p> Response accepted </p> <p> Redirecting... </p>")
        jQuery("#textBox").append(s"<p> Response code ${res.get.statusCode} </p>")

        val responseCode = res.get.statusCode
        responseCode match{
          case 200 => redirect(res.get.body)
          case 204 => askForWork()
        }
      case e: Failure[SimpleHttpResponse] =>
        jQuery("#textBox").append("<p> Response not accepted </p>")
    })
  }

  def main(): Unit = {
    jQuery("#loader").hide()
    jQuery("#textBox").append(s"<p> Sending Request to server $WorkServerAddress </p>")

    askForWork()
  }
}
