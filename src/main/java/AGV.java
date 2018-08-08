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

    private ArrayList<Reservation> reservations;

    private Point pickUpLocation;
    private ArrayList<Crossroad> destinations;
    private ArrayList<Point> destinations2;
    private Point currentDestination;
    private Point deliveryLocation;
    private Task currentTask;
    private AssemblyPoint currentAssembly;
    //Purpose: When visiting assembly driving back to coupled cross road
    private Crossroad currentCrossroad;

    private Point latestPos = new Point(0,0);
    int counter = 0;
    int lastTick = 0;

    Point firstDestination;



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
        destinations2 = new ArrayList<>();
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
        System.out.print(getRoadModel().getPosition(this  ));

        System.out.print("  ");

        System.out.println(state);
        */
        factory.addData(latestPos,getRoadModel().getPosition(this),counter-lastTick);

        lastTick = counter;
        latestPos = getRoadModel().getPosition(this);


    }

    @Override
    protected void tickImpl(TimeLapse time) {
        final PDPModel pm = getPDPModel();
        final RoadModel rm = getRoadModel();
        counter++;

        if(!burnIn()){ return; }

        if (!time.hasTimeLeft()) { return; }

        if(state == AGVState.IDLE){
                Iterator it = RoadModels.findClosestObjects(rm.getPosition(this),rm).iterator();
            while(it.hasNext()){
                try {
                     Task t  = (Task)it.next();
                     Point p = t.getPickupLocation();
                    if(Point.distance(p,new Point(10,3))==0) {
                        curr = Optional.fromNullable(RoadModels.findClosestObject(p, rm, Parcel.class));
                        pickUpLocation = curr.get().getPickupLocation();
                        deliveryLocation = curr.get().getDeliveryLocation();
                        currentTask = t;
                    }
                } catch (Exception ex){}
           }
           destinations2 = new ArrayList<>();
           for(Point p: rm.getShortestPathTo(this,pickUpLocation)){
                destinations2.add(p);
           }
           System.out.print(String.format("%d arrived in     ",counter));
           System.out.println(calculateTicksOutToIn(destinations2));
           currentDestination = destinations2.get(0); destinations2.remove(0);
           path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));

           rm.followPath(this,path,time);
           state = AGVState.INBOUND;
           return;
        }

        if(state == AGVState.INBOUND && !rm.getPosition(this).equals(currentDestination)){
            rm.followPath(this,path,time);
            return;
        }

        if(state == AGVState.INBOUND && rm.getPosition(this).equals(currentDestination)){
            if(destinations2.size() == 0){
                pm.pickup(this,curr.get(),time);
                InboundPoint ip = (InboundPoint)curr.get().getPickupLocation();
                ip.setStored(false);

                destinations = factory.findPossiblePaths(startExplorerAnts,currentTask).
                        get(rng.nextInt(factory.findPossiblePaths(startExplorerAnts,currentTask).size()));

                System.out.println(counter);
                //firstDestination = currentDestination;
                destinations2 = new ArrayList<>();
                double duration = calculateTicksAssembly(destinations);
                path = new LinkedList<>(rm.getShortestPathTo(this,nextDestination()));
                for(Point p: path){
                    destinations2.add(p);
                }
                System.out.print("inb to ass  ");
                System.out.print(counter);
                System.out.print("  ");
                System.out.print(calculateTicksInToAss(destinations2));
                System.out.print("  ass to out  ");
                System.out.print(counter);
                System.out.print("  ");
                System.out.println(duration);

                destinations2.remove(0);


                currentDestination = destinations2.get(0); destinations2.remove(0);
                path = new LinkedList<>(rm.getShortestPathTo(this, currentDestination));

                print();
                state = AGVState.INBOUNDTOASSEMBLY;

            } else{
                currentDestination = destinations2.get(0); destinations2.remove(0);
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                rm.followPath(this,path,time);

            }

            return;
        }

        if(state == AGVState.INBOUNDTOASSEMBLY && !rm.getPosition(this).equals(currentDestination)){
            rm.followPath(this,path,time);
            return;
        }

        if(state == AGVState.INBOUNDTOASSEMBLY && rm.getPosition(this).equals(currentDestination)){
            if(destinations2.size() == 0){
                state = AGVState.DRIVINGTOASSEMBLYcross;

                return;

            }else {
                currentDestination = destinations2.get(0); destinations2.remove(0);
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                rm.followPath(this,path,time);
                return;
            }
        }

        if(state == AGVState.DRIVINGTOASSEMBLYcross && !rm.getPosition(this).equals(currentDestination)){
            rm.followPath(this,path,time);
            return;
        }

        if(state == AGVState.DRIVINGTOASSEMBLYcross && rm.getPosition(this).equals(currentDestination)){
            if(needToVisitAssembly(currentDestination)){
                System.out.println(counter);

                currentDestination = currentAssembly;
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                state = AGVState.DRIVINGTOASSEMBLY;
            } else {
                currentDestination = nextDestination();
                System.out.println(counter);

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
                System.out.println(counter);

                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                state = AGVState.DRIVINGTOASSEMBLYcross;
            } else{
                Iterator it = rm.getShortestPathTo(this,deliveryLocation).iterator();
                destinations2 = new ArrayList<>();
                while (it.hasNext()){
                    Point p = (Point)it.next();
                    destinations2.add(p);
                }
                double a = calculateTicksAssToOut(destinations2);
                destinations2.remove(0);
                currentDestination = destinations2.get(0); destinations2.remove(0);
                path = new LinkedList<>(rm.getShortestPathTo(this, currentDestination));
                state = AGVState.DELEVERING;
                System.out.print("Assembly to out");
                System.out.print(a);
                System.out.print("  ");
                System.out.println(counter);
            }
            print();
            return;
        }

        if (state == AGVState.DELEVERING && !rm.getPosition(this).equals(currentDestination)) {
            if (time.hasTimeLeft()) {
                rm.followPath(this, path, time);
            }
            return;
        }

        if (state == AGVState.DELEVERING && rm.getPosition(this).equals(currentDestination)) {
            if(destinations2.size() == 0){
                pm.deliver(this, curr.get(), time);
                curr = Optional.absent();
                state = AGVState.IDLE;
                return;
            } else {
                currentDestination = destinations2.get(0); destinations2.remove(0);
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));

            }

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

    private double calculateTicksAssembly(ArrayList<Crossroad> crs){
        double d = 0;
        //32 to go in and out of assembly
        for(Crossroad cr: crs){
            if(needToVisitAssembly(cr)){
                d += 32;
            }
        }

        for(int i = 1; i < crs.size();i++){
            Point p0 = crs.get(i-1);
            Point p1 = crs.get(i);
            double xDiff = abs(p0.x - p1.x);
            double yDiff = abs(p0.y - p1.y);
            if(yDiff == 0 && xDiff == 5){d+=37;}
            else if((xDiff == 0 && yDiff ==10) ||(xDiff == 10 && yDiff ==0) ){d+=73;}
            else if((xDiff == 10 && yDiff ==10)){d += 103;}
            else if((xDiff == 8 && yDiff ==0)){d += 59;}
            else if((xDiff == 5 && yDiff ==10)){d += 82;}
            else {System.out.println("length not found");}
        }

        return d;

    }

    private double calculateTicksAssToOut(ArrayList<Point> crs){
        double d = 0;
        for(int i = 1; i < crs.size();i++){
            Point p0 = crs.get(i-1);
            Point p1 = crs.get(i);
            double xDiff = abs(p0.x - p1.x);
            double yDiff = abs(p0.y - p1.y);

            if(p0 instanceof Crossroad && p1 instanceof DeliveryPoint){
                d+= 17;
            }
            else if(yDiff == 0 && xDiff == 5){d+=37;}
            else if((xDiff == 0 && yDiff ==10) ||(xDiff == 10 && yDiff ==0) ){d+=73;}
            else if((xDiff == 0 && yDiff ==4)){d += 30;}
            else if((xDiff == 17 && yDiff ==0)){d += 124;}
            else if((xDiff == 1 && yDiff ==0)){d += 9;}
            else if((xDiff == 10 && yDiff ==10)){d += 103;}
            else if((xDiff == 8 && yDiff ==10)){d += 93;}
            else {System.out.println("length not found");}

        }

        return d+10; //+10 because delivery time

    }

    private double calculateTicksOutToIn(ArrayList<Point> crs){
        double d = 0;

        for(int i = 1; i < crs.size();i++){
            Point p0 = crs.get(i-1);
            Point p1 = crs.get(i);
            double xDiff = abs(p0.x - p1.x);
            double yDiff = abs(p0.y - p1.y);

            if(xDiff == 0 && yDiff ==2){ d+= 15; }
            else if(p0 instanceof Crossroad && p1 instanceof InboundPoint){d+=17;}
            else if((xDiff == 1 && yDiff ==0)){d += 8;}
            else if((xDiff == 6 && yDiff ==0)){d += 44;}
            else {
                System.out.println("Distance not found");}
        }

        return d;

    }

    private double calculateTicksInToAss(ArrayList<Point> crs){
        double d = 0;

        for(int i = 1; i < crs.size();i++){
            double b = d;
            Point p0 = crs.get(i-1);
            Point p1 = crs.get(i);
            double xDiff = abs(p0.x - p1.x);
            double yDiff = abs(p0.y - p1.y);

            if(xDiff == 0 && yDiff ==2){d+= 25;}
            else if((xDiff == 1 && yDiff ==0)){d += 8;}
            else if(yDiff == 0 && xDiff == 5 && p0.x - p1.x < 0){d+=36;}
            else if(yDiff == 0 && xDiff ==5 && p0.x - p1.x > 0){d+=37;}
            else if((xDiff == 0 && yDiff ==4)){d += 29;}
            else if((xDiff == 0 && yDiff ==10) ){d+=72;}

            else{
                System.out.println("Distance not found");}
        }

        return d;

    }

   




}
