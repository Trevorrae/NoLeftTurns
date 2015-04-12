// CS 2XB3 Lab 2 - Final Project
// Hassaan Malik - 1224997
// Trevor Rae - 1324949
// Paul Warnick - 1300963

/*
 * Description:
 * 
 * Used as a helper for Edge Weighted Graph to create edges.
 */

package graph;

public class Edge {
	private final int v;
	private final int w;
	private final double weight;
	
	public Edge(int v, int w, double weight){
		this.v = v;
		this.w = w;
		this.weight = weight;
	}
	
	public double weight(){ return weight; }
	
	public int either(){ return v; }
	
	public int getW() { return w; }
	
	public int other(int vertex){
		if(vertex == v){ return w; }
		
		else if(vertex == w){ return v; }
		
		else throw new RuntimeException("bad street");
	}
	
	public int compareTo(Edge that){
		if(this.weight() < that.weight()){
			return -1;
		}
		else if (this.weight() > that.weight()){
			return 1;
		}
		else return 0;
	}
	
	public String toString(){ return String.format("%d-%d %.2f", v, w, weight); }
}
