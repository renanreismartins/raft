package org.example

data class Follower(
    override val address: Source,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Destination>,
    override val received: List<ReceivedMessage> = emptyList(),
    override val sent: List<SentMessage> = emptyList(),
    override val config: Config = Config(),
): Node(address, name, state, network, peers, received) {

    private fun process(state: Int, messages: List<Message>, message: Message): Pair<Int, List<Message>> {
        return when(message) {
            is Heartbeat -> ((state + message.content.toInt()) to messages)
            is RequestForVotes -> (state to (if (shouldVote(messages)) messages + VoteFromFollower(address, Destination.from(message.src), "VOTE FROM FOLLOWER") else messages))
            is VoteFromFollower -> (state to messages)
        }
    }

    override fun tick(): Node {
        val (node, messages) = tickWithoutSideEffects()
        messages.forEach { send(it) }
        return node
    }

    override fun tickWithoutSideEffects(): Pair<Node, List<Message>> {
        val tickMessages = network.get(this.address)

        val (newState, messagesToSend) = tickMessages.fold(state to emptyList<Message>()) { (s, messages), msg ->
            process(s, messages, msg)
        }

        val messageLog = received + tickMessages.map { ReceivedMessage(it, network.clock) }
        val newSent = sent + messagesToSend.map { SentMessage(it, network.clock) }

        if (shouldPromote(messageLog)) {
            // TODO when creating a candidate (promoting a follower), add a 'VoteFromFollower' from self to received messageLogMove this to a Candidate constructor that takes a Follower
            val candidate = promote(messageLog, newSent)
            peers.forEach { peer -> candidate.send(RequestForVotes(this.address, peer, "REQUEST FOR VOTES")) } // TODO Refactor to return the messages to be sent instead of a side effect
            return candidate to messagesToSend
        }

        return this.copy(state = newState, received = messageLog, sent = newSent) to messagesToSend
    }

    //TODO UNIT TEST
    private fun shouldPromote(messageLog: List<ReceivedMessage>): Boolean {
        // On system startup, Follower hasn't received any messages and should promote itself after electionTimeout
        val hasReachedFirstTimeoutAfterStartup = messageLog.isEmpty() && network.clock > config.electionTimeout
        // During normal operation of the system, has not received messages in electionTimeout period
        val hasReachedTimeoutWithLastMessage = messageLog.isNotEmpty() && network.clock - messageLog.last().receivedAt > config.electionTimeout
        return hasReachedTimeoutWithLastMessage || hasReachedFirstTimeoutAfterStartup
    }

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

    //TODO unit test
    fun shouldVote(tickMessages: List<Message>): Boolean {
        return (sent + tickMessages).filterIsInstance<VoteFromFollower>().isEmpty()
    }

    private fun promote(newReceived: List<ReceivedMessage>, newSent: List<SentMessage>): Candidate {
        return Candidate(this.address, this.name, this.state, this.network, this.peers, newReceived, newSent)
    }
}