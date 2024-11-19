package org.example

data class Follower(
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
) : Node(address, name, state, network, peers, votedFor) {

    override fun handleMessage(message: Message): Node =
        when (message) {
            is Heartbeat -> (this.copy(state = state + message.content.toInt()))
            is RequestForVotes -> {
                if (shouldVote()) {
                    toSend(VoteFromFollower(address, Destination.from(message.src), term, "VOTE FROM FOLLOWER"))
                } else {
                    this
                }
            }
            // TODO: We should not be adding messages to the Log without checking if it should be there
            is AppendEntry -> appendEntryResponse(message)
            is VoteFromFollower -> this
            is ClientCommand -> this
            is AppendEntryResponse -> this
        }

    override fun tickWithoutSideEffects(): Node {
        val tickMessages = network.get(this.address)
        val node = tickMessages.fold(this as Node, Node::process)

        // TODO: Move the first part (above this comment) into a Node method, then introduce a postProcess method for this logic (and logic in Leader)
        // We know we can do the casting because even after processing all the messages, the type returned is always
        // a Follower. After the to-do above or some other refactoring we will remove the casting.
        return if ((node as Follower).shouldPromote()) node.promote() else node
    }

    // TODO UNIT TEST
    private fun shouldPromote(): Boolean {
        // On system startup, Follower hasn't received any messages and should promote itself after electionTimeout
        val hasReachedFirstTimeoutAfterStartup = received().isEmpty() && network.clock > config.electionTimeout
        // During normal operation of the system, has not received messages in electionTimeout period
        val hasReachedTimeoutWithLastMessage =
            received().isNotEmpty() && network.clock - received().last().receivedAt > config.electionTimeout
        return hasReachedTimeoutWithLastMessage || hasReachedFirstTimeoutAfterStartup
    }

    // TODO unit test
    fun shouldVote(): Boolean = (sent() + messages.toSend).filterIsInstance<VoteFromFollower>().isEmpty()
//    fun shouldVote(message: RequestForVotes): Boolean {
//        return votedFor == null || votedFor == message.src// TODO takes in consideration the type? Address == Source?
//    }

    override fun add(vararg message: SentMessage): Follower =
        this.copy(messages = messages.copy(sent = sent() + message))

    override fun add(vararg message: ReceivedMessage): Follower =
        this.copy(messages = messages.copy(received = received() + message))

    // todo unit test, test the messages state and the term
    private fun promote(): Candidate {
        val requestForVotes = peers.map { peer -> RequestForVotes(this.address, peer, term, "REQUEST FOR VOTES") }
        val messages =
            this.messages
                .received(
                    VoteFromFollower(
                        this.address,
                        Destination.from(this.address),
                        term,
                        "Vote from self"
                    ).toReceived()
                )
                .toSend(requestForVotes)

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
            lastApplied = this.lastApplied,
            commitIndex = this.commitIndex,
        )
    }
}
