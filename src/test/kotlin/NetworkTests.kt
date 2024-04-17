import org.example.Address
import org.example.Message
import org.example.Network
import org.example.NetworkMessage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NetworkTests {

    @Test
    fun `given a message, delivers it when the network tick matches the deliver time`() {
        // Given
        val source = Address("127.0.0.1", 8000)
        val destination = Address("127.0.0.1", 8001)
        val initialMessages = mapOf(
            destination to listOf(
                NetworkMessage(Message(source, destination, "A"), 1),
                NetworkMessage(Message(source, destination, "A"), 2)
            )
        )
        val network = Network(initialMessages)

        // When
        network.tick()
        val receivedMessages = network.get(destination)

        // Then
        assertEquals(1, receivedMessages.size)
        assertEquals(Message(source, destination, "A"), receivedMessages.first());
    }

    @Test
    fun `deliver all the messages that delivery time is at or behind the network clock`() {
        // Given
        val source = Address("127.0.0.1", 8000)
        val destination = Address("127.0.0.1", 8001)
        val initialMessages = mapOf(
            destination to listOf(
                NetworkMessage(Message(source, destination, "A"), 1),
                NetworkMessage(Message(source, destination, "B"), 2)
            )
        )
        val network = Network(initialMessages)

        // When
        network.tick()
        network.tick()

        val receivedMessages = network.get(destination)

        // Then
        assertEquals(2, receivedMessages.size)
        assertEquals(Message(source, destination, "A"), receivedMessages.first())
        assertEquals(Message(source, destination, "B"), receivedMessages.last())
    }

    @Test
    fun `adding a message to the network with delay 0, the message will be delivered at the next tick`() {
        // Given
        val source = Address("127.0.0.1", 8000)
        val destination = Address("127.0.0.1", 8001)

        val network = Network()
        network.tick()

        network.add(Message(source, destination, "A"), 0)
        network.tick()

        // When
        val received = network.get(destination)

        // Then
        assertEquals(1, received.size)
        assertEquals(Message(source, destination, "A"), received.first())
    }

    @Test
    fun `Messages for previous ticks are delivered on the next network time`() {
        // Given
        val source = Address("127.0.0.1", 8000)
        val destination = Address("127.0.0.1", 8001)

        val network = Network()
        network.add(Message(source, destination, "A"), 0)
        network.add(Message(source, destination, "B"), 0)
        network.tick()
        network.tick()

        // When
        val received = network.get(destination)

        // Then
        assertEquals(2, received.size)
        assertEquals(Message(source, destination, "A"), received.first())
        assertEquals(Message(source, destination, "B"), received.last())
    }
}