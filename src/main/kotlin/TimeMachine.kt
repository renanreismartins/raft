package org.example

// TODO Make this take vararg of Nodes
data class TimeMachine(val network: Network, val nodeA: Node, val nodeB: Node, val nodeC: Node? = null) {
    /**
     * The Network must come first, as the Nodes rely on its clock when they tick
     * E.g. NodeA sends to NodeB on tick 0, the earliest this message will arrive
     *      is tick 1. If we tick NodeB before ticking the Network, NodeB will
     *      still be on tick 0. If we tick the network first, we move to tick 1, and
     *      then when NodeB ticks, it will be on tick 1.
     */
    fun tick(): TimeMachine {
        return this.copy(
            network = network.tick(),
            nodeA = nodeA.tick(),
            nodeB = nodeB.tick(),
            nodeC = nodeC?.tick()
        )
    }

    fun tick(count: Int): TimeMachine = (0..count).fold(this) { acc, _ -> acc.tick() }
}
