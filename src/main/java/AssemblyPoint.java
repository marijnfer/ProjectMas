import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Iterator;

public class AssemblyPoint extends Point implements CommUser, TickListener {

    private static int RANGE = 50;
    private static int RESERVATIONRESET = 10;

    private Optional<CommDevice> comDevice;
    private ArrayList<Point> backwardsReachable;
    private int stationNr;
    private ArrayList<AssemblyPoint> reachable;

    private int resources;
    private ArrayList<ExploreInfo> exploreTree;
    private ArrayList<Reservation> reservations;
    private int tickCounter = 0;

    private boolean print = true;



    public AssemblyPoint(double px, double py, int stationNr){
        super(px,py);
        backwardsReachable = new ArrayList<>();
        reachable = new ArrayList<>();
        exploreTree = new ArrayList<>();
        this.stationNr = stationNr;
        resources = 10;
        reservations = new ArrayList<>();
    }


    @Override
    public void tick(TimeLapse timeLapse){
        tickCounter++;
        handleMessages();
        sendInfo();
        updateReservations();
    }

    private void handleMessages(){
        ImmutableList<Message> messages = comDevice.get().getUnreadMessages();
        for(Message m: messages){
            if(m.getContents() instanceof ExploreMessage){
                ExploreMessage em = (ExploreMessage)m.getContents();
                switch (em.getMessage()){
                    case "Build tree":
                        updateExploreInfo(em);

                }
            }
        }

    }

    private void updateExploreInfo(ExploreMessage em){
        ExploreInfo ei = em.getInfo();
        int index = containsPoint(ei.getSender());
        if(index !=-1){
            exploreTree.remove(index);
        }
        exploreTree.add(ei);

    }

    /**
     *
     * @param p
     * @return -1 if exploreTree doesn't contain the point otherwise the index
     */
    private int containsPoint(Point p){
        for(int i = 0; i<exploreTree.size(); i++){
            ExploreInfo ei = exploreTree.get(i);
            /*
            System.out.print(Point.distance(ei.getSender(),p));
            System.out.print(p);
            System.out.print("  ");
            System.out.print(exploreTree.size());
            System.out.print("  ");
            System.out.println(this);
            */
            if(Point.distance(ei.getSender(),p) == 0){
                return  i;
            }
        }

        return -1;
    }

    /**
     * Send explore information to all backwards reachable nodes
     */
    private void sendInfo(){
        /*
        Iterator it = backwardsReachable.iterator();
        while (it.hasNext()){
            AssemblyPoint ap = (AssemblyPoint)it.next();
            ExploreMessage em = new ExploreMessage("Build tree",new ExploreInfo(this,resources,exploreTree));
            comDevice.get().send(em,ap);
        }
        */

        for(Point p:  backwardsReachable){
            if(p instanceof AssemblyPoint){
                AssemblyPoint ap = (AssemblyPoint)p;
                ExploreMessage em = new ExploreMessage("Build tree",new ExploreInfo(this,resources,exploreTree));
                comDevice.get().send(em,ap);
            }
            if(p instanceof Crossroad){
                Crossroad cr = (Crossroad)p;
                if(cr.getFunction() == 10){
                    ExploreMessage em = new ExploreMessage("Build tree",new ExploreInfo(this,resources,exploreTree));
                    comDevice.get().send(em,cr);
                }
            }
        }

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

    public void makeReservation(Reservation res){
        Iterator it = reservations.iterator();
        while(it.hasNext()){
            Reservation r = (Reservation)it.next();
            if(r.overlapping(res)){
                reservations.remove(r);
                reservations.add(res);
                /*
                if(print && Point.distance(new Point(10,41),this) ==0)System.out.print(this);
                if(print && Point.distance(new Point(10,41),this)==0)System.out.println(String.format("  AP reservation made %d  %d",res.getStartTick(),res.getStopTick()));
                return;
                */
                if(print)System.out.print(this);
                if(print)System.out.println(String.format("  AP reservation made %d  %d",res.getStartTick(),res.getStopTick()));
                return;
            }
        }
        reservations.add(res);
        if(print)System.out.print(this);
        if(print)System.out.println(String.format("AP reservation made %d  %d",res.getStartTick(),res.getStopTick()));
    }

    public Reservation getReservationTick(int tick){
        for(Reservation r: reservations){
            if(r.containsTick(tick)){
                return r;
            }
        }
        return null;
    }

    private void updateReservations(){
        ArrayList<Reservation> temp = new ArrayList<>();
        for(Reservation r : reservations){
            int endEvaporation = r.getTimeStamp() + RESERVATIONRESET;
            if(r.getStopTick() <= tickCounter){ }
            else if(endEvaporation <= tickCounter){}
            else {temp.add(r);}
        }
    }

    public ArrayList<Reservation> getReservations() {
        return reservations;
    }

    public int getStationNr(){
        return stationNr;
    }

    public void addBackwardsReachable(Point as){
        backwardsReachable.add(as);
        //as.addReachable(this);

    }

    public int firstAvailableMoment(int tick, AGV agv){
        // No need to wait
        if(!containsTick(tick-5,agv) && !containsTick(tick+37,agv)) return tick;
        Reservation res;
        if(containsTickReservation(tick+37,agv) != null){
            res = containsTickReservation(tick+37,agv);
        } else{
            res = containsTickReservation(tick-5,agv);
        }
        tick = res.getStopTick();

        while(true){
            if(!containsTick(tick-5,agv) && !containsTick(tick+37,agv)) return tick+5;
            tick++;

        }

    }

    private boolean containsTick(int tick, AGV agv){
        for(Reservation r: reservations){
            if(r.containsTick(tick) && r.getAgv() != agv) return true;
        }
        return false;
    }

    private Reservation containsTickReservation(int tick, AGV agv){
        for(Reservation r: reservations){
            if(r.containsTick(tick) && r.getAgv() != agv) return r;
        }
        return null;
    }

}