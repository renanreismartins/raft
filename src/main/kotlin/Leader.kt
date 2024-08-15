package org.example


//TODO maybe we should make all the constructors except the Followe as private
// this way all nodes can only be initialized as Follower and only transition to a new
// state thought the state machine
data class Leader(
    override val address: Source,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Destination>,
    override val messages: Messages = Messages(),
    override val config: Config = Config(),
): Node(address, name, state, network, peers) {

    override fun handleMessage(message: Message): Node {
        return when(message) {
            is Heartbeat -> this.copy(state = state + message.content.toInt())
            is RequestForVotes -> this
            is VoteFromFollower -> this
        }
    }

    override fun tickWithoutSideEffects(): Node {
        val tickMessages = network.get(this.address)

        val node = tickMessages.fold(this as Node) { node, msg ->
            node.process(msg)
        }

        val nodesWeHaveSentMessagesTo =
            // TODO: Remember the buffer! The OR can be replaced with appending the buffer content
            node.sent()
                .filter { it.sentAt > network.clock - config.heartbeatTimeout || it.sentAt == network.clock }
                .map { it.message.dest }

        val nodesWeNeedToSendHeartbeatTo = peers.toSet() - nodesWeHaveSentMessagesTo.toSet()

        val heartbeats = nodesWeNeedToSendHeartbeatTo.map { Heartbeat(address, it, "0") }

        return node.add(*heartbeats.map { SentMessage(it, network.clock) }.toTypedArray())
    }

    //TODO It seems the 'received =' can be removed as Kotlin can infer the type
    override fun add(vararg message: SentMessage): Leader = this.copy(messages = messages.copy(sent = sent() + message))
    override fun add(vararg message: ReceivedMessage): Leader = this.copy(messages = messages.copy(received = received() + message))

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }
}
