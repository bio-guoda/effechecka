package effechecka

import akka.NotUsed
import akka.stream.scaladsl.{Concat, Flow, GraphDSL, Sink, Source}
import akka.stream.{ActorMaterializer, SourceShape}
import akka.util.ByteString
import com.typesafe.config.Config
import effechecka.repackaged.io.eels.Row
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.effechecka.selector.DateTimeSelector

import scala.concurrent.Await
import scala.concurrent.duration._

trait OccurrenceCollectionFetcherHDFS
  extends OccurrenceCollectionFetcher
    with HDFSUtil {

  implicit def config: Config

  protected implicit val configHadoop: Configuration
  protected implicit val fs: FileSystem

  implicit val materializer: ActorMaterializer

  def occurrencesTsvFor(ocRequest: OccurrenceRequest): Source[ByteString, NotUsed] = {
    val occurrenceSource = Source.fromGraph(graphForOccurrences(ocRequest))
    val aGraph = GraphDSL.create(occurrenceSource) { implicit builder =>
      (occurrenceSource) =>
        import GraphDSL.Implicits._
        val toTsv = Flow[Occurrence].map { occ => ByteString(CsvUtils.toOccurrenceRow(occ)) }
        val out = builder.add(toTsv)
        occurrenceSource ~> out
        SourceShape.of(out.out)
    }
    val header = Source.single[ByteString](ByteString(Seq("taxonName", "taxonPath", "lat", "lng", "eventStartDate", "occurrenceId", "firstAddedDate", "source", "occurrenceUrl").mkString("\t")))
    Source.combine(header, Source.fromGraph(aGraph))(Concat[ByteString])
  }

  private def graphForOccurrences(ocRequest: OccurrenceRequest) = {
    val selector = ocRequest.selector
    val pathBase = s"occurrence/${pathForSelector(selector)}"

    val patternFor1 = patternFor(pathBase) match {
      case Some(pattern) => Some(pattern.withFilter(pathFilterWithDateRange(ocRequest.added)))
      case _ => None
    }

    GraphDSL.create(new ParquetReaderSourceShape(patternFor1, ocRequest.limit)) { implicit builder =>
      (occurrenceCollection) =>
        import GraphDSL.Implicits._
        val toOccurrences = Flow[Row]
          .map(row => {
            val startDate = java.lang.Long.parseLong(row.get("start").toString)
            Occurrence(taxon = row.get("taxon").toString,
              lat = java.lang.Double.parseDouble(row.get("lat").toString),
              lng = java.lang.Double.parseDouble(row.get("lng").toString),
              start = startDate,
              end = startDate,
              id = row.get("id").toString,
              added = java.lang.Long.parseLong(row.get("added").toString),
              source = row.get("source").toString
            )
          })
        val out = builder.add(toOccurrences)
        occurrenceCollection ~> out
        SourceShape(out.out)
    }
  }

  def occurrencesFor(ocRequest: OccurrenceRequest): Iterator[Occurrence] = {
    val occurrenceSource = Source.fromGraph(graphForOccurrences(ocRequest))
    Await.result(occurrenceSource
      .runWith(Sink.seq), 30.second)
      .iterator
  }

  def monitoredOccurrencesFor(source: String,
                              added: DateTimeSelector = DateTimeSelector(),
                              occLimit: Option[Int] = None): Source[ByteString, NotUsed] = {

    val patternFor1 = patternFor(s"source-of-monitored-occurrence/source=$source") match {
      case Some(pattern) => Some(pattern.withFilter(pathFilterWithDateRange(Some(added))))
      case _ => None
    }

    val aGraph = GraphDSL.create(new ParquetReaderSourceShape(patternFor1, occLimit)) { implicit builder =>
      (rows) =>
        import GraphDSL.Implicits._
        val toMonitoredOccurrences = Flow[Row]
          .map(row => {
            val values = row.values
            (values.head.toString, if (values.size > 1) Some(values(1).toString) else None)
          })
          .map { case (occurrenceId, uuidOption) =>
            ByteString(s"\n$occurrenceId\t${uuidOption.getOrElse("")}")
          }
        val out = builder.add(toMonitoredOccurrences)
        rows ~> out
        SourceShape(out.out)
    }
    val header = Source.single[ByteString](ByteString("occurrenceId\tmonitorUUID"))
    Source.combine(header, Source.fromGraph(aGraph))(Concat[ByteString])
  }

  def monitors(): List[OccurrenceMonitor] = {
    val aGraph = GraphDSL.create(new ParquetReaderSourceShape(patternFor(s"occurrence-summary"), None)) { implicit builder =>
      (rows) =>
        import GraphDSL.Implicits._
        val monitors = builder.add(toMonitors)
        rows ~> monitors
        SourceShape(monitors.out)
    }
    Await.result(Source.fromGraph(aGraph).runWith(Sink.seq), 30.second).toList
  }

  private def toMonitors: Flow[Row, OccurrenceMonitor, NotUsed] = {
    Flow[Row]
      .map(row => {
        val statusOpt = row.get("status").toString
        val recordOpt = Some(Integer.parseInt(row.get("itemCount").toString))
        val lastModified = java.lang.Long.parseLong(row.get("lastModified").toString)
        OccurrenceMonitor(selectorFromRow(row), Some(statusOpt), recordOpt, lastModified)
      })
  }

  private def selectorFromRow(row: Row): Selector = {
    SelectorParams(taxonSelector = row.get("taxonSelector").toString,
      traitSelector = row.get("traitSelector").toString,
      wktString = row.get("wktString").toString).withUUID()
  }

  def monitorOf(selector: Selector): Option[OccurrenceMonitor] = {
    val aGraph = GraphDSL.create(new ParquetReaderSourceShape(patternFor(s"occurrence-summary/${pathForSelector(selector)}"), None)) { implicit builder =>
      (rows) =>
        import GraphDSL.Implicits._
        val monitors = builder.add(toMonitors)
        rows ~> monitors
        SourceShape(monitors.out)
    }
    Await.result(Source.fromGraph(aGraph).runWith(Sink.seq), 30.second)
      .toList
      .sortWith(_.lastModified > _.lastModified)
      .headOption
  }

  def statusOf(req: OccurrenceRequest): Option[String] = {
    monitorOf(req.selector) match {
      case Some(aMonitor) => aMonitor.status
      case None => None
    }
  }

}

