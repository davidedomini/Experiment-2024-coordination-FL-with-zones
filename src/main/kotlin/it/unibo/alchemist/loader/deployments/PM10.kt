package it.unibo.alchemist.loader.deployments

import java.io.File
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlinx.serialization.json.Json
import it.unibo.alchemist.model.interfaces.GeoPosition
import it.unibo.alchemist.model.implementations.positions.LatLongPosition
import kotlinx.serialization.Serializable

@Serializable
data class Station (
    val name: String,
    val latitude: String,
    val longitude: String,
    val file_path: String
)

class PM10 : Deployment<GeoPosition> {

    override fun stream(): Stream<GeoPosition> {
        return loadFromJson()
            .map {
                LatLongPosition(it.latitude.doublyfy(), it.longitude.doublyfy())
            }
            .asSequence()
            .asStream()
    }

    companion object {
        private fun Any?.doublyfy(): Double = when(this) {
            is Double -> this
            is String -> replace(",", ".").toDouble()
            is Int -> toDouble()
            is Long -> toDouble()
            is Float -> toDouble()
            else -> throw IllegalArgumentException("Cannot convert $this to Double")
        }

        private fun loadFromJson(): List<Station> =
            Json.decodeFromString<List<Station>>(File("PM10-data/data-summary3.json").readText())
    }

}