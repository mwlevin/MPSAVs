/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mpsavs;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author mlevin
 */
public class Node 
{
    public int cost;
    public Link prev;
    
    private Set<Link> incoming, outgoing;
    
    private int id, type;
    
    public Node(int id, int type)
    {
        this.id = id;
        this.type = type;
        
        incoming = new HashSet<>();
        outgoing = new HashSet<>();
    }
    
    public int getType()
    {
        return type;
    }
    
    public Set<Link> getIncoming()
    {
        return incoming;
    }
    
    public Set<Link> getOutgoing()
    {
        return outgoing;
    }
    
    public void addIncoming(Link l)
    {
        incoming.add(l);
    }
    
    public void addOutgoing(Link l)
    {
        outgoing.add(l);
    }
    
    public String toString()
    {
        return ""+id;
    }
    
    public int getId()
    {
        return id;
    }
    
    public int hashCode()
    {
        return id;
    }
    
    
}
