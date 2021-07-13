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
public class CNode 
{
    private List<Integer> waiting; // stores the time they arrived
    
    private double lambda;

    private Node origin, dest;

    public CNode(Node origin, Node dest, double lambda)
    {
        this.origin = origin;
        this.dest = dest;
        this.lambda = lambda / Network.dt; // convert trips per hour to trips per timestep
        
        waiting = new ArrayList<>();
    }
    
    public void step()
    {
        double prob = 1.0/lambda;
        
        if(Network.rand.nextDouble() < prob)
        {
            waiting.add(Network.t);
        }
    }
    
    public int getHOLTime()
    {
        if(waiting.isEmpty())
        {
            return 0;
        }
        
        return Network.t - waiting.get(0);
    }
}
