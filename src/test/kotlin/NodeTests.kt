import org.example.Address
import org.example.Candidate
import org.example.Follower
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeTests {



    @Test
    fun `When a tick happens we increase the clock`() {
        // Given
        val f = Follower(Address("127.0.0.1", 9001), "NodeA")

        // When
        val tickedFollower = f.tick().tick()

        // Then
        assertEquals(2, tickedFollower.clock)
    }

    @Test
    fun `A Node can send a message to another Node`() {
        // Given
        val nodeA = Follower(Address("127.0.0.1", 9001), "NodeA")
        val nodeB = Follower(Address("127.0.0.1", 9002), "NodeB")

        // When
        nodeA.send(nodeB.address, "1")
        val newNodeB = nodeB.tick()

        // Then
        assertEquals(1, newNodeB.state)
    }

    /*
    We need to develop a Network class that will allow us to insert messages from the tests.
    The messages should include a 'delay', the delay will be how many ticks a Node has to wait to consume? (or process?)
    the message. The reason to use a delay instead of an absolute value for the clock is that Nodes can have different clocks
    and the message could be not consumed. This also represents better the network delays, give the algorithm does not include
    timestamps on the messages.

    Possible implementation: The network will deliver only messages with delay = 0 and decrement the delay of the remaining
    messages for that node. (Should the network 'wrap' the Message, so we do not add artificial mechanisms as 'delay' on the
    messages?)
     */

    @Test
    fun `Follower becomes a Candidate if it does not receive a heartbeat before the election timeout (3 ticks)`() {
        // Given
        val follower = Follower(Address("127.0.0.1", 9001), "NodeA")

        // When
        val candidate = follower.tick().tick().tick().tick() // TODO: we should assert on the 3rd or 4th tick?

        assertTrue(candidate is Candidate)
    }

//    fun `Follower stays as a follower if it receives a heartbeat before the election timeout (3 ticks)`() {
//        // Given
//        val follower = Follower(Address("127.0.0.1", 9001), "NodeA")
//
//        // When
//        val stillFollower = follower.tick().tick().tick() // It is still a 'follower' instead of a candidate
//
//        assertTrue(stillFollower is Follower)
//    }

    /*
       fun `Follower moves to Candidate after election timeout (3 ticks)`() {
           val follower = Follower(Address("127.0.0.1", 9001), "NodeA")
           follower.tick()
           follower.tick()
           follower.tick()
           follower == Candidate
       }

       fun `Creates a RequestVote message`() {
       }


       fun `Node A can send a message to Node B`() {
           val a = Node(Address("127.0.0.1", 9000), "A")
           val electionStarted = a.startElection();

           val b = Node(Address("127.0.0.1", 9001), "A")
       }*/
}