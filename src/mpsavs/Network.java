/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mpsavs;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

/**
 *
 * @author mlevin
 */
public class Network 
{
    public static final boolean PRINT = false;
    
    public int total_customers;
    
    public static final int SAV_CAPACITY = 1;
    
    public static Network active = null;
    
    public static boolean EVs = false;
    public static boolean RIDESHARING = false;
    public static boolean BUSES = true;
    
    public double V = 1;
    
    private RunningAvg avgC, ivtt, emptyTT;
    private List<Node> nodes;
    private Set<Link> links;
    private Set<CNode> cnodes;
    private List<Node> enodes;
    private List<BusRoute> buses;
    
    private Set<SAV> savs;
    
    private TTMatrix tts;
    
    
    public static int t;
    
    public static double dt = 30.0/3600;
    public static int T_hr = 8;
    public static int T = (int)Math.round(1.0/dt * T_hr);
    
    private int[] holtimes;
    
    public static Random rand = new Random(13);
    
    private double total_demand;
    private String name;
    
    private PrintStream cplex_log;
    
    public Network(String name, double scale, int fleet) throws IOException
    {
        active = this;
        this.name = name;
        
        cplex_log = new PrintStream(new FileOutputStream(new File("log.txt")), true);
        
        
        nodes = new ArrayList<>();
        links = new HashSet<>();
        cnodes = new HashSet<>();
        savs =  new HashSet<>();
        enodes = new ArrayList<>();
        buses = new ArrayList<>();
        
        Scanner filein = new Scanner(new File("data/"+name+"/network/nodes.txt"));
        filein.nextLine();
        
        Node.next_idx = 0;
        
        while(filein.hasNextInt())
        {
            int id = filein.nextInt();
            int type = filein.nextInt();
            
            Node temp = new Node(id, type);
            nodes.add(temp);
            
            filein.nextLine();
            
            if(type == 200)
            {
                enodes.add(temp);
            }
        }
        filein.close();
        
        Map<Integer, Node> nodemap = createNodeIdsMap();
        
        filein = new Scanner(new File("data/"+name+"/network/links.txt"));
        filein.nextLine();
        
        while(filein.hasNextInt())
        {
            int id = filein.nextInt();
            int type = filein.nextInt();
            int source_id = filein.nextInt();
            int dest_id = filein.nextInt();
            double length = filein.nextDouble()/5280.0;
            double speed = filein.nextDouble();
            
            filein.nextLine();
            
            links.add(new Link(id, nodemap.get(source_id), nodemap.get(dest_id), 
                    length, (int)Math.ceil(length/speed / dt)));
        }
        filein.close();
        
        tts = new TTMatrix(this);
        
        filein = new Scanner(new File("data/"+name+"/transit/bus_route_link.txt"));
        
        filein.nextLine();
        
        int bus_id = 0;
        BusRoute tempBus = null;
        Map<Integer, Link> linksmap = createLinkIdsMap();
        
        while(filein.hasNextInt())
        {
            int id = filein.nextInt();  
            int seq = filein.nextInt();
            int linkid = filein.nextInt();
            boolean stop = filein.nextInt() == 1;
            filein.nextLine();
            
            if(id != bus_id)
            {
                if(tempBus != null)
                {
                    buses.add(tempBus);
                }
                tempBus = new BusRoute(id, linksmap.get(linkid));
                bus_id = id;
            }
 
            if(stop)
            {
                tempBus.add(linksmap.get(linkid).getEnd());
            }
            
            
        }
        
        if(tempBus != null)
        {
            buses.add(tempBus);
        }
        
        if(PRINT)
        {
            System.out.println(buses.size()+" bus routes");
        }
        
        
        
        
        total_demand = 0;
        
        filein = new Scanner(new File("data/"+name+"/demand/static_od.txt"));
        filein.nextLine();
        
        while(filein.hasNextInt())
        {
            int id = filein.nextInt();
            int type = filein.nextInt();
            int origin_id = filein.nextInt();
            int dest_id = filein.nextInt();
            double demand = filein.nextDouble() * scale; // trips per hour
            
            filein.nextLine();
            
            Node origin = nodemap.get(origin_id);
            Node dest = nodemap.get(dest_id);
            
            if(origin.getType() == 1000)
            {
                origin = origin.getOutgoing().iterator().next().getEnd();
            }
            
            if(dest.getType() == 1000)
            {
                dest = dest.getIncoming().iterator().next().getStart();
            }
            
            // search for duplicates
            CNode existing = null;
            for(CNode c : origin.getCNodes())
            {
                if(c.getDest() == dest)
                {
                    existing = c;
                    break;
                }
            }
            if(existing == null)
            {
                cnodes.add(new CNode(origin, dest, demand));
            }
            else
            {
                existing.addDemand(demand);
            }
            
            total_demand += demand;
        }
        filein.close();
        
        
        
        
        if(PRINT)
        {   
            System.out.println("Total demand: "+total_demand);
        }
        
        List<Node> savnodes = new ArrayList<Node>();
        
        for(Node n : nodes)
        {
            if(n.getType() != 1000)
            {
                savnodes.add(n);
            }
        }
        
        int idx = 0;
        
        for(int f = 0; f < fleet; f++)
        {
            SAV temp;
            
            if(EVs)
            {
                temp = new SAEV(savnodes.get(idx));
            }
            else
            {
                temp = new SAV(savnodes.get(idx));
            }
            
            savs.add(temp);
            
            idx++;
            
            if(idx >= savnodes.size())
            {
                idx = 0;
            }
        }
        
        Set<Node> remove = new HashSet<>();
        Set<Link> removeL = new HashSet<>();
        
        for(Node n : nodes)
        {
            if(n.getType() == 1000)
            {
                remove.add(n);
            }
        }
        
        for(Link l : links)
        {
            if(l.getStart().getType() == 1000 || l.getEnd().getType() == 1000)
            {
                removeL.add(l);
            }
        }
        
        for(Node n : remove)
        {
            nodes.remove(n);
        }
        
        for(Link l : removeL)
        {
            links.remove(l);
        }
        
        
    }
    
