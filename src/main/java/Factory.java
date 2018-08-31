import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModelImpl;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Point;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

public class Factory {

    private final CollisionGraphRoadModelImpl roadModel;
    private final Simulator simulator;
    private ArrayList<Crossroad> crossroads;
    private ArrayList<DeliveryPoint> deliverPoints;
    private ArrayList<InboundPoint> inboundPoints;
    private ArrayList<AssemblyPoint> assemblyPoints;
    private ArrayList<Connect>  connections;
    private int deliveryCounter;

    private ArrayList<Connect> allConnections;

    public Factory(Simulator simulator){
        this.simulator = simulator;
        roadModel = getRoadModel();
        crossroads = new ArrayList<>();
        deliverPoints = new ArrayList<>();
        inboundPoints= new ArrayList<>();
        assemblyPoints = new ArrayList<>();
        connections = new ArrayList<>();
        allConnections = new ArrayList<>();
        getPoints();
    }


    private CollisionGraphRoadModelImpl getRoadModel(){
        for(Object o: simulator.getModels()){
            if(o instanceof CollisionGraphRoadModelImpl){
                return (CollisionGraphRoadModelImpl) o;
            }
        }
        return null;
    }

    /**
     * @return All points in the graph
     */
    public Iterator<Point> getAllPointsIterator(){
        Iterator<Point> it = roadModel.getGraph().getNodes().iterator();
        return it;
    }

    /**
     * Place all important points in the graph in the appropriate arraylist
     */
    private void getPoints(){
        Iterator<Point> it = getAllPointsIterator();
        while(it.hasNext()){
            Point p = it.next();
            if( p instanceof Crossroad){
                crossroads.add((Crossroad)p);
            }
            if( p instanceof InboundPoint){
                inboundPoints.add((InboundPoint)p);
            }
            if( p instanceof DeliveryPoint){
                deliverPoints.add((DeliveryPoint)p);
            }

            if(p instanceof AssemblyPoint){
                assemblyPoints.add((AssemblyPoint)p);
            }
        }
    }

    public ArrayList<InboundPoint> getInboundPoints(){
        return inboundPoints;
    }

    public ArrayList<Crossroad> getCrossroads() {
        return crossroads;
    }

    public ArrayList<AssemblyPoint> getAssemblyPoints() {
        return assemblyPoints;
    }

    /**
     * Alternate between DeliveryPoints for task generation.
     * (Distribute the DeliveryPoints evenly among all tasks)
     * @return
     */
    public Point nextDeliveryPoint(){
        if(deliveryCounter == deliverPoints.size()-1){
            deliveryCounter = 0;
            return deliverPoints.get(0);
        } else {
            deliveryCounter++;
            return deliverPoints.get(deliveryCounter);
        }
    }

    /**
     * Generate a tasks. The first station always need to be visited. Other stations
     * are generated according to some probability.
     * @return
     */
    public ArrayList<Boolean> taskGenerator(){
        ArrayList<Boolean> temp = new ArrayList<>();
        //Always do task 0
        temp.add(true);
        for(int i = 1; i < 6; i++){
            double random = simulator.getRandomGenerator().nextDouble();
            switch (i){
                case 1:
                    if(random <= 0.8){temp.add(true);} else{temp.add(false);} break;
                case 2:
                    if(random <= 0.5){temp.add(true);} else{temp.add(false);} break;
                case 3:
                    if(random <= 0.5){temp.add(true);} else{temp.add(false);} break;
                case 4:
                    if(random <= 0.5){temp.add(true);} else{temp.add(false);} break;
                case 5:
                    if(random <= 0.5){temp.add(true);} else{temp.add(false);} break;
            }
        }
        return temp;
    }

    /**
     * Determines all possible paths that can be traveled for completing a task
     * @param crossroad Start the search from this crossroad
     * @param task The stations that need to be visited depend on the task.
     * @return Arraylist of possible paths
     */
    public ArrayList<ArrayList<Crossroad>> findPossiblePaths(Crossroad crossroad,Task task){

        ArrayList<ArrayList<Crossroad>> paths = new ArrayList<>();
        ArrayList<ArrayList<Crossroad>> tempCr = new ArrayList<>();
        ArrayList<Double> ticks = new ArrayList<>();
        ArrayList<Double> tempTicks = new ArrayList<>();

        ArrayList<Crossroad> t = new ArrayList<>();
        t.add(crossroad);tempCr.add(t);tempTicks.add(0.0);

        while(tempCr.size() !=0){
            ArrayList<Crossroad> first = tempCr.get(0);
            tempCr.remove(0);
            ArrayList<Crossroad> con = findConnections(first.get(first.size()-1));

            if(con != null) {
                for (Crossroad cr : con) {
                    if (viableConnection(task, first, cr)) {
                        ArrayList<Crossroad> copy = new ArrayList<>(first);
                        copy.add(cr);
                        if (cr.assemblyPointPresent() && cr.getAssemblyPoint().getStationNr() == task.lastStation()) {
                            paths.add(copy);
                        } else {
                            tempCr.add(copy);
                        }

                    }
                }
            }

        }
        return paths;
    }

    /**
     * Each possible path contains only one of each station (in the same order).
     * Return true if the current crossroad doesn't lie on a shortcut that skips a station
     * that needs to be visited. Also returns true if that crossroad isn't connected to any
     * station (keep search). Suppose that this is a bad path, this will return false in the
     * next call of viableConnection with the connected crossroad.
     *
     * @param task
     * @param current partial path
     * @param toAdd crossroad to add to current
     * @return
     */
    private boolean viableConnection(Task task,ArrayList<Crossroad> current, Crossroad toAdd){
        int lastStation = lastStation(current);
        int nextStation = task.nextStation(lastStation);

        if(!toAdd.assemblyPointPresent()){
            return true;
        } else{
            int toAddStation = toAdd.getAssemblyPoint().getStationNr();
            if(toAddStation <= nextStation){return true;}
        }
        return false;
    }

