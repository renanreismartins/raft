package org.example

data class TimeMachine(val network: Network, val nodeA: Node, val nodeB: Node) {
    fun tick(): TimeMachine {
        return this.copy(
            network = network.tick(), nodeA = nodeA.tick(), nodeB = nodeB.tick()
        )
    }

    fun tick(count: Int): TimeMachine = (0..count).fold(this) { acc, _ -> acc.tick() }
}
