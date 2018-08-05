
import com.github.rinde.rinsim.geom.Point;

import java.util.ArrayList;

public class ExploreInfo {
    private Point sender;
    private int resources;
    private ArrayList<ExploreInfo> list;

    public ExploreInfo(AssemblyPoint sender, int resources, ArrayList<ExploreInfo> list){
        this.sender = sender;
        this.resources = resources;
        this.list = list;
    }

    public Point getSender(){
        return sender;
    }
}
