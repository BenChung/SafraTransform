package edu.cmu.cdm.safras;

import java.util.*;

/**
 * 1-pass transformer implementation. Recommended.
 */
public class MonopassTransformer implements SafraTransition {

	/**
	 * Deletes all nodes in the list and all of their children w.r.t. names
	 * @param nodes The nodes to delete
	 * @param names The set of names to alter as deletions happen
	 */
	private static void deleteNodes(List<SafraNode> nodes, BitSet names) {
		if (nodes.isEmpty()) return;
		for (int i = 0; i < nodes.size(); i++) {
			SafraNode sn = nodes.get(i);
			names.clear(sn.getName()-1);
			SafraNode.dispose(sn);
		}
	}

	/**
	 * The recursive bit of a safra transition. 1 traverse implementation.
	 *
	 * @param transition The transition system
	 * @param elem The initial safra tree
	 * @param fresh True iff the node was created in this pass
	 * @param initNames The set of names going in. Pretend like delete hasn't happened yet here. Used to pretend like we're creating before deleting.
	 * @param realNames The actual set of names used in the tree, with deletes.
	 * @param seen The set of states seen traversing DFS l-r. Note that bit i is true iff i has **not** been seen yet. Simplifies arith.
	 * @param finals The set of final states in the DFA. Used for determining labels in create.
	 * @return
	 */
	private static Optional<SafraNode> transform(Map<Integer, BitSet> transition, SafraNode elem, boolean fresh,
												 BitSet initNames, BitSet realNames, BitSet seen, BitSet finals) {
		//Unmark
		boolean mark = false;

		//Setup the new label
		BitSet newLabel = new BitSet(elem.getLabel().length());

		//A (possible) new child node
		Optional<SafraNode> newNode = Optional.empty();

		BitSet oldLabel = elem.getLabel();

		//If this is a new node, then the first two steps are assumed to have happened, so we copy these.
		if (fresh) {
			mark = elem.isMark();
			newLabel.or(oldLabel);
		}

		//Skip steps 2-3 if the node is a new one
		if (!fresh) {

			//Update
			//For each state in the label, run it past the transition system
			for (int i = oldLabel.nextSetBit(0); i >= 0; i = oldLabel.nextSetBit(i + 1)) {
				//For each state, update it with the transition set
				newLabel.or(transition.getOrDefault(i, new BitSet()));
			}

			//Create
			//If the labels intersect
			if (newLabel.intersects(finals)) {
				//Intersect the bitsets
				BitSet subLabel = new BitSet(finals.size());
				subLabel.or(newLabel);
				subLabel.and(finals);

				//Compute the new name
				int name = initNames.nextClearBit(0);

				//Create the new node
				newNode = Optional.of(SafraNode.create(name + 1, subLabel, true, new ArrayList<SafraNode>()));

				//Update both nametrackers
				initNames.set(name);
				realNames.set(name);
			}
		}


		//Horizontal merge
		//seen must be inverted!
		newLabel.and(seen);

		//kill empty
		if (newLabel.isEmpty()) {
			//Note that this node has been deleted
			realNames.clear(elem.getName()-1);
			//Go away
			return Optional.empty();
		}

		//The initial cardinality of the RTL seen traversal
		//Since the children of this node will eventually
		//have label sets disjoint from those on their right,
		//their union has the same cardinality as the change in size
		//as the sweep happens.
		int pre = seen.cardinality();

		//New children nodes go here
		ArrayList<SafraNode> outChild = new ArrayList<>(elem.getSubnodes().size() + newNode.map(n->1).orElse(0));
		//traverse children
		List<SafraNode> nodes = elem.getSubnodes();
		for (int i = 0; i < nodes.size(); i++) {
			transform(transition, nodes.get(i), false, initNames, realNames, seen, finals).ifPresent(outChild::add);
		}

		//If we're adding a new node, then add it at the rightmost position and recurse
		newNode.flatMap(nn -> transform(transition, nn, true, initNames, realNames, seen, finals)).ifPresent(outChild::add);

		//Terminal cardinality
		int post = seen.cardinality();


		//add current label to seen
		newLabel.flip(0, newLabel.size());
		seen.and(newLabel);
		newLabel.flip(0, newLabel.size());

		//Since children subset newLabel, if |children| = |newLabel| then children=newlabel
		if (pre - post == newLabel.cardinality()) {
			//Go away
			deleteNodes(outChild, realNames);

			//Simplified result
			return Optional.of(SafraNode.create(elem.getName(), newLabel, true, new ArrayList<>()));
		}

		return Optional.of(SafraNode.create(elem.getName(), newLabel, mark, outChild));
	}


	/**
	 * Safra transition function
	 * @param am The Automata to build a tree for
	 * @param source The tree to build off of
	 * @param character The letter to transition under
	 * @return The new Safra tree
	 */
	@Override
	public SafraTree transition(Automata am, SafraTree source, int character) {
		//The set of names as if the delete hasn't happened yet, see transform source
		BitSet fakeNames = new BitSet(2*am.nStates);
		fakeNames.or(source.getUsedNames());

		//The real set of new names
		BitSet newNames = new BitSet(2*am.nStates);
		newNames.or(source.getUsedNames());

		//The states that have been seen in labels sweeping l-to-r
		BitSet seen = new BitSet(am.nStates);
		seen.flip(0,am.nStates); //seen is inverted

		//If the node exists, do the transform
		Optional<SafraNode> result = source.getNode().flatMap(node ->
				transform(am.ts.under(character), node, false, fakeNames, newNames, seen, am.finals));


		return new SafraTree(result, newNames);
	}
}
