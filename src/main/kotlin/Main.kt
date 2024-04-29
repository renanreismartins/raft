package org.example

fun main() {
    println("Hello World!")
}

typealias NetworkDelay = Int
typealias DeliveryTime = Int
data class NetworkMessage(val message: Message, val deliveryTime: DeliveryTime)

class Network(initialMessages: Map<Address, List<NetworkMessage>> = emptyMap()) {
    var clock = 0
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

// TODO: Figure out a good type for this when we start cleaning
data class Config(val electionTimeout: Int = 3)
data class Message(val src: Address, val dest: Address, val content: String)
data class Address(val host: String, val port: Int)


typealias ReceivedAt = Int
typealias MessageLogEntry = Pair<ReceivedAt, Message>
abstract class Node(
    open val address: Address,
    open val name: String,
    open val state: Int,
    open val network: Network,
    open val peers: List<Address>,
    open val messages: List<MessageLogEntry> = emptyList(),
    open val config: Config = Config(),
) {
    fun tick(ticks: Int): Node {
        return (0 .. ticks).fold(this) { acc, _ -> acc.tick() }
    }
    abstract fun tick(): Node
    abstract fun receive(message: Message): Node
    fun send(destination: Address, content: String) {
        network.add(Message(this.address, destination, content)) // TODO add tiny type Source and Destination
    }
}

data class Candidate(override val address: Address,
     override val name: String,
     override val state: Int = 0,
     override val network: Network,
     override val peers: List<Address>,
     override val messages: List<MessageLogEntry> = emptyList(),
     override val config: Config = Config(),
): Node(address, name, state, network, peers, messages) {

    override fun tick(): Node {
        TODO("Not yet implemented")
    }

    //TODO MOVE TO NODE
    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

}

data class Follower(
    override val address: Address,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Address>,
    override val messages: List<MessageLogEntry> = emptyList(),
    override val config: Config = Config(),
): Node(address, name, state, network, peers, messages) {

    override fun tick(): Node {
        val tickMessages = network.get(this.address)

        val newState = tickMessages.fold(state) { acc, msg ->
            println("RECEIVED $msg")
            acc
//            msg.content.toInt() + acc
        }

        val messageLog = messages + tickMessages.map { network.clock to it }

        if (messageLog.isEmpty() && network.clock > config.electionTimeout) {
            val candidate = Candidate(address, name, state, network, peers, messageLog)

            peers.forEach { peer -> candidate.send(peer, "REQUEST FOR VOTES") } // TODO Refactor to return the messages to be sent instead of a side effect

            return candidate
        }

        return if (messageLog.isNotEmpty() && network.clock - messageLog.last().first > config.electionTimeout) Candidate(address, name, state, network, peers, messageLog) else return this.copy(state = newState, messages = messageLog)
    }

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }
}