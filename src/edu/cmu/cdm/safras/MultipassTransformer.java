package edu.cmu.cdm.safras;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Multi pass Safra transformation implementation.
 * Reference implementation, slower.
 */
public class MultipassTransformer implements SafraTransition {

	/**
	 * Deletes the nodes in the list and all of their children
	 * @param nodes The nodes to delete
	 * @param names The name set to update
	 */
	private static void deleteNodes(List<SafraNode> nodes, BitSet names) {
		for (SafraNode sn : nodes) {
			names.clear(sn.getName()-1);
			deleteNodes(sn.getSubnodes(), names);
		}
	}

	@Override
	public SafraTree transition(Automata automata, SafraTree iTree, int character) {
		if (!iTree.getNode().isPresent())
			return iTree;

		//The set of transitions in the Buchi automata
		Map<Integer, BitSet> transition = automata.ts.under(character);
		//Final transitions in the automata
		BitSet finals = automata.finals;

		//The initial tree (must be non-empty)
		SafraNode tree = iTree.getNode().get();
		//The inital set of used names
		BitSet used = iTree.getUsedNames();

		//The new (updated) set of used names
		BitSet newUsed = new BitSet(used.size());
		newUsed.or(used);

		//Standard Safra progression
		SafraNode um = unmark(tree);
		SafraNode up = update(um, transition);
		SafraNode cr = create(up, newUsed, finals);
		SafraNode hm = hmerge(cr, new BitSet(tree.getLabel().size()));
		Optional<SafraNode> pr = prune(hm, newUsed);
		Optional<SafraNode> vm = pr.map(node->vmerge(node, newUsed));
		return new SafraTree(vm, newUsed);
	}

	/**
	 * Unmark the source tree
	 * @param source the tree to unmark
	 * @return An unmarked Safra tree
	 */
	private SafraNode unmark(SafraNode source) {
		return SafraNode.create(source.getName(), source.getLabel(), false, source.getSubnodes().stream().map(this::unmark).collect(Collectors.toList()));
	}

	/**
	 * Update all labels based on the transition function
	 * @param source The source tree
	 * @param transition The transition function
	 * @return An updated tree
	 */
	private SafraNode update(SafraNode source, Map<Integer, BitSet> transition) {
		BitSet oldLabel = source.getLabel();

		BitSet label = new BitSet(oldLabel.size());
		for (int i = oldLabel.nextSetBit(0); i >= 0; i = oldLabel.nextSetBit(i + 1)) {
			label.or(transition.getOrDefault(i, new BitSet()));
		}

		return SafraNode.create(source.getName(), label, source.isMark(), source.getSubnodes().stream()
				.map(node -> this.update(node, transition)).collect(Collectors.toList()));
	}

	/**
	 * Create a new child if the label set intersects the final set
	 * @param source The source tree
	 * @param usedNames The set of names used in the tree initially, used to create the new name
	 * @param finalSet The set of final states
	 * @return A tree with children added
	 */
	private SafraNode create(SafraNode source, BitSet usedNames, BitSet finalSet) {
		//The child's label
		BitSet subLabel = new BitSet(source.getLabel().size());
		subLabel.or(source.getLabel());
		subLabel.and(finalSet);

		//Repeat for the children
		List<SafraNode> outChildren = source.getSubnodes().stream().map(node -> create(node, usedNames, finalSet)).collect(Collectors.toList());

		//If there are some states remaining, then add the child on the right.
		if (subLabel.cardinality() > 0) {
			int name = usedNames.nextClearBit(0);
			outChildren.add(SafraNode.create(name + 1, subLabel, true, new LinkedList<>()));
			usedNames.set(name);
		}
		return SafraNode.create(source.getName(), source.getLabel(), source.isMark(), outChildren);
	}

	/**
	 * Horizontal merge.
	 * @param source The source tree
	 * @param leftAcc The set of states seen
	 * @return A tree with states seen to the left of the current one removed
	 */
	private SafraNode hmerge(SafraNode source, BitSet leftAcc) {
		BitSet newLabel = new BitSet(source.getLabel().size());
		newLabel.or(leftAcc);
		newLabel.flip(0, newLabel.size());
		newLabel.and(source.getLabel());

		List<SafraNode> merged = source.getSubnodes().stream().map(node -> hmerge(node, leftAcc)).collect(Collectors.toList());

		leftAcc.or(source.getLabel());

		return SafraNode.create(source.getName(), newLabel, source.isMark(),
				merged);
	}

	/**
	 * Prunes empty nodes.
	 * @param source The input tree.
	 * @param usedNames The set of used names, repopulated.
	 * @return A tree with all empty nodes removed
	 */
	private Optional<SafraNode> prune(SafraNode source, BitSet usedNames) {
		if (source.getLabel().cardinality() == 0) {
			usedNames.clear(source.getName()-1);
			deleteNodes(source.getSubnodes(), usedNames);
			return Optional.empty();
		}

		List<SafraNode> res = source.getSubnodes().stream().map(node -> prune(node, usedNames))
				.flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())
				.collect(Collectors.toList());
		return Optional.of(SafraNode.create(source.getName(), source.getLabel(), source.isMark(), res));
	}

	/**
	 * Adds all child states to set
	 * @param toTrav The tree to traverse
	 * @param set The set to update
	 */
	private void traverseSet(SafraNode toTrav, BitSet set) {
		set.or(toTrav.getLabel());
		for (SafraNode s : toTrav.getSubnodes())
			traverseSet(s, set);
	}

	/**
	 * Vertical merge.
	 * @param source The source tree
	 * @param usedNames The set of names used
	 * @return A vertical merged tree.
	 */
	private SafraNode vmerge(SafraNode source, BitSet usedNames) {
		BitSet sub = new BitSet(source.getLabel().size());

		//Get the states active in the children
		for (SafraNode s : source.getSubnodes())
			traverseSet(s, sub);

		//If their union equals the current label, delete them.
		if (sub.equals(source.getLabel())) {
			deleteNodes(source.getSubnodes(), usedNames);
			return SafraNode.create(source.getName(), source.getLabel(), true, new LinkedList<>());
		}

		//Recurse
		return SafraNode.create(source.getName(), source.getLabel(), source.isMark(), source.getSubnodes().stream().map(node -> vmerge(node, usedNames)).collect(Collectors.toList()));
	}
}
