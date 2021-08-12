/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mpsavs;

/**
 *
 * @author micha
 */
public class SAEV extends SAV 
{
    // battery is measured in miles
    private double battery;
    public static double max_battery = 100; //263 * 0.8; // tesla range
    
    public static double charge_rate = 200; // 200 miles per 15 min
    
    public SAEV(Node loc)
    {
        super(loc);
        
        
        battery = (Network.rand.nextDouble()/2 + 0.5)*max_battery;
    }
    
    public int getDelay(Path path)
    {
        
        double minEndBattery = Network.active.getLength(path.getDest(), getNearestCharger(path.getDest()));
        
        double consumed = path.getLength() + Network.active.getLength(getLocation(), path.getOrigin());
        
        if(battery - consumed < minEndBattery)
        {
            // recharge until max
            
            int output = 0;
            
            Node closestCharger = getBestCharger(getLocation(), path.getOrigin());
            
            double projectedBattery = battery - Network.active.getLength(getLocation(), closestCharger);
            
            output += Network.active.getTT(getLocation(), closestCharger); // travel time to charger
            
            output += Math.ceil( (max_battery - projectedBattery)/charge_rate / Network.dt); // time to charge
            
            output += Network.active.getTT(closestCharger, path.getOrigin());
            
            return output;
        }
        else
        {
            return Network.active.getTT(getLocation(), path.getOrigin());
        }
    }
    
    public void dispatch(Path path)
    {
        
        
        double minEndBattery = Network.active.getLength(path.getDest(), getNearestCharger(path.getDest()));
        
        double consumed = path.getLength() + Network.active.getLength(getLocation(), path.getOrigin());
        
        
        if(battery - consumed < minEndBattery) // assume recharging
        {
            Node closestCharger = getBestCharger(getLocation(), path.getOrigin());
            
            if(Network.PRINT)
            {
                System.out.println("\tCharging at node "+closestCharger);
            }
            
            //System.out.println("\tTT to charger: "+Network.active.getTT(getLocation(), closestCharger));
            
            double projectedBattery = battery - Network.active.getLength(getLocation(), closestCharger);
            
           
            
            //System.out.println("\tCharge time: "+Math.ceil( (max_battery - projectedBattery)/charge_rate / Network.dt));
            
            //System.out.println("\tTT to path: "+Network.active.getTT(closestCharger, path.getOrigin()));
            
            battery = max_battery - Network.active.getLength(closestCharger, path.getOrigin()) - path.getLength();
        }
        else // no recharging
        {
            battery -= consumed;
        }
        
        super.dispatch(path);
    }
    
    public static Node getBestCharger(Node q, Node r)
    {
        int min = Integer.MAX_VALUE;
        Node best = null;
        
        for(Node n : Network.active.getENodes())
        {
            int temp = Network.active.getTT(q, n) + Network.active.getTT(n, r);
            
            if(temp < min)
            {
                min = temp;
                best = n;
            }
        }
        
        return best;
    }
    
    public static Node getNearestCharger(Node start)
    {
        double min = max_battery;
        Node best = null;
        
        for(Node n : Network.active.getENodes())
        {
            double temp = Network.active.getLength(start, n);
            
            if(temp < min)
            {
                min = temp;
                best = n;
            }

        }
        
        return best;
    }
    
    
}
