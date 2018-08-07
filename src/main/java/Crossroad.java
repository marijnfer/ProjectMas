import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;

public class Crossroad extends Point implements CommUser, TickListener{
    private static int RANGE = 50;
    private Optional<CommDevice> comDevice;
    private ArrayList<CommUser> backwardsReachable;
    private Pheromone pheromone;
    private AssemblyPoint assemblyPoint;
    private int function = -1;

    private ArrayList<ExploreInfo> exploreTree;


    public Crossroad(double px, double py){
        super(px,py);
        backwardsReachable = new ArrayList<>();
        exploreTree = new ArrayList<>();
    }


    @Override
    public void tick(TimeLapse timeLapse){
        if(pheromone != null){
            pheromone.evaporate();
        }
        handleMessages();
    }


    private void handleMessages(){
        ImmutableList<Message> messages = comDevice.get().getUnreadMessages();
        for(Message m: messages){
            if(m.getContents() instanceof ExploreMessage){
                ExploreMessage em = (ExploreMessage)m.getContents();
                switch (em.getMessage()){
                    case "Build tree":
                        updateExploreInfo(em);

                }
            }
        }
    }

    private void updateExploreInfo(ExploreMessage em){
        ExploreInfo ei = em.getInfo();
        int index = containsPoint(ei.getSender());
        if(index !=-1){
            exploreTree.remove(index);
        }
        exploreTree.add(ei);
    }

    /**
     *
     * @param p
     * @return -1 if exploreTree doesn't contain the point otherwise the index
     */
    private int containsPoint(Point p){
        for(int i = 0; i<exploreTree.size(); i++){
            ExploreInfo ei = exploreTree.get(i);
            if(Point.distance(ei.getSender(),p) == 0){
                return  i;
            }
        }

        return -1;
    }

    public void setAssemblyPoint(AssemblyPoint assemblyPoint){
        this.assemblyPoint = assemblyPoint;
    }

    public void setPheromone(Pheromone pheromone){
        this.pheromone = pheromone;
    }


    @Override
    public void afterTick(TimeLapse timeLapse) {
    }

    @Override
    public void setCommDevice(CommDeviceBuilder builder) {
        if (RANGE >= 0) {
            builder.setMaxRange(RANGE);
        }
        comDevice = Optional.of(builder
                .build());
    }
    @Override
    public Optional<Point> getPosition() {
        return Optional.of((Point)this);
    }

    public double getX(){
        return this.x;
    }

    public void setBackwardsReachable(ArrayList<CommUser> cds){
        backwardsReachable = cds;
    }

    public CommUser getCommUser() {
        return this;
    }

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
}
