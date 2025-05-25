package org.example

import org.example.statemachine.RaftLogger

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
    // Address has specialised types (Source and Destination) and even though they have the same values
    // when trying to get the messages for a host, the messages might not be returned because of the the
    // comparison of the Address subtypes. Using this approach we do not need to break the equals and hashcode contracts.
    // For example, we want to get messages from a host, sometimes we will refer to it as a Source and sometimes as a
    // Destination, depending where we are in the code, we would like to not have that distinction when getting the
    // messages.
    // get(Source("host", port)) and get(Destination("host", port))
    private val messages = initialMessages.mapKeys { it.key.host to it.key.port }.toMutableMap()

    fun get(address: Address): List<Message> {
        val hostAndPort = address.host to address.port
        // get all messages for address with 0 delay and store
        val (messagesToDeliver, remaining) =
            messages[hostAndPort]
                ?.partition { (_, deliveryTime) -> deliveryTime <= clock }
                ?: (listOf<NetworkMessage>() to listOf())

        messages[hostAndPort] = remaining

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
        RaftLogger.logTick(clock)
        return this
    }
}