    public boolean isBusServed(CNode c)
    {
        for(BusRoute b : buses)
        {
            if(b.isServed(c))
            {
                return true;
            }
        }
        
        return false;
    }
    
    public Node getBusDropoff(CNode c)
    {
        Node best = c.getDest();
        int min = getTT(c.getOrigin(), c.getDest());
        
        for(BusRoute b : buses)
        {
            Node u = b.closestStop(c);
            
            if(u == null)
            {
                continue;
            }
            
            int temp = getTT(c.getOrigin(), u);
            if(temp < min)
            {
                best = u;
                min = temp;
            }
        }
        
        return best;
    }
    
    public int getTT(Node r, Node s)
    {
        return tts.getTT(r, s);
    }
    
    public double getLength(Node r, Node s)
    {
        return tts.getLength(r, s);
    }
    
    public void simulate() throws IOException
    {
        holtimes = new int[T];
        
        avgC = new RunningAvg();
        ivtt = new RunningAvg();
        emptyTT = new RunningAvg();
        
        total_customers = 0;
        
        PrintStream output = new PrintStream(new FileOutputStream(new File("waiting.txt")), true);
        output.println("time\tnum waiting\tHOL time");
        
        for(t = 0; t < T; t++)
        {
            if(PRINT)
            {
                System.out.println(t);
            }
            
            int holtime = getHOLTime();
            holtimes[t] = holtime;
            
            output.println(t+"\t"+getNumWaiting()+"\t"+holtime);
            
            for(CNode n : cnodes)
            {
                n.step();

            }
            
            try
            {
                mdpp();
            }
            catch(IloException ex)
            {
                ex.printStackTrace(System.err);
                System.exit(0);
            }
            
            
            for(SAV v : savs)
            {
                v.step();
            }
        }
        
        System.out.println("Total customers: "+total_customers
                +" "+((double)total_customers/T_hr)+" "+total_demand);
        System.out.println("Avg service time: "+avgC);
        
        output.close();
    }
    
    public int[] getHOLTimes()
    {
        return holtimes;
    }
    
    
    public List<Node> getENodes()
    {
        return enodes;
    }
    
    
    public double stableRegionMaxServed() throws IloException, IOException
    {
        if(RIDESHARING)
        {
            return stableRegionMaxServedRS();
        }
        else if(EVs)
        {
            return stableRegionMaxServedEV();
        }
        else
        {
            return stableRegionMaxServed1();
        }
    }
    
    private String printPath(Path path)
    {
        Node q = path.get(0);
        
        String output = ""+q;
        
        for(CNode c : path.getServed())
        {
            output += "\t"+c.getOrigin()+"\t"+c.getDest();
        }
        
        for(Node n : path)
        {
            output += "\t"+n;
        }
        
        return output;
    }
    
    public void loadRSPaths(Map<Node, Map<Path, IloNumVar>> gamma) throws IOException
    {
        Map<Integer, Node> nodesmap = createNodeIdsMap();
        
        for(int i = 1; i <= SAV_CAPACITY; i++)
        {
            Scanner filein = new Scanner(new File("data/"+name+"/rs_paths_"+i+".txt"));
            
            while(filein.hasNextLine())
            {
                Scanner chopper = new Scanner(filein.nextLine());
                
                
                Node q = nodesmap.get(chopper.nextInt());
                
                Path temp = new Path();
                
                for(int j = 0; j < i; j++)
                {
                    temp.add(nodesmap.get(chopper.nextInt()).getCNode(nodesmap.get(chopper.nextInt())));
                }
                
                while(chopper.hasNextInt())
                {
                    temp.add(nodesmap.get(chopper.nextInt()));
                }
                
                if(!gamma.containsKey(q))
                {
                    gamma.put(q, new HashMap<>());
                }
                
                gamma.get(q).put(temp, null);
            }
        }
    }
    
    public void createRSPaths() throws IloException, IOException
    {
        
        int count = 0;
        
        PrintStream fileout = new PrintStream(new FileOutputStream("data/"+name+"/rs_paths_1.txt"), true);
        
        for(Node q : nodes)
        {
            
            for(CNode c : cnodes)
            {
                Path path = createSRPath(q, new CNode[]{c});
                fileout.println(printPath(path));
                
                count++;
            }
        }
            
        fileout.close();

        if(SAV_CAPACITY >= 2)
        {
            fileout = new PrintStream(new FileOutputStream("data/"+name+"/rs_paths_2.txt"), true);
            
            for(Node q : nodes)
            {
                for(CNode c1 : cnodes)
                {
                    for(CNode c2 : cnodes)
                    {
                        if(c1 != c2)
                        {
                            
                            Path path = createSRPath(q, new CNode[]{c1, c2});
                            
                            fileout.println(printPath(path));
                            count++;
                        }
                    }
                }
            }
            
            fileout.close();
        }

        
            
        if(SAV_CAPACITY >= 3)
        {
            fileout = new PrintStream(new FileOutputStream("data/"+name+"/rs_paths_3.txt"), true);
            
            for(Node q : nodes)
            {
                for(CNode c1 : cnodes)
                {
                    for(CNode c2 : cnodes)
                    {
                        for(CNode c3 : cnodes)
                        {
                            if(c1 != c2 && c2 != c3 && c1 != c3)
                            {
                                Path path = createSRPath(q, new CNode[]{c1, c2, c3});
                                fileout.println(printPath(path));
                                count++;
                            }
                        }
                    }
                }
            }
            
            fileout.close();
        }
    
        
        System.out.println(count+" paths");
    }
    
