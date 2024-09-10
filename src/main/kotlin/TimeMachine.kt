package org.example

class TimeMachine(
    initialNetwork: Network,
    vararg initialNodes: Node,
) {
    /**
     * The Network must come first, as the Nodes rely on its clock when they tick
     * E.g. NodeA sends to NodeB on tick 0, the earliest this message will arrive
     *      is tick 1. If we tick NodeB before ticking the Network, NodeB will
     *      still be on tick 0. If we tick the network first, we move to tick 1, and
     *      then when NodeB ticks, it will be on tick 1.
     */

    private var network = initialNetwork
    private var nodes = initialNodes

    fun tick(): TimeMachine {
        network.tick()
        nodes = nodes.map(Node::tick).toTypedArray()
        return this
    }

    fun tick(count: Int): TimeMachine = (0..count).fold(this) { acc, _ -> acc.tick() }

    operator fun component1(): Network = network

    operator fun component2(): Node = nodes[0]

    operator fun component3(): Node = nodes[1]

    operator fun component4(): Node = nodes[2]

    operator fun component5(): Node = nodes[3]
}
