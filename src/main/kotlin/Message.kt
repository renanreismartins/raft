package org.example

typealias Timestamp = Int //TODO Make this a Comparable, so when we change it to a 'Date' type for the real world, we do not need to change the usages

// TODO add tiny type Source and Destination
sealed class Message(open val src: Source, open val dest: Destination, open val content: String) //TODO remove 'content' from the Message and add it to the subclasses if we have one type of Message without 'content'
data class RequestForVotes(override val src: Source, override val dest: Destination, override val content: String) : Message(src, dest, content)
data class VoteFromFollower(override val src: Source, override val dest: Destination, override val content: String) : Message(src, dest, content)
data class Heartbeat(override val src: Source, override val dest: Destination, override val content: String) : Message(src, dest, content)
// TODO We need to make these generic, and AppendEntries content should be List<T> - find a way to do this with generics, or remove the parent content
//data class AppendEntries(override val src: Address, override val dest: Address, override val content: String) : Message(src, dest, content)
data class SentMessage(val message: Message, val sentAt: Timestamp)
data class ReceivedMessage(val message: Message, val receivedAt: Timestamp)

// TODO: Make a map <Destination, List<ReceivedMessage>> for the received
data class Messages(val received: List<ReceivedMessage> = emptyList(), val sent: List<SentMessage> = emptyList(), val toSend: List<Message> = emptyList()) {
    //TODO is flush the best name to represent this operation
    //TODO After this change, do we still need the extension methods Message.toSent() and toReceived?
    fun flush(sentAt: Int): Messages {
        return this.copy(
            sent = sent + toSend.map { SentMessage(it, sentAt) },
            toSend = emptyList()
        )
    }

    fun toSend(message: Message): Messages {
        return this.copy(toSend = toSend + message)
    }

    fun toSend(newMessages: List<Message>): Messages {
        return this.copy(toSend = toSend + newMessages)
    }
}