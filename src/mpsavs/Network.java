/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mpsavs;

import java.io.File;
import java.io.IOException;
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
    public static final int SAV_CAPACITY = 1;
    
    public static Network active = null;
    
    private Set<Node> nodes;
    private Set<Link> links;
    private Set<CNode> cnodes;
    
    private Set<SAV> savs;
    
    private TTMatrix tts;
    
    
    public static int t;
    
    public static double dt = 30.0/3600;
    public static int T_hr = 1;
    public static int T = (int)Math.round(1.0/dt * T_hr);
    
    
    
    public static Random rand = new Random(1000);
    
    private String name;
    
    public Network(String name, double scale, int fleet) throws IOException
    {
        active = this;
        this.name = name;
        
        
        nodes = new HashSet<>();
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
            
            links.add(new Link(id, nodemap.get(source_id), nodemap.get(dest_id), (int)Math.round(length/speed / dt)));
        }
        filein.close();
        
        double total = 0;
        
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
            
            total += demand;
        }
        filein.close();
        
        System.out.println("Total demand: "+total);
        
        
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
    
    public void simulate()
    {
        for(t = 0; t < T; t++)
        {
            for(CNode n : cnodes)
            {
                n.step();
            }
            
            dispatch();
            
            for(SAV v : savs)
            {
                v.step();
            }
        }
        

    }
    
    public void dispatch()
    {
        
    }
    public Set<Node> getNodes()
    {
        return nodes;
    }
    
    public Set<Link> getLinks()
    {
        return links;
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
            n.prev = null;
        }
        
        source.cost = 0;
        
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
                    v.prev = uv;
                    Q.add(v);
                }
            }
        }
    }
}
