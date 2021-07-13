/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mpsavs;

/**
 *
 * @author mlevin
 */
public class Link 
{
    private int id;
    
    private Node start, end;
    
    private int tt;
    
    public Link(int id, Node start, Node end, int tt)
    {
        this.id = id;
        this.start = start;
        this.end = end;
        this.tt = tt;
        
        start.addOutgoing(this);
        end.addIncoming(this);
    }
    
    public int getTT()
    {
        return tt;
    }
    
    public int hashCode()
    {
        return id;
    }
    
    public Node getStart()
    {
        return start;
    }
    
    public Node getEnd()
    {
        return end;
    }
    
    public String toString()
    {
        return ""+id;
    }
    
    public int getId()
    {
        return id;
    }
}
