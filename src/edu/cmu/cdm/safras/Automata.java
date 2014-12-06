package edu.cmu.cdm.safras;

import java.util.BitSet;
import java.util.List;

/**
 * Internal representation of a Buechi automata
 */
public class Automata {
	/** The number of states in the automata */
	int nStates;
	/** The inital states */
	BitSet initial;
	/** The final states */
	BitSet finals;
	/** The alphabet size */
	int alphsize;

	/** The transition system for the automata */
	TransitionSystem ts;

	public Automata(int nStates, BitSet intial,
					BitSet finals, int alphsize, List<TransitionSystem.Transition> transitions) {
		this.nStates = nStates;
		this.initial = intial;
		this.finals = finals;
		this.alphsize = alphsize;
		this.ts = new TransitionSystem(transitions);
	}
}