    public double stableRegionMaxServedRS() throws IloException, IOException
    {
        if(cplex == null)
        {
            cplex = new IloCplex();
            cplex.setOut(cplex_log);
        }
        else
        {
            cplex.clearModel();
        }
        
        Map<Node, Map<Path, IloNumVar>> gamma = new HashMap<>();
        
        loadRSPaths(gamma);
        
        for(Node q : gamma.keySet())
        {
            for(Path pi : gamma.get(q).keySet())
            {
                gamma.get(q).put(pi, cplex.numVar(0, Integer.MAX_VALUE));
            }
        }
        
        IloNumVar alpha = cplex.numVar(0, Integer.MAX_VALUE);
        
        // (40)
        
        IloLinearNumExpr lhs = cplex.linearNumExpr();
        
        
        for(Node q : gamma.keySet())
        {
            for(Path pi : gamma.get(q).keySet())
            {
                lhs.addTerm(gamma.get(q).get(pi), pi.getTT());
            }
        }
        
        cplex.addLe(lhs, savs.size());
        
        
        for(Node r : gamma.keySet())
        {
            lhs = cplex.linearNumExpr();
            IloLinearNumExpr rhs = cplex.linearNumExpr();
            
            for(Path pi : gamma.get(r).keySet())
            {
                lhs.addTerm(gamma.get(r).get(pi), 1);
            }
            
            for(Node q : gamma.keySet())
            {
                for(Path pi : gamma.get(q).keySet())
                {
                    if(pi.getDest() == r)
                    {
                        rhs.addTerm(gamma.get(q).get(pi), 1);
                    }
                }
            }
            
            cplex.addEq(lhs, rhs);
        }
        
        // (41)
        
        // (10)
        
        for(CNode c : cnodes)
        {
            lhs = cplex.linearNumExpr();
            
            for(Node q : gamma.keySet())
            {
                for(Path pi : gamma.get(q).keySet())
                {
                    if(pi.isServed(c))
                    {
                        lhs.addTerm(1, gamma.get(q).get(pi));
                    }
                }
            }
            
            cplex.addGe(lhs, cplex.prod(alpha, c.getLambda()));
        }
        
        cplex.addMaximize(alpha);
        
        
        
        double output = 0;

        
        for(CNode n : cnodes)
        {
            output += n.getLambda();
        }
        
        output *= cplex.getValue(alpha);
        
        
        
        return output;
    }
    
