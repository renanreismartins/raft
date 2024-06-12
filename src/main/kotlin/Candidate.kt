package org.example

data class Candidate(
    override val address: Address,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Address>,
    override val messages: List<MessageLogEntry> = emptyList(),
    override val config: Config = Config(),
): Node(address, name, state, network, peers, messages) {

    private fun process(state: Int, messages: List<Message>, message: Message): Pair<Int, List<Message>> {
        return when(message) {
            is Heartbeat -> ((state + message.content.toInt()) to messages)
            is RequestForVotes -> (state to messages + VoteFromFollower(address, message.src, "VOTE FROM FOLLOWER"))
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

        val messageLog = messages + tickMessages.map { network.clock to it }

        if (shouldBecomeLeader(messageLog)) {
            // TODO Create constructors for each Node type that takes a Node so we don't need to pass all of these params every time
            val leader = Leader(address, name, newState, network, peers, messageLog, config)
            // TODO Refactor to return the messages to be sent instead of a side effect
            peers.forEach { peer -> leader.send(Heartbeat( leader.address, peer, "I (${leader.name}) AM YOUR LEADER NOW")) }
            return leader
        }

        return this.copy(state = newState, messages = messageLog)
    }

    fun shouldBecomeLeader(messageLog: List<MessageLogEntry>): Boolean {
        //TODO + 1 represents the Vote for Self, do we want to add it to the MessageLogEntry and remove it from here
        return messageLog.count { m -> m.second is VoteFromFollower } + 1 > clusterSize() / 2
    }

    private fun clusterSize(): Int  {
        return peers.size + 1
    }

    //TODO MOVE TO NODE
    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

}