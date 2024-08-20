package org.example

data class Follower(
    override val address: Source,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Destination>,
    override val messages: Messages = Messages(),
    override val term: Int = 0,
    override val config: Config = Config(),
): Node(address, name, state, network, peers) {

    override fun handleMessage(message: Message): Node {
        return when(message) {
            is Heartbeat -> (this.copy(state = state + message.content.toInt()))
            is RequestForVotes -> {
                if (shouldVote()) {
                    toSend(VoteFromFollower(address, Destination.from(message.src), term, "VOTE FROM FOLLOWER"))
                } else {
                    this
                }
            }
            is VoteFromFollower -> this
        }
    }

    override fun tickWithoutSideEffects(): Node {
        val tickMessages = network.get(this.address)

        val node = tickMessages.fold(this as Node) { node, msg ->
            node.process(msg)
        }

        // TODO: Move the first part (above this comment) into a Node method, then introduce a postProcess method for this logic (and logic in Leader)
        if ((node as Follower).shouldPromote()) {
            return node.promote()
        }

        return node
    }

    //TODO UNIT TEST
    private fun shouldPromote(): Boolean {
        // On system startup, Follower hasn't received any messages and should promote itself after electionTimeout
        val hasReachedFirstTimeoutAfterStartup = received().isEmpty() && network.clock > config.electionTimeout
        // During normal operation of the system, has not received messages in electionTimeout period
        val hasReachedTimeoutWithLastMessage = received().isNotEmpty() && network.clock - received().last().receivedAt > config.electionTimeout
        return hasReachedTimeoutWithLastMessage || hasReachedFirstTimeoutAfterStartup
    }

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

    //TODO unit test
    //TODO consider the election term instead of all the sent messages
    fun shouldVote(): Boolean {
        return (sent() + messages.toSend).filterIsInstance<VoteFromFollower>().isEmpty()
    }

    //TODO It seems the 'received =' can be removed as Kotlin can infer the type
    override fun add(vararg message: SentMessage): Follower = this.copy(messages = messages.copy(sent = sent() + message))
    override fun add(vararg message: ReceivedMessage): Follower = this.copy(messages = messages.copy(received = received() + message))

    private fun promote(): Candidate {
        val requestForVotes = peers.map { peer -> RequestForVotes(this.address, peer, term, "REQUEST FOR VOTES") }
        val messages = this.messages
            .received(VoteFromFollower(this.address, Destination.from(this.address), term, "Vote from self").toReceived())
            .toSend(requestForVotes)

        return Candidate(this.address, this.name, this.state, this.network, this.peers, messages)
    }

}
