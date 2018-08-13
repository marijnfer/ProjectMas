import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;

import java.util.ArrayList;
import java.util.Iterator;

public class InboundPoint extends Point  implements TickListener {
    private boolean stored;
    private ArrayList<Reservation> reservations;
    private static int RESERVATIONRESET = 10;
    private int tickCounter = 0;


    public InboundPoint(double px, double py){
        super(px,py);
        stored = false;
        reservations = new ArrayList<>();
    }

    public void setStored(boolean stored){
        this.stored = stored;
    }

    public boolean getStored(){
        return stored;
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
    }

    @Override
    public void tick(TimeLapse timeLapse){
        updateReservations();
    }


    public void makeReservation(Reservation res){
        Iterator it = reservations.iterator();
        while(it.hasNext()){
            Reservation r = (Reservation)it.next();
            if(r.overlapping(res)){
                reservations.remove(r);
                reservations.add(res);
                System.out.print(this);
                System.out.println(String.format("  IP reservation made %d  %d",res.getStartTick(),res.getStopTick()));
                return;
            }
        }
        reservations.add(res);
        System.out.print(this);
        System.out.println(String.format("  IP reservation made %d  %d",res.getStartTick(),res.getStopTick()));
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





}
