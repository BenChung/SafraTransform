package edu.cmu.cdm.safras;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

public class SafraNode {
	private int name;
	private BitSet label;
	private boolean mark;
	private List<SafraNode> subnodes;

	//Caching, turn on only if you're running out of memory
	//Dynamic flyweight, basically
	private static int ctr = 0;
	public static boolean cacheValues = false;
	private static SafraNode[] cache = new SafraNode[30];
	private static final int cachesize = 30;

	/**
	 * Dispose of a Safra node. Call only if the node would normally go to the GC.
	 * @param sn The node to dispose
	 */
	public static void dispose(SafraNode sn) {
		if (!cacheValues) return;
		for (int i = 0; i < sn.subnodes.size(); i++)
			dispose(sn.subnodes.get(i));

		if (ctr < cachesize) {
			cache[ctr++] = sn;
		}
	}

	/**
	 * Create a new Safra node
	 * @param name The name of the node
	 * @param label The node's label
	 * @param mark If the node is marked
	 * @param subnodes The nodes that are child to this node
	 * @return The new node
	 */
	public static SafraNode create(int name, BitSet label, boolean mark, List<SafraNode> subnodes) {
		if (ctr > 0 && cacheValues) {
			SafraNode outp = cache[--ctr];
			outp.name = name;
			outp.label = label;
			outp.mark = mark;
			outp.subnodes = subnodes;
			return outp;
		}
		return new SafraNode(name, label, mark, subnodes);
	}

	private SafraNode(int name, BitSet label, boolean mark, List<SafraNode> subnodes) {
		this.name = name;
		this.label = label;
		this.mark = mark;
		this.subnodes = subnodes;
	}

	public int getName() {
		return name;
	}

	public BitSet getLabel() {
		return label;
	}

	public boolean isMark() {
		return mark;
	}

	public List<SafraNode> getSubnodes() {
		return subnodes;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SafraNode safraNode = (SafraNode) o;

		if (mark != safraNode.mark) return false;
		if (name != safraNode.name) return false;
		if (!label.equals(safraNode.label)) return false;

		if (subnodes.size() != safraNode.subnodes.size()) return false;
		for (int i = 0; i < subnodes.size(); i++) {
			if (!subnodes.get(i).equals(safraNode.subnodes.get(i))) return false;
		}
		return true;
	}

	private boolean rec = false;
	@Override
	public int hashCode() {
		if (rec)
			return 0;
		rec = true;
		int result = name;
		result = 31 * result + label.hashCode();
		result = 31 * result + (mark ? 1 : 0);
		int listHash = 1;

		for (int i = 0; i < subnodes.size(); i++) {
			SafraNode e = subnodes.get(i);
			listHash = 31 * listHash + (e == null ? 0 : e.hashCode());
		}
		result = 31 * result + listHash;
		rec = false;
		return result;
	}

	@Override
	public String toString() {
		return "{"+name+","+label.toString()+","+mark+",{"+subnodes.stream().map(SafraNode::toString).collect(Collectors.joining(","))+"}}";
	}
}
