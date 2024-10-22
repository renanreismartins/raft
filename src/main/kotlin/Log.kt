package org.example

// TODO: Log should not contain 'Message's, instead it should ONLY contain entries for the state machine (AppendEntries/ClientCommand)
//       make a new type to represent the required info just for Log.
class Log(
    val messages: List<Message> = emptyList(),
) {
    fun add(message: Message): Log =
        Log(
            messages = this.messages + message,
        )

    fun lastLogIndex(): Int = messages.size

    fun prevLogIndex(): Int = if (messages.size <= 1) 0 else messages.size - 1

    //TODO why null could be 0?
    fun prevLogTerm(): Int? = if (prevLogIndex() <= 0) null else messages.get(prevLogIndex()).term

    // AppendEntries RPC, received implementation 5.3
    fun prevLogIndexCheck(
        index: Int,
        term: Int,
    ): Boolean {
        if (index == 0) {
            return true
        }
        return messages.getOrNull(index - 1)?.let { it.term == term } ?: false
    }

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
