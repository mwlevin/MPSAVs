package mpsavs;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author mlevin
 */
public class SAV 
{
    private int id;
    
    private static int next_id = 1;
    
    
    private Node location;
    private Path path;
    
    
    public SAV(Node loc)
    {
        id = next_id++;
        this.location = loc;
    }
}
