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
import java.util.Iterator;

public class AssemblyPoint extends Point implements CommUser, TickListener {

    private static int RANGE = 50;
    private int initMessageTicks =3;
    private Optional<CommDevice> comDevice;
    private ArrayList<AssemblyPoint> backwardsReachable;
    private int stationNr;
    private ArrayList<AssemblyPoint> reachable;

    private boolean initMessageSend;
    private int resources;
    private ArrayList<ExploreInfo> exploreTree;



    public AssemblyPoint(double px, double py, int stationNr){
        super(px,py);
        backwardsReachable = new ArrayList<>();
        reachable = new ArrayList<>();
        exploreTree = new ArrayList<>();
        this.stationNr = stationNr;
        resources = 10;
    }


    @Override
    public void tick(TimeLapse timeLapse){
        handleMessages();
        sendInfo();
        /*handleMessages();
        sendInfo();
        if(stationNr == 3){
            int i = 0;
        }
        if(exploreTree.size() != 0){
            int i = 0;
        }*/


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
            /*
            System.out.print(Point.distance(ei.getSender(),p));
            System.out.print(p);
            System.out.print("  ");
            System.out.print(exploreTree.size());
            System.out.print("  ");
            System.out.println(this);
            */
            if(Point.distance(ei.getSender(),p) == 0){
                return  i;
            }
        }

        return -1;
    }

    /**
     * Send explore information to all backwards reachable nodes
     */
    private void sendInfo(){
        Iterator it = backwardsReachable.iterator();
        while (it.hasNext()){
            AssemblyPoint ap = (AssemblyPoint)it.next();
            ExploreMessage em = new ExploreMessage("Build tree",new ExploreInfo(this,resources,exploreTree));
            comDevice.get().send(em,ap);
        }
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



    public int getStationNr(){
        return stationNr;
    }

    public void addBackwardsReachable(AssemblyPoint as){
        backwardsReachable.add(as);
        //as.addReachable(this);

    }


}