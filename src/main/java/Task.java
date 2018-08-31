import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;

import java.util.ArrayList;

/**
 * Which stations to visit
 */
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

    /**
     * Next station to visit (index of first true value after index lastVisited)
     * @param lastVisited
     * @return
     */
    public int nextStation(int lastVisited){
        for(int i = lastVisited+1; i<tasks.size();i++){
           if(tasks.get(i)){
               return i;
           }
        }
        return -1;
    }

    /**
     * Last station to visit (index of last true value of tasks)
     * @return
     */
    public int lastStation(){
        for(int i = tasks.size()-1; i >= 0; i--){
            if(tasks.get(i)){
                return i;
            }
        }
        return -1;
    }
}
