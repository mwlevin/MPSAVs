/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mpsavs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author micha
 */
public class MetaSimulation 
{
    private int fleetsize;
    private String name;
    
    
    
    
    public MetaSimulation(String name,  int fleetsize)
    {
        this.name = name;
        this.fleetsize = fleetsize;
        
    }
    
    public double lineSearch() throws IOException
    {
        Network network = new Network(name, 1, fleetsize);
        double real_total = network.getTotalDemand();
        network = null;
        
        //System.out.println(total_demand);
        
        double bot = fleetsize/2;
        double top = fleetsize*12;
        
        double mid = 0;
        double diff = 0.2;
        
        
        
        while(top - bot > diff)
        {
            mid = (bot+top)/2;
            
            System.out.println(bot+" "+top+" "+mid+" scale="+(mid / real_total));
            
            if(isStableMC(mid / real_total))
            {
                bot = mid;
            }
            else
            {
                top = mid;
            }
            
            
        }
        
        //mid = (bot+top)/2;
        
        return mid;
    }
    

    public boolean isStableMC(double scale) throws IOException
    {
        int count = 0;
        
        for(int i = 0; i < 10; i++)
        {
            Network.rand = new Random(i*100+1);
            
            if(isStable(scale))
            {
                count++;
            }
        }
        
        //System.out.println(count);
        
        return count>6;
    }
    
    
    private double total_demand;
    
    public boolean isStable(double scale) throws IOException
    {
        
        Network network = new Network(name, scale, fleetsize);
        
        total_demand = network.getTotalDemand();
        
        network.simulate();
        
        int[] hol = network.getHOLTimes();
        
        
        double[] stableCheck = getStableDefi(hol);
        
        //System.out.println(stableCheck[stableCheck.length-1]);
        double[] filtered = getLowPassFilter(stableCheck);
        
        double[] diff = new double[8];
        
        int count = 0;
        
        for(int i = 0; i < diff.length; i++)
        {
            diff[i] = (filtered[filtered.length-1] - filtered[filtered.length-1-(i+1)*60])/( (i+1)*60) 
                    / network.getTotalDemand() / Network.dt;
  
            System.out.println("diff "+((i+1)*60)+" = "+diff[i]);
            
            if(diff[i] < 0.2)
            {
                count++;
            }
        }
        
        
        
        return diff[diff.length-1] < 0.2 || count >= diff.length/2;
    }
    
    public double[] getLowPassFilter(double[] avg)
    {
        int buffer = 120; // 1 hr
        double[] output = new double[avg.length - buffer];
        
        for(int i = buffer; i < avg.length; i++)
        {
            double sum = 0;
            for(int j = i-buffer; j < i; j++)
            {
                sum += avg[j];
            }
            sum /= buffer;
            output[i - buffer] = sum;
        }
        
        return output;
    }
    public double[] getStableDefi(int[] hol)
    {
        double[] output = new double[hol.length];
        
        for(int i = 0; i < output.length; i++)
        {
            double sum = 0;
            for(int j = 0; j <= i; j++)
            {
                sum += hol[j];
            }
            
            sum /= (i+1);
            
            output[i] = sum;
        }
        
        return output;
        
    }
}
