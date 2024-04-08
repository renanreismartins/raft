package org.example

fun main() {
    println("Hello World!")
}


val network = mutableMapOf<Address, List<Message>>()


typealias NetworkDelay = Int
data class NetworkMessage(val message: Message, val delay: NetworkDelay)

/**
 * TODO: We need to convert to absolute times for consumptions. Otherwise we have problems because
 * delay is relative.
 * Imagine Node A and B, A ticking first - with A1 meaning A at tick 1
 * A1 ticks and sends message to B, which cannot be consumed on B1, must be at B2
 * B1 ticks and sends a message to A, which cannot be consumed at A1, must be at A2
 *
 * In the current system, with a relative delay = 0, message A -> B would be consumed at B1, as it's on the network with delay 0 before B ticks
 * But, message B -> A would be correct
 *
 * If you change the default delay to 1, message A -> B would be consumed at B2 (good), but the message B -> A would be wrong
 * The message is sent to A, when A2 ticks, the delay decrements. Instead, we need to tick A again, so the message will only be consumed at A3
 */
class Network(initialMessages: Map<Address, List<NetworkMessage>> = emptyMap()) {
    private val messages = initialMessages.toMutableMap()

    fun get(address: Address): List<Message> {
        // get all messages for address with 0 delay and store
        val (messagesToDeliver, remaining) = messages[address]
            ?.partition { (_, delay) ->
                delay == 0
            } ?: (listOf<NetworkMessage>() to listOf())

        // decrement remaining messages for that address
        messages[address] = remaining.map { networkMessage ->
            networkMessage.copy(delay = networkMessage.delay - 1)
        }

        // return store messages
        return messagesToDeliver.map { it.message }
    }

    fun add(message: Message, delay: NetworkDelay = 0) {
        messages[message.dest] = messages[message.dest]?.plus(NetworkMessage(message, delay)) ?: listOf()
    }
}

data class Message(val src: Address, val dest: Address, val content: String)
data class Address(val host: String, val port: Int)

abstract class Node(open val address: Address, open val name: String, open val clock: Int = 0, open val state: Int) {
    abstract fun tick(): Node
    abstract fun receive(message: Message): Node
    abstract fun send(address: Address, content: String)
}

data class Candidate(override val address: Address,
     override val name: String,
     override val clock: Int = 0,
     override val state: Int = 0
): Node(address, name, clock, state) {

    override fun tick(): Node {
        TODO("Not yet implemented")
    }

    //TODO MOVE TO NODE
    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

    //TODO MOVE TO NODE
    override fun send(address: Address, content: String) {
        TODO("Not yet implemented")
    }

}

data class Follower(
    override val address: Address,
    override val name: String,
    override val clock: Int = 0,
    override val state: Int = 0
): Node(address, name, clock, state) {

    //TODO: This has to be moved to the Node class
    override fun tick(): Node {
        val messages = receiveMessages()

        val newState = messages.fold(state) { acc, msg ->
            msg.content.toInt() + acc
        }

        return this.copy(clock =  clock + 1, state = newState)
    }

    private fun receiveMessages(): List<Message> {
        val messages = network.getOrDefault(this.address, emptyList())
        network[this.address] = emptyList()
        return messages
    }

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

    override fun send(destination: Address, content: String) {
        val message = Message(this.address, destination, content) // TODO add tiny type Source and Destination
        val messages = network.getOrDefault(destination, emptyList()) + message
        network[destination] = messages
    }
}
