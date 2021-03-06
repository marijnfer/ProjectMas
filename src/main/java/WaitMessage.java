import com.github.rinde.rinsim.core.model.comm.MessageContents;

/**
 * Message for wait communication
 * Possibly need to wait for an Crossroad, InboundPoint, DeliveryPoint.
 * Crossroad: crossroad and coupled assemblyPoint (specified by message)
 */
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

    public WaitMessage(String message, InboundPoint ip) {
        this.message = message;
        this.ip =ip;
    }

    public WaitMessage(String message, DeliveryPoint dp){
        this.message = message;
        this. dp = dp;
    }

    public Double getDistance() {
        return distance;
    }

    public String getMessage() {
        return message;
    }

    public Crossroad getCr() {
        return cr;
    }

    public InboundPoint getIp() {
        return ip;
    }

    public DeliveryPoint getDp() {
        return dp;
    }
}
