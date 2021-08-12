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
        

        
        Node best = null;
        int min = Integer.MAX_VALUE;
        int last_walkable_node = -1;
        
        for(int i = 0; i < size(); i++)
        {
            Node n = get(i);
            
            if(Network.active.getLength(n, d) < 0.25)
            {
                last_walkable_node = i;
            }
        }
        
        for(int i = 0; i < last_walkable_node; i++)
        {
            Node n = get(i);
            
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
        int o_idx = -1;
        int d_idx = -1;
        
        for(int i = 0; i < size(); i++)
        {
            Node n = get(i);
            if(o_idx == -1 && Network.active.getLength(n, c.getOrigin()) < 0.25)
            {
                o_idx = i;
            }
            else if(Network.active.getLength(n, c.getDest()) < 0.25)
            {
                d_idx = i;
            }
        }
        //int o_idx = indexOf(c.getOrigin());
        //int d_idx = indexOf(c.getDest());
        
        return o_idx >= 0 && o_idx < d_idx;
    }
}
