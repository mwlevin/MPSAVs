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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import static mpsavs.Network.dt;

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
        /*
        Scanner filein = new Scanner(new File("data/coacongress/network/nodes_old.txt"));
        PrintStream fileout = new PrintStream(new FileOutputStream(new File("data/coacongress/network/nodes.txt")), true);
        
        fileout.println(filein.nextLine());
        
        int count = 0;
        while(filein.hasNext())
        {
            int id = filein.nextInt();
            int type = filein.nextInt();
            double lng = filein.nextDouble();
            double lat = filein.nextDouble();
            double elev = filein.nextDouble();
            
            if(type == 100 && Math.random() < 0.05)
            {
                count++;
                type = 200;
            }
            
            fileout.println(id+"\t"+type+"\t"+lng+"\t"+lat+"\t"+elev);
        }
        fileout.close();
        
        System.out.println(count);
        
        */
        
        //stableRegionTest("SiouxFalls", 100, 500, 25);
        //System.exit(0);
        
        // TODO code application logic here
        
        /*
        Network network = new Network("SiouxFalls", 2000.0/28835, 100);
        
        double sr2 = network.stableRegionMaxServed();
        
        MetaSimulation test = new MetaSimulation("SiouxFalls", 100);
        
        //System.out.println(test.isStable(200.0/28835));
        
        System.out.println(test.lineSearch(sr2));
*/
        
        
        
        
        Network network = new Network("SiouxFalls", 800.0/28835, 400);

        //network.loadRSPaths(new HashMap<>() );
        //Network network = new Network("coacongress", 480.0/62836, 100);
        
        //network.test();
        
        //Network network = new Network("test", 45, 100);
        
        /*
        network.simulate();
        
        
        System.out.println("dispatch delay: "+network.getAvgDispatchDelay());
        System.out.println("pickup delay: "+network.getAvgPickupDelay());
        System.out.println("bus served: "+((double)network.getBusServed() / Network.T_hr));
        System.out.println("avg IVTT: "+network.getAvgIVTT());
        System.out.println("empty TT: "+network.getEmptyTT());
        System.out.println("charging time: "+network.getChargingTime());
        System.out.println("avg C: "+network.getAvgC());
        
        System.out.println("____________________________");
        
         */
        
        
        System.out.println(network.stableRegionMaxServed());
        
        
        
        
        //network.createRSPaths();
        //network.createSRPath(network.findNode(1), new CNode[]{network.findCustomer(10, 1), network.findCustomer(15, 20)});
        
    
        
        
        //System.out.println("actual empty time: "+test.emptyTT / test.T_hr * test.dt);
        
        //System.out.println(test.total_customers);
    }
    
    public static void stableRegionTest(String name, int min, int max, int inc) throws IOException, IloException
    {
        PrintStream fileout = new PrintStream(new FileOutputStream(new File("sr_"+name+""
                + (Network.EVs? "_EVs":"") + (Network.BUSES? "_BUS":"") + ".txt"), true), true);
        
        fileout.println("Fleet size\tSim stable demand\tSim avgC\tCalc stable demand\tCalc avgC");
        for(int i = min; i <= max; i += inc)
        {
            Network network = new Network(name, 1, i);
            
            double sr2 = network.stableRegionMaxServed();
            double avgC2 = network.getAvgC();
            
            MetaSimulation test = new MetaSimulation(name, i);
            
            
            double sr = test.lineSearch(sr2);
            double avgC = Network.active.getAvgC();
            
            
            
            fileout.println(i+"\t"+sr+"\t"+avgC+"\t"+sr2+"\t"+avgC2);
        }
        fileout.close();
    }
    
}
