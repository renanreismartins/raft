package org.example

data class Follower(
    override val address: Address,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Address>,
    override val messages: List<MessageLogEntry> = emptyList(),
    override val config: Config = Config(),
): Node(address, name, state, network, peers, messages) {


    fun process(message: Message) {
        val state = 0
        when(message) {
            is Heartbeat -> ((state + message.content.toInt()) to emptyList<Message>())
            is RequestForVotes -> (state to listOf(VoteFromFollower(address, message.src, "Future content here"))) //TODO add the message here instead of calling 'send'
            is VoteFromFollower -> (state to emptyList<Message>()) //TODO add the message here instead of calling 'send'; this should never happen, decide how to handle
        }
    }

    override fun tick(): Node {
        val tickMessages = network.get(this.address)

        val newState = tickMessages.fold(state) { acc, msg ->
            try {
                msg.content.toInt() + acc
            } catch (e: NumberFormatException) {
                println("RECEIVED $msg")

                if ("REQUEST FOR VOTES" == msg.content) {
                    send(msg.src, "VOTE FROM FOLLOWER") //TODO REMOVE THIS SIDE EFFECT
                }

                acc
            }
        }

        val messageLog = messages + tickMessages.map { network.clock to it }

        if (messageLog.isEmpty() && network.clock > config.electionTimeout) {
            val candidate = Candidate(address, name, state, network, peers, messageLog)

            peers.forEach { peer -> candidate.send(peer, "REQUEST FOR VOTES") } // TODO Refactor to return the messages to be sent instead of a side effect

            return candidate
        }

        //TODO In this scenario we are promoting a Follower to Candidate but not sending a Request For Votes, write a test and fix
        return if (messageLog.isNotEmpty() && network.clock - messageLog.last().first > config.electionTimeout) Candidate(
            address,
            name,
            state,
            network,
            peers,
            messageLog
        ) else return this.copy(state = newState, messages = messageLog)
    }

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }
}