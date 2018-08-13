import com.github.rinde.rinsim.geom.Point;

public class Reservation {
    private AGV agv;
    private int startTick;
    private int stopTick;
    private int timeStamp;
    private Point point;


    public Reservation(AGV agv, double startTick, double stopTick, int timeStamp,Point point){
        this.agv = agv;
        this.startTick = (int)startTick;
        this.stopTick= (int)stopTick;
        this.timeStamp = timeStamp;
        this.point = point;
    }



    public AGV getAgv() {
        return agv;
    }

    public int getTimeStamp(){
        return timeStamp;
    }

    public int getStartTick(){
        return startTick;
    }

    public int getStopTick(){
        return stopTick;
    }

    public boolean containsTick(int tick){
        if(tick >= startTick && tick <= stopTick) return true;
        return false;
    }

    public boolean overlapping(Reservation res){
        if(startTick <= res.getStopTick() && stopTick >= res.getStopTick()) return true;
        if(startTick <= res.getStartTick() && stopTick >= res.getStartTick()) return true;
        if(startTick >= res.getStartTick() && stopTick <= res.getStopTick()) return true;
        return false;
    }

    public Point getPoint() {
        return point;
    }
}
