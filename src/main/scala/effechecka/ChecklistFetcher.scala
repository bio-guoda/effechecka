package effechecka

import com.datastax.driver.core._
import scala.collection.JavaConversions._

trait ChecklistFetcher extends Configure {
  def fetchChecklistItems(taxonSelector: String, wktString: String, limit: Int = 20): List[Map[String, Any]] = {
    val results: ResultSet = session.execute(checklistSelect(limit), normalizeTaxonSelector(taxonSelector), wktString)
    val items: List[Row] = results.all.toList
    items.map(item => Map("taxon" -> item.getString("taxon"), "recordcount" -> item.getInt("recordcount")))
  }

  def fetchChecklistStatus(taxonSelector: String, wktString: String): Option[String] = {
    val results: ResultSet = session.execute(checklistStatusSelect, normalizeTaxonSelector(taxonSelector), wktString)
    val items: List[Row] = results.all.toList
    items.map(_.getString("status")).headOption
  }

  def session: Session = {
    val cluster = Cluster.builder()
      .addContactPoint(config.getString("effechecka.cassandra.host")).build()
    cluster.connect("idigbio")
  }

  def checklistSelect(limit: Int): String = {
    s"SELECT taxon,recordcount FROM idigbio.checklist WHERE taxonselector = ? AND wktstring = ? ORDER BY recordcount DESC LIMIT $limit"
  }

  def checklistStatusSelect: String = {
    "SELECT status FROM idigbio.checklist_registry WHERE taxonselector = ? AND wktstring = ? LIMIT 1"
  }

  def insertChecklistRequest(taxonSelector: String, wktString: String): String = {
    val values = Seq(normalizeTaxonSelector(taxonSelector), wktString, "requested").map("'" + _ + "'").mkString(",")
    session.execute(s"INSERT INTO idigbio.checklist_registry (taxonselector, wktstring, status) VALUES ($values) using TTL 600")
    "requested"
  }

  def normalizeTaxonSelector(taxonSelector: String) = {
    taxonSelector.replace(',', '|')
  }

}

