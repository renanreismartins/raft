package org.example

typealias NetworkDelay = Int
typealias DeliveryTime = Int

data class NetworkMessage(
    val message: Message,
    val deliveryTime: DeliveryTime,
)

class Network(
    initialMessages: Map<Address, List<NetworkMessage>> = emptyMap(),
) {
    var clock = 0

    // TODO Write an ADR
    // The keys are a Pair because we want to use the Host and Port to get the messages. This is because
    // Address now has specialised types (Source and Destination) and even tho they have the same values
    // they are compared differently so the messages were not returned
    // This is to not break the contract of hashcode and equals
    private val messages = initialMessages.mapKeys { it.key.host to it.key.port }.toMutableMap()

    fun get(address: Address): List<Message> {
        val hostAndPort = address.host to address.port
        // get all messages for address with 0 delay and store
        val (messagesToDeliver, remaining) =
            messages[hostAndPort]
                ?.partition { (_, deliveryTime) ->
                    deliveryTime <= clock
                } ?: (listOf<NetworkMessage>() to listOf())

        messages[hostAndPort] = remaining

        // return store messages
        return messagesToDeliver.map { it.message }
    }

    fun add(
        message: Message,
        delay: NetworkDelay = 0,
    ) {
        val networkMessage = NetworkMessage(message, clock + 1 + delay)
        messages[message.dest.host to message.dest.port] =
            messages[message.dest.host to message.dest.port]?.plus(networkMessage) ?: listOf(networkMessage)
    }

    fun tick(ticks: Int = 1): Network {
        clock += ticks
        return this
    }
}
