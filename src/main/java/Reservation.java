public class Reservation {
    private AGV agv;
    private int tick;
    private int duration;
    private int timeStamp;

    public Reservation(AGV agv, int tick, int duration, int timeStamp){
        this.agv = agv;
        this.tick = tick;
        this.duration = duration;
        this.timeStamp = timeStamp;
    }

    public int getDuration() {
        return duration;
    }

    public int getTick() {
        return tick;
    }

    public AGV getAgv() {
        return agv;
    }

    public int getTimeStamp(){
        return timeStamp;
    }
}
