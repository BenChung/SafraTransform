package edu.cmu.cdm.safras;

public class Pair<T,V> {
	private T l;
	private V r;

	public Pair(T l, V r) {
		this.l = l;
		this.r = r;
	}

	public T getL() {
		return l;
	}

	public V getR() {
		return r;
	}
}
