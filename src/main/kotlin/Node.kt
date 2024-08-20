package org.example

// TODO: Figure out a good type for this when we start cleaning
data class Config(val electionTimeout: Int = 5, val heartbeatTimeout: Int = 2)

sealed class Node(
    open val address: Source,
    open val name: String,
    open val state: Int,
    open val network: Network,
    open val peers: List<Destination>,
    open val messages: Messages = Messages(),
    open val term: Int = 0,
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