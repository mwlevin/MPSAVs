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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

/**
 *
 * @author mlevin
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception
    {
        
        //stableRegionTest("SiouxFalls", 100, 500, 25);
        //System.exit(0);
        
        // TODO code application logic here
        
        //MetaSimulation test = new MetaSimulation("SiouxFalls", 100);
        
        //System.out.println(test.isStable(620.0/28835));
        
        //System.out.println(test.lineSearch());
        
        //Network network = new Network("SiouxFalls", 480.0/28835, 100);
        
        Network network = new Network("coacongress", 480.0/62836, 100);
        
        //network.test();
        
        //Network network = new Network("test", 45, 100);
        
        
        //network.simulate();
        
        //System.out.println(network.getAvgDispatchDelay());
        //System.out.println(network.getAvgPickupDelay());
        
        network.createRSPaths();
        
        //System.out.println(network.stableRegionMaxServed()+" "+network.getTotalDemand());
    
        
        
        //System.out.println("actual empty time: "+test.emptyTT / test.T_hr * test.dt);
        
        //System.out.println(test.total_customers);
    }
    
    public static void stableRegionTest(String name, int min, int max, int inc) throws IOException, IloException
    {
        PrintStream fileout = new PrintStream(new FileOutputStream(new File("sr_"+name+""
                + (Network.EVs? "EVs":"") + ".txt"), true), true);
        
        fileout.println("Fleet size\tSim stable demand\tSim avgC\tCalc stable demand\tCalc avgC");
        for(int i = min; i <= max; i += inc)
        {
            MetaSimulation test = new MetaSimulation(name, i);
            
            double sr = test.lineSearch();
            double avgC = Network.active.getAvgC();
            
            Network network = new Network(name, 1, i);
            
            double sr2 = network.stableRegionMaxServed();
            double avgC2 = network.getAvgC();
            
            fileout.println(i+"\t"+sr+"\t"+avgC+"\t"+sr2+"\t"+avgC2);
        }
        fileout.close();
    }
    
}
