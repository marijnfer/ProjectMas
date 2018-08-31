import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.*;

import static java.lang.StrictMath.abs;

public class AGV  extends Vehicle implements CommUser {

    private int id;
    RandomGenerator rng;
    private static int RANGE = 15;
    private int burnInTick;
    private Optional<CommDevice> comDevice;
    ArrayList<Crossroad> startExplorerAnts;

    private Queue<Point> path;
    private Optional<Parcel> curr;
    private Factory factory;
    private AGVState state;

    private Point pickUpLocation;
    private ArrayList<Point> destinations1;     //DeliveryPoint to InboundPoint
    private ArrayList<Point> destinations2;     //InboundPoint to Assembly
    private ArrayList<Crossroad> destinations3; //Assembly
    private ArrayList<Point> destinations4;     //Assembly to DeliveryPoint
    private Point currentDestination;
    private Point deliveryLocation;
    private Task currentTask;
    private AssemblyPoint currentAssembly;
    private Crossroad currentCrossroad;//Purpose: When visiting assembly driving back to coupled cross road
    private Point previousDP;
    private int counter = 0;

    private ArrayList<Reservation> allReservations;
    private AGVState previousState;

    private Crossroad waitCrossroad;
    private InboundPoint waitInbound;
    private DeliveryPoint waitDelivery;
    private CommUser waitComDevice;

    private boolean waitMessageSended = false;
    private int waitMessageCounter = 5;

    private ArrayList<ExploreInfo> pheromones;