    public Path createSRPath(Node q, CNode[] customers) throws IloException
    {
        //System.out.println("Creating path "+q+"- "+Arrays.toString(customers));
        
        if(customers.length == 1)
        {
            Path output = new Path();
            output.add(q);
            output.add(customers[0].getOrigin());
            output.add(customers[0].getDest());
            
            output.add(customers[0]);
            
            return output;
        }
        if(cplex == null)
        {
            cplex = new IloCplex();
            cplex.setOut(cplex_log);
        }
        else
        {
            cplex.clearModel();
        }
        
        Node dummy = new Node(0, 0);
        
        Node[] nodes = new Node[1 + customers.length*2 + 1];
        nodes[0] = q;
        nodes[nodes.length-1] = dummy;
        
        for(int i = 0; i < customers.length; i++)
        {
            nodes[1 + i*2] = customers[i].getOrigin();
            nodes[1 + i*2 + 1] = customers[i].getDest();
        }
        
        IloIntVar[] sigma = new IloIntVar[nodes.length];
        
        for(int i = 0; i < nodes.length; i++)
        {
            sigma[i] = cplex.intVar(0, nodes.length);
        }
        
        IloIntVar[][] f = new IloIntVar[nodes.length][nodes.length];
        
        for(int i = 0; i < f.length-1; i++)
        {
            for(int j = 1; j < f.length; j++)
            {
                // ignore links from q to customer destination
                if(i == 0 && j % 2 == 0)
                {
                    continue;
                }
                
                if(i == 0 && j == f.length-1)
                {
                    continue;
                }
                
                if(j == f.length-1 && i%2 == 1)
                {
                    continue;
                }
                
                if(i != j)
                {
                    f[i][j] = cplex.intVar(0, 1);
                }
            }
        }
        
        // (39e)
        
        for(int i = 0; i < customers.length; i++)
        {
            cplex.addLe(sigma[1 + 2*i], cplex.sum(1, sigma[1 + 2*i + 1]));
        }
        
        // (39f)
        
        int M = nodes.length;
        
        for(int i = 0; i < f.length; i++)
        {
            for(int j = 1; j < f.length; j++)
            {
                if(f[i][j] == null)
                {
                    continue;
                }
                
                cplex.addLe(sigma[i], cplex.sum(cplex.sum(1, sigma[j]), cplex.prod(M, f[i][j]) ));
            }
        }
        
        cplex.addEq(sigma[0], 0);
        
        // (39b)
        
        IloLinearNumExpr lhs = cplex.linearNumExpr();
        
        for(int c = 0; c < customers.length; c++)
        {
            lhs.addTerm(1, f[0][1 + c*2]);

        }
        
        cplex.addEq(lhs, 1);
        
        lhs = cplex.linearNumExpr();
        
        for(int c = 0; c < customers.length; c++)
        {
            lhs.addTerm(1, f[1 + c*2+1][nodes.length-1]);

        }
        
        cplex.addEq(lhs, 1);
        
        // (39c)
        
        
        
        for(int c = 0; c < customers.length; c++)
        {
            lhs = cplex.linearNumExpr();
            
            for(int i = 0; i < nodes.length-1; i++)
            {
                if(c*2+1 == i)
                {
                    continue;
                }
                
                lhs.addTerm(1, f[i][c*2+1]);
            }
            cplex.addEq(lhs, 1);
            
            
            lhs = cplex.linearNumExpr();
            
            for(int i = 1; i < nodes.length-1; i++)
            {
                if(c*2+1 == i)
                {
                    continue;
                }
                
                lhs.addTerm(1, f[c*2+1][i]);
            }
            
            cplex.addEq(lhs, 1);
            
            lhs = cplex.linearNumExpr();
            
            for(int i = 1; i < nodes.length-1; i++)
            {
                if(1+c*2+1 == i)
                {
                    continue;
                }
                
                lhs.addTerm(1, f[i][1+c*2+1]);
            }
            
            cplex.addEq(lhs, 1);
            
            lhs = cplex.linearNumExpr();
            
            for(int i = 1; i < nodes.length; i++)
            {
                if(1+c*2+1 == i)
                {
                    continue;
                }
                
                lhs.addTerm(1, f[1+c*2+1][i]);
            }
            
            cplex.addEq(lhs, 1);

        }
        
        // (39a)
        lhs = cplex.linearNumExpr();
        for(int i = 0; i < nodes.length; i++)
        {
            for(int j = 1; j < nodes.length-1; j++)
            {
                if(f[i][j] != null)
                {               
                    lhs.addTerm(f[i][j], getTT(nodes[i], nodes[j]));
                }
            }
        }
        
        cplex.addMinimize(lhs);
        
        cplex.solve();
        
        //System.out.println(cplex.getObjValue());
        
        /*
        for(int j = 0; j < nodes.length; j++)
        {
            System.out.print("\t"+nodes[j]);
        }
        System.out.println();
        
        for(int i = 0; i < nodes.length; i++)
        {
            System.out.print(nodes[i]+"\t");
            
            for(int j = 0; j < nodes.length; j++)
            {
                if(f[i][j] != null)
                {
                    System.out.print((int)cplex.getValue(f[i][j])+"\t");
                }
                else
                {
                    System.out.print("\t");
                }
            }
            
            System.out.println();
        }
        */

        Path output = new Path();
        
        int curr = 0;
        
        while(curr != nodes.length-1)
        {
            inner: for(int j = 0; j < nodes.length; j++)
            {
                if(f[curr][j] != null && cplex.getValue(f[curr][j]) == 1)
                {
                    output.add(nodes[curr]);
                    curr = j;
                    break inner;
                }
            }
        }
        
        for(CNode c : customers)
        {
            output.add(c);
        }
        //System.out.println(output);
        
        return output;
        
        
    }
    
    public void test() throws Exception
    {
        Node q = nodes.get(0);
        
        CNode c1 = null;
        CNode c2 = null;
        
        Iterator<CNode> iter = cnodes.iterator();
        
        int count = 0;
        
        while(iter.hasNext())
        {
            count++;
            CNode temp = iter.next();
            
            if(temp.getOrigin().getId() == 3 && temp.getDest().getId() == 22)
            {
                c1 = temp;
            }
            if(temp.getOrigin().getId() == 4 && temp.getDest().getId() == 20)
            {
                c2 = temp;
            }
        }
        
        
        System.out.println(q+" "+c1+" "+c2);
        createSRPath(q, new CNode[]{c1, c2});
    }
    
