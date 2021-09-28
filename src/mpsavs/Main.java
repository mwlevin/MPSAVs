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
        System.out.println("SR: "+sr2);
        MetaSimulation test = new MetaSimulation("SiouxFalls", 100);
        
        //System.out.println(test.isStable(220.0/28835));
        
        System.out.println(test.lineSearch(sr2));
*/
        
        
        
        
        Network network = new Network("SiouxFalls", 150.0/28835, 100);

        //network.test();
        
        //Network network = new Network("test", 45, 100);
        
        
        network.simulate();
        
        
        System.out.println("dispatch delay: "+network.getAvgDispatchDelay());
        System.out.println("pickup delay: "+network.getAvgPickupDelay());
        System.out.println("bus served: "+((double)network.getBusServed() / Network.T_hr));
        System.out.println("avg IVTT: "+network.getAvgIVTT());
        System.out.println("empty TT: "+network.getEmptyTT());
        System.out.println("charging time: "+network.getChargingTime());
        System.out.println("avg C: "+network.getAvgC());
        System.out.println("avg served: "+network.getAvgServed());
        
        System.out.println("____________________________");
        
         
       
        
        System.out.println("served: "+network.stableRegionMaxServed());
        
        
        
        
        //network.createRSPaths();
        //network.createSRPath(network.findNode(1), new CNode[]{network.findCustomer(10, 1), network.findCustomer(15, 20)});
        
    
        
        
        //System.out.println("actual empty time: "+test.emptyTT / test.T_hr * test.dt);
        
        //System.out.println(test.total_customers);
    }
    
    public static void stableRegionTest(String name, int min, int max, int inc) throws IOException, IloException
    {
        double[] demands = new double[]{
            183.0259441,
227.8505261,
287.2128105,
338.4519983,
392.0209461,
429.2815741,
498.4133186,
540.3955935,
584.3037796,
646.0191452,
709.6293585,
744.474802,
819.454244,
895.7538834,
929.9935892,
984.6109291,
1045.130285
        };
    
        /*
        PrintStream fileout = new PrintStream(new FileOutputStream(new File("sr_"+name+""
                + (Network.RIDESHARING? "_RS":"") + (Network.EVs? "_EVs":"") + (Network.BUSES? "_BUS":"") + ".txt"), true), true);
        */
        PrintStream fileout = new PrintStream(new FileOutputStream(new File("sr_check.txt"), true), true);
        fileout.println("Fleet size\tSim stable demand\tSim avgC\tSim IVTT\tSim empty time\tSim charging time\tSim avg served\tCalc stable demand\tCalc avgC\tCalc IVTT\tCalc empty TT\tCalc charging time \tCalc avg served");
        
        
        int idx = 0;
        
        for(int i = min; i <= max; i += inc)
        {
            Network network = new Network(name, 1, i);
            
            double sr2 = network.stableRegionMaxServed();
            double avgC2 = network.getAvgC();
            double ivtt2 = network.getAvgIVTT();
            double emptyTT2 = network.getEmptyTT();
            double charging2 = network.getChargingTime();
            double served2 = network.getAvgServed();
            
            //MetaSimulation test = new MetaSimulation(name, i);
            
            Network test = new Network(name, demands[idx]/28835, i);
            
            double sr = sr2;
            test.simulate();
            double avgC = Network.active.getAvgC();
            double ivtt = Network.active.getAvgIVTT();
            double emptyTT = Network.active.getEmptyTT();
            double charging = Network.active.getChargingTime();
            double served = Network.active.getAvgServed();
            
            idx++;
            
            fileout.println(i+"\t"+sr+"\t"+avgC+"\t"+ivtt+"\t"+emptyTT+"\t"+charging+"\t"+served+"\t"+sr2+"\t"+avgC2+"\t"+ivtt2+"\t"+emptyTT2+"\t"+charging2+"\t"+served2);
        }
        fileout.close();
    }
    
}
