package effechecka

import org.specs2.mutable.Specification

class ChecklistFetcherSpec extends Specification with ChecklistFetcher{

  "Cassandra driver" should {
    "return a greeting for GET requests to the root path" in {
      val checklist = fetchChecklistItems("Insecta|Mammalia", "ENVELOPE(-150,-50,40,10)")
      checklist.foreach(println)
      checklist must contain(Map("taxon" -> "checklist item", "recordcount" -> 1))
    }

    "create a wellformed status query" in {
          val checklist = fetchChecklistItems("Insecta|Mammalia", "ENVELOPE(-150,-50,40,10)")
          checklist.foreach(println)
          checklist must contain(Map("taxon" -> "checklist item", "recordcount" -> 1))
        }
  }


}