import com.github.rinde.rinsim.geom.Point;

import java.util.ArrayList;

public class Connect {
    private Crossroad crossroad;
    private ArrayList<Crossroad> coupled;
    private ArrayList<Double> distances;

    public Connect(Crossroad crossroad){
        this.crossroad = crossroad;
        coupled = new ArrayList<>();
        distances = new ArrayList<>();

    }

    public void addCoupledCrossroad(Crossroad cr) {
        distances.add(Point.distance(cr,crossroad));
        coupled.add(cr);
    }

    public Crossroad getCrossroad() {
        return crossroad;
    }

    public ArrayList<Crossroad> getCoupled() {
        return coupled;
    }
}
