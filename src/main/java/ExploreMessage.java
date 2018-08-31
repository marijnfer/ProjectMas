import com.github.rinde.rinsim.core.model.comm.MessageContents;

import java.util.ArrayList;

/**
 * Message to send an arraylist of all pheromones with their AssemblyPoint
 * An assemblyPoint send this to all assemblyPoints that a backwards reachable
 */
public class ExploreMessage implements MessageContents {
    private String message;
    private ArrayList<ExploreInfo> info;

    public ExploreMessage(String message, ArrayList<ExploreInfo> info){
        this.message = message;
        this.info = info;
    }

    public String getMessage(){
        return message;
    }

    public ArrayList<ExploreInfo> getInfo() {
        return info;
    }


}
