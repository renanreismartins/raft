package org.example.statemachine

import org.example.Network
import org.example.Source

object RaftLogger {
    private const val ANSI_RESET = "\u001B[0m"
    private const val ANSI_GREEN = "\u001B[32m"
    private const val ANSI_YELLOW = "\u001B[33m"
    private const val ANSI_BLUE = "\u001B[34m"

    fun logInfo(node: StateMachine, message: String) {
        val COLOR = when (node.role) {
            Role.LEADER -> ANSI_BLUE
            Role.CANDIDATE -> ANSI_YELLOW
            Role.FOLLOWER -> ANSI_GREEN
        }

        println("$COLOR[Clock:${node.network.clock}] [${node.name}] $message$ANSI_RESET")
    }

    fun logInfo(node: StateMachine, message: String, shouldLog: Boolean) {
        if (shouldLog) logInfo(node, message)
    }

    fun debug(node: StateMachine, message: String) {
        val COLOR = when (node.role) {
            Role.LEADER -> ANSI_BLUE
            Role.CANDIDATE -> ANSI_YELLOW
            Role.FOLLOWER -> ANSI_GREEN
        }

        println("$COLOR[Clock:${node.network.clock}] [${node.name}] $message$ANSI_RESET")
    }

    fun debug(node: StateMachine, message: String, shouldLog: Boolean) {
        if (shouldLog) debug(node, message)
    }

    fun logTick(clock: Int) {
        val ANSI_RED = "\u001B[31m"
        println("$ANSI_RED[Clock:$clock]$ANSI_RESET")
    }
}

fun main() {
    val node = StateMachine(Source("", 100), "leader", 0, Network(), listOf()).copy(role = Role.LEADER)
    RaftLogger.logInfo(node, "This is an information message. ℹ️")
    RaftLogger.logInfo(node, "This is a warning message\uD83D. ⚠️")
    RaftLogger.logInfo(node, "This is an error message. 🛑")
    RaftLogger.logInfo(node, "This is a debug message. 🐛")
}