    public double stableRegionMaxServed1() throws IloException
    {
        if(cplex == null)
        {
            cplex = new IloCplex();
            cplex.setOut(cplex_log);
        }
        else
        {
            cplex.clearModel();
        }
        
        IloNumVar alpha = cplex.numVar(0, 1000);
        
        int count = 0;
        
        IloNumVar[][][] v = new IloNumVar[nodes.size()][nodes.size()][nodes.size()];
        
        
        
        RunningAvg ivtt = new RunningAvg();
        
        for(CNode c : cnodes)
        {
            if(BUSES)
            {
                if(c.isBusServed())
                {
                    continue;
                }
                ivtt.add(getTT(c.getOrigin(), c.getBusDropoff()) / (1.0/dt/60) , c.getLambda() );
            }
            else
            {
                ivtt.add(getTT(c.getOrigin(), c.getDest()) / (1.0/dt/60) , c.getLambda() );
            }
        }
        
        System.out.println("Predicted ivtt: "+ivtt.getAverage());
        
        for(int r_idx = 0; r_idx < nodes.size(); r_idx++)
        {
            for(int s_idx = 0; s_idx < nodes.size(); s_idx++)
            {
                Node r = nodes.get(r_idx);
                Node s = nodes.get(s_idx);
                
                if(r == s)
                {
                    continue;
                }

                boolean used = false;
                for(CNode c : r.getCNodes())
                {
                    if( BUSES && c.isBusServed())
                    {
                        continue;
                    }
                    
                    if(BUSES)
                    {
                        if(c.getBusDropoff() == s)
                        {
                            used = true;
                            break;
                        }
                    }
                    else
                    {
                        if(c.getDest() == s)
                        {
                            used = true;
                            break;

                        }
                    }
                }
                    
                if(used)
                {
                    for(int q_idx = 0; q_idx < nodes.size(); q_idx++)
                    {
                        count++;
                        v[q_idx][r_idx][s_idx] = cplex.numVar(0, Integer.MAX_VALUE);
                    }
                }
            }
        }
        
        System.out.println(count+" variables");
        
        double[][] demandMat = new double[nodes.size()][nodes.size()];
        
        for(CNode c : cnodes)
        {
            Node dest = null;

            if(BUSES)
            {
                if(c.isBusServed())
                {
                    continue;
                }
                else
                {
                    dest = c.getBusDropoff();
                }

            }
            else
            {
                dest = c.getDest();
            }


            demandMat[c.getOrigin().getIdx()][dest.getIdx()] += c.getLambda();
        }
        
        // demand constraint
        for(Node r : nodes)
        {
            for(Node s : nodes)
            {
                if(r == s || demandMat[r.getIdx()][s.getIdx()] == 0)
                {
                    continue;
                }
                
                IloLinearNumExpr lhs = cplex.linearNumExpr();


                for(int q = 0; q < v.length; q++)
                {
                    lhs.addTerm(1, v[q][r.getIdx()][s.getIdx()]);
                }

                cplex.addGe(lhs, cplex.prod(demandMat[r.getIdx()][s.getIdx()], alpha));
              
            }
        }
        
        
        // travel time constraint
        IloLinearNumExpr lhs = cplex.linearNumExpr();
        for(int q_idx = 0; q_idx < nodes.size(); q_idx++)
        {
            for(int r_idx = 0; r_idx < nodes.size(); r_idx++)
            {
                for(int s_idx = 0; s_idx < nodes.size(); s_idx++)
                {
                    if(v[q_idx][r_idx][s_idx] != null)
                    {
                        Node q = nodes.get(q_idx);
                        Node r = nodes.get(r_idx);
                        Node s = nodes.get(s_idx);
                        
                        lhs.addTerm(v[q_idx][r_idx][s_idx], (getTT(q, r)+getTT(r, s))*dt);
                        
                        //System.out.println((getTT(q, r)+getTT(r, s))*dt);
                    }
                }
            }
        }
        
        cplex.addLe(lhs, savs.size());
        
        for(int s = 0; s < nodes.size(); s++)
        {
            lhs = cplex.linearNumExpr();
            IloLinearNumExpr rhs = cplex.linearNumExpr();
            
            for(int q = 0; q < nodes.size(); q++)
            {
                for(int r = 0; r < nodes.size(); r++)
                {
                    if(v[q][r][s] != null)
                    {
                        lhs.addTerm(1, v[q][r][s]);
                    }
                    
                    if(v[s][r][q] != null)
                    {
                        rhs.addTerm(1, v[s][r][q]);
                    }
                }
            }
            
            cplex.addEq(lhs, rhs);
        }
        
        
        
        
        
        cplex.addMaximize(alpha);
        
        cplex.solve();
        
        double alpha_ = cplex.getValue(alpha);
        
        double emptyTime = 0;
        double avgC = 0;
        double totalV = 0;
        
        for(int q_idx = 0; q_idx < nodes.size(); q_idx++)
        {
            for(int r_idx = 0; r_idx < nodes.size(); r_idx++)
            {
                for(int s_idx = 0; s_idx < nodes.size(); s_idx++)
                {
                    if(v[q_idx][r_idx][s_idx] != null)
                    {
                        Node q = nodes.get(q_idx);
                        Node r = nodes.get(r_idx);
                        Node s = nodes.get(s_idx);
                        
                        if(cplex.getValue(v[q_idx][r_idx][s_idx]) > 0)
                        {
                            /*
                            System.out.println(q+" "+r+" "+s+" "+cplex.getValue(v[q_idx][r_idx][s_idx])+
                                    " "+getTT(q, r)+" "+getTT(r, s));
                            */
                        }
                        
                        emptyTime += cplex.getValue(v[q_idx][r_idx][s_idx]) * getTT(q, r) / (1.0/dt / 60);
                        
                        avgC += cplex.getValue(v[q_idx][r_idx][s_idx]) * (getTT(q, r) + getTT(r, s)) / (1.0/dt / 60);
                        totalV += cplex.getValue(v[q_idx][r_idx][s_idx]);
                    }
                }
            }
        }
        
        
        System.out.println("predicted empty time: "+emptyTime  / totalV);
        System.out.println("avgC: "+(avgC/totalV));
        
        

        this.avgC = new RunningAvg();
        this.avgC.add(avgC/totalV);
        
        double output = 0;
        double bus_only = 0;
        double sav_cust = 0;
        
        for(CNode n : cnodes)
        {
            output += n.getLambda() * alpha_;
            
            if(n.isBusServed())
            {
                bus_only += n.getLambda() * alpha_;
            }
            else
            {
                sav_cust += n.getLambda() * alpha_;
            }
        }
        
        System.out.println("bus served: "+bus_only + " "+(100.0*bus_only/output)+"%");
        System.out.println("sav customers: "+sav_cust+" "+(100.0*sav_cust/output)+"%");
        
        
        
        return output;
    }
  
