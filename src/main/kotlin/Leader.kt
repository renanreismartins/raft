package org.example


//TODO maybe we should make all the constructors except the Followe as private
// this way all nodes can only be initialized as Follower and only transition to a new
// state thought the state machine
data class Leader(
    override val address: Source,
    override val name: String,
    override val state: Int = 0,
    override val network: Network,
    override val peers: List<Destination>,
    override val received: List<ReceivedMessage> = emptyList(), // TODO: Make a map <Destination, List<ReceivedMessage>>
    override val sent: List<SentMessage> = emptyList(),
    override val config: Config = Config(),
): Node(address, name, state, network, peers, received) {

    private fun process(state: Int, messages: List<Message>, message: Message): Pair<Int, List<Message>> {
        return when(message) {
            is Heartbeat -> ((state + message.content.toInt()) to messages)
            is RequestForVotes -> (state to messages)
            is VoteFromFollower -> (state to messages)
        }
    }

    // 1. Process messages, create messages to send
    // 2. THEN, check sent messages to see which peers we need to send a heartbeat to
    // 3.

    override fun tick(): Node {
        val tickMessages = network.get(this.address)

        val (newState, responses) = tickMessages.fold(state to emptyList<Message>()) { (s, messages), msg ->
            process(s, messages, msg)
        }

        val nodesWeHaveSentMessagesTo =
            sent.filter { it.sentAt > network.clock - config.heartbeatTimeout }
                .map { it.message.dest } + responses.map { it.dest }

        val nodesWeNeedToSendHeartbeatTo = peers.toSet() - nodesWeHaveSentMessagesTo.toSet()

        val heartbeats = nodesWeNeedToSendHeartbeatTo.map { Heartbeat(address, it, "0") }

        val messagesToSend = responses + heartbeats
        messagesToSend.forEach { send(it) } // TODO remove side effect

        val messageLog = this.received + tickMessages.map { ReceivedMessage(it, network.clock) }
        val newSent = this.sent + messagesToSend.map { SentMessage(it, network.clock)}

        return copy(state = newState, received = messageLog, sent = newSent )
    }

    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }
}
