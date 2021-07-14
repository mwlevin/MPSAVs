/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mpsavs;

import java.util.Set;

/**
 *
 * @author micha
 */
public class TTMatrix 
{
    private int[][] mat;
    
    public TTMatrix(Network network)
    {
        Set<Node> nodes = network.getNodes();
        
        mat = new int[nodes.size()][nodes.size()];
        
        for(Node r : nodes)
        {
            network.dijkstras(r);
            
            for(Node s : nodes)
            {
                mat[r.getIdx()][s.getIdx()] = s.cost;
            }
        }
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
