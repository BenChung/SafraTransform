package edu.cmu.cdm.safras;

/**
 * Transition interface for converting a Beuchi automata to a Rabin one
 */
public interface SafraTransition {
	/**
	 * Perform a Safra transition on iTree.
	 * @param automata The automata being used
	 * @param iTree The tree source state
	 * @param character The character at the read head
	 * @return The new state under character from iTree
	 */
	public SafraTree transition(Automata automata, SafraTree iTree, int character);
}
