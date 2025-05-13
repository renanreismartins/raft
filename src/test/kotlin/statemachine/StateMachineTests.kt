package statemachine

import org.example.Destination
import org.example.Heartbeat
import org.example.Network
import org.example.ReceivedMessage
import org.example.Source
import org.example.statemachine.StateMachine
import org.example.statemachine.TimeMachine2
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StateMachineTests {
    @Test
    fun `A Node can send a message to another Node`() {
        // Given
        val network = Network()

        val nodeBAddress = Source("127.0.0.1", 9002)
        val nodeB = StateMachine(nodeBAddress, "NodeB", network = network, peers = emptyList())
        val nodeA = StateMachine(Source("127.0.0.1", 9001), "NodeA", network = network, peers = listOf(Destination.from(nodeBAddress)))
        val message = Heartbeat(nodeA.address, Destination.from(nodeB.address), 0, "1")

        // When
        nodeA.send(message)
        val (_, _, newNodeB) = TimeMachine2(network, nodeA, nodeB).tick()

        // Then
        assertEquals(
            listOf(ReceivedMessage(message, 1)),
            newNodeB.received()
        )
    }

    @Test
    fun `A Node store a message after its receival`() {
        // Given
        val network = Network()
        val message = Heartbeat(Source("127.0.0.1", 9002), Destination("127.0.0.1", 9001), 0, "")
        network.add(message)
        val node = StateMachine(Source("127.0.0.1", 9001), "NodeA", network = network, peers = listOf())

        // When
        val (_, nodeWithMessage) = TimeMachine2(network, node).tick()

        // Then
        assertEquals(message, nodeWithMessage.received().first().message)
    }
}