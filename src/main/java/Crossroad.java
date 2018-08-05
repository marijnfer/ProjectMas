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
    private int stationNr;

    public Crossroad(double px, double py){
        super(px,py);
        backwardsReachable = new ArrayList<>();
        this.stationNr = stationNr;
    }


    @Override
    public void tick(TimeLapse timeLapse){

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
}
