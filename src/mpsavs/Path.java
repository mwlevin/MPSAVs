/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mpsavs;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mlevin
 */
public class Path extends ArrayList<Node>
{
    private List<CNode> served;
    
    public Path(List<CNode> served)
    {
        this.served = served;
    }
    
    public boolean isServed(CNode n)
    {
        return served.contains(n);
    }
    
    public int getPickupDelay(CNode c)
    {
        if(!isServed(c))
        {
            throw new RuntimeException("c is not served");
        }
        
        for(Node n : this)
        {
            if(n == c.getOrigin())
            {
                return Network.active.getTT(get(0), n);
            }
        }
        
        throw new RuntimeException("c is not visited");
    }
    
    public List<CNode> getServed()
    {
        return served;
    }
    
    public Node getDest()
    {
        return get(size()-1);
    }
    
    public Node getOrigin()
    {
        return get(0);
    }
    public int getTT()
    {
        int output = 0;
        
        for(int i = 0; i < size()-1; i++)
        {
            output += Network.active.getTT(get(i), get(i+1));
        }
        
        return output;
    }
}
