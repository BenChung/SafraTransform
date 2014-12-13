SafraTransform
==============

An implementation of Safra's algorithm, converting Beuchi automata to Rabin automata in Java.

This implementation uses a single-traversal version of the standard 5-step method, as seen in MonopassTransformer.java. Additionally,
we use bit sets heavily for performance, providing reasonable efficiency across large automata.

We reccomend starting with Main.java which contains the algorithm entry point. The system takes in automata in a 
obvious format and produces an in-memory representation of the Rabin automata.
