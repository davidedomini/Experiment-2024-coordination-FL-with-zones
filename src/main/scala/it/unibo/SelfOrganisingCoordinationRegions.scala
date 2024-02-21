package it.unibo

import it.unibo.alchemist.model.implementations.nodes.SimpleNodeManager
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import de.siegmar.fastcsv.reader.CsvReader
import it.unibo.PythonModules._
import me.shadaj.scalapy.py
import java.nio.file.Files
import java.nio.file.Paths

class SelfOrganisingCoordinationRegions
  extends AggregateProgram
    with BuildingBlocks
    with StandardSensors
    with ScafiAlchemistSupport {

  private val radius = 200 // TODO - update
  private val localModel: py.Dynamic = utils.init_local_model()
  private val every = 5 // TODO - check

  override def main(): Unit = {
    node.put("id", mid())
    val eoiCode = findStationCode() // TODO - check code name
    val data: py.Dynamic = utils.get_dataset(eoiCode)
    val aggregators = S(radius, nbrRange)
    rep((localModel, 0)) { p => // TODO - add evaluation and logging
      val model = p._1
      val tick = p._2
      val evolvedModel = evolve(model, data)
      val potential = classicGradient(aggregators)
      val info = C[Double, Set[py.Dynamic]](potential, _ ++ _, Set(evolvedModel), Set.empty)
      val aggregatedModel = averageWeights(info)
      val sharedModel = broadcast(aggregators, aggregatedModel)
        mux(impulsesEvery(tick)){ (averageWeights(Set(sharedModel, evolvedModel)), tick+1) } { (evolvedModel, tick+1) }
    }
  }

  private def averageWeights(models: Set[py.Dynamic]): py.Dynamic = ???
  private def evolve(model: py.Dynamic, data: py.Dynamic): py.Dynamic = ???
  private def evaluate(model: py.Dynamic, data: py.Dynamic): Int = ???

  private def impulsesEvery(time: Int): Boolean = time % every == 0

  private def findStationCode()= {
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
