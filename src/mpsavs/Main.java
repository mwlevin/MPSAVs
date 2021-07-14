/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mpsavs;

import java.io.IOException;

/**
 *
 * @author mlevin
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException
    {
        // TODO code application logic here
        
        Network test = new Network("SiouxFalls", 1, 500);
        test.simulate();
    }
    
}
