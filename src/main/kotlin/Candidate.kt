package org.example

data class Candidate(
    override val address: Source,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Destination>,
    override val received: List<ReceivedMessage> = emptyList(),
    override val sent: List<SentMessage> = emptyList(),
    override val config: Config = Config(),
): Node(address, name, state, network, peers, received) {

    // TODO Make process return Node, so we have finer control over how we handle each message
    //      e.g. If a Candidate receives a Heartbeat, it should demote to follower, this is difficult
    //      with the current architecture. This would lead allow us to remove methods to demote/promote
    private fun process(state: Int, messages: List<Message>, message: Message): Pair<Int, List<Message>> {
        return when(message) {
            is Heartbeat -> ((state + message.content.toInt()) to messages)
            is RequestForVotes -> (state to messages)
            is VoteFromFollower -> (state to messages)
        }
    }

    override fun tick(): Node {
        //TODO We have this common logic in the Follower, move it to Node
        val tickMessages = network.get(this.address)

        val (newState, messagesToSend) = tickMessages.fold(state to emptyList<Message>()) { (s, messages), msg ->
            process(s, messages, msg)
        }

        messagesToSend.forEach { send(it) } // TODO remove side effect

        val messageLog = received + tickMessages.map { ReceivedMessage(it, network.clock) }
        val newSent = sent + messagesToSend.map { SentMessage(it, network.clock)}

        if (shouldDemoteToFollower(messageLog)) {
            return demote(newState, messageLog, newSent)
        }

        if (shouldBecomeLeader(messageLog)) {
            val leader = promote(newState, messageLog, newSent)
            // TODO Refactor to return the messages to be sent instead of a side effect
            peers.forEach { peer -> leader.send(Heartbeat( leader.address, peer, "0")) }
            return leader
        }

        return this.copy(state = newState, received = messageLog, sent = newSent)
    }

    fun shouldBecomeLeader(messageLog: List<ReceivedMessage>): Boolean {
        //TODO + 1 represents the Vote for Self, do we want to add it to the MessageLogEntry and remove it from here
        return messageLog.count { m -> m.message is VoteFromFollower } + 1 > clusterSize() / 2
    }

    // TODO We need to make sure that this Heartbeat comes from the real Leader (check term index?)
    //      and we aren't receiving Heartbeats from any other source
    private fun shouldDemoteToFollower(messageLog: List<ReceivedMessage>): Boolean {
        return messageLog.any { it.message is Heartbeat }
    }

    private fun clusterSize(): Int  {
        return peers.size + 1
    }

    //TODO MOVE TO NODE
    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

    private fun promote(newState: Int, newReceived: List<ReceivedMessage>, newSent: List<SentMessage>): Leader {
        return Leader(this.address, this.name, newState, this.network, this.peers, newReceived, newSent, this.config)
    }

    private fun demote(newState: Int, newReceived: List<ReceivedMessage>, newSent: List<SentMessage>): Follower {
        return Follower(this.address, this.name, newState, this.network, this.peers, newReceived, newSent, this.config)
    }
}