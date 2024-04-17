import org.example.Address
import org.example.Candidate
import org.example.Follower
import org.example.Network
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeTests {

    @Test
    fun `When a tick happens we increase the clock`() {
        // Given
        val f = Follower(Address("127.0.0.1", 9001), "NodeA", network = Network())

        // When
        val tickedFollower = f.tick().tick()

        // Then
        assertEquals(2, tickedFollower.clock)
    }

    @Test
    fun `A Node can send a message to another Node`() {
        // Given
        val network = Network()
        val nodeA = Follower(Address("127.0.0.1", 9001), "NodeA", network = network)
        val nodeB = Follower(Address("127.0.0.1", 9002), "NodeB", network = network)

        // When
        nodeA.send(nodeB.address, "1")
        network.tick()
        val newNodeB = nodeB.tick()

        // Then
        assertEquals(1, newNodeB.state)
    }

    @Test
    fun `Follower becomes a Candidate if it does not receive a heartbeat before the election timeout (3 ticks)`() {
        // Given
        val network = Network()
        val follower = Follower(Address("127.0.0.1", 9001), "NodeA", network = network)

        // When
        network.tick(4)
        val candidate = follower.tick(4)

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