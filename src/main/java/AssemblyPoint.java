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

    private ArrayList<ExploreInfo> pheromones;
    private ArrayList<Reservation> reservations;
    private int tickCounter = 0;

    private double pheromone = 0;

    private int amountOfTimesVisited = 0;

    public AssemblyPoint(double px, double py, int stationNr){
        super(px,py);
        backwardsReachable = new ArrayList<>();
        pheromones = new ArrayList<>();
        this.stationNr = stationNr;
        reservations = new ArrayList<>();
    }

    @Override
    public void tick(TimeLapse timeLapse){
        tickCounter++;
        handleMessages();
        sendInfo();
        updateReservations();
        pheromone = pheromone * 0.99;
    }

    /**
     * Handles incoming exlore messages containing the pheromone of all assemblypoints that are reachable
     */
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

    /**
     * Updates the arraylist of all pheromones
     * @param em
     */
    private void updateExploreInfo(ExploreMessage em){
        ArrayList<ExploreInfo> info = em.getInfo();

        for(ExploreInfo exInfo: info){
            if(searchExploreInfo(exInfo) != null){
                ExploreInfo ex = searchExploreInfo(exInfo);
                pheromones.remove(ex);
            }
            pheromones.add(exInfo);
        }
    }

    /**
     * @param info
     * @return ExploreInfo of the same assemblyPoint as info otherwise null
     */
    private ExploreInfo searchExploreInfo(ExploreInfo info){
        for(ExploreInfo exInfo: pheromones){
            if(Point.distance(exInfo.getSender(),info.getSender()) == 0){
                return exInfo;
            }
        }
        return null;
    }

    /**
     * Send explore information to all backwards reachable nodes
     */
    private void sendInfo(){
        for(Point p:  backwardsReachable){
            if(p instanceof AssemblyPoint){
                AssemblyPoint ap = (AssemblyPoint)p;
                ArrayList<ExploreInfo> temp = new ArrayList<>(pheromones);
                temp.add(new ExploreInfo(this,pheromone));

                ExploreMessage em = new ExploreMessage("Build tree",temp);
                comDevice.get().send(em,ap);
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

    /**
     * If possible make a reservation
     * @param res
     */
    public void makeReservation(Reservation res){
        Iterator it = reservations.iterator();
        while(it.hasNext()){
            Reservation r = (Reservation)it.next();
            if(r.overlapping(res)){
                reservations.remove(r);
                reservations.add(res);
                return;
            }
        }
        reservations.add(res);
    }

    /**
     * If the AssemblyPoint contains a reservation on tick return the reservation
     * @param tick
     * @return
     */
    public Reservation getReservationTick(int tick){
        for(Reservation r: reservations){
            if(r.containsTick(tick)){
                return r;
            }
        }
        return null;
    }

    /**
     * Deletes reservations in the past
     * An AGV changes paths constantly thus reservations are updated regularly. Because
     * of this, certain reservations can become irrelevant. To maintain the most accurate
     * reservations list, reservations that aren't updated are removed.
     */
    private void updateReservations(){
        ArrayList<Reservation> temp = new ArrayList<>();
        for(Reservation r : reservations){
            int endEvaporation = r.getTimeStamp() + RESERVATIONRESET;
            if(r.getStopTick() <= tickCounter){ }
            else if(endEvaporation <= tickCounter){}
            else {temp.add(r);}
        }
    }

    public int getStationNr(){
        return stationNr;
    }

    public void addBackwardsReachable(Point as){
        backwardsReachable.add(as);
    }

    /**
     * Determine first available moment an agv can visit this AssemblyPoint
     * @param t
     * @param agv
     * @return
     */
    public int firstAvailableMoment(int t, AGV agv){
        if(reservations.size() > 0){
            int f = 0;
        }
        // No need to wait
        int tick = t;
        if(!containsTick(t-5,agv) && !containsTick(t+37,agv)){
            return tick;
        }
        Reservation res;
        if(containsTickReservation(tick+37,agv) != null){
            res = containsTickReservation(tick+37,agv);
        } else{
            res = containsTickReservation(tick-5,agv);
        }
        tick = res.getStopTick();

        while(true){
            if(!containsTick(tick-5,agv) && !containsTick(tick+37,agv)) {
                return tick+5;

            }
            tick++;

        }

    }

    /**
     * @param tick
     * @param agv
     * @return True if one of the reservations contains tick and it isn't from agv
     */
    private boolean containsTick(int tick, AGV agv){
        for(Reservation r: reservations){
            if(r.containsTick(tick) && r.getAgv() != agv) return true;
        }
        return false;
    }

    /**
     * @param tick
     * @param agv
     * @return if one of the reservations contains tick and it isn't from agv return the reservation otherwise null
     */
    private Reservation containsTickReservation(int tick, AGV agv){
        for(Reservation r: reservations){
            if(r.containsTick(tick) && r.getAgv() != agv) return r;
        }
        return null;
    }

    /**
     * Increase pheromone value
     * and increase amountOfTimesVisited(for statistic purposes)
     * @return pheromone list from this and reachable assembly points
     */
    public ArrayList<ExploreInfo> increasePheromone(){
        amountOfTimesVisited++;
        pheromone += 100;
        ArrayList<ExploreInfo> temp = new ArrayList<>(pheromones);
        temp.add(new ExploreInfo(this,pheromone));
        if(Point.distance(new Point(10,7), this)==0) System.out.println(pheromone);
        return temp;
    }

}