    public double stableRegionMaxServedEV() throws IloException
    {
        if(cplex == null)
        {
            cplex = new IloCplex();
            cplex.setOut(cplex_log);
        }
        else
        {
            cplex.clearModel();
        }
        
        IloNumVar alpha = cplex.numVar(0, 1000);
        
        int b_db = 2;
        
        IloNumVar[][][][] v = new IloNumVar[nodes.size()][nodes.size()][nodes.size()][b_db];
        
        
            
        for(int r_idx = 0; r_idx < nodes.size(); r_idx++)
        {
            for(int s_idx = 0; s_idx < nodes.size(); s_idx++)
            {
                Node r = nodes.get(r_idx);
                Node s = nodes.get(s_idx);

                boolean used = false;
                for(CNode c : r.getCNodes())
                {
                    if(c.getDest() == s)
                    {
                        used = true;
                        break;
                    }
                }
                    
                if(used)
                {
                    for(int q_idx = 0; q_idx < nodes.size(); q_idx++)
                    {
                        for(int b = 0; b < b_db; b++)
                        {
                            v[q_idx][r_idx][s_idx][b] = cplex.numVar(0, Integer.MAX_VALUE);
                        }
                    }
                }
            }
        }
        
        // demand constraint
        for(CNode c : cnodes)
        {
            IloLinearNumExpr lhs = cplex.linearNumExpr();
            
            for(int q = 0; q < v.length; q++)
            {
                for(int b = 0; b < b_db; b++)
                {
                    //if(v[q][c.getOrigin().getIdx()][c.getDest().getIdx()][b] != null)
                    {
                        lhs.addTerm(1, v[q][c.getOrigin().getIdx()][c.getDest().getIdx()][b]);
                    }
                }
            }
            
            cplex.addGe(lhs, cplex.prod(c.getLambda(), alpha));
        }
        
        // travel time constraint
        IloLinearNumExpr lhs = cplex.linearNumExpr();
        for(int q_idx = 0; q_idx < nodes.size(); q_idx++)
        {
            for(int r_idx = 0; r_idx < nodes.size(); r_idx++)
            {
                for(int s_idx = 0; s_idx < nodes.size(); s_idx++)
                {

                    for(int b = 0; b < b_db; b++)
                    {
                        if(v[q_idx][r_idx][s_idx][b] != null)
                        {
                            Node q = nodes.get(q_idx);
                            Node r = nodes.get(r_idx);
                            Node s = nodes.get(s_idx);
                            double tt = 0;
                            
                            tt += getTT(r, s)*dt;
                            
                            double consumed = getLength(q, r) + getLength(r, s);
                            
                            double required = getLength(s, SAEV.getNearestCharger(s));
                            
                            double start_battery = (double)(b+1)/b_db * SAEV.max_battery;
                            
                            if(start_battery - consumed < required)
                            {
                                Node closestCharger = SAEV.getBestCharger(q, r);
                                
                                double projected = start_battery - getLength(q, closestCharger);
                                double rechargeTime = (SAEV.max_battery - projected)/SAEV.charge_rate;
                                
                                
                                tt += rechargeTime * dt;
                                
                                
                                
                                tt += getTT(q, closestCharger) * dt;
                                tt += getTT(closestCharger, r) * dt;
                                
                                //tt += getTT(q, r)*dt;
                            }
                            else
                            {
                                 tt += getTT(q, r) * dt;
                            }
                            
                            // (getTT(q, r)+getTT(r, s))*dt
                            
                            lhs.addTerm(v[q_idx][r_idx][s_idx][b], tt);
                        }
                        
                        //System.out.println((getTT(q, r)+getTT(r, s))*dt);
                    }
                }
            }
        }
        
        cplex.addLe(lhs, savs.size());
        
        for(int s_idx = 0; s_idx < nodes.size(); s_idx++)
        {
            for(int b = 0; b < b_db; b++) // b is the starting level of RHS and end level of LHS
            {
                lhs = cplex.linearNumExpr();
                IloLinearNumExpr rhs = cplex.linearNumExpr();

                for(int q_idx = 0; q_idx < nodes.size(); q_idx++)
                {
                    for(int r_idx = 0; r_idx < nodes.size(); r_idx++)
                    {
                    
                        
                        Node q = nodes.get(q_idx);
                        Node r = nodes.get(r_idx);
                        Node s = nodes.get(s_idx);
                        //lhs.addTerm(1, v[q][r][s]);

                        for(int bp = 0; bp < b_db; bp++)
                        {
                            if(v[q_idx][r_idx][s_idx][bp] != null)
                            {
                                // check if v_qrs at bp ends with b battery
                                double end_battery = 0;
                                double start_battery = (double)(bp+1)/b_db * SAEV.max_battery;
                                
                                double consumed = getLength(q, r) + getLength(r, s);
                            
                                double required = getLength(s, SAEV.getNearestCharger(s));
                                boolean recharge = false;
                                
                                if(start_battery - consumed < required)
                                {
                                    Node closestCharger = SAEV.getBestCharger(q, r);
                                    recharge = true;

                                    end_battery = SAEV.max_battery - getLength(closestCharger, r) - getLength(r, s);
                                }
                                else
                                {
                                    end_battery = start_battery - consumed;
                                }
                                
                                int end_b = (int)Math.max(0, Math.round(end_battery/SAEV.max_battery * b_db)-1);
                                
                                /*
                                System.out.println(q+" "+r+" "+s+" : "+bp+" - "+end_b+" "+recharge+" "+
                                    start_battery+" "+end_battery);
                                */
                                
                                if(end_b == b)
                                {
                                    lhs.addTerm(1.0, v[q_idx][r_idx][s_idx][bp]);
                                }
                            }
                        }

                        if(v[s_idx][r_idx][q_idx][b] != null)
                        {
                            rhs.addTerm(1, v[s_idx][r_idx][q_idx][b]);
                        }
                    }
                }
                
                cplex.addEq(lhs, rhs);
            }
            
            
        }
        
        
        
        
        
        cplex.addMaximize(alpha);
        
        cplex.solve();
        
        double alpha_ = cplex.getValue(alpha);
        
        double emptyTime = 0;
        double avgC = 0;
        double total_v = 0;
        
        for(int q_idx = 0; q_idx < nodes.size(); q_idx++)
        {
            for(int r_idx = 0; r_idx < nodes.size(); r_idx++)
            {
                for(int s_idx = 0; s_idx < nodes.size(); s_idx++)
                {
                    
                    Node q = nodes.get(q_idx);
                    Node r = nodes.get(r_idx);
                    Node s = nodes.get(s_idx);
                        
                    for(int b = 0; b < b_db; b++)
                    {
                        if(v[q_idx][r_idx][s_idx][b] != null)
                        {
                            double consumed = getLength(q, r) + getLength(r, s);
                            
                            double required = getLength(s, SAEV.getNearestCharger(s));
                            
                            int tt = 0;
                            
                            if((double)(b+1)/b_db * SAEV.max_battery - consumed < required)
                            {
                                Node closestCharger = SAEV.getBestCharger(q, r);
                                
                                double projected = (double)(b+1)/b_db * SAEV.max_battery - getLength(q, closestCharger);
                                
                                //System.out.println(((double)(b+1)/b_db * SAEV.max_battery)+" "+getLength(q, closestCharger));
                                int rechargeTime = (int)Math.ceil( (SAEV.max_battery - projected)/SAEV.charge_rate / dt);
                                
                                
                                /*
                                tt += rechargeTime;
                                
                                tt += getTT(q, closestCharger);
                                tt += getTT(closestCharger, r);
                                */
                                tt += getTT(q, r);
                                
                                //System.out.println("recharge "+rechargeTime+" "+projected+ " "+getTT(q, closestCharger)+" "+getTT(closestCharger, r)+" "+getTT(q, r));
                            }
                            else
                            {
                                 tt += getTT(q, r);
                            }
                            //System.out.println(tt+" "+getTT(q, r));
                            
                            
                            emptyTime += cplex.getValue(v[q_idx][r_idx][s_idx][b]) * tt / (1.0/dt / 60);
                            avgC += cplex.getValue(v[q_idx][r_idx][s_idx][b]) * (tt + getTT(r, s)) / (1.0/dt / 60);
                            total_v += cplex.getValue(v[q_idx][r_idx][s_idx][b]);
                            
                            //System.out.println(q+" "+r+" "+s+" : "+b+" - "+ cplex.getValue(v[q_idx][r_idx][s_idx][b]));
                        }
                        
                    }
                }
            }
        }
        
        System.out.println("predicted empty time: "+emptyTime / total_v);
        System.out.println("avg C: "+avgC / total_v);
        
        this.avgC = new RunningAvg();
        this.avgC.add(avgC / total_v);
        
        double output = 0;
        
        for(CNode n : cnodes)
        {
            output += n.getLambda() * alpha_;
        }
        
        
        
        return output;
    }
    
