package org.example

// TODO: Figure out a good type for this when we start cleaning
data class Config(val electionTimeout: Int = 5, val heartbeatTimeout: Int = 2)

typealias Timestamp = Int //TODO Make this a Comparable, so when we change it to a 'Date' type for the real world, we do not need to change the usages

// TODO: Make a map <Destination, List<ReceivedMessage>> for the received
data class Messages(val received: List<ReceivedMessage> = emptyList(), val sent: List<SentMessage> = emptyList(), val toSend: List<Message> = emptyList()) {
    //TODO is flush the best name to represent this operation
    //TODO After this change, do we still need the extension methods Message.toSent() and toReceived?
    fun flush(sentAt: Int): Messages {
        return this.copy(
            sent = sent + toSend.map { SentMessage(it, sentAt) },
            toSend = emptyList()
        )
    }

    fun toSend(message: Message): Messages {
        return this.copy(toSend = toSend + message)
    }

    fun toSend(newMessages: List<Message>): Messages {
        return this.copy(toSend = toSend + newMessages)
    }
}
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
        node.messages.toSend.forEach { send(it) } // TODO encapsulate access to messages?
        return node.flushMessages()
    }


    private fun flushMessages(): Node {
        return when (this) {
            is Follower -> this.copy(messages = messages.flush(network.clock) )
            is Candidate -> this.copy(messages = messages.flush(network.clock) )
            is Leader -> this.copy(messages = messages.flush(network.clock) )
        }
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
    fun toSend(message: Message): Node {
        return when (this) {
            is Follower -> this.copy(messages = messages.toSend(message) )
            is Candidate -> this.copy(messages = messages.toSend(message) )
            is Leader -> this.copy(messages = messages.toSend(message) )
        }
    }

    fun toSend(newMessages: List<Message>): Node {
        return when (this) {
            is Follower -> this.copy(messages = messages.toSend(newMessages) )
            is Candidate -> this.copy(messages = messages.toSend(newMessages) )
            is Leader -> this.copy(messages = messages.toSend(newMessages) )
        }
    }

    fun send(message: Message) {
        network.add(message)
    }

    fun Message.toSent(): SentMessage = SentMessage(this, network.clock)
    fun Message.toReceived(): ReceivedMessage = ReceivedMessage(this, network.clock)

    fun sent() = messages.sent
    fun received() = messages.received
}