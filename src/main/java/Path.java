import com.github.rinde.rinsim.geom.Point;

import java.util.ArrayList;

public class Path {
    Point start;
    ArrayList<Connect> connections;

    public Path(Point start, ArrayList<Connect> connections){
        this.connections = connections;
        this.start = start;
    }


}
