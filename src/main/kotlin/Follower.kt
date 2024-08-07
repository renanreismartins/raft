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
            is Heartbeat -> ((this.copy(state = state + received.content.toInt())) to emptyList())
            is RequestForVotes -> (this to (if (shouldVote(accumulatedToSend)) listOf(VoteFromFollower(address, Destination.from(received.src), "VOTE FROM FOLLOWER")) else emptyList()))
            is VoteFromFollower -> (this to emptyList())
        }

        val n2 = n.copy(received = n.received + ReceivedMessage(received, network.clock), sent = n.sent + newToSend.map { SentMessage(it, network.clock) } )
        return n2 to accumulatedToSend + newToSend
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

        if ((node as Follower).shouldPromote()) {
            // TODO when creating a candidate (promoting a follower), add a 'VoteFromFollower' from self to received messageLog. Move this to a Candidate constructor that takes a Follower
            val candidate = node.promote()
            val requestForVotes = peers.map { peer -> RequestForVotes(this.address, peer, "REQUEST FOR VOTES") }
            return candidate.copy(sent = candidate.sent + requestForVotes.map { SentMessage(it, network.clock) }) to messagesToSend + requestForVotes
        }

        return node to messagesToSend
    }

    //TODO UNIT TEST
    private fun shouldPromote(): Boolean {
        // On system startup, Follower hasn't received any messages and should promote itself after electionTimeout
        val hasReachedFirstTimeoutAfterStartup = received.isEmpty() && network.clock > config.electionTimeout
        // During normal operation of the system, has not received messages in electionTimeout period
        val hasReachedTimeoutWithLastMessage = received.isNotEmpty() && network.clock - received.last().receivedAt > config.electionTimeout
        return hasReachedTimeoutWithLastMessage || hasReachedFirstTimeoutAfterStartup
    }

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

    //TODO unit test
    fun shouldVote(tickMessages: List<Message>): Boolean {
        return (sent + tickMessages).filterIsInstance<VoteFromFollower>().isEmpty()
    }

    private fun promote(): Candidate {
        return Candidate(this.address, this.name, this.state, this.network, this.peers, this.received, this.sent)
    }
}