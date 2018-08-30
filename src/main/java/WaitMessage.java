import com.github.rinde.rinsim.core.model.comm.MessageContents;

public class WaitMessage implements MessageContents {
    String message;
    Crossroad cr;
    InboundPoint ip;
    DeliveryPoint dp;
    Double distance;

    public WaitMessage(String message, Crossroad cr){
        this.message = message;
        this. cr = cr;
    }

    public WaitMessage(String message, Crossroad cr, double distance){
        this.message = message;
        this. cr = cr;
        this.distance = distance;
    }

    public Double getDistance() {
        return distance;
    }

    public WaitMessage(String message, InboundPoint ip) {
        this.message = message;
        this.ip =ip;
    }

    public WaitMessage(String message, DeliveryPoint dp){
        this.message = message;
        this. dp = dp;
    }
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Crossroad getCr() {
        return cr;
    }

    public void setCr(Crossroad cr) {
        this.cr = cr;
    }

    public InboundPoint getIp() {
        return ip;
    }

    public DeliveryPoint getDp() {
        return dp;
    }
}
