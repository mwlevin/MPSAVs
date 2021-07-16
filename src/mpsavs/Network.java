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
import java.util.HashMap;
import java.util.HashSet;
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
    public int total_customers;
    
    public static final int SAV_CAPACITY = 1;
    
    public static Network active = null;
    
    public double V = 10;
    
    private List<Node> nodes;
    private Set<Link> links;
    private Set<CNode> cnodes;
    
    private Set<SAV> savs;
    
    private TTMatrix tts;
    
    
    public static int t;
    
    public static double dt = 30.0/3600;
    public static int T_hr = 30;
    public static int T = (int)Math.round(1.0/dt * T_hr);
    
    
    
    public static Random rand = new Random(1000);
    
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
        
        Scanner filein = new Scanner(new File("data/"+name+"/network/nodes.txt"));
        filein.nextLine();
        
        while(filein.hasNextInt())
        {
            int id = filein.nextInt();
            int type = filein.nextInt();
            
            nodes.add(new Node(id, type));
            
            filein.nextLine();
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
                    length, (int)Math.round(length/speed / dt)));
        }
        filein.close();
        
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
            
            cnodes.add(new CNode(origin, dest, demand));
            
            total_demand += demand;
        }
        filein.close();
        
        System.out.println("Total demand: "+total_demand);
        
        
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
            savs.add(new SAV(savnodes.get(idx)));
            
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
        
        tts = new TTMatrix(this);
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
        total_customers = 0;
        
        PrintStream output = new PrintStream(new FileOutputStream(new File("waiting.txt")), true);
        output.println("time\tnum waiting\tHOL time");
        
        for(t = 0; t < T; t++)
        {
            System.out.println(t);
            
            output.println(t+"\t"+getNumWaiting()+"\t"+getHOLTime());
            
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
        
        output.close();
    }
    
    
    public double stableRegionMaxServed() throws IloException
    {
        IloCplex cplex = new IloCplex();
        
        IloNumVar alpha = cplex.numVar(0, 1000);
        
        
        IloNumVar[][][] v = new IloNumVar[nodes.size()][nodes.size()][nodes.size()];
        
        
            
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
                    
                        v[q_idx][r_idx][s_idx] = cplex.numVar(0, Integer.MAX_VALUE);
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
                lhs.addTerm(1, v[q][c.getOrigin().getIdx()][c.getDest().getIdx()]);
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
                        
                        emptyTime += cplex.getValue(v[q_idx][r_idx][s_idx]) * getTT(q, r);
                        
                    }
                }
            }
        }
        
        System.out.println("predicted empty time: "+emptyTime * dt);
        
        double output = 0;
        
        for(CNode n : cnodes)
        {
            output += n.getLambda() * alpha_;
        }
        
        
        
        return output;
    }
  
    
    public void mdpp() throws IloException
    {
        IloCplex cplex = new IloCplex();
        
        cplex.setOut(cplex_log);
        
        List<CNode> nc = getWaiting();
        List<SAV> nv = getAvailable();
        
        if(nc.size() == 0 || nv.size() == 0)
        {
            return;
        }
        
        List<Path> paths = new ArrayList<>();
        
        for(CNode c : nc)
        {
            paths.add(new Path(c));
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
            
            System.out.println("\tSolved cplex");

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
        
        cplex.end();
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
        
        System.out.println("\tDispatch SAV "+sav.getId()+" path "+sav.getLocation()+"-"+path
            +" :"+sav.getDelay(path)+" "+path.getTT());
        
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
    
    public double emptyTT;
    
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
