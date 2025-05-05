package org.example.statemachine

import org.example.Network

class TimeMachine2(
    val network: Network,
    vararg val nodes: StateMachine,
) {
    /**
     * The Network must come first, as the Nodes rely on its clock when they tick
     * E.g. NodeA sends to NodeB on tick 0, the earliest this message will arrive
     *      is tick 1. If we tick NodeB before ticking the Network, NodeB will
     *      still be on tick 0. If we tick the network first, we move to tick 1, and
     *      then when NodeB ticks, it will be on tick 1.
     */

    fun tick(): TimeMachine2 =
        TimeMachine2(
            network = network.tick(),
            *nodes.map(StateMachine::tick).toTypedArray(),
        )

    fun tick(count: Int): TimeMachine2 = (0..< count).fold(this) { acc, _ -> acc.tick() }

    operator fun component1(): Network = network

    operator fun component2(): StateMachine = nodes[0]

    operator fun component3(): StateMachine = nodes[1]

    operator fun component4(): StateMachine = nodes[2]

    operator fun component5(): StateMachine = nodes[3]
}