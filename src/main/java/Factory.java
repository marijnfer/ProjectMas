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

    private int deliveryCounter;

    public Factory(Simulator simulator){
        this.simulator = simulator;
        roadModel = getRoadModel();

        crossroads = new ArrayList<>();
        deliverPoints = new ArrayList<>();
        inboundPoints= new ArrayList<>();
        assemblyPoints = new ArrayList<>();
        connections = new ArrayList<>();

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
        /*
        if(deliveryCounter == deliverPoints.size()-1){
            deliveryCounter = 0;
            return deliverPoints.get(0);
        } else {
            deliveryCounter++;
            return deliverPoints.get(deliveryCounter);
        }*/
        /*
        if(deliveryCounter == crossroads.size()-1){
            deliveryCounter = 0;
            return crossroads.get(0);
        } else {
            deliveryCounter++;
            return crossroads.get(deliveryCounter);
        }
        */
        if(deliveryCounter == assemblyPoints.size()-1){
            deliveryCounter = 0;
            return assemblyPoints.get(0);
        } else {
            deliveryCounter++;
            return assemblyPoints.get(deliveryCounter);
        }
    }

    public void setSearchInboundPoint(InboundPoint ip, boolean bool){
        for(InboundPoint i : inboundPoints){
            if(i.equals(ip)){
                if(i.getStored()){
                    i.setStored(bool);
                    System.out.println(bool);
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
        return temp;
    }

    public ArrayList<Path> sendAnts(Iterator cross){
        ArrayList<Path> paths = new ArrayList<>();
        while (cross.hasNext()){
            Crossroad cr = (Crossroad)cross.next();


        }
        return paths;
    }

    public void addConnect(Crossroad cr){
        connections.add(new Connect(cr));
        System.out.println("con added");
    }

    public void buildConnects() {
        for (Connect con : connections) {
            for(Connection c: getRoadModel().getGraph().getConnections()){
                if(searchCrossroad(c.to()) != null){
                    Crossroad cross = searchCrossroad(c.to());
                    if (Point.distance(c.from(),con.getCrossroad()) == 0 ) {
                        con.addCoupledCrossroad(searchCrossroad(c.to()));
                    }
                }
            }
        }
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

        }
        return null;
    }
}
