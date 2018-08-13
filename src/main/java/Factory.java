import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModelImpl;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Point;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.Collectors;

public class Factory {

    private final CollisionGraphRoadModelImpl roadModel;
    private final Simulator simulator;

    private ArrayList<Crossroad> crossroads;
    private ArrayList<DeliveryPoint> deliverPoints;
    private ArrayList<InboundPoint> inboundPoints;
    private ArrayList<AssemblyPoint> assemblyPoints;
    private ArrayList<Connect>  connections;
    private ArrayList<Task> tasks;


    private ArrayList<Connect> allConnections;


    private int deliveryCounter;

    public Factory(Simulator simulator){
        this.simulator = simulator;
        roadModel = getRoadModel();

        crossroads = new ArrayList<>();
        deliverPoints = new ArrayList<>();
        inboundPoints= new ArrayList<>();
        assemblyPoints = new ArrayList<>();
        connections = new ArrayList<>();
        tasks = new ArrayList<>();
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

    public Iterator<Point> getAllPointsIterator(){
        Iterator<Point> it = roadModel.getGraph().getNodes().iterator();
        return it;
    }
    public java.util.Set<Point> getAllPoints(){
        return roadModel.getGraph().getNodes();
    }
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

        ArrayList<Crossroad> temp = (ArrayList) crossroads.stream().sorted(Comparator.comparing(Crossroad::getX))
                .collect(Collectors.toList());

        for(int i = 0; i < temp.size()/4-1;i++){
            //Gather comms previous points
            Iterator it1 = temp.subList(i*4,i*4+4).iterator();
            ArrayList<CommUser> cds = new ArrayList<>();
            while(it1.hasNext()){
                Crossroad ap = (Crossroad)it1.next();
                cds.add(ap.getCommUser());
            }

            Iterator it2 = temp.subList(i*4+4,i*4+8).iterator();
            while (it2.hasNext()){
                Crossroad ap = (Crossroad)it2.next();
                ap.setBackwardsReachable(cds);

            }
        }
    }

    public ArrayList<InboundPoint> getInboundPoints(){
        return inboundPoints;
    }

    public ArrayList<Crossroad> getCrossroads() {
        return crossroads;
    }

    public ArrayList<DeliveryPoint> getDeliverPoints() {
        return deliverPoints;
    }

    public ArrayList<AssemblyPoint> getAssemblyPoints() {
        return assemblyPoints;
    }

    public Point nextDeliveryPoint(){
        if(deliveryCounter == deliverPoints.size()-1){
            deliveryCounter = 0;
            return deliverPoints.get(0);
        } else {
            deliveryCounter++;
            return deliverPoints.get(deliveryCounter);
        }
    }

    public void setSearchInboundPoint(InboundPoint ip, boolean bool){
        for(InboundPoint i : inboundPoints){
            if(i.equals(ip)){
                if(i.getStored()){
                    i.setStored(bool);
                }
                return;
            }
        }
        System.out.println("not found");

    }

    public int getFullPOints(){
        int i = 0;
        Iterator it = inboundPoints.iterator();
        while (it.hasNext()){
            InboundPoint ip = (InboundPoint)it.next();
            if(ip.getStored()){
                i++;
            }
        }
        return i;
    }

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

        ArrayList<Boolean> temp2 = new ArrayList<>();
        temp2.add(true);temp2.add(true);temp2.add(true);temp2.add(true);temp2.add(true);temp2.add(true);
        return temp;




        //return temp;
    }

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
                    //check if viable connection
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


    private boolean viableConnection(Task task,ArrayList<Crossroad> current, Crossroad toAdd){
        int lastStation = lastStation(current);
        int nextStation = task.nextStation(lastStation);
        //if no assemblyPoint is present => keep searching
        if(!toAdd.assemblyPointPresent()){
            return true;
        } else{
            int toAddStation = toAdd.getAssemblyPoint().getStationNr();
            if(toAddStation <= nextStation){return true;}
        }
        return false;
    }

    private int lastStation(ArrayList<Crossroad> crs){
        int lastIndex = crs.size() -1;
        int station = -1;
        while(station == -1){
        //AssemblyPoint present => crossroad present
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
        int i = 0;
    }

    private Crossroad searchCrossroad(Point p){
        for(Crossroad cr : crossroads){
            if(Point.distance(cr,p)==0){
                return cr;
            }
        }
        return null;
    }

    public ArrayList<Crossroad> getCrossroadsStation(int nr) {
        ArrayList<Crossroad> temp = new ArrayList<>();
        for(Crossroad c: crossroads){
            if(c.assemblyPointPresent() && c.getAssemblyPoint().getStationNr() == nr){
                temp.add(c);
            }
        }
        return temp;
    }

    public ArrayList<Connect> getConnections() {
        return connections;
    }

    public void buildAllconnections(){
        for(Connect c: connections){
            for(Crossroad cr: c.getCoupled()){
                Connect con = new Connect(c.getCrossroad());
                con.addCoupledCrossroad(cr);
                allConnections.add(con);
            }
        }
    }

    public void addData(Point c1, Point c2,int ticks){
        try{
            Connect c = searchAllconnection(c1,c2);
            c.addTravelTime(ticks);
        } catch (Exception e){}
    }

    public Connect searchAllconnection(Point c1, Point c2){
        for(Connect con: allConnections){
            if(Point.distance(c1,con.getCrossroad())==0){
                if(Point.distance(c2,con.getCoupled().get(0))==0){
                    return con;
                }
            }
        }
        return null;
    }

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
                }            }


        return null;
    }

    private Crossroad searchCrossRoad(Crossroad cr){
        for(Crossroad c: crossroads){
            if(Point.distance(c,cr)==0){
                return  c;
            }
        }
        return null;
    }

    private AssemblyPoint searchAssembly(AssemblyPoint as){
        for(AssemblyPoint a: assemblyPoints){
            if(Point.distance(a,as)==0){
                return  a;
            }
        }
        return null;
    }

    public Connect searchConnect(Crossroad cr){
        for(Connect con: connections){
            if(Point.distance(cr,con.getCrossroad()) == 0) return con;
        }
        return null;
    }
}

