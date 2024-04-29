package org.example

// TODO: Figure out a good type for this when we start cleaning
data class Config(val electionTimeout: Int = 3)

typealias ReceivedAt = Int
typealias MessageLogEntry = Pair<ReceivedAt, Message>

data class Message(val src: Address, val dest: Address, val content: String)
data class Address(val host: String, val port: Int)

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