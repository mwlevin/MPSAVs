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
    
    private RunningAvg pickupDelay, dispatchDelay;
    
    private double lambda;

    private Node origin, dest;
    
    private boolean isBusServed;
    private Node closestDropoff;

    public CNode(Node origin, Node dest, double lambda)
    {
        this.origin = origin;
        this.dest = dest;
        this.lambda = lambda * Network.dt; // convert trips per hour to trips per timestep
        
        waiting = new LinkedList<>();
        
        pickupDelay = new RunningAvg();
        dispatchDelay = new RunningAvg();
        
        origin.addCNode(this);
        
        isBusServed = Network.BUSES && Network.active.isBusServed(this);
        
        if(Network.BUSES)
        {
            closestDropoff = Network.active.getBusDropoff(this);
        }
    }
    
    
    
    public boolean isBusServed()
    {
        return isBusServed;
    }
    
    public Node getBusDropoff()
    {
        return closestDropoff;
    }
    
    public void addDemand(double l)
    {
        this.lambda += l * Network.dt;
    }
    
    public Node getOrigin()
    {
        return origin;
    }
    
    public Node getDest()
    {
        return dest;
    }
    
    public void busPickup()
    {
        waiting.remove(0);
    }
    
    public void pickup(SAV sav, Path path)
    {
        int waitingForPickup = sav.getDelay(path) + path.getPickupDelay(this);
        
        pickupDelay.add(waitingForPickup);
        
        int waitingForDispatch = waiting.remove(0);
        
        dispatchDelay.add(waitingForDispatch);
    }
    
    public String toString()
    {
        return origin.getId()+"-"+dest.getId();
    }
    
    public RunningAvg getDispatchDelay()
    {
        return dispatchDelay;
    }
    
    public RunningAvg getPickupDelay()
    {
        return pickupDelay;
    }
    
    public void step()
    {
        double prob = lambda;
        
        
        if(Network.rand.nextDouble() < prob)
        {
            Network.active.total_customers++;
            waiting.add(Network.t);
            
            if(Network.PRINT)
            {
                System.out.println("\tNew customer "+this+" "+waiting.size());
            }
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
