import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;


import java.util.ArrayList;

public class AssemblyParcel3 extends Parcel implements TickListener{
    private ArrayList<AssemblyPoint> reachable;

    public AssemblyParcel3(ParcelDTO dto) {
        super(dto);
        reachable = new ArrayList<>();
    }

    private void addReachable(AssemblyPoint as){
        reachable.add(as);
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    @Override
    public void tick(TimeLapse timeLapse){

    }

}