    /**
     * Search the last station visited on crs
     * @param crs
     * @return
     */
    private int lastStation(ArrayList<Crossroad> crs){
        int lastIndex = crs.size() -1;
        int station = -1;
        while(station == -1){
            if(crs.get(lastIndex).assemblyPointPresent()){
                return crs.get(lastIndex).getAssemblyPoint().getStationNr();
            } else{
                lastIndex--;
            }
            if(lastIndex == -1){
                return lastIndex;
            }
        }
        return station;
    }

    /**
     * @param cr
     * @return All crossroads that are connected to cr
     */
    public ArrayList<Crossroad> findConnections(Crossroad cr){
        for(Connect con: connections){
            if(Point.distance(con.getCrossroad(),cr)==0){
                return  con.getCoupled();
            }
        }
        return null;
    }

    public void addConnect(Crossroad cr){
        if(cr.y > 5 && cr.x < 45){
            connections.add(new Connect(cr));
        }
    }

    /**
     * Create the list of connections. (easy look up of all connected crossroads)
     */
    public void buildConnects() {
        for (Connect con : connections) {
            for(Connection c: getRoadModel().getGraph().getConnections()){
                if(searchCrossroad(c.to()) != null){
                    if (Point.distance(c.from(),con.getCrossroad()) == 0) {
                        if(c.to().x != 5)
                        con.addCoupledCrossroad(searchCrossroad(c.to()));
                    }
                }
            }
        }
    }

    /**
     * @param p
     * @return the crossroad with the same location as p
     */
    private Crossroad searchCrossroad(Point p){
        for(Crossroad cr : crossroads){
            if(Point.distance(cr,p)==0){
                return cr;
            }
        }
        return null;
    }

    /**
     *  Fill the list of connections. (easy look up of all connected crossroads)
     */
    public void buildAllconnections(){
        for(Connect c: connections){
            for(Crossroad cr: c.getCoupled()){
                Connect con = new Connect(c.getCrossroad());
                con.addCoupledCrossroad(cr);
                allConnections.add(con);
            }
        }
    }

    /**
     * Make a reservation with the appropriate AssemblyPoint, Crossroad, InboundPoint or DeliveryPoint.
     * @param res
     * @param p
     */
    public void makeReservations(Reservation res, Point p){
        if(p instanceof Crossroad){
            Crossroad cr = searchCrossroad((Crossroad)p);
            cr.makeReservation(res);
        }
        if(p instanceof AssemblyPoint){
            AssemblyPoint ap = (AssemblyPoint)p;
            ap.makeReservation(res);
        }
        if(p instanceof InboundPoint){
            ((InboundPoint) p).makeReservation(res);
        }
        if(p instanceof DeliveryPoint){
            ((DeliveryPoint) p).makeReservation(res);
        }
    }

    /**
     * @param p
     * @param tick
     * @param agv
     * @return Return a reservation of the appropriate AssemblyPoint, Crossroad, InboundPoint
     * or DeliveryPoint if it contains tick
     */
    public Reservation getRervations(Point p, int tick, AGV agv){
            if(p instanceof Crossroad){
                Crossroad cr = (Crossroad)p;
                if(cr.getReservationTick(tick) != null){
                    if(cr.getReservationTick(tick).getAgv() != agv){
                        return (cr.getReservationTick(tick));
                    }
                }
        }
            if(p instanceof AssemblyPoint){
                AssemblyPoint cr = (AssemblyPoint)p;
                if(cr.getReservationTick(tick) != null){
                    if(cr.getReservationTick(tick).getAgv() != agv){
                        return (cr.getReservationTick(tick));
                    }
                }            }

            if(p instanceof InboundPoint){
                InboundPoint cr= (InboundPoint)p;
                if(cr.getReservationTick(tick) != null){
                    if(cr.getReservationTick(tick).getAgv() != agv){
                        return (cr.getReservationTick(tick));
                    }
                }            }

            if(p instanceof DeliveryPoint){
                DeliveryPoint cr = (DeliveryPoint)p;
                if(cr.getReservationTick(tick) != null){
                    if(cr.getReservationTick(tick).getAgv() != agv){
                        return (cr.getReservationTick(tick));
                    }
                }
            }

        return null;
    }

    /**
     * @return Points to spawn AGV's
     */
    public ArrayList<Point> getSpawnPoints(){
        Iterator it = getAllPointsIterator();
        ArrayList<Point> temp = new ArrayList<>();
        ArrayList<Point> temp2 = new ArrayList<>();



        while(it.hasNext()){
            Point p = (Point)it.next();
            if(p.y == 5){
                if(p.x >= 30 && p.x < 45) temp.add(p);
            }
        }
        temp.sort(Comparator.comparing(r -> r.x));

        temp2.sort(Comparator.comparing(r -> r.y));
        temp.addAll(temp2);

        return temp;
    }

    /**
     *
     * @param cr
     * @param tick
     * @param agv
     * @return Returns the first moment that the an agv can visit the assembly
     */
    public int nextFreeAssembly(Crossroad cr, int tick, AGV agv){
            return cr.getAssemblyPoint().firstAvailableMoment(tick,agv);
    }

}

