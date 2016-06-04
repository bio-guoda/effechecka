package effechecka

import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, Authorization}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.testkit.TestSubscriber
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.scalactic.ConversionCheckedTripleEquals
import org.scalatest._
import spray.json._

import scala.concurrent.duration._

class EmailUtilsSpec extends WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach
  with ConversionCheckedTripleEquals with SubscriptionProtocols {

  "url for selector" in {
    val selector: OccurrenceSelector = OccurrenceSelector("taxa", "wkt", "traits")
    EmailUtils.urlFor(selector) should be(new URL("http://apihack-c18.idigbio.org/view?taxonSelector=taxa&wktString=wkt&traitSelector=traits"))
  }

  "uuid url for selector" in {
    val selector: OccurrenceSelector = OccurrenceSelector("taxa", "wkt", "traits")
    EmailUtils.uuidUrlFor(selector) should be(new URL("http://apihack-c18.idigbio.org/view?uuid=4fd9a47e-c8c2-53a0-bcc5-9fbbd3d86526"))
  }

  "url for subscription event added before" in {
    val event: SubscriptionEvent = SubscriptionEvent(OccurrenceSelector("taxa", "wkt", "traits"), new URL("mailto:john@doe"), "notify", Some("2012-01-01"))
    EmailUtils.urlFor(event) should be(new URL("http://apihack-c18.idigbio.org/view?uuid=4fd9a47e-c8c2-53a0-bcc5-9fbbd3d86526&addedBefore=2012-01-01"))
  }

  "url for subscription event added after" in {
    val event: SubscriptionEvent = SubscriptionEvent(OccurrenceSelector("taxa", "wkt", "traits"), new URL("mailto:john@doe"), "notify", None, Some("2012-01-01"))
    EmailUtils.urlFor(event) should be(new URL("http://apihack-c18.idigbio.org/view?uuid=4fd9a47e-c8c2-53a0-bcc5-9fbbd3d86526&addedAfter=2012-01-01"))
  }

  "url for subscription event no added before/after" in {
    val event: SubscriptionEvent = SubscriptionEvent(OccurrenceSelector("taxa", "wkt", "traits"), new URL("mailto:john@doe"), "notify")
    EmailUtils.urlFor(event) should be(new URL("http://apihack-c18.idigbio.org/view?uuid=4fd9a47e-c8c2-53a0-bcc5-9fbbd3d86526"))
  }

  "unsubscribe text for event" in {
    val selector: OccurrenceSelector = OccurrenceSelector("taxa", "wkt", "traits")
    val event = SubscriptionEvent(selector, new URL("mailto:foo@bar"), "subscribe")
    EmailUtils.unsubscribeTextFor(event) should be(s"If you no longer wish to receive these email, please visit ${EmailUtils.unsubscribeUrlFor(event)} .")
  }

  "subscribe email" in {
    val selector: OccurrenceSelector = OccurrenceSelector("taxa", "wkt", "traits")
    val event = SubscriptionEvent(selector, new URL("mailto:foo@bar"), "subscribe")
    val email: Email = EmailUtils.emailFor(event)
    email.to should be("foo@bar")
    email.subject should be("[freshdata] subscribed to freshdata search")
    email.text should include(EmailUtils.uuidUrlFor(selector).toString)
    email.text should include(EmailUtils.unsubscribeUrlFor(event).toString)
  }

  "unsubscribe email" in {
    val selector: OccurrenceSelector = OccurrenceSelector("taxa", "wkt", "traits")
    val event = SubscriptionEvent(selector, new URL("mailto:foo@bar"), "unsubscribe")
    val email: Email = EmailUtils.emailFor(event)
    email.to should be("foo@bar")
    email.subject should be("[freshdata] unsubscribed from freshdata search")
    email.text should include(EmailUtils.uuidUrlFor(selector).toString)
  }

  "notify email" in {
    val selector: OccurrenceSelector = OccurrenceSelector("taxa", "wkt", "traits")
    val event = SubscriptionEvent(selector, new URL("mailto:foo@bar"), "notify")
    val email: Email = EmailUtils.emailFor(event)
    email.to should be("foo@bar")
    email.subject should be("[freshdata] new data is available for your freshdata search")
    email.text should include(EmailUtils.uuidUrlFor(selector).toString)
    email.text should include(EmailUtils.unsubscribeUrlFor(event).toString)
  }

  "mailgun request for email" in {
    val anEmail = Email(to = "john@doe", subject = "hello!", text = "some text")
    val httpRequest: HttpRequest = EmailUtils.mailgunRequestFor(anEmail, "someApiKey")
    httpRequest.uri.toString should be("https://api.mailgun.net/v3/effechecka.org/messages")
    httpRequest.headers should contain(Authorization(BasicHttpCredentials("api", "someApiKey")))
    httpRequest.method should be(HttpMethods.POST)
  }

}