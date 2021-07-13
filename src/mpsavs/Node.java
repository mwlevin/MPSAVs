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
    private Set<Link> incoming, outgoing;
    
    private int id;
    
    public Node(int id)
    {
        this.id = id;
        
        incoming = new HashSet<>();
        outgoing = new HashSet<>();
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
