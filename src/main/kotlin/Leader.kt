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
    override val log: List<Message> = emptyList(),
    override val term: Int = 1,
    override val config: Config = Config(),
    override val commitIndex: Int = 0,
    override val lastApplied: Int = 0,
    val nextIndex: Map<Destination, Int> = peers.associateWith { log.size },
    val matchIndex: Map<Destination, Int> = peers.associateWith { 0 },
): Node(address, name, state, network, peers) {


    override fun handleMessage(message: Message): Node {
        return when(message) {
            is Heartbeat -> this.copy(state = state + message.content.toInt())
            is RequestForVotes -> this
            is VoteFromFollower -> this
            //TODO ADR to explain our AppendEntries is AppendEntry - we decide to send one message per entry
            is ClientCommand -> this.copy(log = log + message).toSend(message)
            is AppendEntry -> this
            is AppendEntryResponse -> this
        }
    }

    fun toSend(command: ClientCommand) : Node {
        return this.toSend(peers.map { AppendEntry(this.address, it, this.term, command.content) })
    }

    override fun tickWithoutSideEffects(): Node {
        val tickMessages = network.get(this.address)

        val node = tickMessages.fold(this as Node) { node, msg ->
            node.process(msg)
        }

        val nodesWeHaveSentMessagesTo =
            // TODO: encapsulate? also add election term in the future
            node.sent()
                .filter { it.sentAt > network.clock - config.heartbeatTimeout }
                .map { it.message.dest } +
                    node.messages.toSend.map { it.dest }

        val nodesWeNeedToSendHeartbeatTo = peers.toSet() - nodesWeHaveSentMessagesTo.toSet()
        val heartbeats = nodesWeNeedToSendHeartbeatTo.map { Heartbeat(address, it, term, "0") }

        return node.toSend(heartbeats)
    }

    //TODO It seems the 'received =' can be removed as Kotlin can infer the type
    override fun add(vararg message: SentMessage): Leader = this.copy(messages = messages.copy(sent = sent() + message))
    override fun add(vararg message: ReceivedMessage): Leader = this.copy(messages = messages.copy(received = received() + message))

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }
}
