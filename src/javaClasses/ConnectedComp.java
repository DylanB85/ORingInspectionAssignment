package javaClasses;

import java.util.LinkedList;

public class ConnectedComp {
	
	private LinkedList<Integer> linkList;
	
	public ConnectedComp() {
		linkList = new LinkedList<Integer>();
	}
	
	public boolean isEmpty()
	{
		return (linkList.size()==0);
	}
	
	public void enqueue(int position)
	{
		linkList.add(position);
	}
	
	public int dequeue()
	{
		int position = linkList.get(0);
		linkList.remove(0);
		return position;
	}
}
