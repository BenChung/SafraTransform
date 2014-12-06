package edu.cmu.cdm.safras;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.util.*;

public class Main {


	/**
	 * Parse a file filename into an internal automata
	 * @param filename The file to parse
	 * @return An abstract represntation of a Buchi automata
	 * @throws IOException If the file cannot be read
	 */
	private static Automata parse(String filename) throws IOException {
		/*
			This function is a state machine, with the current state stored in state
		 */
		List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(filename));
		int state = 0;
		int nstates = 0;
		int ntrans = 0, alphsize = 0;
		BitSet initals = new BitSet(), finals = new BitSet();
		List<TransitionSystem.Transition> transitions = new LinkedList<>();

		for (String line : lines) {
			//File start
			if (line.equals("BUECHI") && state == 0) {
				state = 1;
				//The file's started, let's get off to the races
				continue;
			}
			if (state == 0) continue; // File hasn't started yet
			if (line.startsWith("#")) continue; //Comment

			switch (state) {
				case 1:
					//First item is the number of states
					nstates = Integer.parseInt(line);
					state = 2;
					continue;
				case 2:
					//Second is the alphabet size
					alphsize = Integer.parseInt(line);
					state = 6;
					continue;
				case 6:
					//Third (sorry about the name) is the number of transitions
					ntrans = Integer.parseInt(line);
					state = 3;
					continue;
				case 3:
					//Fourth (name etc etc) is the list of transitions
					String[] split = line.split(" ");
					transitions.add(new TransitionSystem.Transition(Integer.parseInt(split[0])-1, Integer.parseInt(split[4])-1, Integer.parseInt(split[2])));

					if (--ntrans <= 0) {
						//If we've read all of them, then stop
						state = 4;
						continue;
					}
					continue;
				case 4:
					//Fifth is initial states
					Arrays.asList(line.split(" ")).stream().filter(str -> !str.isEmpty()).map(Integer::parseInt)
							.map(n->n-1)
							.forEach(initals::set);
					state = 5;
					continue;
				case 5:
					//Sixth is final states
					Arrays.asList(line.split(" ")).stream().filter(str -> !str.isEmpty()).map(Integer::parseInt)
							.map(n -> n - 1)
							.forEach(finals::set);
					break;
			}
			//We're done, go away now.
			break;
		}
		return new Automata(nstates, initals, finals, alphsize, transitions);
	}


	public static void main(String[] args) throws IOException {
		//Parse the input automata
		Automata source = parse(args[0]);

		//The starting state
		SafraNode initial;

		//The set of names used
		BitSet used = new BitSet(2*source.nStates);
		//Create the initial tree
		if (!source.initial.intersects(source.finals)) {
			initial = SafraNode.create(1, source.initial, false, new ArrayList<>());
			used.set(0);
		} else {
			BitSet bs = new BitSet(source.initial.size());
			bs.or(source.initial);
			bs.and(source.finals);
			if (bs.equals(source.initial)) { //source subset initial
				initial = SafraNode.create(1, source.initial, true, new ArrayList<>());
				used.set(0);
			} else {
				SafraNode ic = SafraNode.create(2, bs, false, new ArrayList<>());
				initial = SafraNode.create(1, source.initial, true, (ArrayList<SafraNode>) Arrays.asList(ic));
				used.set(0,1);
			}
		}

		//The set of transitions of the Rabin automata
		HashMap<SafraTree, HashMap<Integer, SafraTree>> transitions = new HashMap<>();

		//The BFS frontier
		Queue<SafraTree> frontier = new LinkedList<>();
		//A constant-time guard for aforementioned frontier
		HashSet<SafraTree> frontierSet = new HashSet<>();

		//The nodes that have been seen. Improves memory performance.
		HashMap<SafraTree,SafraTree> treeNodes = new HashMap<>();

		//The initial tree
		SafraTree tree = new SafraTree(Optional.of(initial), used);
		frontier.add(tree);
		frontierSet.add(tree);


		//Multipass is the reference (slower) implementation, monopass is the fast one
		//SafraTransition tform = new MultipassTransformer();
		SafraTransition tform = new MonopassTransformer();

		//Timing start
		long st = System.nanoTime();

		//BFS
		while (!frontier.isEmpty()) {
			SafraTree elem = frontier.poll();
			frontierSet.remove(elem);

			//Generate the new map for the transition function
			HashMap<Integer, SafraTree> trans = new HashMap<>();
			for (int i = 1; i <= source.alphsize; i++) {
				//Find the transition
				SafraTree tgt = tform.transition(source, elem, i);

				//If we haven't seen the result before, add it to the frontier
				if (!treeNodes.containsKey(tgt) && !frontierSet.contains(tgt)
						&& !transitions.containsKey(tgt) && !tgt.equals(elem)) {
					frontier.offer(tgt);
					frontierSet.add(tgt);
				}

				//If the set doesn't have the target yet, add it in.
				if (!treeNodes.containsKey(tgt)) {
					trans.put(i, tgt);
					treeNodes.put(tgt,tgt);
				} else {
					//Otherwise, use the target that's already there and toss the new one out.
					trans.put(i, treeNodes.get(tgt));
					tgt.getNode().ifPresent(SafraNode::dispose);
				}

			}
			//Save the new transition
			transitions.put(elem, trans);
		}
		//Compute # of seconds elapsed
		float rt = (System.nanoTime() - st)/1000000000.0f;

		//Result
		System.out.println("time: " + rt + " states: " + transitions.size());
	}
}