    public AGV(Point startPosition, int capacity,Factory factory,RandomGenerator rng,ArrayList<Crossroad> startExplorerAnts, int id) {
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
        previousDP = new Point(-10,-10);
        deliveryLocation = new Point(-20,-20);
        allReservations = new ArrayList<>();
        this.id = id;
        burnInTick = 20 + id*10;
        pheromones = new ArrayList<>();
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

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
        counter++;
        handleMessages();


        if(currentDestination != null&& state != AGVState.WAIT && waitComDevice == null) {
            Point pos = getPosition().get();
            //Wait for crossroad or assembly
            if(currentDestination instanceof Crossroad){
                double distance = Point.distance(pos,currentDestination);
                if( needToVisitAssembly(currentDestination) && distance <= 2){
                    WaitMessage wm = new WaitMessage("Need to wait AS",(Crossroad)currentDestination,distance);
                    comDevice.get().broadcast(wm);
                    waitMessageSended = true;
                } else {
                    int index = destinations3.indexOf(currentDestination);
                    if(index != -1){
                        double dis = Point.distance(getPosition().get(), currentDestination);
                        if (dis <= 2 && getPosition().get().x > 5) {
                            WaitMessage wm = new WaitMessage("Need to wait CR", (Crossroad) currentDestination, dis);
                            comDevice.get().broadcast(wm);
                        }
                    }

                }
            }

            //Arriving at inboundpoint
            double d1 = pos.x-pickUpLocation.x;
            if(d1> 0 && d1 <= 3 && pos.y == 5){
                WaitMessage wm = new WaitMessage("Need to wait IP", (InboundPoint)pickUpLocation);
                comDevice.get().broadcast(wm);
            }

            //Arriving at delivery point
            double d2 = pos.x - deliveryLocation.x;
            if(d2>=0 && d2 <= 5&& pos.y == 5 && state != AGVState.TOINBOUND){
                WaitMessage wm = new WaitMessage("Need to wait DP", (DeliveryPoint)deliveryLocation);
                comDevice.get().broadcast(wm);
            }
        }

        if(waitMessageSended) waitMessageCounter--;
        if(waitMessageCounter == 0 && waitMessageSended) waitMessageSended = false;

        if(state == AGVState.WAIT){
            return;
        }

        // Send a message to the agv that is waiting on this agv
        if(waitComDevice != null){
            if(waitCrossroad != null && rm.getShortestPathTo(getPosition().get(),waitCrossroad).size() > 10){
                WaitMessage wm = new WaitMessage("Stop waiting AS",waitCrossroad);
                WaitMessage wm1 = new WaitMessage("Stop waiting CR",waitCrossroad);

                comDevice.get().send(wm,waitComDevice);
                comDevice.get().send(wm1,waitComDevice);

                waitComDevice = null;
                waitCrossroad = null;
            }

            if(waitInbound != null){
                if(getPosition().get().x < waitInbound.x){
                    WaitMessage wm = new WaitMessage("Stop waiting IP", waitInbound);
                    comDevice.get().send(wm, waitComDevice);
                    waitComDevice = null;
                    waitInbound = null;
                }
            }

            if(waitDelivery != null) {
                if (getPosition().get().x < waitDelivery.x) {
                    WaitMessage wm = new WaitMessage("Stop waiting DP", waitDelivery);
                    comDevice.get().send(wm, waitComDevice);
                    waitComDevice = null;
                    waitDelivery = null;
                }
            }
        }

        //Refresh the reservations every 10 ticks
        if(counter % 10 == 0)sendReservation();

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
            previousDP = deliveryLocation;
            deliveryLocation = curr.get().getDeliveryLocation();
            currentTask = (Task)curr.get();

            int d1 = buildDestinationsDeliveryToInbound(getPosition().get(),counter);
            ArrayList<ArrayList<Integer>> d2 = buildDestinationsInboundToAssembly(pickUpLocation,d1,startExplorerAnts);
            ArrayList<Integer> d2End = new ArrayList<>();
            for(ArrayList<Integer> ar: d2) d2End.add(ar.get(ar.size()-1));
            double d3 = buildDestinationsAssembly(startExplorerAnts,d2End);
            Point startd2 = destinations3.get(0);
            double d2final = d2End.get(startExplorerAnts.indexOf(startd2));
            double d4 = buildDestinationsAssemblyToOut(destinations3.get(destinations3.size()-1),deliveryLocation,
                    (int)(d3));

            for(Point p: rm.getShortestPathTo(pickUpLocation,destinations3.get(0))) destinations2.add(p);
            reservateInboundToAssembly(d2.get(startExplorerAnts.indexOf(startd2)),destinations2);

            sendReservation();

            currentDestination = destinations1.get(0); destinations1.remove(0);
            path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));

            rm.followPath(this,path,time);
            state = AGVState.TOINBOUND;
            destinations3.remove(0);

            return;
        }

        if((state == AGVState.TOINBOUND) && !rm.getPosition(this).equals(currentDestination)){
            try{
                rm.followPath(this,path,time);
            }catch (Exception e){}
            return;
        }

        //Arrived at a point while driving towards the inbound
        if(state == AGVState.TOINBOUND && rm.getPosition(this).equals(currentDestination)){
            if(destinations1.size() == 0){ //arrived at the InboundPoint
                try{
                    pm.pickup(this,curr.get(),time);
                } catch (Exception e){}
                InboundPoint ip = (InboundPoint)curr.get().getPickupLocation();
                ip.setStored(false);
                allReservations.clear();

                ArrayList<ArrayList<Integer>> d2 = buildDestinationsInboundToAssembly(pickUpLocation,counter,startExplorerAnts);
                ArrayList<Integer> d2End = new ArrayList<>();
                for(ArrayList<Integer> ar: d2) d2End.add(ar.get(ar.size()-1));

                double d3 = buildDestinationsAssembly(startExplorerAnts,d2End);
                Point startd2 = destinations3.get(0);
                double d2final = d2End.get(startExplorerAnts.indexOf(startd2));
                double d4 = buildDestinationsAssemblyToOut(destinations3.get(destinations3.size()-1),deliveryLocation, (int)(d3));

                destinations2.clear();
                for(Point p: rm.getShortestPathTo(pickUpLocation,destinations3.get(0))) destinations2.add(p);
                reservateInboundToAssembly(d2.get(startExplorerAnts.indexOf(startd2)),destinations2);

                sendReservation();

                currentDestination = destinations2.get(0); destinations2.remove(0);
                path = new LinkedList<>(rm.getShortestPathTo(this, currentDestination));
                state = AGVState.INBOUNDTOASSEMBLY;
            } else { //Continue to driving to the InboundPoint
                currentDestination = destinations1.get(0); destinations1.remove(0);
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                try{
                    rm.followPath(this,path,time);
                }catch (Exception e){}
            }
            return;
        }

        if(state == AGVState.INBOUNDTOASSEMBLY && !rm.getPosition(this).equals(currentDestination)){
            try{
                rm.followPath(this,path,time);
            }catch (Exception e){}
            return;
        }

        if(state == AGVState.INBOUNDTOASSEMBLY && rm.getPosition(this).equals(currentDestination)){
            if(destinations2.size() == 0){ //Arrived at the assembly
                state = AGVState.DRIVINGTOASSEMBLYcross;
                return;

            }else {// Arrived at a point towards the assembly (update the path to the best one)
                allReservations.clear();

                ArrayList<Crossroad> begin = new ArrayList<>();
                for(Crossroad c: startExplorerAnts){
                    if(c.y >= getPosition().get().y) begin.add(c);
                }

                ArrayList<ArrayList<Integer>> d2 = buildDestinationsInboundToAssembly(currentDestination,counter,begin);
                ArrayList<Integer> d2End = new ArrayList<>();
                for(ArrayList<Integer> ar: d2) d2End.add(ar.get(ar.size()-1));
                double d3 = buildDestinationsAssembly(begin,d2End);
                Point startd2 = destinations3.get(0);
                double d2final = d2End.get(begin.indexOf(startd2));
                double d4 = buildDestinationsAssemblyToOut(destinations3.get(destinations3.size()-1),deliveryLocation, (int)(d3));
                destinations2.clear();

                for(Point p: rm.getShortestPathTo(getPosition().get(),destinations3.get(0))) destinations2.add(p);
                reservateInboundToAssembly(d2.get(begin.indexOf(startd2)),destinations2);

                sendReservation();
                if(Point.distance(getPosition().get(),destinations3.get(0) )==0){
                    state = AGVState.DRIVINGTOASSEMBLYcross;
                    destinations3.remove(0);
                    currentDestination = nextDestination();
                    path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                } else{
                    destinations2.remove(0);
                    currentDestination = destinations2.get(0); destinations2.remove(0);
                    path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                    try{
                        rm.followPath(this,path,time);
                    } catch (Exception e){ }

                }
                return;
            }
        }

        if(state == AGVState.DRIVINGTOASSEMBLYcross && !rm.getPosition(this).equals(currentDestination)){
            rm.followPath(this,path,time);
            return;
        }

        //Arrived at a crossroad in the assembly
        if(state == AGVState.DRIVINGTOASSEMBLYcross && rm.getPosition(this).equals(currentDestination)){
            if(needToVisitAssembly(currentDestination)){ //go visit the assembly (path updated after you visited the assembly)
                currentDestination = currentAssembly;
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                state = AGVState.DRIVINGTOASSEMBLY;
            } else { //update to the best available path
                if(startExplorerAnts.indexOf(getPosition().get()) == -1 &&
                        factory.findPossiblePaths((Crossroad)getPosition().get(),currentTask).size() >0){
                    allReservations.clear();
                    double d3 =buildDestinationsAssemblyDuring((Crossroad)getPosition().get());
                    path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                    destinations4.clear();

                    double d4 = buildDestinationsAssemblyToOut(destinations3.get(destinations3.size()-1),deliveryLocation,
                            (int)(d3));

                    currentDestination = nextDestination();
                    path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                    state = AGVState.DRIVINGTOASSEMBLYcross;

                } else{
                    currentDestination = nextDestination();
                    path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                    state = AGVState.DRIVINGTOASSEMBLYcross;
                }

            }
            return;
        }

        if(state == AGVState.DRIVINGTOASSEMBLY && !rm.getPosition(this).equals(currentAssembly)){
            rm.followPath(this,path,time);
            return;
        }

        //Arrived at the assemblyPoint
        if(state == AGVState.DRIVINGTOASSEMBLY && rm.getPosition(this).equals(currentAssembly)) {
            currentDestination = currentCrossroad;
            path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
            state = AGVState.DRIVINGAWAYASSEMBLY;
            return;
        }

        if(state == AGVState.DRIVINGAWAYASSEMBLY && !rm.getPosition(this).equals(currentCrossroad)) {
            rm.followPath(this,path,time);
            return;
        }

        //Arrived at the crossroad connected to the assemblyPoint you just visited
        if(state == AGVState.DRIVINGAWAYASSEMBLY && rm.getPosition(this).equals(currentCrossroad)) {
            //increase pheromone of that assemblyPoint
            ArrayList<ExploreInfo> temp = currentAssembly.increasePheromone();
            for(ExploreInfo ex: temp){
                removeExploreInfo(ex);
                pheromones.add(ex);
            }

            if(destinations3.size() == 0){ //Done assembling, drive towards DeliveryPoint
                currentDestination = destinations4.get(0); destinations4.remove(0);
                path = new LinkedList<>(rm.getShortestPathTo(this, currentDestination));
                state = AGVState.DELEVERING;
            } else{ //continue assembling
                allReservations.clear();
                double d3 = buildDestinationsAssemblyDuring((Crossroad)getPosition().get());
                currentDestination = nextDestination();
                path = new LinkedList<>(rm.getShortestPathTo(this,currentDestination));
                state = AGVState.DRIVINGTOASSEMBLYcross;
            }
            return;
        }

        if (state == AGVState.DELEVERING && !rm.getPosition(this).equals(currentDestination)) {
            if (time.hasTimeLeft()) {
                try{
                    rm.followPath(this, path, time);
                } catch (Exception e){}
            }
            return;
        }

        //Arrived at deliveryPoint
        if (state == AGVState.DELEVERING && rm.getPosition(this).equals(currentDestination)) {
            if (destinations4.size() == 0) {
                try{
                    pm.deliver(this, curr.get(), time);
                } catch (Exception e){}
                curr = Optional.absent();
                currentCrossroad = null;
                currentAssembly = null;
                state = AGVState.IDLE;
                return;
            } else {
                currentDestination = destinations4.get(0);
                destinations4.remove(0);
                path = new LinkedList<>(rm.getShortestPathTo(this, currentDestination));
            }
        }

    }

    /**
     * @return the next crossroad to visit while assembling (destinations 3)
     */
    private Point nextDestination() {
        Crossroad p = destinations3.get(0);
        if(p.assemblyPointPresent()){
            currentAssembly = p.getAssemblyPoint();
            currentCrossroad = p;
        }
        destinations3.remove(0);
        return p;
    }

    /**
     * Determine the path from the DeliveryPoint to an InboundPoint (destinations1).
     * Make the necessary reservations
     * @param location
     * @param tick
     * @return tick of arrival
     */
    private int buildDestinationsDeliveryToInbound(Point location, int tick){
        RoadModel rm = getRoadModel();
        for(Point p: rm.getShortestPathTo(location,pickUpLocation)) destinations1.add(p);
        ArrayList<Integer> ticks = calculateTicksDeliveryToInbound(destinations1,tick);

        if(Point.distance(destinations1.get(0),previousDP) == 0){
            Reservation res = new Reservation(this,tick+1, tick +19,counter,previousDP);
            allReservations.add(res);
        }

        destinations1.remove(0);//first element is the current location

        int end = ticks.get(ticks.size()-1);
        Reservation res = new Reservation(this,end-21,end,counter,pickUpLocation);
        allReservations.add(res);
        
        //make reservations
        ArrayList<Point> reservationsPoints = getPointsToReservateDeliveryToInbound(destinations1);
        if(reservationsPoints.size() > 0) {
            for (int i = 0; i < reservationsPoints.size(); i++) {
                try{
                    int t = ticks.get(destinations1.indexOf(reservationsPoints.get(i))+1);
                    Reservation r = new Reservation(this, t - 5, t+5,
                            counter,reservationsPoints.get(i));
                    allReservations.add(r);
                } catch (Exception e){ }
            }
        }
        return ticks.get(ticks.size()-1);
    }

    /**
     * Determine which points to make a reservation (Delivery To Inbound)
     * @param pts
     * @return
     */
    private ArrayList<Point> getPointsToReservateDeliveryToInbound(ArrayList<Point> pts){
        ArrayList<Point> temp = new ArrayList<>();
        for(Point p: pts){
            if(p.y == 5 && p.x >20){
                temp.add(p);
            } else{
                return temp;
            }
        }
        return null;
    }

    /**
     * Determine the course of ticks when traveling from location (during state = inbound)
     * to one of the endpoints (4 different entries to the assembly)
     * @param location from
     * @param tick
     * @param endPoints to
     * @return
     */
    private ArrayList<ArrayList<Integer>> buildDestinationsInboundToAssembly(Point location, int tick, ArrayList<Crossroad> endPoints){
        RoadModel rm = getRoadModel();
        ArrayList<ArrayList<Integer>> durations = new ArrayList<>();
        for(Point destination: endPoints) {
            ArrayList<Point> temp = new ArrayList<>();
            for (Point p : rm.getShortestPathTo(location, destination)) temp.add(p);
            ArrayList<Integer> ticks = calculateTicksInboundToAssembly(temp,tick);

            durations.add(ticks);
        }
        return durations;
    }

    /**
     * Make the necessary reservation for traveling from the inbound to the assembly
     * @param ticks
     * @param points
     */
    private void reservateInboundToAssembly(ArrayList<Integer> ticks, ArrayList<Point> points){
        Point p = points.get(0);
        points.remove(0);
        ArrayList<Point> toReservate = toReserveCrossRoads(points);
        if(toReservate != null) {
            toReservate.add(0, p);
            points.add(0, p);
            if (Point.distance(points.get(0), pickUpLocation) == 0) {
                Reservation r = new Reservation(this, ticks.get(0) + 1, ticks.get(0) + 29, counter, pickUpLocation);
                allReservations.add(r);
            }
            for (int i = 1; i < toReservate.size(); i++) {
                double moment = ticks.get(points.indexOf(toReservate.get(i)));
                Reservation res = new Reservation(this, moment - 5, moment + 5, counter, toReservate.get(i));
                allReservations.add(res);
            }
        }
    }

    /**
     * Determine which crossroads need to be reserved when traveling from the inbound to the assembly
     * @param pts
     * @return
     */
    private ArrayList<Point> toReserveCrossRoads(ArrayList<Point> pts){
        ArrayList<Point> temp = new ArrayList<>();
        for(Point p: pts){
            if(p.y == 5 && p.x != 5){
                temp.add(p);
            } else{
                return temp;
            }
        }
        return null;
    }

    /**
     * Build the path for the assembly when you already are in the assembly and determine which path is best
     * based on time of arrival at the deliveryPoint and the total pheromone cost.
     * @param cross
     * @return tick when done assembling
     */
    private double buildDestinationsAssemblyDuring(Crossroad cross){
        ArrayList<ArrayList<Crossroad>> possiblePaths = new ArrayList<>();;
        for(ArrayList<Crossroad> pts: factory.findPossiblePaths(cross,currentTask)){
            possiblePaths.add(pts);
        }

        try{
            destinations3 = possiblePaths.get(rng.nextInt(possiblePaths.size()));
        } catch (Exception e){
            factory.findPossiblePaths(cross,currentTask);

        }

        // check which path is the shortest depending on time of arrival at DeliveryPoint and total pheromone cost
        ArrayList<Double> ticksDelivery = new ArrayList<>();
        for(ArrayList<Crossroad> crs: possiblePaths){
            ArrayList<Point> temp = new ArrayList<>();
            for(Point p: getRoadModel().getShortestPathTo(crs.get(crs.size()-1),deliveryLocation)) temp.add(p);

            ArrayList<Integer> ar1 = calculateTicksAssemblyDuring(crs,counter);
            ArrayList<Integer> ar = calculateTicksAssemblyToDelivery(temp,ar1.get(ar1.size()-1));

            double totalPher = calculatePheromones(crs);
            double pathScore = ar.get(ar.size()-1)+totalPher;

            ticksDelivery.add(pathScore);
        }

        double min = 999999999;
        int index = -1;
        for(int i = 0; i < ticksDelivery.size(); i++){
            if(ticksDelivery.get(i) < min){
                min = ticksDelivery.get(i);
                index = i;
            }
        }

        destinations3 = possiblePaths.get(index);


        ArrayList<Integer> ticks = calculateTicksAssemblyDuring(destinations3,counter);
        destinations3.remove(0);


        for (int j = 0; j < destinations3.size(); j++) {
            double moment = ticks.get(j+1);
            Crossroad cr = destinations3.get(j);
            if (cr.x > 5) {
                if (cr.assemblyPointPresent()) {
                    if (!needToVisitAssembly(cr)) {
                        //If you don't need to visit the AP => just make a reservation for the crossroad
                        Reservation res = new Reservation(this, moment - 5, moment+ 5, counter, cr);
                        allReservations.add(res);
                    } else { //if you need to visit the AP => reservate the crossroad when driving out of AP and the AP itself
                        Reservation res2 = new Reservation(this, moment - 32-5, moment+ 5, counter,
                                cr.getAssemblyPoint());
                        Reservation res3 = new Reservation(this, moment- 5, moment+ 5, counter, cr);
                        allReservations.add(res2);
                        allReservations.add(res3);
                    }
                } else {
                    Reservation res = new Reservation(this, moment - 5, moment + 5, counter, cr);
                    allReservations.add(res);
                }

            }
        }
        return ticks.get(ticks.size()-1);
    }

    /**
     * Calculate the remaining course of ticks when assembling
     * @param crs
     * @param startTick
     * @return
     */
    private ArrayList<Integer> calculateTicksAssemblyDuring(ArrayList<Crossroad> crs, int startTick){
        ArrayList<Integer> temp = new ArrayList<>();
        int d = startTick;
        temp.add(d);

        for(int i = 1; i < crs.size();i++){
            int duration = temp.get(i-1);
            Point p0 = crs.get(i-1);
            Point p1 = crs.get(i);
            double xDiff = abs(p0.x - p1.x);
            double yDiff = abs(p0.y - p1.y);
            if(yDiff == 0 && xDiff == 5){duration+=37;}
            else if((xDiff == 0 && yDiff ==10) ||(xDiff == 10 && yDiff ==0) ){duration+=73;}
            else if((xDiff == 10 && yDiff ==10)){duration += 103;}
            else if((xDiff == 8 && yDiff ==0)){duration += 59;}
            else if((xDiff == 5 && yDiff ==10)){duration += 82;}
            else {
                System.out.println("length not found");}

            if(p0 instanceof Crossroad){
                Crossroad cr = (Crossroad)p1;
                if(needToVisitAssembly(cr)) {
                    duration = factory.nextFreeAssembly(cr, duration, this)+32;
                }
            }
            temp.add(duration);
        }
        return temp;
    }

    /**
     * Build the path for the assembly when you still need to start assembling and determine which path is best
     * based on time of arrival at the deliveryPoint and the total pheromone cost.
     * @param startSearch
     * @param d2
     * @return
     */
    private double buildDestinationsAssembly(ArrayList<Crossroad> startSearch,ArrayList<Integer> d2) {
        ArrayList<ArrayList<Crossroad>> possiblePaths = new ArrayList<>();;

        for (int i = 0; i < startSearch.size(); i++) {
            Crossroad startRoad = startSearch.get(i);
            for(ArrayList<Crossroad> pts: factory.findPossiblePaths(startRoad,currentTask)){
                possiblePaths.add(pts);
            }
        }

        // check which path is best based on time of arrival at the delivery point and total pheromone cost
        ArrayList<Double> ticksDelivery = new ArrayList<>();
        for(ArrayList<Crossroad> crs: possiblePaths){
            int start = startSearch.indexOf(crs.get(0));
            ArrayList<Point> temp = new ArrayList<>();
            for(Point p: getRoadModel().getShortestPathTo(crs.get(crs.size()-1),deliveryLocation)) temp.add(p);

            ArrayList<Integer> ar1 = calculateTicksAssembly(crs,d2.get(start));
            ArrayList<Integer> ar = calculateTicksAssemblyToDelivery(temp,ar1.get(ar1.size()-1));

            double totalPher = calculatePheromones(crs);
            double pathScore = ar.get(ar.size()-1)+totalPher;

            ticksDelivery.add(pathScore);

        }

        double min = 999999999;
        int index = -1;
        for(int i = 0; i < ticksDelivery.size(); i++){
            if(ticksDelivery.get(i) < min){
                min = ticksDelivery.get(i);
                index = i;
            }
        }

        destinations3 = possiblePaths.get(index);

        int i = startSearch.indexOf(destinations3.get(0));
        ArrayList<Integer> finalTicks = calculateTicksAssembly(destinations3,d2.get(i));

        int begin = 0;
        if(destinations3.get(0).x == 5)  begin = 1;

        for (int j = begin; j < destinations3.size(); j++) {
            double moment = 0;
            try{
                moment = finalTicks.get(j);
            } catch (Exception e){
                int a = 0;
            }
            Crossroad cr = destinations3.get(j);
            if (cr.x > 5) {
                if (cr.assemblyPointPresent()) {
                    if (!needToVisitAssembly(cr)) {//If you don't need to visit the AP => just make a reservation for the crossroad
                        Reservation res = new Reservation(this, moment - 5, moment+ 5, counter, cr);
                        allReservations.add(res);
                    } else { //if you need to visit the AP => reservate the crossroad when driving out of AP and the AP itself
                        Reservation res2 = new Reservation(this, moment - 32-5, moment+ 5, counter, cr.getAssemblyPoint());// AP
                        Reservation res3 = new Reservation(this, moment- 5, moment+ 5, counter, cr);//drive out of AP
                        allReservations.add(res2);
                        allReservations.add(res3);
                    }
                } else {
                    Reservation res = new Reservation(this, moment - 5, moment + 5, counter, cr);
                    allReservations.add(res);
                }

            }
        }


        return finalTicks.get(finalTicks.size()-1);

    }

    /**
     * Calculate how long you have to wait (not used for assembly)
     * @param res
     * @param tick
     * @return
     */
    private int howLongToWait(Reservation res,int tick){
        if(res.containsTick(tick)){
            return 20+res.getStopTick()-tick;
        }
        return 0;
    }

    /**
     * Build the path towards the deliveryPoint and make the necessary reservations
     * @param location
     * @param deliveryLocation
     * @param tick
     * @return
     */
    private double buildDestinationsAssemblyToOut(Point location, Point deliveryLocation,int tick){
        RoadModel rm = getRoadModel();
        destinations4.clear();
        for(Point p: rm.getShortestPathTo(location,deliveryLocation)) destinations4.add(p);

        ArrayList<Integer> ticks = calculateTicksAssemblyToDelivery(destinations4,tick);

        doAssemblyToDeliveryReservations(destinations4,ticks);

        destinations4.remove(0);

        return ticks.get(ticks.size()-1);
    }

    /**
     * Add the necessary reservations when traveling from the end of the assembly towards the delivery point
     * @param pts
     * @param ticks
     */
    private void doAssemblyToDeliveryReservations(ArrayList<Point> pts, ArrayList<Integer> ticks){

        int moment = ticks.get(ticks.size()-1);
        Reservation res = new Reservation(this,moment-23,moment+9,counter,deliveryLocation);
        allReservations.add(res);

        for(int i = 1; i < pts.size(); i++){
            Point p = pts.get(i);
            if(p.x == 45 ){//last points that needs a reservation
                if(p.y != 39){
                    Reservation res1 = new Reservation(this,ticks.get(i)-5, ticks.get(i)+5, counter,p);
                    allReservations.add(res1);
                }
                return;
            } else {
                Reservation res2 = new Reservation(this,ticks.get(i)-5,ticks.get(i)+5,
                        counter,p);
                allReservations.add(res2);
            }
        }

    }

    /**
     * Used to refresh the reservations
     */
    private void sendReservation(){
        allReservations.sort(Comparator.comparing(r -> r.getStartTick()));
        for(Reservation res: allReservations){
            res.setTimpSamp(counter);
            factory.makeReservations(res,res.getPoint());
        }
    }

    /**
     * @param p
     * @return true if the agv needs to visit the assembly located at point p
     */
    private boolean needToVisitAssembly(Point p){
        try{
            Crossroad cr = (Crossroad)p;
            if(cr.assemblyPointPresent()){
                int nr = cr.getAssemblyPoint().getStationNr();
                if(currentTask.getTasks().get(nr)){
                    return true;
                }
            }
        } catch (Exception e){}
        return false;
    }

    /**
     * @return true if burnIn periode is over
     */
    private boolean burnIn(){
        if(burnInTick == 0){
            return true;
        }
        burnInTick--;
        return false;
    }

    /**
     * @param crs
     * @param startTick
     * @return course of ticks in assembly
     */
    private ArrayList<Integer> calculateTicksAssembly(ArrayList<Crossroad> crs, int startTick){
        ArrayList<Integer> temp = new ArrayList<>();
        int d = startTick;

        temp.add(d);

        for(int i = 1; i < crs.size();i++){
            int duration = temp.get(i-1);
            Point p0 = crs.get(i-1);
            Point p1 = crs.get(i);
            double xDiff = abs(p0.x - p1.x);
            double yDiff = abs(p0.y - p1.y);
            if(yDiff == 0 && xDiff == 5){duration+=37;}
            else if((xDiff == 0 && yDiff ==10) ||(xDiff == 10 && yDiff ==0) ){duration+=73;}
            else if((xDiff == 10 && yDiff ==10)){duration += 103;}
            else if((xDiff == 8 && yDiff ==0)){duration += 59;}
            else if((xDiff == 5 && yDiff ==10)){duration += 82;}
            else {
                System.out.println("length not found");}

            if(p0 instanceof Crossroad){
                Crossroad cr = (Crossroad)p1;
                if(needToVisitAssembly(cr)) {
                    duration = factory.nextFreeAssembly(cr, duration, this)+32;
                }
            }
            temp.add(duration);
        }
        return temp;
    }

    /**
     * @param crs
     * @param startTick
     * @return course of ticks when traveling from the assembly to the delivery point
     */
    private ArrayList<Integer> calculateTicksAssemblyToDelivery(ArrayList<Point> crs, int startTick){
        int d = startTick;
        ArrayList<Integer> ticks = new ArrayList<>();
        ticks.add(startTick);

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
            else if((xDiff ==3  && yDiff ==0)){d += 22;}
            else if((xDiff ==2  && yDiff ==0)){d += 15;}
            else {System.out.println("length not found");}

            if(factory.getRervations(crs.get(i),d,this)!= null){
                d += howLongToWait(factory.getRervations(crs.get(i),d,this),d);
            }

            if(i == crs.size()-1){
                d += 10; //+10 because delivery time
            }

            ticks.add(d);
        }
        return ticks;
    }

    /**
     * @param crs
     * @param startTick
     * @return course of ticks when traveling from the delivery point to the inbound point
     */
    private ArrayList<Integer> calculateTicksDeliveryToInbound(ArrayList<Point> crs, int startTick){
        ArrayList<Integer> ticks = new ArrayList<>();
        int d = startTick;
        ticks.add(d);

        for(int i = 1; i < crs.size();i++){
            Point p0 = crs.get(i-1);
            Point p1 = crs.get(i);
            double xDiff = abs(p0.x - p1.x);
            double yDiff = abs(p0.y - p1.y);

            if(xDiff == 0 && yDiff ==2){ d+= 15; }
            else if(p0 instanceof Crossroad && p1 instanceof InboundPoint){d+=17;}
            else if((xDiff == 1 && yDiff ==0)){d += 8;}
            else if((xDiff == 6 && yDiff ==0)){d += 44;}
            else if((xDiff == 2 && yDiff ==0)){d += 15;}
            else if((xDiff ==3  && yDiff ==0)){d += 22;}
            else if(yDiff == 0 && xDiff == 5){d+=37;}
            else if((xDiff == 0 && yDiff ==4)){d += 30;}
            else if((xDiff == 0 && yDiff ==10)){d+=73;}





            else {
                System.out.println("Distance not found");}

            if(factory.getRervations(crs.get(i),d,this)!= null){
                d += howLongToWait(factory.getRervations(crs.get(i),d,this),d);
            }
            ticks.add(d);
        }

        return ticks;
    }

    private ArrayList<Integer> calculateTicksInboundToAssembly(ArrayList<Point> crs,int startTick){
        ArrayList<Integer> ticks = new ArrayList<>();
        ticks.add(startTick);
        int d = startTick;

        for(int i = 1; i < crs.size();i++){
            Point p0 = crs.get(i-1);
            Point p1 = crs.get(i);
            double xDiff = abs(p0.x - p1.x);
            double yDiff = abs(p0.y - p1.y);

            if(xDiff == 0 && yDiff ==2){d+= 24;}
            else if((xDiff == 1 && yDiff ==0)){d += 8;}
            else if(yDiff == 0 && xDiff == 5 && p0.x - p1.x < 0){d+=36;}
            else if(yDiff == 0 && xDiff ==5 && p0.x - p1.x > 0){d+=37;}
            else if((xDiff == 0 && yDiff ==4)){d += 29;}
            else if((xDiff == 0 && yDiff ==10) ){d+=72;}
            else{System.out.println("Distance not found");}

            if(factory.getRervations(crs.get(i),d,this)!= null){
                d += howLongToWait(factory.getRervations(crs.get(i),d,this),d);
            }
            ticks.add(d);
        }

        return ticks;

    }

    /**
     * Loop over all message and handle them individually
     */
    private void handleMessages() {
        ImmutableList<Message> messages = comDevice.get().getUnreadMessages();

        for (Message mes : messages) {
            WaitMessage wm = (WaitMessage)mes.getContents();
            handleMessage(wm,mes.getSender());
        }
    }

    /**
     * Handle a message
     * @param wm
     * @param sender
     */
    private void handleMessage(WaitMessage wm, CommUser sender){
        if(wm.getMessage() == "Need to wait AS" && waitComDevice == null) {
            Crossroad cr = wm.getCr();
            AssemblyPoint as = cr.getAssemblyPoint();
            Point pos = getPosition().get();
            double distance = Point.distance(as, pos);
            if (distance < wm.getDistance()) {
                if (cr.x == as.x && pos.x == cr.x) {
                    if (as.y < cr.y) {
                        if (as.y < pos.y && pos.y < cr.y) {
                            WaitMessage message = new WaitMessage("You need to wait AS", cr);
                            waitComDevice = sender;
                            waitCrossroad = cr;
                            comDevice.get().send(message, waitComDevice);

                        }
                    } else {
                        if (cr.y < pos.y && pos.y < as.y) {
                            WaitMessage message = new WaitMessage("You need to wait AS", cr);
                            waitComDevice = sender;
                            waitCrossroad = cr;
                            comDevice.get().send(message, waitComDevice);
                        }
                    }
                }

                if (cr.y == as.y && pos.y == cr.y) {
                    if (as.y < cr.y) {
                        if (as.x < pos.x && pos.x < cr.x) {
                            WaitMessage message = new WaitMessage("You need to wait AS", cr);
                            waitComDevice = sender;
                            waitCrossroad = cr;
                            comDevice.get().send(message, waitComDevice);
                        }
                    } else {
                        if (cr.x < pos.x && pos.x < as.x) {
                            WaitMessage message = new WaitMessage("You need to wait AS", cr);
                            waitComDevice = sender;
                            waitCrossroad = cr;
                            comDevice.get().send(message, waitComDevice);
                        }
                    }
                    return;
                }
            }
        }
        if(wm.getMessage() == "Need to wait IP" && waitComDevice == null){
            InboundPoint ip = wm.getIp();
            Point pos = getPosition().get();

            if(pos.x == ip.x){
                if(ip.y <= pos.y && pos.y < ip.y+2){
                    WaitMessage message = new WaitMessage("You need to wait IP", ip);
                    waitComDevice =sender;
                    waitInbound = ip;
                    comDevice.get().send(message, waitComDevice);
                }
            }
        }

        if(wm.getMessage() == "Need to wait DP" && waitComDevice == null){
            DeliveryPoint dp = wm.getDp();
            Point pos = getPosition().get();

            if(pos.x == dp.x){
                if(dp.y <= pos.y && pos.y < dp.y+2){
                    WaitMessage message = new WaitMessage("You need to wait DP", dp);
                    waitComDevice =sender;
                    waitDelivery = dp;
                    comDevice.get().send(message, waitComDevice);
                }
            }
        }
        if(wm.getMessage() == "Need to wait CR"){
            if(state == AGVState.DRIVINGAWAYASSEMBLY || currentDestination == wm.getCr()){
                double dis = Point.distance(currentDestination,getPosition().get());
                if(dis <= wm.getDistance() && dis <= 2) {
                    WaitMessage message = new WaitMessage("You need to wait CR", wm.getCr());
                    waitComDevice = sender;
                    waitCrossroad = wm.getCr();
                    comDevice.get().send(message, waitComDevice);
                }
            }
        }

        if(wm.getMessage() == "You need to wait AS"){
            previousState = state;
            state = AGVState.WAIT;
            return;
        }

        if(wm.getMessage() == "You need to wait IP"){
            previousState = state;
            state = AGVState.WAIT;
            return;
        }

        if(wm.getMessage() == "You need to wait DP"){
            previousState = state;
            state = AGVState.WAIT;
            return;
        }
        if(wm.getMessage() == "You need to wait CR"){
            previousState = state;
            state = AGVState.WAIT;

            return;
        }

        if(wm.getMessage() == "Stop waiting AS" || wm.getMessage() == "Stop waiting IP"){
            state = previousState;
        }

        if(wm.getMessage() == "Stop waiting DP"){
            state = previousState;
        }
    }

    /**
     * Remove ExploreInfo in pheromones from the same assembly point as info
     * @param info
     */
    private void removeExploreInfo(ExploreInfo info){
        for(ExploreInfo ex: pheromones ){
            if(Point.distance(ex.getSender(),info.getSender()) == 0){
                pheromones.remove(ex);
                return;
            }
        }
    }

    /**
     * @param ap
     * @return pheromone of ap
     */
    private double getPheromone(AssemblyPoint ap){
        for(ExploreInfo info: pheromones){
            if(Point.distance(ap,info.getSender())==0) return info.getPheromone();
        }
        return 0;
    }

    /**
     * @param crs
     * @return Total pheromone cost of when traversing crs
     */
    private double calculatePheromones(ArrayList<Crossroad> crs){
        double d = 0;
        for(Crossroad cr: crs){
            if(needToVisitAssembly(cr)) d += getPheromone(cr.getAssemblyPoint());
        }
        return d;
    }

}

