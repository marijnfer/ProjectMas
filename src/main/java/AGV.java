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

import static java.lang.StrictMath.abs;

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
    //Purpose: When visiting assembly driving back to coupled cross road
    private Crossroad currentCrossroad;

    private Point latestPos = new Point(0,0);
    int counter = 0;
    int lastTick = 0;


    public AGV(Point startPosition, int capacity,Factory factory,RandomGenerator rng,ArrayList<Crossroad> startExplorerAnts) {
        super(VehicleDTO.builder()
                .capacity(capacity)
                .startPosition(startPosition)
                .speed(0.5)
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
            currentCrossroad = p;
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


    private synchronized void print(){
        /*
        System.out.print(counter);
        System.out.print("  ");
        System.out.print(counter-lastTick);
        System.out.print("  ");
        System.out.print(currentCrossroad);
        System.out.print("  ");
        System.out.println(getRoadModel().getPosition(this  ));
        factory.addData(latestPos,getRoadModel().getPosition(this),counter-lastTick);
        lastTick = counter;
        latestPos = getRoadModel().getPosition(this);


        System.out.print(counter);
        System.out.print("  ");
        System.out.println(state); */
    }

    @Override
    protected void tickImpl(TimeLapse time) {
        final PDPModel pm = getPDPModel();
        final RoadModel rm = getRoadModel();
        counter++;


        /*
        for(Crossroad cr: startExplorerAnts){
            if(rm.getPosition(this).equals(cr)){
                double total = counter + calculateTicks(destinations,currentTask);
                System.out.print("Totalllllllllllllllllll  ");

                System.out.println(total);
            }

        }   */

        if(!burnIn()){ return; }

        if (!time.hasTimeLeft()) { return; }

        if(state == AGVState.IDLE){
            Iterator it = RoadModels.findClosestObjects(rm.getPosition(this),rm).iterator();
            while(it.hasNext()){
                try {
                     Task t  = (Task)it.next();
                     Point p = t.getPickupLocation();
                    if(Point.distance(p,new Point(0,18))==0) {
                        curr = Optional.fromNullable(RoadModels.findClosestObject(
                                p, rm, Parcel.class));
                        pickUpLocation = curr.get().getPickupLocation();
                        deliveryLocation = curr.get().getDeliveryLocation();
                        destinations = factory.findPossiblePaths(startExplorerAnts,t).get(rng.nextInt(factory.findPossiblePaths(startExplorerAnts,t).size()));
                        currentTask = t;


                    }
                } catch (Exception ex){}
           }
           path = new LinkedList<>(rm.getShortestPathTo(this, pickUpLocation));

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

            path = new LinkedList<>(rm.getShortestPathTo(this, currentDestination));
            state = AGVState.DRIVINGTOASSEMBLYcross;
            print();
            return;
        }

        if(state == AGVState.DRIVINGTOASSEMBLYcross && !rm.getPosition(this).equals(currentDestination)){
            rm.followPath(this,path,time);
            return;
        }

        if(state == AGVState.DRIVINGTOASSEMBLYcross && rm.getPosition(this).equals(currentDestination)){
            if(needToVisitAssembly(currentDestination)){
                currentDestination = currentAssembly;
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                state = AGVState.DRIVINGTOASSEMBLY;
            } else {
                currentDestination = nextDestination();
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                state = AGVState.DRIVINGTOASSEMBLYcross;
            }
            print();
            return;
        }

        if(state == AGVState.DRIVINGTOASSEMBLY && !rm.getPosition(this).equals(currentAssembly)){
            rm.followPath(this,path,time);
            return;
        }

        if(state == AGVState.DRIVINGTOASSEMBLY && rm.getPosition(this).equals(currentAssembly)) {
            currentDestination = currentCrossroad;
            path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
            state = AGVState.DRIVINGAWAYASSEMBLY;
            print();
            return;
        }

        if(state == AGVState.DRIVINGAWAYASSEMBLY && !rm.getPosition(this).equals(currentCrossroad)) {
            rm.followPath(this,path,time);
            return;
        }

        if(state == AGVState.DRIVINGAWAYASSEMBLY && rm.getPosition(this).equals(currentCrossroad)) {
            if(destinations.size() != 0){
                currentDestination = nextDestination();
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                state = AGVState.DRIVINGTOASSEMBLYcross;
            } else{

                currentDestination = deliveryLocation;
                path = new LinkedList<>(rm.getShortestPathTo(this, currentDestination));
                state = AGVState.DELEVERING;
            }
            print();
            return;
        }




        /*

         */
        if (state == AGVState.DELEVERING && !rm.getPosition(this).equals(deliveryLocation)) {
            if (time.hasTimeLeft()) {
                currentDestination = deliveryLocation;
                path = new LinkedList<>(rm.getShortestPathTo(this, currentDestination));
                rm.followPath(this, path, time);
            }
            return;
        }

        if (state == AGVState.DELEVERING && rm.getPosition(this).equals(deliveryLocation)) {
            pm.deliver(this, curr.get(), time);
            curr = Optional.absent();
            state = AGVState.IDLE;
            print();
            return;
        }
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

    private double calculateTicks(ArrayList<Crossroad> crs, Task t){
        double d = 0;
        for(Crossroad cr: crs){
            if(needToVisitAssembly(cr)){
                d += 32;
            }
        }

        //16 to go in and out of assembly
        for(int i = 1; i < crs.size();i++){
            Point p0 = crs.get(i-1);
            Point p1 = crs.get(i);
            double xDiff = abs(p0.x - p1.x);
            double yDiff = abs(p0.y - p1.y);

            if(yDiff == 0 && xDiff == 5){d+=37;}
            else if((xDiff == 0 && yDiff ==10) ||(xDiff == 10 && yDiff ==0) ){d+=73;}
            else if((xDiff == 10 && yDiff ==10)){d += 103;}
            else {System.out.println("length not found");}

        }

        return d;

    }



}
