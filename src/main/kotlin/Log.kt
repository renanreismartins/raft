package org.example

class Log(
    val messages: List<Message> = emptyList(),
) {
    fun add(message: Message): Log =
        Log(
            messages = this.messages + message,
        )

    fun prevLogIndex(): Int = if (messages.size <= 1) 0 else messages.size - 1

    fun prevLogTerm(): Int? = if (prevLogIndex() <= 0) null else messages.get(prevLogIndex()).term

    // AppendEntries RPC, received implementation 5.3
    fun prevLogIndexCheck(
        index: Int,
        term: Int,
    ): Boolean = messages.getOrNull(index)?.let { it.term == term } ?: false

    /*
    TODO We are refactoring the Nodes to use Log, we found out that on our current implementation in Leader the
    val nextIndex is calculated as the size of the messageLog. We believe that is incorrect because the docs state:
    for each server, index of the next log entry to send to that server (initialized to leader last log index + 1)
    So we believe the correct implementation to nextIndex is prevLogIndex() + 1 + 1;
    The first + 1 is to get the lastLog index and the second + 1 is accordingly to the docs.

    We are adding this method here just to not alter behaviour on the refactoring, but might be removed after confirming
    the previous assumptions
     */
    fun size(): Int = messages.size
}
