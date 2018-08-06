import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class AGV  extends Vehicle implements CommUser {

    RandomGenerator rng;
    private static int RANGE = 100;
    private int burnInTick = 20;

    private Optional<Point> destination;
    private Queue<Point> path;
    private Optional<CommDevice> comDevice;
    private Optional<Parcel> curr;
    private Factory factory;
    private AGVState state;

     ArrayList<Crossroad> startExplorerAnts;


    public AGV(Point startPosition, int capacity,Factory factory,RandomGenerator rng,ArrayList<Crossroad> startExplorerAnts) {
        super(VehicleDTO.builder()
                .capacity(capacity)
                .startPosition(startPosition)
                .speed(1)
                .build());
        curr = Optional.absent();
        this.rng = rng;
        destination = Optional.absent();
        path = new LinkedList<>();
        curr = Optional.absent();
        this.factory = factory;
        state = AGVState.IDLE;
        this.startExplorerAnts = startExplorerAnts;
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    void nextDestination() {
        //destination = Optional.of(roadModel.get().getRandomPosition(rng));
        path = new LinkedList<>(getRoadModel().getShortestPathTo(this,
                destination.get()));
    }

    @Override
    public Optional<Point> getPosition() {
        if (getRoadModel().containsObject(this)) {
            return Optional.of(getRoadModel().getPosition(this));
        }
        return Optional.absent();
    }

    @Override
    public void setCommDevice(CommDeviceBuilder builder) {
        if (RANGE >= 0) {
            builder.setMaxRange(RANGE);
        }
        comDevice = Optional.of(builder.build());

    }

    @Override
    protected void tickImpl(TimeLapse time) {
        //int size = RoadModels.findClosestObjects(getRoadModel().getPosition(this),getRoadModel(),5000).size();
       // System.out.println(String.format("All %d",size));

        final PDPModel pm = getPDPModel();
        final RoadModel rm = getRoadModel();

        if(!burnIn()){ return; }

        if (!time.hasTimeLeft()) { return; }

        if(state == AGVState.IDLE){
            Iterator it =RoadModels.findClosestObjects(rm.getPosition(this),getRoadModel()).iterator();
            while(it.hasNext()){
                try {
                     Task t  = (Task)it.next();
                     Point p = t.getPickupLocation();
                    if(Point.distance(p,new Point(0,14))==0) {
                        curr = Optional.fromNullable(RoadModels.findClosestObject(
                                p, rm, Parcel.class));
                        factory.sendAnts(startExplorerAnts,t);
                    }
                } catch (Exception e){}


           }
           destination =  Optional.of(curr.get().getPickupLocation());
            nextDestination();


            rm.followPath(this,path,time);
            state = AGVState.GETTING;

            return;
        }

        if(state == AGVState.IDLE && Point.distance(getRoadModel().getPosition(this),new Point(0,13))==0){
            curr = Optional.fromNullable(RoadModels.findClosestObject(
                    rm.getPosition(this), rm, Parcel.class));
            destination =  Optional.of(curr.get().getPickupLocation());

            nextDestination();
            rm.followPath(this,path,time);
            state = AGVState.GETTING;
            return;
        }


        if(state == AGVState.GETTING && !rm.getPosition(this).equals(destination.get())){
            rm.followPath(this,path,time);
            return;
        }


        if(state == AGVState.GETTING && rm.getPosition(this).equals(destination.get())){
            pm.pickup(this,curr.get(),time);
            destination = Optional.of(curr.get().getDeliveryLocation());


            nextDestination();
            if(time.hasTimeLeft()){
                rm.followPath(this,path,time);
            }
            state = AGVState.DELEVERING;
            //InboundPoint ip = (InboundPoint)curr.get().getPickupLocation();

            InboundPoint ip = (InboundPoint)curr.get().getPickupLocation();
            ip.setStored(false);
            //factory.setSearchInboundPoint((InboundPoint)curr.get().getPickupLocation(),false);
            return;
        }


        if(state == AGVState.DELEVERING && !rm.getPosition(this).equals(destination.get())){
            if(time.hasTimeLeft()){
                rm.followPath(this,path,time);
            }

            return;
        }

        if(state == AGVState.DELEVERING && rm.getPosition(this).equals(destination.get())){
            pm.deliver(this,curr.get(),time);
            curr = Optional.absent();
            state = AGVState.IDLE;
            return;
        }

        }

    private boolean burnIn(){
            if(burnInTick == 0){
                return true;
            }
            burnInTick--;
            return false;
    }

}
