import org.example.Heartbeat
import org.example.Network
import org.example.NetworkMessage
import org.example.Address
import org.example.Destination
import org.example.Source
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NetworkTests {

    @Test
    fun `Given a message, delivers it when the network tick matches the deliver time`() {
        // Given
        val source = Source("127.0.0.1", 8000)
        val destination = Destination("127.0.0.1", 8001)
        val initialMessages = mapOf<Address, List<NetworkMessage>>(
            destination to listOf(
                NetworkMessage(Heartbeat(source, destination, "A"), 1),
                NetworkMessage(Heartbeat(source, destination, "A"), 2)
            )
        )
        val network = Network(initialMessages)

        // When
        network.tick()
        val receivedMessages = network.get(destination)

        // Then
        assertEquals(1, receivedMessages.size)
        assertEquals(Heartbeat(source, destination, "A"), receivedMessages.first());
    }

    @Test
    fun `Deliver all the messages that delivery time is at or behind the network clock`() {
        // Given
        val source = Source("127.0.0.1", 8000)
        val destination = Destination("127.0.0.1", 8001)
        val initialMessages = mapOf<Address, List<NetworkMessage>>(
            destination to listOf(
                NetworkMessage(Heartbeat(source, destination, "A"), 1),
                NetworkMessage(Heartbeat(source, destination, "B"), 2)
            )
        )
        val network = Network(initialMessages)

        // When
        network.tick()
        network.tick()

        val receivedMessages = network.get(destination)

        // Then
        assertEquals(2, receivedMessages.size)
        assertEquals(Heartbeat(source, destination, "A"), receivedMessages.first())
        assertEquals(Heartbeat(source, destination, "B"), receivedMessages.last())
    }

    @Test
    fun `Adding a message to the network with delay 0, the message will be delivered at the next tick`() {
        // Given
        val source = Source("127.0.0.1", 8000)
        val destination = Destination("127.0.0.1", 8001)

        val network = Network()
        network.tick()

        network.add(Heartbeat(source, destination, "A"), 0)
        network.tick()

        // When
        val received = network.get(destination)

        // Then
        assertEquals(1, received.size)
        assertEquals(Heartbeat(source, destination, "A"), received.first())
    }

    @Test
    fun `Messages for previous ticks are delivered on the next network time`() {
        // Given
        val source = Source("127.0.0.1", 8000)
        val destination = Destination("127.0.0.1", 8001)

        val network = Network()
        network.add(Heartbeat(source, destination, "A"), 0)
        network.add(Heartbeat(source, destination, "B"), 0)
        network.tick()
        network.tick()

        // When
        val received = network.get(destination)

        // Then
        assertEquals(2, received.size)
        assertEquals(Heartbeat(source, destination, "A"), received.first())
        assertEquals(Heartbeat(source, destination, "B"), received.last())
    }
}