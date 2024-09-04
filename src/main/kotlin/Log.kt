package org.example

class Log(
    val messages: List<Message> = emptyList(),
    val index: Int = 1,
) {
    fun add(message: Message): Log {
        return Log(
            messages = this.messages + message,
            index = this.index + 1,
        )
    }
}
