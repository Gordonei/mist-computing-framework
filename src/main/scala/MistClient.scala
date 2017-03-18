package mist.client

// Scala.js
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
import fr.hmil.roshttp.body.JSONBody.JSONObject
import fr.hmil.roshttp.body.Implicits._

object MistClient extends JSApp{

  //val WorkServerAddress = "127.0.0.1"
  val WorkServerAddress = "mist-computing-demo.xyz"
  val WorkServerPort = 8000

  implicit class When[A](a: A) {
    def when(f: Boolean)(g: A => A) = if (f) g(a) else a
  }

  // HTTP Request forming
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

  // Appending text to Text Box
  def appendText(messages: Iterable[String]): Unit = {
    messages.foreach {
      message => jQuery("#textBox").append(s"<p> $message </p>")
    }
  }

  // Making GET request for work
  def askForWork(): Unit = {
    val req = WorkRequest.send()

    req.onComplete({
      case res: Success[SimpleHttpResponse] =>
        appendText(Seq("Request sent successfully."))

        val responseBody = res.get.body
        doTheWork(responseBody)

      case e: Failure[SimpleHttpResponse] =>
        appendText(Seq("Request for work failed"))
        throw new Exception("Couldn't get any work")
    })
  }

  // PUT result to server
  def redirect (redirectUrl: String): Unit = {
    dom.window.location.href = redirectUrl
    println(s"Redirect URL: $redirectUrl")
    appendText(Seq("Redirected."))
  }

  def doTheWork(responseBody: String): Unit = {
    val work = new WorkUnit(responseBody)

    appendText(Seq("You've been asked to encode:",s"${work.payload}"))
    jQuery("#loader").show()

    val workResult = Applications.process(work.payload,work.aid)

    //Sending the response
    val jsonData = work.formResponse(workResult)

    val req = WorkRequest.put(jsonData)
    req.onComplete({
      case res:Success[SimpleHttpResponse] =>
        jQuery("#loader").hide()
        appendText(Seq("Response accepted","Redirecting..."))
        appendText(Seq(s"Response code ${res.get.statusCode}"))

        val responseCode = res.get.statusCode
        responseCode match{
          // 200 means there is a response in the link
          case 200 => redirect(res.get.body)
          // 204 means there is more work to do
          case 204 => askForWork()
        }
      case e: Failure[SimpleHttpResponse] =>
        appendText(Seq("Response not accepted"))
    })
  }

  def main(): Unit = {
    jQuery("#loader").hide()
    appendText(Seq(s"Sending Request to server $WorkServerAddress"))

    askForWork()
  }
}

object Applications {
  def process(payload: String, aid: String): String = {
    // Application ID selects what work to do
    aid match {
      case "sha256" => g.sha256(payload).asInstanceOf[String]
      case _ =>
        MistClient.appendText(Seq(s"Request for work failed"))
        throw new Exception("Couldn't get any work")
    }
  }
}

class WorkUnit(rawBody: String){
  val params = g.JSON.parse(rawBody)

  // Payload
  val payload = params.payload.asInstanceOf[String]
  // Application ID
  val aid = params.aid.asInstanceOf[String]
  // Work ID
  private val wid = params.wid.asInstanceOf[String]
  // Client ID
  private val cid = params.cid.asInstanceOf[String]

  println(s"$payload aid=$aid wid=$wid cid=$cid")

  def formResponse (responsePayload: String) =
    JSONObject("payload" -> responsePayload,"wid" -> wid,"cid" -> cid,"aid" -> aid)
}
