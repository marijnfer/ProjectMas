import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;

import java.util.ArrayList;
import java.util.Iterator;

public class Crossroad extends Point implements TickListener{
    private static int RESERVATIONRESET = 10;
    private AssemblyPoint assemblyPoint;
    private int function = -1;
    private int tickCounter = 0;
    private ArrayList<Reservation> reservations;

    public Crossroad(double px, double py){
        super(px,py);
        reservations = new ArrayList<>();
    }

    @Override
    public void tick(TimeLapse timeLapse){
        tickCounter++;
        updateReservations();
    }

    public void setAssemblyPoint(AssemblyPoint assemblyPoint){
        this.assemblyPoint = assemblyPoint;
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
    }

    /**
     * @return True if this crossroad is directly connected to a assembly point
     */
    public boolean assemblyPointPresent(){
        if(assemblyPoint == null){return  false;}
        return true;
    }

    public AssemblyPoint getAssemblyPoint() {
        return assemblyPoint;
    }

    public void setFunction(int function){
        this.function = function;
    }

    public int getFunction(){
        return function;
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
     * If the Crossroad contains a reservation on tick return the reservation
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

}
