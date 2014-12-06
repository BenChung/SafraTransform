package edu.cmu.cdm.safras;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Represents a Beuchi transition system read from a file
 */
public class TransitionSystem {
	/**
	 * A transition in the system
	 */
	public static class Transition {
		private final int from;
		private final int to;
		private final int under;

		public Transition(int from, int to, int under) {
			this.from = from;
			this.to = to;
			this.under = under;
		}
	}

	Map<Integer, Map<Integer, BitSet>> innerMap;

	public TransitionSystem(List<Transition> transitions) {
		transitions.sort((a, b) -> a.from - b.from);

		//Stream magic. Makes a state->(letter->bitset) map somehow.
		Collector<Transition,?,BitSet> ip =
				Collectors.<Transition,BitSet>reducing(new BitSet(),
						trans->{ BitSet res = new BitSet(); res.set(trans.to); return res;},
						(bs1,bs2)->{bs2.or(bs1); return bs2; });

		Collector<Transition, ?, Map<Integer, BitSet>> innerColl =
				Collectors.groupingBy(transi->transi.from, ip);

		innerMap = transitions.stream()
				.collect(Collectors.groupingBy(trans -> trans.under, innerColl));


	}

	/**
	 * Get the transition map under character
	 * @param character The character being transitioned under
	 * @return The map with the transitions state->new states.
	 */
	public Map<Integer, BitSet> under(int character) {
		return innerMap.get(character);
	}
}
