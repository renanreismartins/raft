package org.example

data class Candidate(
    override val address: Source,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Destination>,
    override val votedFor: Address? = null,
    override val messages: Messages = Messages(),
    override val log: Log = Log(),
    override val term: Int = 0,
    override val config: Config = Config(),
    override val commitIndex: Int = 0,
    override val lastApplied: Int = 0,
    val termStartedAt: Int = network.clock + 1,
) : Node(address, name, state, network, peers, votedFor) {
    override fun handleMessage(message: Message): Node {
        return when (message) {
            is Heartbeat -> {
                if (message.term < term) {
                    return this
                }
                return copy(state = (state + message.content.toInt())).demote()
            }
            is RequestForVotes -> this //TODO A Candidate can receive a request for vote
            // from another candidate. We can have two nodes trying to be a candidate
            // at the same time. The logic applied in Follower can be used here
            is VoteFromFollower -> {
                if (shouldBecomeLeader()) {
                    val appendEntries =
                        peers.map { peer ->
                            AppendEntry(
                                address,
                                peer,
                                content = "noop",
                                term,
                                // TODO: Figure out what values to use here (maybe nextIndex?)
                                prevLogIndex = log.prevLogIndex(),
                                prevLogTerm = log.prevLogTerm() ?: 0,
                                leaderCommit = this.commitIndex,
                            )
                        }
                    return promote().toSend(appendEntries)
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
            return Candidate(
                this.address,
                this.name,
                this.state,
                this.network,
                this.peers,
                this.votedFor,
                messages,
                this.log,
                this.term + 1,
            )
        }
        return newNode
    }

    fun hasReachedElectionTimeout(): Boolean = network.clock - termStartedAt >= config.electionTimeout

    fun shouldBecomeLeader(): Boolean = received().count { m -> m.message is VoteFromFollower } > clusterSize() / 2

    private fun clusterSize(): Int = peers.size + 1

    override fun add(vararg message: ReceivedMessage): Candidate = this.copy(messages = messages.copy(received = received() + message))

    override fun add(vararg message: SentMessage): Candidate = this.copy(messages = messages.copy(sent = sent() + message))

    private fun promote(): Leader =
        Leader(
            address = this.address,
            this.name,
            this.state,
            this.network,
            this.peers,
            this.votedFor,
            this.messages,
            this.log,
            this.term,
            this.config,
            lastApplied = this.lastApplied,
            commitIndex = this.commitIndex,
        )
}
