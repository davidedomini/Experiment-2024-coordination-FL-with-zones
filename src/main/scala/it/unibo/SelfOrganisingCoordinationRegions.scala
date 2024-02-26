package it.unibo

import it.unibo.alchemist.model.implementations.nodes.SimpleNodeManager
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import de.siegmar.fastcsv.reader.CsvReader
import it.unibo.PythonModules._
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.PyQuote
import java.nio.file.Files
import java.nio.file.Paths

class SelfOrganisingCoordinationRegions
  extends AggregateProgram
    with BuildingBlocks
    with StandardSensors
    with ScafiAlchemistSupport {

  private val radius = 200_000
  private val localModel: py.Dynamic = utils.rnn_factory()
  private val every = 5
  private val epochs = 2

  override def main(): Unit = {
    val eoiCode = findStationCode()
    val data: py.Dynamic = utils.get_dataset(eoiCode)
    val aggregators = S(radius, nbrRange)
    rep((localModel, 0)) { p =>
      val model = p._1
      val tick = p._2 + 1
      val (evolvedModel, trainLoss, valLoss) = evolve(model, data)
      node.put("TrainLoss", trainLoss)
      node.put("ValidationLoss", valLoss)
      val potential = classicGradient(aggregators)
      val info = C[Double, Set[py.Dynamic]](potential, _ ++ _, Set(sample(evolvedModel)), Set.empty)
      val aggregatedModel = averageWeights(info)
      val sharedModel = broadcast(aggregators, aggregatedModel)
      if (aggregators) { snapshot(sharedModel, eoiCode, tick-1) }
      mux(impulsesEvery(tick)){ (averageWeights(Set(sample(sharedModel), sample(evolvedModel))), tick) } { (evolvedModel, tick) }
    }
  }

  private def averageWeights(models: Set[py.Dynamic]): py.Dynamic = {
    val averageWeights = utils.average_weights(models.toSeq.toPythonProxy)
    val freshRNN = utils.rnn_factory()
    freshRNN.load_state_dict(averageWeights)
    freshRNN
  }

  private def sample(model: py.Dynamic): py.Dynamic = model.state_dict()

  private def evolve(model: py.Dynamic, data: py.Dynamic): (py.Dynamic, py.Dynamic, py.Dynamic) = {
    val trainLoader = data.get_train_loader()
    val valLoader = data.get_val_loader()
    val result = utils.local_train(model, epochs, trainLoader, valLoader)
    val trainLoss = py"$result[0]"
    val valLoss = py"$result[1]"
    val newWeights = py"$result[2]"
    val freshRNN = utils.rnn_factory()
    freshRNN.load_state_dict(newWeights)
    (freshRNN, trainLoss, valLoss)
  }

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

  private def snapshot(model: py.Dynamic, eoiCode: String, time: Int): Unit = {
    torch.save(
      model.state_dict(),
        s"networks/station-$eoiCode-$time"
    )
  }

}
