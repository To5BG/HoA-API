package voting.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import voting.annotations.TestSuite;

import java.time.LocalDateTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static voting.annotations.TestSuite.TestType.UNIT;


@TestSuite(testType = UNIT)
class ProposalTest {

	private Proposal proposal;

	@BeforeEach
	void setUp() {
		this.proposal = new Proposal("Proposal", "TestExample", 1, LocalDateTime.now());
	}

	@Test
	void isWinningChoiceTest() {
		assertFalse(proposal.isWinningChoice());
	}

	@Test
	void setWinningChoiceTest() {
		proposal.setWinningChoice(true);
		assertTrue(proposal.isWinningChoice());
	}

	@Test
	void getVotesTest() {
		assertTrue(proposal.getVotes().isEmpty());
	}

	@Test
	void setVotesTest() {
		HashMap<Integer, Integer> map = new HashMap<>();
		map.put(1, 1);
		proposal.setVotes(map);
		assertEquals(map, proposal.getVotes());
	}

	@Test
	void failedVote() {
		assertTrue(proposal.getVotes().isEmpty());
		proposal.vote(1, 1);
		assertTrue(proposal.getVotes().isEmpty());
		assertEquals(0, proposal.getVoteCount());
	}

	@Test
	void successfulVote() {
		assertTrue(proposal.getVotes().isEmpty());
		proposal.setStatus("ongoing");
		proposal.vote(1, 1);
		HashMap<Integer, Integer> votes = new HashMap<>();
		votes.put(1, 1);
		assertEquals(votes, proposal.getVotes());
		assertEquals(1, proposal.getVoteCount());
	}

	@Test
	void findOutcome() {
		proposal.setStatus("ongoing");
		// One positive
		proposal.vote(1, 1);
		assertTrue(proposal.findOutcome());

		// Majority is no longer valid
		proposal.vote(2, 0);
		assertFalse(proposal.findOutcome());

		// Idempotence
		proposal.vote(1, 1);
		assertFalse(proposal.findOutcome());

		// Majority valid once more
		proposal.vote(2, 1);
		assertTrue(proposal.findOutcome());
	}

	@Test
	void conclude() {
		boolean ans = proposal.conclude();
		assertEquals("finished", proposal.getStatus());
		assertFalse(ans);
	}
}