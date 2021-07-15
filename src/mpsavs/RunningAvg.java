/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mpsavs;

import java.io.Serializable;

/**
 * This class implements a running average. It stores the total count and the weight only.
 * @author Michael
 */
public class RunningAvg implements Serializable
{
	private double value;
	private double count;
        
        private double x_squared;
	
        /**
         * Constructs the {@link RunningAvg} with 0.
         */
	public RunningAvg()
	{
		value = 0;
                count = 0;
                x_squared = 0;
	}
        
        
        
  
        /**
         * Adds the given value with a weight of 1
         * @param val the value to be added
         */
	public void add(double val)
	{
            value += val;
            x_squared += val*val;
            count++;
	}
        
        /**
         * Adds the given value at the given weight
         * @param val the value to be added
         * @param weight the weight of the value
         */
        public void add(double val, double weight)
        {
            value += val * weight;
            x_squared += val*val * weight;
            count += weight;
        }
	
        public void add(RunningAvg rhs)
        {
            if(rhs.count > 0)
            {
                add(rhs.value/rhs.count, rhs.count);
            }
        }
        /**
         * Returns the total weight
         * @return the total weight
         */
	public double getCount()
	{
            return count;
	}
	
        public double getTotal()
        {
            return value;
        }
        
        /**
         * Returns the running average
         * @return the running average
         */
	public double getAverage()
	{
            if(count > 0)
            {
		return value / count;
            }
            else
            {
                return 0;
            }
	}
        
        public double getStDev()
        {
            if(count > 0)
            {
                double avg = value / count;
                return Math.sqrt(x_squared / count - avg * avg);
            }
            else
            {
                return 0;
            }
        }
	
        /**
         * Resets the total value and weight to 0
         */
	public void reset()
	{
            value = 0;
            count = 0;
	}
}