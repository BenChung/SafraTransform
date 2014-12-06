package edu.cmu.cdm.safras;

import java.util.BitSet;
import java.util.Optional;

/**
 * A Safra tree, a pair of a tree and the names used in that tree. Used to simplify computation.
 */
public class SafraTree {

	public Optional<SafraNode> getNode() {
		return node;
	}

	/**
	 * The root of this tree
	 */
	private final Optional<SafraNode> node;

	public BitSet getUsedNames() {
		return usedNames;
	}

	/**
	 * The set of names used in this tree
	 */
	private final BitSet usedNames;

	@Override
	public String toString() {
		return node.toString();
	}

	/**
	 * Create a new safra tree from a root node and the set of names used in the tree
	 * @param node The root node
	 * @param usedNames The set of names
	 */
	public SafraTree(Optional<SafraNode> node, BitSet usedNames) {
		this.node = node;
		this.usedNames = usedNames;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SafraTree safraTree = (SafraTree) o;

		if (node != null ? !node.equals(safraTree.node) : safraTree.node != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return node != null ? node.hashCode() : 0;
	}
}
