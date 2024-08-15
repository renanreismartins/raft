package org.example

data class Follower(
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
            is Heartbeat -> (this.copy(state = state + message.content.toInt()))
            is RequestForVotes -> {
                // TODO: BUFFER instead of filter!
                if (shouldVote(sentMessages().filter { it.sentAt == network.clock }.map { it.message })) {
                    add(VoteFromFollower(address, Destination.from(message.src), "VOTE FROM FOLLOWER").toSent())
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
            // TODO when creating a candidate (promoting a follower), add a 'VoteFromFollower' from self to received messageLog. Move this to a Candidate constructor that takes a Follower
            val candidate = node.promote()
            val requestForVotes = peers.map { peer -> RequestForVotes(this.address, peer, "REQUEST FOR VOTES") }

            return candidate.add(*requestForVotes.map { SentMessage(it, network.clock) }.toTypedArray())
        }

        return node
    }

    //TODO UNIT TEST
    private fun shouldPromote(): Boolean {
        // On system startup, Follower hasn't received any messages and should promote itself after electionTimeout
        val hasReachedFirstTimeoutAfterStartup = receivedMessages().isEmpty() && network.clock > config.electionTimeout
        // During normal operation of the system, has not received messages in electionTimeout period
        val hasReachedTimeoutWithLastMessage = receivedMessages().isNotEmpty() && network.clock - receivedMessages().last().receivedAt > config.electionTimeout
        return hasReachedTimeoutWithLastMessage || hasReachedFirstTimeoutAfterStartup
    }

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

    //TODO unit test
    fun shouldVote(tickMessages: List<Message>): Boolean {
        return (sentMessages() + tickMessages).filterIsInstance<VoteFromFollower>().isEmpty()
    }

    //TODO It seems the 'received =' can be removed as Kotlin can infer the type
    override fun add(vararg message: SentMessage): Follower = this.copy(messages = messages.copy(sent = sentMessages() + message))
    override fun add(vararg message: ReceivedMessage): Follower = this.copy(messages = messages.copy(received = receivedMessages() + message))

    private fun promote(): Candidate {
        return Candidate(this.address, this.name, this.state, this.network, this.peers, this.messages)
    }

}
