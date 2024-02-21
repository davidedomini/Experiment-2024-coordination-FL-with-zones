package it.unibo

import scala.io.Source
import it.unibo.alchemist.model.implementations.nodes.SimpleNodeManager
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import de.siegmar.fastcsv.reader.CsvReader

import java.io.{File, FileInputStream, InputStream}
import java.nio.file.Files
import java.nio.file.Paths

class SelfOrganisingCoordinationRegions
  extends AggregateProgram
    with BuildingBlocks
    with StandardSensors
    with ScafiAlchemistSupport {

  private val radius = 1 // TODO - update

  override def main(): Unit = {
    node.put("id", mid())
    node.put("Code", loadData())

    /*val initialModel: Int = mid()
    val leader = S(radius, nbrRange)
    val potential = distanceTo(leader)
    if (leader) node.put("Leader", true)

    rep((initialModel, 0)) { p =>
      val localModel = p._1
      val tick = p._2
      val models = collectIntoSet(potential, localModel)
      val aggregateModel = averageWeights(models)
      val zoneModel = broadcast(leader, aggregateModel)
      broadcast(leader, zoneModel)
      node.put("ID", mid())
      node.put("Zone model", zoneModel)
      (zoneModel, tick + 1)
    }*/

  }

  //private def averageWeights(models: Set[Int]): Int = (models.sum / models.size).toInt

  private def loadData()= {
    val n = node.asInstanceOf[SimpleNodeManager[Any]].node
    val pos = alchemistEnvironment.getPosition(n)
    val lat = BigDecimal(pos.getCoordinate(0)).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble.toString
    val lon = BigDecimal(pos.getCoordinate(1)).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble.toString
    val f = new String(Files.readAllBytes(Paths.get("pm10locationsrounded.csv")))
    CsvReader.builder().ofNamedCsvRecord(f)
      .stream()
      .filter(nr => checkLatLon(lat, lon, nr.getField("Latitude"), nr.getField("Longitude")))
      .map(_.getField("Name"))
      .toList
      .get(0)
  }

  private def checkLatLon(nodeLat: String, nodeLon: String, csvLat: String, csvLon: String): Boolean =
    csvLat.contains(nodeLat) && csvLon.contains(nodeLon)

}
