/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mpsavs;

import java.util.ArrayList;

/**
 *
 * @author micha
 */
public class BusRoute extends ArrayList<Node>
{
    private int id;
    
    public BusRoute(int id, Link start)
    {
        this.id = id;
        add(start.getStart());
    }
    
    public int getId()
    {
        return id;
    }
    
    public Node closestStop(CNode c)
    {
        Node o = c.getOrigin();
        Node d = c.getDest();
        
        if(!contains(d))
        {
            return null;
        }
        
        Node best = null;
        int min = Integer.MAX_VALUE;
        
        for(Node n : this)
        {
            if(n == d)
            {
                break;
            }
            
            int temp = Network.active.getTT(o, n);
            if(temp < min)
            {
                best = n;
                min = temp;
            }
        }
        
        return best;
    }
    
    public boolean isServed(CNode c)
    {
        int o_idx = indexOf(c.getOrigin());
        int d_idx = indexOf(c.getDest());
        
        return o_idx >= 0 && o_idx < d_idx;
    }
}
