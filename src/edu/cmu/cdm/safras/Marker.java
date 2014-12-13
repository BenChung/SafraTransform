package edu.cmu.cdm.safras;

import java.util.BitSet;

/**
 * Created by Ben Chung on 12/12/2014.
 *
 * Utility class to check containment in a given Rabin pair
 */
public class Marker {
	/**
	 * Sees if tree is in the right set.
	 * @param tree The tree to check
	 * @param state The Beuchi state to generate with
	 * @return True iff the Beuchi state is in a marked node in tree
	 */
	public boolean inR(SafraNode tree, int state) {
		if (tree.getLabel().get(state) && tree.isMark())
			return true;
		return tree.getSubnodes().stream().map(sn->inR(sn, state)).reduce(false, (l, r) -> l || r);
	}

	/***
	 * Sees if tree is in the left set.
	 * @param tree The tree to check
	 * @param state The Beuchi state to generate with
	 * @return True iff the state does not exist in the tree
	 */
	public boolean inL(SafraNode tree, int state) {
		if (tree.getLabel().get(state))
			return false;
		return tree.getSubnodes().stream().map(sn->inL(sn,state)).reduce(true, (l,r) -> l && r);
	}
}
