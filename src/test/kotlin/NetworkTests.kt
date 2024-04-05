import org.example.Address
import org.example.Message
import org.example.Network
import org.example.NetworkMessage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NetworkTests {

    @Test
    fun `get messages from address returns messages with delay 0 and decrements delay of other messages`() {
        val source = Address("127.0.0.1", 8000)
        val destination = Address("127.0.0.1", 8001)
        val initialMessages = mapOf(
            destination to listOf(
                NetworkMessage(Message(source, destination, "A"), 0),
                NetworkMessage(Message(source, destination, "B"), 1)
            )
        )
        val network = Network(initialMessages)

        val removed = network.get(destination)
        assertEquals(listOf(Message(source, destination, "A")), removed)

        val remaining = network.get(destination)
        assertEquals(listOf(Message(source, destination, "B")), remaining)
    }
}