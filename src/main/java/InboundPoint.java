import com.github.rinde.rinsim.geom.Point;

public class InboundPoint extends Point {
    private boolean stored;

    public InboundPoint(double px, double py){
        super(px,py);
        stored = false;

    }

    public void setStored(boolean stored){
        this.stored = stored;
    }

    public boolean getStored(){
        return stored;
    }
}
