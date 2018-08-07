import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import java.util.ArrayList;

public class Crossroad extends Point implements CommUser, TickListener{
    private static int RANGE = 50;
    private Optional<CommDevice> comDevice;
    private ArrayList<CommUser> backwardsReachable;
    private Pheromone pheromone;
    private AssemblyPoint assemblyPoint;

    public Crossroad(double px, double py){
        super(px,py);
        backwardsReachable = new ArrayList<>();
    }


    @Override
    public void tick(TimeLapse timeLapse){
        if(pheromone != null){
            pheromone.evaporate();
        }

    }

    public void setAssemblyPoint(AssemblyPoint assemblyPoint){
        this.assemblyPoint = assemblyPoint;
    }

    public void setPheromone(Pheromone pheromone){
        this.pheromone = pheromone;
    }


    @Override
    public void afterTick(TimeLapse timeLapse) {
    }

    @Override
    public void setCommDevice(CommDeviceBuilder builder) {
        if (RANGE >= 0) {
            builder.setMaxRange(RANGE);
        }
        comDevice = Optional.of(builder
                .build());
    }
    @Override
    public Optional<Point> getPosition() {
        return Optional.of((Point)this);
    }

    public double getX(){
        return this.x;
    }

    public void setBackwardsReachable(ArrayList<CommUser> cds){
        backwardsReachable = cds;
    }

    public CommUser getCommUser() {
        return this;
    }

    public boolean assemblyPointPresent(){
        if(assemblyPoint == null){return  false;}
        return true;
    }

    public AssemblyPoint getAssemblyPoint() {
        return assemblyPoint;
    }
}
