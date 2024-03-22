import org.example.Address
import org.example.Follower
import org.junit.jupiter.api.Test

class NodeTests {

    @Test
    fun `When a tick happens we increase the clock`() {
        val f = Follower(Address("127.0.0.1", 9001), "NodeA")
        val tickedFollower = f.tick().tick()
        tickedFollower.clock == 2
    }

    @Test
    fun `A Node can send a message to another Node`() {
        val nodeA = Follower(Address("127.0.0.1", 9001), "NodeA")
        val nodeB = Follower(Address("127.0.0.1", 9002), "NodeB")
        nodeA.send(nodeB, "messageA")
    }



    /*
// one node cluster
    fun `Follower stays as a follower if it receives a heartbeat from a Leader before the election timeout (3 ticks)`() {
        Follower(Address("127.0.0.1", 9001), "NodeA")
        follower.tick().receive(LeaderHeartBeat())
        follower.consume()
        follower.tick()
        follower.tick()
        follower == Follower
    }


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