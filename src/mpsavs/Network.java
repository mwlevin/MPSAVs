/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mpsavs;

import java.util.Random;
import java.util.Set;

/**
 *
 * @author mlevin
 */
public class Network 
{
    private Set<Node> nodes;
    private Set<Link> links;
    private Set<CNode> cnodes;
    
    public static int t;
    public static double dt = 30.0/3600;
    
    public static Random rand = new Random(1000);
    
    public Network()
    {
        
    }
}
