import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;

import java.util.ArrayList;

public class Task extends Parcel {
    ArrayList<Boolean> tasks;
    public Task(ParcelDTO dto) {
        super(dto);

    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

    public ArrayList<Boolean> getTasks() {
        return tasks;
    }

    public void setTasks(ArrayList<Boolean> t){
        tasks = t;
    }

    public void removeFirstTask(){
        tasks.remove(0);
    }
}
