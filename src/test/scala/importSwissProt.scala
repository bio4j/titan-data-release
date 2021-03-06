package bio4j.data.titan.test

import com.bio4j.angulillos.UntypedGraph
import com.bio4j.angulillos.titan._
import com.thinkaurelius.titan.core._
import com.bio4j.model._
import com.bio4j.data._
import scala.compat.java8.OptionConverters._
import scala.collection.JavaConverters._
import ohnosequences.fastarious._

case object importSwissProt {

  lazy val conf = TitanConf(new java.io.File("db"))

  def cleanDB = {

    import sys.process._

    Seq("rm", "-rf", "db").!
  }

  def closeDB =
    titan.shutdown()

  // the graph
  lazy val uniProtGraph =
    new UniProtGraph(conf.untypedGraph)

  // the underlying Titan instance
  lazy val titan =
    uniProtGraph.raw().asInstanceOf[TitanUntypedGraph]

  lazy val uniProtImport =
    uniprot.Process(uniProtGraph)

  // the iterator of entries
  def entries =
    uniprot.Entry.fromUniProtLines( io.Source.fromFile("uniprot_sprot.xml").getLines )

  def isoformSequences =
    fasta.parseFastaDropErrors( io.Source.fromFile("uniprot_sprot_varsplic.fasta").getLines )

  def initTypes = {

    val mgmt = titan.titanGraph().openManagement()
    val schema = new TitanUntypedGraphSchema()

    // init types
    schema.createSchema(mgmt,uniProtGraph)
    mgmt.commit()
  }

  def theWholeThing = {

    cleanDB

    initTypes

    importProteins(10000)
    importGeneNames(10000)
    importComments(10000)
    importKeywordTypes
    importKeywords(10000)
    importAnnotations(10000)
    importIsoforms(10000)
    importIsoformSequences(10000)

    closeDB
  }

  // These methods follow the (a possible) order in which they must be run.

  def importProteins(commitAfterEntries: Int = 1000) =
    entries.zipWithIndex foreach {
      case (entry, index) =>
        uniProtImport.entryProteins.process(entry, uniProtGraph)
        if(index % commitAfterEntries == 0) {
          println { s"Proteins: committing after ${index} entries" }
          titan.commit()
        }
    }

  def importGeneNames(commitAfterEntries: Int = 1000) =
    entries.zipWithIndex foreach {

      case (entry, index) =>
        uniProtImport.entryGeneNames.process(entry, uniProtGraph)
        if(index % commitAfterEntries == 0) {
          println { s"Gene names: committing after ${index} entries" }
          titan.commit()
        }
    }

  def importComments(commitAfterEntries: Int = 1000) =
    entries.zipWithIndex foreach {

      case (entry, index) =>
        uniProtImport.comments.process(entry, uniProtGraph)
        if(index % commitAfterEntries == 0) {
          println { s"Comments: committing after ${index} entries" }
          titan.commit()
        }
    }

  // TODO load from file
  def importKeywordTypes =
    KeywordTypes.fromFile.foreach { keywordRow =>
      uniProtImport.keywordTypes.process(keywordRow, uniProtGraph)
    }

  def importKeywords(commitAfterEntries: Int = 1000) =
    entries.zipWithIndex foreach {

      case (entry, index) =>
        uniProtImport.keywords.process(entry, uniProtGraph)
        if(index % commitAfterEntries == 0) {
          println { s"Keywprds: committing after ${index} entries" }
          titan.commit()
        }
    }

  def importAnnotations(commitAfterEntries: Int = 1000) =
    entries.zipWithIndex foreach {

      case (entry, index) =>
        uniProtImport.annotations.process(entry, uniProtGraph)
        if(index % commitAfterEntries == 0) {
          println { s"Annotations: committing after ${index} entries" }
          titan.commit()
        }
    }

  def importIsoforms(commitAfterEntries: Int = 1000) =
    entries.zipWithIndex foreach {

      case (entry, index) =>
        uniProtImport.isoforms.process(entry, uniProtGraph)
        if(index % commitAfterEntries == 0) {
          println { s"Isoforms: committing after ${index} entries" }
          titan.commit()
        }
    }

  // TODO get FASTA with sequences etc
  def importIsoformSequences(commitAfterEntries: Int = 1000) =
    isoformSequences.zipWithIndex foreach {

      case (fa, index) =>
        uniProtImport.isoformSequences.process(uniprot.IsoformFasta(fa), uniProtGraph)
        if(index % commitAfterEntries == 0) {
          println { s"Isoform sequences: committing after ${index} entries" }
          titan.commit()
        }
    }
}
