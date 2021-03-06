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
    public static double DETECT_EPSILON = 1;
    
    
    private int fleetsize;
    private String name;
    
    
    
    
    public MetaSimulation(String name,  int fleetsize)
    {
        this.name = name;
        this.fleetsize = fleetsize;
        
    }
    
    public double lineSearch(double guess) throws IOException
    {
        Network network = new Network(name, 1, fleetsize);
        double real_total = network.getTotalDemand();
        network = null;
        
        //System.out.println(total_demand);
        
        double top = guess*1.2;
        double bot = guess*0.4; 
        /*
        double bot = fleetsize/2;
        double top;
        
        if(Network.BUSES)
        {
            top = fleetsize*25;
        }
        else if(Network.EVs)
        {
            top = fleetsize*6;
        }
        else
        {
            top = fleetsize*12;
        }
        */
        
        double mid = 0;
        double diff = 0.2;
        
        
        
        while(top - bot > diff)
        {
            mid = (bot+top)/2;
            
            
            
            boolean output = isStableMC(mid / real_total);
            
            System.out.println("\n*****");
            System.out.println("Line search checking "+bot+" "+top+" "+mid+" scale="+(mid / real_total)+" "+output);
            System.out.println("*****\n");
            
            if(output)
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
        
        return count>5;
    }
    
    
    private double total_demand;
    
    public boolean isStable(double scale) throws IOException
    {
        
        Network network = new Network(name, scale, fleetsize);
        
        total_demand = network.getTotalDemand();
        
        network.simulate();
        
        double[] hol = network.getHOLTimes();
        
        
        //double[] stableCheck = getStableDefi(hol);
        
        //System.out.println(stableCheck[stableCheck.length-1]);
        double[] filtered = getLowPassFilter(hol);
        
        /*
        for(int i = 0; i < filtered.length; i++)
        {
            System.out.println(i+"\t"+filtered[i]);
        }
        */
        
        double[] diff = new double[4];
        
        int count = 0;
        
        for(int i = 0; i < diff.length; i++)
        {
            diff[i] = (filtered[filtered.length-1] - filtered[filtered.length-1-(i+1)*60])/( (i+1)*60) 
                    / network.getTotalNonBusDemand() / Network.dt;
  
            System.out.println("diff "+((i+1)*60)+" = "+diff[i]);
            
            if(diff[i] < DETECT_EPSILON)
            {
                count++;
            }
        }
        
        
        
        return diff[diff.length-1] < DETECT_EPSILON || count >= diff.length/2;
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
    public double[] getStableDefi(double[] hol)
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
