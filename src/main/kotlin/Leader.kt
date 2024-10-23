package org.example

// TODO maybe we should make all the constructors except the Followe as private
// this way all nodes can only be initialized as Follower and only transition to a new
// state thought the state machine
data class Leader(
    override val address: Source,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Destination>,
    override val votedFor: Address? = null,
    override val messages: Messages = Messages(),
    override val log: Log = Log(),
    override val term: Int = 1,
    override val config: Config = Config(),
    override val commitIndex: Int = 0,
    override val lastApplied: Int = 0,
    val nextIndex: Map<Destination, Int> = peers.associateWith { log.size() },
    val matchIndex: Map<Destination, Int> = peers.associateWith { 0 },
) : Node(address, name, state, network, peers, votedFor) {
    override fun handleMessage(message: Message): Node {
        return when (message) {
            is Heartbeat -> this.copy(state = state + message.content.toInt())
            is RequestForVotes -> this
            is VoteFromFollower -> this
            // TODO ADR to explain our AppendEntries is AppendEntry - we decide to send one message per entry
            // TODO when adding to the log, should we increase the commitIndex?
            is ClientCommand -> this.copy(log = log.add(message), commitIndex = this.commitIndex + 1)
            is AppendEntry -> this
            is AppendEntryResponse -> {
                val senderKey = Destination.from(message.src)
                val currentNextIndex = nextIndex.getOrDefault(senderKey, 0)
                val currentMatchIndex = matchIndex.getOrDefault(senderKey, 0)
                if (message.success) {
                    // + 1 because if this was successful the node has replicated the log entry, and
                    // we only send one message per AppendEntriesRPC
                    return this.copy(
                        nextIndex = this.nextIndex + (senderKey to currentNextIndex + 1),
                        matchIndex = this.matchIndex + (senderKey to currentMatchIndex + 1),
                    )
                }

                return this.copy(
                    nextIndex = this.nextIndex + (senderKey to currentNextIndex - 1),
                )
            }
        }
    }

    override fun tickWithoutSideEffects(): Node {
        val tickMessages = network.get(this.address)

        val node =
            tickMessages.fold(this as Node) { node, msg ->
                node.process(msg)
            }

        val nodesWeHaveSentMessagesTo =
            // TODO: encapsulate? also add election term in the future
            node
                .sent()
                .filter { it.sentAt > network.clock - config.heartbeatTimeout }
                .map { it.message.dest } +
                node.messages.toSend.map { it.dest }

        if (node is Leader) {
            // TODO check official implementations on <=
            val updates =
                nextIndex
                    .filter { it.value <= node.lastLogIndex() }
                    // TODO unit test when moving to the Log class
                    .map { AppendEntry(node.address, it.key, "", node.term, node.prevLogIndex(), node.prevLogTerm(), node.commitIndex) }

            val nodesWeNeedToSendHeartbeatTo = peers.toSet() - (nodesWeHaveSentMessagesTo.toSet() + updates.map { it.dest }.toSet())
            val heartbeats = nodesWeNeedToSendHeartbeatTo.map { Heartbeat(address, it, term, "0") }
            return node.toSend(heartbeats + updates)
        }
        return node
    }

    override fun add(vararg message: SentMessage): Leader = this.copy(messages = messages.copy(sent = sent() + message))

    override fun add(vararg message: ReceivedMessage): Leader = this.copy(messages = messages.copy(received = received() + message))
}