    private IloCplex cplex;
    public void mdpp() throws IloException
    {
        if(cplex == null)
        {
            cplex = new IloCplex();
        
            cplex.setOut(cplex_log);
        }
        else
        {
            cplex.clearModel();
        }
        
        List<CNode> nc = getWaiting();
        List<SAV> nv = getAvailable();
        
        if(nc.size() == 0 || nv.size() == 0)
        {
            return;
        }
        
        List<Path> paths = new ArrayList<>();
        
        for(CNode c : nc)
        {
            if(BUSES)
            {
                if(c.isBusServed())
                {
                    c.busPickup();
                    //System.out.println(c+" served by bus");
                }
                else
                {
                    Path temp = new Path();
                    temp.add(c, c.getOrigin(), c.getBusDropoff());
                    paths.add(temp);
                    //System.out.println(c+" bus dropoff at "+getBusDropoff(c));
                }
            }
            else
            {
                paths.add(new Path(c));
            }
                
        }
        
        IloIntVar[][] mat = new IloIntVar[paths.size()][nv.size()];
        
        for(int pi = 0; pi < mat.length; pi++)
        {
            for(int v = 0; v < mat[pi].length; v++)
            {
                mat[pi][v] = cplex.intVar(0, 1);
            }
        }
        
        // each vehicle assigned at most one path
        for(int v = 0; v < nv.size(); v++)
        {
            IloLinearNumExpr lhs = cplex.linearNumExpr();
            
            for(int pi = 0; pi < paths.size(); pi++)
            {
                lhs.addTerm(1, mat[pi][v]);
            }
            
            cplex.addLe(lhs, 1);
        }
        
        // each customer assigned at most one vehicle
        
        for(CNode c : nc)
        {
            IloLinearNumExpr lhs = cplex.linearNumExpr();
            
            for(int pi = 0; pi < paths.size(); pi++)
            {
                if(paths.get(pi).isServed(c))
                {
                    for(int v = 0; v < nv.size(); v++)
                    {
                        lhs.addTerm(1, mat[pi][v]);
                    }
                }
            }
            
            cplex.addLe(lhs, 1);
        }
        
        // objective
        IloLinearNumExpr obj = cplex.linearNumExpr();
        
        boolean hasObj = false;
        
        for(int pi = 0; pi < mat.length; pi++)
        {
            double total_h = 0;
            
            Path path = paths.get(pi);
            
            for(CNode c : cnodes)
            {
                if(path.isServed(c))
                {
                    total_h += c.getHOLTime();
                }
            }
            
            for(int v = 0; v < mat[pi].length; v++)
            {
                double VD = V * (path.getTT()+nv.get(v).getDelay(path));
                
                //System.out.println("delay : "+nv.get(v).getDelay(path)+" "+path.getTT());
                
                if(VD > total_h)
                {
                    cplex.addEq(mat[pi][v], 0);
                }
                else
                {
                    obj.addTerm(VD-total_h, mat[pi][v]);
                    
                    hasObj = true;
                }
                
                
            }
        }
        
        
        
        cplex.addMinimize(obj);
        
        if(hasObj)
        {
            cplex.setParam(IloCplex.Param.TimeLimit, 60*5);
            cplex.solve();
            
            if(Network.PRINT)
            {
                System.out.println("\tSolved cplex");
            }

            for(int pi = 0; pi < mat.length; pi++)
            {
                for(int v = 0; v < mat[pi].length; v++)
                {
                    if(cplex.getValue(mat[pi][v]) == 1)
                    {
                        dispatchSAV(nv.get(v), paths.get(pi));
                    }
                }
            }
            
        }
        
        //cplex.end();
        cplex_log.flush();
    }
    
