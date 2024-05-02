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

    override fun tick(): Node {
        val tickMessages = network.get(this.address)

        val newState = tickMessages.fold(state) { acc, msg ->
            try {
                msg.content.toInt() + acc
            } catch (e: NumberFormatException) {
                println("RECEIVED $msg")
                acc
            }
        }

        val messageLog = messages + tickMessages.map { network.clock to it }
        return this.copy(state = newState, messages = messageLog)
    }

    //TODO MOVE TO NODE
    override fun receive(message: Message): Node {
        TODO("Not yet implemented")
    }

}