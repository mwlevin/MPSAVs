/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mpsavs;

import java.util.List;
import java.util.Set;

/**
 *
 * @author micha
 */
public class TTMatrix 
{
    private int[][] mat;
    private double[][] length;
    
    public TTMatrix(Network network)
    {
        List<Node> nodes = network.getNodes();
        
        mat = new int[nodes.size()][nodes.size()];
        length = new double[nodes.size()][nodes.size()];
        
        for(Node r : nodes)
        {
            if(r.getType() == 1000)
            {
                continue;
            }
            
            network.dijkstras(r);
            
            for(Node s : nodes)
            {
                if(s.getType() == 1000)
                {
                    continue;
                }
                
                mat[r.getIdx()][s.getIdx()] = s.cost;
                length[r.getIdx()][s.getIdx()] = s.length;
            }
        }
    }
    
    public double getLength(Node r, Node s)
    {
        if(r.getType() == 1000 || s.getType() == 1000)
        {
            throw new RuntimeException("using centroid node");
        }
        
        return length[r.getIdx()][s.getIdx()];
    }
    public int getTT(Node r, Node s)
    {
        if(r.getType() == 1000 || s.getType() == 1000)
        {
            throw new RuntimeException("using centroid node");
        }
        
        return mat[r.getIdx()][s.getIdx()];
    }
}
