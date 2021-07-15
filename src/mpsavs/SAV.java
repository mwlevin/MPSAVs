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
    private int time_to_arrive;
    
    private Path path;
    
    
    public SAV(Node loc)
    {
        id = next_id++;
        this.location = loc;
    }
    
    public void step()
    {
        if(time_to_arrive > 0)
        {
            time_to_arrive--;
        }
        else
        {
            path = null;
        }
    }
    
    public boolean isParked()
    {
        return time_to_arrive == 0;
    }
    
    public Node getLocation()
    {
        return location;
    }
    
    public int getDelay(Path path)
    {
        return Network.active.getTT(location, path.getOrigin());
    }
    
    public int getId()
    {
        return id;
    }
    
    public void dispatch(Path path)
    {
        this.path = path;
        location = path.getDest();
        time_to_arrive = path.getTT() + getDelay(path);
    }
}
