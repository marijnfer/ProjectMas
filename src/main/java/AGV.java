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
    ArrayList<Crossroad> startExplorerAnts;

    private Queue<Point> path;
    private Optional<CommDevice> comDevice;
    private Optional<Parcel> curr;
    private Factory factory;
    private AGVState state;

    private Point pickUpLocation;
    private ArrayList<Crossroad> destinations;
    private Point currentDestination;
    private Point deliveryLocation;
    private Task currentTask;
    private AssemblyPoint currentAssembly;




    public AGV(Point startPosition, int capacity,Factory factory,RandomGenerator rng,ArrayList<Crossroad> startExplorerAnts) {
        super(VehicleDTO.builder()
                .capacity(capacity)
                .startPosition(startPosition)
                .speed(1)
                .build());
        curr = Optional.absent();
        this.rng = rng;
        path = new LinkedList<>();
        curr = Optional.absent();
        this.factory = factory;
        state = AGVState.IDLE;
        this.startExplorerAnts = startExplorerAnts;
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    private Point nextDestination() {
        Crossroad p = destinations.get(0);
        if(p.assemblyPointPresent()){
            currentAssembly = p.getAssemblyPoint();
        }
        destinations.remove(0);
        return p;
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
                        pickUpLocation = curr.get().getPickupLocation();
                        deliveryLocation = curr.get().getDeliveryLocation();
                        destinations = factory.findPossiblePaths(startExplorerAnts,t).get(0);
                        currentTask = t;

                    }
                } catch (Exception ex){}
           }
           path = new LinkedList<>(getRoadModel().getShortestPathTo(this, pickUpLocation));

            rm.followPath(this,path,time);
            state = AGVState.INBOUND;

            return;
        }

        if(state == AGVState.INBOUND && !rm.getPosition(this).equals(pickUpLocation)){
            rm.followPath(this,path,time);
            return;
        }


        if(state == AGVState.INBOUND && rm.getPosition(this).equals(pickUpLocation)){
            pm.pickup(this,curr.get(),time);
            InboundPoint ip = (InboundPoint)curr.get().getPickupLocation();
            ip.setStored(false);

            currentDestination =  nextDestination();
            path = new LinkedList<>(getRoadModel().getShortestPathTo(this, currentDestination));
            state = AGVState.DRIVINGTOASSEMBLYcross;
            return;
        }

        if(state == AGVState.DRIVINGTOASSEMBLYcross && !rm.getPosition(this).equals(currentDestination)){
            rm.followPath(this,path,time);
            return;
        }

        if(state == AGVState.DRIVINGTOASSEMBLYcross && rm.getPosition(this).equals(currentDestination)){
            if(needToVisitAssembly(currentDestination)){
                state = AGVState.DRIVINGTOASSEMBLY;
                currentDestination = currentAssembly;
                path = new LinkedList<>(getRoadModel().getShortestPathTo(this,currentDestination));
            } else {
                currentDestination = nextDestination();
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                state = AGVState.DRIVINGTOASSEMBLYcross;
            }
            return;
        }

        if(state == AGVState.DRIVINGTOASSEMBLY && !rm.getPosition(this).equals(currentAssembly)){
            rm.followPath(this,path,time);
            return;
        }

        if(state == AGVState.DRIVINGTOASSEMBLY && rm.getPosition(this).equals(currentAssembly)) {
            if(destinations.size() != 0){
                currentDestination = nextDestination();
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                state = AGVState.DRIVINGTOASSEMBLYcross;
            } else{
                currentDestination = deliveryLocation;
                path = new LinkedList<>(getRoadModel().getShortestPathTo(this, currentDestination));
                state = AGVState.DELEVERING;
            }
            return;
        }
        if (state == AGVState.DELEVERING && !rm.getPosition(this).equals(deliveryLocation)) {
            if (time.hasTimeLeft()) {
                currentDestination = deliveryLocation;
                path = new LinkedList<>(getRoadModel().getShortestPathTo(this, currentDestination));
                rm.followPath(this, path, time);
            }

            return;
        }

        if (state == AGVState.DELEVERING && rm.getPosition(this).equals(deliveryLocation)) {
            pm.deliver(this, curr.get(), time);
            curr = Optional.absent();
            state = AGVState.IDLE;
            return;
        }

        /*
        if(state == AGVState.ASSEMBLING && rm.getPosition(this).equals(currentDestination)) {




            if (state == AGVState.DELEVERING && rm.getPosition(this).equals(deliveryLocation)) {
                pm.deliver(this, curr.get(), time);
                curr = Optional.absent();
                state = AGVState.IDLE;
                return;
            }
        }       */
    }

    private boolean needToVisitAssembly(Point p){
        try{
            Crossroad cr = (Crossroad)p;
            if(cr.assemblyPointPresent()){
                int nr = cr.getAssemblyPoint().getStationNr();
                if(currentTask.getTasks().get(nr)){
                    return true;
                }
            }
        } catch(Exception e) {e.printStackTrace(System.out);}

        return false;
    }

    private boolean burnIn(){
                if(burnInTick == 0){
                    return true;
                }
                burnInTick--;
                return false;
        }

}
