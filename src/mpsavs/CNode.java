/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mpsavs;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author mlevin
 */
public class CNode 
{
    private List<Integer> waiting; // stores the time they arrived
    
    private RunningAvg waitingTime;
    
    private double lambda;

    private Node origin, dest;

    public CNode(Node origin, Node dest, double lambda)
    {
        this.origin = origin;
        this.dest = dest;
        this.lambda = lambda * Network.dt; // convert trips per hour to trips per timestep
        
        waiting = new LinkedList<>();
        
        waitingTime = new RunningAvg();
    }
    
    public Node getOrigin()
    {
        return origin;
    }
    
    public Node getDest()
    {
        return dest;
    }
    
    public void pickup(SAV sav, Path path)
    {
        int delay = sav.getDelay(path) + path.getPickupDelay(this);
        
        waitingTime.add(delay);
        
        waiting.remove(0);
    }
    
    public String toString()
    {
        return origin.getId()+"-"+dest.getId();
    }
    
    public void step()
    {
        double prob = lambda;
        
        
        if(Network.rand.nextDouble() < prob)
        {
            waiting.add(Network.t);
        }
    }
    
    public int getNumWaiting()
    {
        return waiting.size();
    }
    
    public double getLambda()
    {
        return lambda / Network.dt;
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
