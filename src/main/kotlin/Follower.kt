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

    override fun process(accumulatedToSend: List<Message>, received: Message): Pair<Node, List<Message>> {
        val (n, newToSend) =  when(received) {
            is Heartbeat -> ((this.copy(state = state + received.content.toInt())) to accumulatedToSend)
            is RequestForVotes -> (this to (if (shouldVote(accumulatedToSend)) accumulatedToSend + VoteFromFollower(address, Destination.from(received.src), "VOTE FROM FOLLOWER") else accumulatedToSend))
            is VoteFromFollower -> (this to accumulatedToSend)
        }

        return n to newToSend
    }

    override fun tick(): Node {
        val (node, messages) = tickWithoutSideEffects()
        messages.forEach { send(it) }
        return node
    }

    override fun tickWithoutSideEffects(): Pair<Node, List<Message>> {
        val tickMessages = network.get(this.address)

        val (node, messagesToSend) = tickMessages.fold(this as Node to emptyList<Message>()) { (n, messages), msg ->
            n.process(messages, msg)
        }

        val messageLog = received + tickMessages.map { ReceivedMessage(it, network.clock) }
        val newSent = sent + messagesToSend.map { SentMessage(it, network.clock) }

        if (shouldPromote(messageLog)) {
            // TODO when creating a candidate (promoting a follower), add a 'VoteFromFollower' from self to received messageLogMove this to a Candidate constructor that takes a Follower
            val candidate = promote(messageLog, newSent)
            peers.forEach { peer -> candidate.send(RequestForVotes(this.address, peer, "REQUEST FOR VOTES")) } // TODO Refactor to return the messages to be sent instead of a side effect
            return candidate to messagesToSend
        }

        //return node to messagesToSend
        return n.copy(received = messageLog, sent = newSent) to messagesToSend
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