
import com.github.rinde.rinsim.geom.Point;

/**
 * Pheromone with its AssemblyPoint
 */
public class ExploreInfo {
    private Point sender;
    private double pheromone;

    public ExploreInfo(AssemblyPoint sender, double pheromone){
        this.sender = sender;
        this.pheromone = pheromone;
    }

    public Point getSender(){
        return sender;
    }

    public double getPheromone() {
        return pheromone;
    }
}
