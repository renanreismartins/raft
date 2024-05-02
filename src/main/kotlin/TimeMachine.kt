package org.example

data class TimeMachine(val network: Network, val nodeA: Node, val nodeB: Node) {
    fun tick(): TimeMachine {
        return this.copy(
            // the Network must come first, as the Nodes rely on its clock when they tick
            network = network.tick(),
            nodeA = nodeA.tick(),
            nodeB = nodeB.tick()
        )
    }

    fun tick(count: Int): TimeMachine = (0..count).fold(this) { acc, _ -> acc.tick() }
}
