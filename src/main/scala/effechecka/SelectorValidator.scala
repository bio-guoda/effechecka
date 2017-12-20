package effechecka

import org.locationtech.spatial4j.context.jts.JtsSpatialContext
import org.locationtech.spatial4j.io.WKTReader

import scala.util.Try

trait SelectorValidator {

  def valid(selector: SelectorParams): Boolean = {
    Seq(validTaxonList _, validWktString _).forall(_ (selector))
  }

  def invalid(selector: SelectorParams): Boolean = {
    !valid(selector)
  }

  def validWktString(selector: SelectorParams): Boolean = {
    Try {
      new WKTReader(JtsSpatialContext.GEO, null).parse(selector.wktString)
    }.isSuccess
  }

  def validTaxonList(selector: SelectorParams): Boolean = {
    selector.taxonSelector.matches("""[a-zA-Z,|\s]*""")
  }

  def validationReport(selector: SelectorParams): String = {
    val wktStatus = try {
      new WKTReader(JtsSpatialContext.GEO, null).parse(selector.wktString)
      None
    } catch {
      case e: Exception => Some(s"unsupported wktString [${selector.wktString}]: ${e.getMessage}");
    }

    val taxonStatus = {
      if (validTaxonList(selector))
        None
      else Some(s"unsupported taxon selector [${selector.taxonSelector}]: taxon names may only contain A-Za-z and whitespaces")
    }
    Seq(wktStatus, taxonStatus).flatten.mkString("\n")
  }

}
