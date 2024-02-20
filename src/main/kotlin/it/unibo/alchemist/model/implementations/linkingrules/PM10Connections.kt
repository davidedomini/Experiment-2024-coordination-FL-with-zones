package it.unibo.alchemist.model.implementations.linkingrules

import it.unibo.alchemist.model.implementations.neighborhoods.Neighborhoods
import it.unibo.alchemist.model.interfaces.*

class PM10Connections : LinkingRule<Any, GeoPosition> {

    override fun computeNeighborhood(
        center: Node<Any>?,
        environment: Environment<Any, GeoPosition>?
    ): Neighborhood<Any> {
        val rangeLink = 800_000.0
        val close = environment!!
            .getNodesWithinRange(center, rangeLink)
            .associateWith { environment.getDistanceBetweenNodes(center, it) }
        return Neighborhoods.make(environment, center!!, close.keys)
    }

    override fun isLocallyConsistent(): Boolean = true

}