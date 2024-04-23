package org.example

fun main() {
    println("Hello World!")
}

typealias NetworkDelay = Int
typealias DeliveryTime = Int
data class NetworkMessage(val message: Message, val deliveryTime: DeliveryTime)

class Network(initialMessages: Map<Address, List<NetworkMessage>> = emptyMap()) {
    private var clock = 0
    private val messages = initialMessages.toMutableMap()

    fun get(address: Address): List<Message> {
        // get all messages for address with 0 delay and store
        val (messagesToDeliver, remaining) = messages[address]
            ?.partition { (_, deliveryTime) ->
                deliveryTime <= clock
            } ?: (listOf<NetworkMessage>() to listOf())

        messages[address] = remaining

        // return store messages
        return messagesToDeliver.map { it.message }
    }

    fun add(message: Message, delay: NetworkDelay = 0) {
        val networkMessage = NetworkMessage(message, clock + 1 + delay)
        messages[message.dest] = messages[message.dest]?.plus(networkMessage) ?: listOf(networkMessage)
    }

    fun tick(ticks: Int = 1): Network {
        clock += ticks
        return this
    }
}

data class Message(val src: Address, val dest: Address, val content: String)
data class Address(val host: String, val port: Int)

abstract class Node(
    open val address: Address,
    open val name: String,
    open val clock: Int = 0,
    open val state: Int,
    open val network: Network,
    open val messages: List<Message> = emptyList()
) {
    fun tick(ticks: Int): Node {
        return (0 .. ticks).fold(this) { acc, _ -> acc.tick() }
    }
    abstract fun tick(): Node
    abstract fun receive(message: Message): Node
    abstract fun send(address: Address, content: String)
}

data class Candidate(override val address: Address,
     override val name: String,
     override val clock: Int = 0,
     override val state: Int = 0,
     override val network: Network,
     override val messages: List<Message> = emptyList()
): Node(address, name, clock, state, network, messages) {

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
    override val state: Int = 0,
    override val network: Network,
    override val messages: List<Message> = emptyList(),
): Node(address, name, clock, state, network, messages) {

    //TODO: This has to be moved to the Node class
    override fun tick(): Node {
        val tickMessages = network.get(this.address)

        val newState = tickMessages.fold(state) { acc, msg ->
            msg.content.toInt() + acc
        }


        return this.copy(clock = clock + 1, state = newState, messages = messages + tickMessages)
    }

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

    override fun send(destination: Address, content: String) {
        network.add(Message(this.address, destination, content)) // TODO add tiny type Source and Destination
    }
}
