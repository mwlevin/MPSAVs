/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mpsavs;

import ilog.concert.IloException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

/**
 *
 * @author mlevin
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, IloException
    {
        // TODO code application logic here
        
        Network test = new Network("SiouxFalls", 45.0/28835, 10);
        
        
        test.simulate();
        
        System.out.println(test.getAvgDispatchDelay());
        System.out.println(test.getAvgPickupDelay());
        
        
        System.out.println(test.stableRegionMaxServed()+" "+test.getTotalDemand());
        System.out.println("actual empty time: "+test.emptyTT / test.T_hr * test.dt);
        
        System.out.println(test.total_customers);
    }
    
}
