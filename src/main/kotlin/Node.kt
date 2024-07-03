package org.example

// TODO: Figure out a good type for this when we start cleaning
data class Config(val electionTimeout: Int = 3)

sealed class Address(open val host: String, open val port: Int)
data class Source(override val host: String, override val port: Int) : Address(host, port) {
    companion object {
        fun from(dest: Destination) : Source {
            return Source(dest.host, dest.port)
        }

        fun from(dest: List<Destination>) : List<Source> {
            return dest.map { it -> Source(it.host, it.port) }
        }
    }
}
data class Destination(override val host: String, override val port: Int) : Address(host, port) {
    companion object {
        fun from(src: Address) : Destination {
            return Destination(src.host, src.port)
        }

        fun from(src: List<Source>) : List<Destination> {
            return src.map { it -> Destination(it.host, it.port) }
        }
    }
}

typealias ReceivedAt = Int //TODO Make this a Comparable, so when we change it to a 'Date' type for the real world, we do not need to change the usages

data class SentMessage(val message: Message, val sentAt: ReceivedAt)
data class ReceivedMessage(val message: Message, val receivedAt: ReceivedAt)

// TODO add tiny type Source and Destination
sealed class Message(open val src: Source, open val dest: Destination, open val content: String) //TODO remove 'content' from the Message and add it to the subclasses if we have one type of Message without 'content'
data class RequestForVotes(override val src: Source, override val dest: Destination, override val content: String) : Message(src, dest, content)
data class VoteFromFollower(override val src: Source, override val dest: Destination, override val content: String) : Message(src, dest, content)
data class Heartbeat(override val src: Source, override val dest: Destination, override val content: String) : Message(src, dest, content)
// TODO We need to make these generic, and AppendEntries content should be List<T> - find a way to do this with generics, or remove the parent content
//data class AppendEntries(override val src: Address, override val dest: Address, override val content: String) : Message(src, dest, content)


abstract class Node(
    open val address: Address,
    open val name: String,
    open val state: Int,
    open val network: Network,
    open val peers: List<Destination>,
    open val received: List<ReceivedMessage> = emptyList(),
    open val sent: List<SentMessage> = emptyList(),
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