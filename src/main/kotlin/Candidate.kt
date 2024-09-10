package org.example

data class Candidate(
    override val address: Source,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Destination>,
    override val messages: Messages = Messages(),
    override val log: Log = Log(),
    override val term: Int = 0,
    override val config: Config = Config(),
    override val commitIndex: Int = 0,
    override val lastApplied: Int = 0,
    val termStartedAt: Int = network.clock + 1,
) : Node(address, name, state, network, peers) {
    // TODO Make process return Node, so we have finer control over how we handle each message
    //      e.g. If a Candidate receives a Heartbeat, it should demote to follower, this is difficult
    //      with the current architecture. This would allow us to remove methods to demote/promote

    override fun handleMessage(message: Message): Node {
        return when (message) {
            is Heartbeat -> {
                if (message.term < term) {
                    return this
                }
                return copy(state = (state + message.content.toInt())).demote()
            }
            is RequestForVotes -> this
            is VoteFromFollower -> {
                if (shouldBecomeLeader()) {
                    val heartbeats = peers.map { peer -> Heartbeat(address, peer, term, "0") }
                    return promote().toSend(heartbeats)
                }
                return this
            }
            is ClientCommand -> this
            is AppendEntry -> this
            is AppendEntryResponse -> this
        }
    }

    override fun tickWithoutSideEffects(): Node {
        // TODO We have this common logic in the Follower, move it to Node
        val tickMessages = network.get(this.address)

        val newNode =
            tickMessages.fold(this as Node) { node, msg ->
                node.process(msg)
            }

        if (newNode is Candidate && newNode.hasReachedElectionTimeout()) {
            return Candidate(this.address, this.name, this.state, this.network, this.peers, messages, this.log, this.term + 1)
        }
        return newNode
    }

    fun hasReachedElectionTimeout(): Boolean = network.clock - termStartedAt >= config.electionTimeout

    fun shouldBecomeLeader(): Boolean = received().count { m -> m.message is VoteFromFollower } > clusterSize() / 2

    private fun clusterSize(): Int = peers.size + 1

    // TODO MOVE TO NODE
    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

    // TODO It seems the 'received =' can be removed as Kotlin can infer the type
    override fun add(vararg message: ReceivedMessage): Candidate = this.copy(messages = messages.copy(received = received() + message))

    override fun add(vararg message: SentMessage): Candidate = this.copy(messages = messages.copy(sent = sent() + message))

    private fun promote(): Leader =
        Leader(
            address = this.address,
            this.name,
            this.state,
            this.network,
            this.peers,
            this.messages,
            this.log,
            this.term,
            this.config,
            lastApplied = this.lastApplied,
            commitIndex = this.commitIndex,
        )

    private fun demote(): Follower =
        Follower(
            this.address,
            this.name,
            this.state,
            this.network,
            this.peers,
            this.messages,
            this.log,
            this.term,
            this.config,
            lastApplied = this.lastApplied,
            commitIndex = this.commitIndex,
        )
}
