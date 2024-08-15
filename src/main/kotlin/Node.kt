package org.example

// TODO: Figure out a good type for this when we start cleaning
data class Config(val electionTimeout: Int = 5, val heartbeatTimeout: Int = 2)

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


typealias Timestamp = Int //TODO Make this a Comparable, so when we change it to a 'Date' type for the real world, we do not need to change the usages

// TODO: Make a map <Destination, List<ReceivedMessage>> for the received
data class Messages(val received: List<ReceivedMessage> = emptyList(), val sent: List<SentMessage> = emptyList(), val toSend: List<Message> = emptyList())
data class SentMessage(val message: Message, val sentAt: Timestamp)
data class ReceivedMessage(val message: Message, val receivedAt: Timestamp)

// TODO add tiny type Source and Destination
sealed class Message(open val src: Source, open val dest: Destination, open val content: String) //TODO remove 'content' from the Message and add it to the subclasses if we have one type of Message without 'content'
data class RequestForVotes(override val src: Source, override val dest: Destination, override val content: String) : Message(src, dest, content)
data class VoteFromFollower(override val src: Source, override val dest: Destination, override val content: String) : Message(src, dest, content)
data class Heartbeat(override val src: Source, override val dest: Destination, override val content: String) : Message(src, dest, content)
// TODO We need to make these generic, and AppendEntries content should be List<T> - find a way to do this with generics, or remove the parent content
//data class AppendEntries(override val src: Address, override val dest: Address, override val content: String) : Message(src, dest, content)


sealed class Node(
    open val address: Source,
    open val name: String,
    open val state: Int,
    open val network: Network,
    open val peers: List<Destination>,
    open val messages: Messages = Messages(),
    open val config: Config = Config(),
) {
    fun tick(ticks: Int): Node {
        return (0 .. ticks).fold(this) { acc, _ -> acc.tick() }
    }

    fun tick(): Node {
        val node = tickWithoutSideEffects()
        // TODO: All 'pending' messages are also added to the sent list, but we haven't sent them yet.
        //   instead, we should add a 'buffer' on the Node, which gets added to in process/handleMessage and then flushed here
        node.sentMessages().filter { it.sentAt == network.clock }.forEach { send(it.message) }
        return node
    }

    abstract fun tickWithoutSideEffects(): Node
    abstract fun handleMessage(message: Message): Node

    fun process(received: Message): Node {
        return add(received.toReceived()).handleMessage(received)
    }
    abstract fun receive(message: Message): Node

    //TODO add receiving a list would avoid having to do the convoluted calls transforming a list in a typedArray and then using the * to destruct the array?
    // as in node.add(*heartbeats.map { SentMessage(it, network.clock) }.toTypedArray())
    abstract fun add(vararg message: SentMessage): Node
    abstract fun add(vararg message: ReceivedMessage): Node

    fun send(message: Message) {
        network.add(message)
    }

    fun Message.toSent(): SentMessage = SentMessage(this, network.clock)
    fun Message.toReceived(): ReceivedMessage = ReceivedMessage(this, network.clock)

    fun sentMessages() = messages.sent
    fun receivedMessages() = messages.received
}