    public int getHOLTime()
    {
        int output = 0;
        
        for(CNode n : cnodes)
        {
            output += n.getHOLTime();
        }
        
        return output;
    }
    
    public double getAvgIVTT()
    {
        return ivtt.getAverage();
    }
    
    public int getBusServed()
    {
        int output = 0;
        for(CNode n : cnodes)
        {
            if(n.isBusServed())
            {
                output += n.getPickupDelay().getCount();
            }
        }
        return output;
    }
    public int getNumWaiting()
    {
        int output = 0;
        
        for(CNode n : cnodes)
        {
            output += n.getNumWaiting();
        }
        
        return output;
    }
    public List<CNode> getWaiting()
    {
        List<CNode> output = new ArrayList<>();
        
        for(CNode n : cnodes)
        {
            if(n.getNumWaiting() > 0)
            {
                output.add(n);
            }
        }
        
        return output;
    }
    
    public List<SAV> getAvailable()
    {
        List<SAV> output = new ArrayList<>();
        
        for(SAV v : savs)
        {
            if(v.isParked())
            {
                output.add(v);
            }
        }
        
        return output;
    }
    
    public double getAvgDispatchDelay()
    {
        RunningAvg output = new RunningAvg();
        
        for(CNode node : cnodes)
        {
            output.add(node.getDispatchDelay());
        }
        
        return output.getAverage();
    }
    
    public double getAvgC()
    {
        return avgC.getAverage();
    }
    
    public double getAvgPickupDelay()
    {
        RunningAvg output = new RunningAvg();
        
        for(CNode node : cnodes)
        {
            output.add(node.getPickupDelay());
        }
        
        return output.getAverage();
    }
    
    public void dispatchSAV(SAV sav, Path path)
    {
        
        if(!sav.isParked())
        {
            throw new RuntimeException("Dispatching moving SAV");
        }
        
        if(PRINT)
        {
            System.out.println("\tDispatch SAV "+sav.getId()+" path "+sav.getLocation()+"-"+path
                +" :"+sav.getDelay(path)+" "+path.getTT());
        }
        
        avgC.add( (sav.getDelay(path)+path.getTT()) / (1.0/dt / 60));
        emptyTT.add(sav.getDelay(path) / (1.0/dt / 60));
        ivtt.add(path.getTT() / (1.0/dt / 60) );
        
        sav.dispatch(path);
        
        for(CNode c : path.getServed())
        {
            if(c.getNumWaiting() > 0)
            {
                c.pickup(sav, path);
            }
        }
    }
    public List<Node> getNodes()
    {
        return nodes;
    }
    
    public Set<Link> getLinks()
    {
        return links;
    }
    
    public double getTotalDemand()
    {
        return total_demand;
    }
    
    public double getEmptyTT()
    {
        return emptyTT.getAverage();
    }
    
    public Map<Integer, Link> createLinkIdsMap()
    {
        Map<Integer, Link> output = new HashMap<>();
        
        for(Link l : links)
        {
            output.put(l.getId(), l);
        }
        
        return output;
    }
    
    
    public Map<Integer, Node> createNodeIdsMap()
    {
        Map<Integer, Node> output = new HashMap<>();
        
        for(Node n : nodes)
        {
            output.put(n.getId(), n);
        }
        
        return output;
    }
    
    public void dijkstras(Node source)
    {
        for(Node n : nodes)
        {
            n.cost = Integer.MAX_VALUE;
            n.length = Integer.MAX_VALUE;
            n.prev = null;
        }
        
        source.cost = 0;
        source.length = 0;
        
        Set<Node> Q = new HashSet<Node>();
        Q.add(source);
        
        while(!Q.isEmpty())
        {
            Node u = null;
            int best = Integer.MAX_VALUE;
            
            for(Node n : Q)
            {
                if(n.cost < best)
                {
                    best = n.cost;
                    u = n;
                }
            }
            
            Q.remove(u);
            
            for(Link uv : u.getOutgoing())
            {
                Node v = uv.getEnd();
                int tt = uv.getTT();
                
                if(u.cost + tt < v.cost)
                {
                    v.cost = u.cost + tt;
                    v.length = u.length + uv.getLength();
                    v.prev = uv;
                    Q.add(v);
                }
            }
        }
    }
}
