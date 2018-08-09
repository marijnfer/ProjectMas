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

import java.util.*;

import static java.lang.StrictMath.abs;

public class AGV  extends Vehicle implements CommUser {

    RandomGenerator rng;
    private static int RANGE = 100;
    private int burnInTick = 20;
    private Optional<CommDevice> comDevice;
    ArrayList<Crossroad> startExplorerAnts;

    private Queue<Point> path;
    private Optional<Parcel> curr;
    private Factory factory;
    private AGVState state;

    private ArrayList<Reservation> reservations;

    private Point pickUpLocation;
    private ArrayList<Point> destinations1;
    private ArrayList<Point> destinations2;
    private ArrayList<Crossroad> destinations3; //assembly
    private ArrayList<Point> destinations4;
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
        destinations1 = new ArrayList<>();
        destinations2 = new ArrayList<>();
        destinations3 = new ArrayList<>();
        destinations4 = new ArrayList<>();
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    private Point nextDestination() {
        Crossroad p = destinations3.get(0);
        if(p.assemblyPointPresent()){
            currentAssembly = p.getAssemblyPoint();
            currentCrossroad = p;
        }
        destinations3.remove(0);
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
        System.out.print(counter);
        System.out.print("  ");
        System.out.println(state);

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
            Iterator it = RoadModels.findClosestObjects(getPosition().get(),rm).iterator();
            ArrayList<Task> t = new ArrayList<>();
            while (it.hasNext()){
                try {
                    Task ta = (Task)it.next();
                    t.add(ta);
                } catch (Exception e){}
            }

            curr = Optional.fromNullable(t.get(rng.nextInt(t.size())));
            pickUpLocation = curr.get().getPickupLocation();
            deliveryLocation = curr.get().getDeliveryLocation();
            currentTask = (Task)curr.get();

            double d1 = buildDestinationsToInbound(getPosition().get());
            double d3 = buildDestinationsAssembly();
            double d2 = buildDestinationsInboundToAss(pickUpLocation,destinations3.get(0));
            destinations3.remove(0);
            double d4 = buildDestinationsAssemblyToOut(destinations3.get(destinations3.size()-1),deliveryLocation);

           currentDestination = destinations1.get(0); destinations1.remove(0);
           path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));

           rm.followPath(this,path,time);
           state = AGVState.TOINBOUND;
           System.out.println(String.format("%f   %f   %f   %f",d1,d2,d3,d4));
           print();
           return;
        }

        if((state == AGVState.TOINBOUND) && !rm.getPosition(this).equals(currentDestination)){
            try{
                rm.followPath(this,path,time);
            }catch (Exception e){}
            return;
        }

        if(state == AGVState.TOINBOUND && rm.getPosition(this).equals(currentDestination)){
            if(destinations1.size() == 0){
                pm.pickup(this,curr.get(),time);
                InboundPoint ip = (InboundPoint)curr.get().getPickupLocation();
                ip.setStored(false);

                currentDestination = destinations2.get(0); destinations2.remove(0);
                path = new LinkedList<>(rm.getShortestPathTo(this, currentDestination));
                state = AGVState.INBOUNDTOASSEMBLY;
                print();
            } else {
                currentDestination = destinations1.get(0); destinations1.remove(0);
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                try{
                    rm.followPath(this,path,time);
                } catch (Exception e){}
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
                print();
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

                currentDestination = currentAssembly;
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                state = AGVState.DRIVINGTOASSEMBLY;
                print();
            } else {
                currentDestination = destinations3.get(0); destinations3.remove(0);
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
            if(destinations3.size() == 0){
                currentDestination = destinations4.get(0); destinations4.remove(0);
                path = new LinkedList<>(rm.getShortestPathTo(this, currentDestination));
                state = AGVState.DELEVERING;
                print();
            } else{
                currentDestination = destinations3.get(0);destinations3.remove(0);
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                state = AGVState.DRIVINGTOASSEMBLYcross;
            }
            return;
        }

        if (state == AGVState.DELEVERING && !rm.getPosition(this).equals(currentDestination)) {
            if (time.hasTimeLeft()) {
                rm.followPath(this, path, time);
            }
            return;
        }

        if (state == AGVState.DELEVERING && rm.getPosition(this).equals(currentDestination)) {
            if(destinations4.size() == 0){
                pm.deliver(this, curr.get(), time);
                curr = Optional.absent();
                currentCrossroad = null;
                currentAssembly = null;
                state = AGVState.IDLE;
                print();
                return;
            } else {
                currentDestination = destinations4.get(0); destinations4.remove(0);
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
            }

        }

    }

    private void buildDestinations(){
        RoadModel rm = getRoadModel();
        Point p2 = null;

        destinations3 = factory.findPossiblePaths(startExplorerAnts, currentTask).
                get(rng.nextInt(factory.findPossiblePaths(startExplorerAnts, currentTask).size()));


        if(state == AGVState.INBOUNDTOASSEMBLY) p2 = getPosition().get();
        else                                    p2 = pickUpLocation;
        for(Point p: rm.getShortestPathTo(p2,destinations3.get(0))) destinations2.add(p);

    }

    private double buildDestinationsToInbound(Point location){
        RoadModel rm = getRoadModel();
        for(Point p: rm.getShortestPathTo(location,pickUpLocation)) destinations1.add(p);
        double duration = calculateTicksOutToIn(destinations1);
        destinations1.remove(0);//first element is the current location
        return duration;
    }

    private double buildDestinationsInboundToAss(Point location, Point destination){
        RoadModel rm = getRoadModel();
        for(Point p: rm.getShortestPathTo(location,destination)) destinations2.add(p);
        double duration = calculateTicksInToAss(destinations2);
        destinations2.remove(0);
        return duration;
    }

    private double buildDestinationsAssembly(){
        destinations3 = factory.findPossiblePaths(startExplorerAnts,currentTask)
                .get(rng.nextInt(factory.findPossiblePaths(startExplorerAnts,currentTask).size()));
        double duration = calculateTicksAssembly(destinations3);
        //Need to remove first manually because this location is needed
        return duration;
    }

    private double buildDestinationsAssemblyToOut(Point location, Point deliveryLocation){
        RoadModel rm = getRoadModel();
        for(Point p: rm.getShortestPathTo(location,deliveryLocation)) destinations4.add(p);
        double duration = calculateTicksAssToOut(destinations4);
        destinations4.remove(0);
        return duration;
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

    private void makeReservations(ArrayList<Point> pts, String type){
        ArrayList<Double> ticks = new ArrayList<>();
        ArrayList<ArrayList<Point>> list = pointsLists(pts);
        switch (type){
            case "ass to out":
                for(ArrayList<Point> al: list){
                    ticks.add(calculateTicksAssToOut(al));
                }
                break;
            case "out to in":
                for(ArrayList<Point> al: list){
                    ticks.add(calculateTicksOutToIn(al));
                }
        }

        for(int i = 0; i < pts.size(); i++){
            Reservation res = new Reservation(this,ticks.get(i),10,counter);
            factory.makeReservations(res,pts.get(i));
        }
    }

    private void makeReservationsAssembly(ArrayList<Crossroad> crs){

    }

    private ArrayList<ArrayList<Point>> pointsLists(ArrayList<Point> pts){
        ArrayList<ArrayList<Point>> temp = new ArrayList<>();
        ArrayList<Point> lastElement = new ArrayList<>();


        for(int i = 0; i < pts.size(); i++){
            lastElement.add(pts.get(0));
            temp.add(lastElement);
        }

        return temp;
    }

    private ArrayList<ArrayList<Crossroad>> crossRoadLists(ArrayList<Crossroad> pts){
        ArrayList<ArrayList<Crossroad>> temp = new ArrayList<>();
        ArrayList<Crossroad> lastElement = new ArrayList<>();


        for(int i = 0; i < pts.size(); i++){
            lastElement.add(pts.get(0));
            temp.add(lastElement);
        }

        return temp;
    }

}
