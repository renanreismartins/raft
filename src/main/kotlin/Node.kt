package org.example

// TODO: Figure out a good type for this when we start cleaning
data class Config(val electionTimeout: Int = 3)

typealias ReceivedAt = Int //TODO Make this a Comparable, so when we change it to a 'Date' type for the real world, we do not need to change the usages
typealias MessageLogEntry = Pair<ReceivedAt, Message> // TODO Make a real class with named props

data class Address(val host: String, val port: Int)


// TODO add tiny type Source and Destination
sealed class Message(open val src: Address, open val dest: Address, open val content: String) //TODO remove 'content' from the Message and add it to the subclasses if we have one type of Message without 'content'
data class RequestForVotes(override val src: Address, override val dest: Address, override val content: String) : Message(src, dest, content)
data class VoteFromFollower(override val src: Address, override val dest: Address, override val content: String) : Message(src, dest, content)
data class Heartbeat(override val src: Address, override val dest: Address, override val content: String) : Message(src, dest, content)
// TODO We need to make these generic, and AppendEntries content should be List<T> - find a way to do this with generics, or remove the parent content
//data class AppendEntries(override val src: Address, override val dest: Address, override val content: String) : Message(src, dest, content)


abstract class Node(
    open val address: Address,
    open val name: String,
    open val state: Int,
    open val network: Network,
    open val peers: List<Address>,
    open val received: List<MessageLogEntry> = emptyList(),
    open val sent: List<MessageLogEntry> = emptyList(), //TODO create a different type for sent and received
    open val config: Config = Config(),
) {
    fun tick(ticks: Int): Node {
        return (0 .. ticks).fold(this) { acc, _ -> acc.tick() }
    }
    abstract fun tick(): Node
    abstract fun receive(message: Message): Node
    fun send(message: Message) {
        network.add(message)
    }
}