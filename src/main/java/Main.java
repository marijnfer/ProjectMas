/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.ui.renderers.WarehouseRenderer;

import javax.measure.unit.SI;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Example showcasing the {@link CollisionGraphRoadModelImpl} with an
 * {@link WarehouseRenderer} and {@link AGVRenderer}.
 * @author Rinde van Lon
 */
public final class Main {
    private static final double VEHICLE_LENGTH = 1d;
    private static final int NUM_AGVS = 1;
    private static final long TEST_END_TIME = 10 * 60 * 1000L;
    private static final int TEST_SPEED_UP = 16;
    static final int SERVICE_DURATION = 10000;
    boolean BURNIN = true;
    int burnTickLeft = 10;

    private Main() {}

    /**
     * @param args - No args.
     */
    public static void main(String[] args) {
        run(true);
    }

    /**
     * Runs the example.
     * @param testing If <code>true</code> the example will run in testing mode,
     *          automatically starting and stopping itself such that it can be run
     *          from a unit test.
     */
    public static void run(final boolean testing) {

        View.Builder viewBuilder = View.builder()
                .with(WarehouseRenderer.builder()
                        .withMargin(VEHICLE_LENGTH))
                .with(AGVRenderer.builder()
                        .withDifferentColorsForVehicles()
                        .withVehicleCoordinates())
                .with(RoadUserRenderer.builder()
                        .withImageAssociation(Task.class, "/Afbeelding1.png")
                        .withImageAssociation(AssemblyParcel0.class, "/ap0.png")
                        .withImageAssociation(AssemblyParcel1.class, "/ap1.png")
                        .withImageAssociation(AssemblyParcel2.class, "/ap2.png")
                        .withImageAssociation(AssemblyParcel3.class, "/ap3.png")
                        .withImageAssociation(AssemblyParcel4.class, "/ap4.png")
                        .withImageAssociation(AssemblyParcel5.class, "/ap5.png"));


        if (testing) {
            viewBuilder = viewBuilder
                    //.withAutoPlay()
                    //.withSimulatorEndTime(TEST_END_TIME)
                    .withTitleAppendix("Factory")
                    //.withSpeedUp(TEST_SPEED_UP)
                    //.withNoResizing()
                    .withResolution(700,700)
                    ;
        } else {
            viewBuilder = viewBuilder.withTitleAppendix("Warehouse");
        }

        final Simulator sim = Simulator.builder()
                .addModel(
                        RoadModelBuilders.dynamicGraph(Layout.getGraph())
                                .withCollisionAvoidance()
                                .withDistanceUnit(SI.METER)
                                .withVehicleLength(VEHICLE_LENGTH))
                .addModel(viewBuilder)
                .addModel(CommModel.builder())
                .addModel(DefaultPDPModel.builder())
                .build();

        final Factory factory = new Factory(sim);

        Iterator it = factory.getInboundPoints().iterator();
        ArrayList<DeliveryPoint> deliveryPoints = factory.getDeliverPoints();

        while (it.hasNext()){
            InboundPoint ip = (InboundPoint)it.next();
            ip.setStored(true);
            Task p = new Task( //
                    Parcel.builder(ip, factory.nextDeliveryPoint())
                            .serviceDuration(SERVICE_DURATION)
                            .neededCapacity(1 + sim.getRandomGenerator().nextInt(5))
                            .buildDTO());
            p.setTasks(factory.taskGenerator());
            sim.register(p);
        }

        Iterator it1 = factory.getAssemblyPoints().iterator();
        while (it1.hasNext()){
            AssemblyPoint ap = (AssemblyPoint)it1.next();
            sim.register(ap);
            switch (ap.getStationNr()){
                case 0:
                    AssemblyParcel0 p = new AssemblyParcel0( Parcel.builder(ap, factory.nextDeliveryPoint())
                            .serviceDuration(SERVICE_DURATION)
                            .neededCapacity(1 + sim.getRandomGenerator().nextInt(5))
                            .buildDTO());
                    sim.register(p);
                    break;
                case 1:
                    AssemblyParcel1 p1 = new AssemblyParcel1( Parcel.builder(ap, factory.nextDeliveryPoint())
                            .serviceDuration(SERVICE_DURATION)
                            .neededCapacity(1 + sim.getRandomGenerator().nextInt(5))
                            .buildDTO());
                    sim.register(p1);
                    break;

                case 2:
                    AssemblyParcel2 p2 = new AssemblyParcel2( Parcel.builder(ap, factory.nextDeliveryPoint())
                            .serviceDuration(SERVICE_DURATION)
                            .neededCapacity(1 + sim.getRandomGenerator().nextInt(5))
                            .buildDTO());
                    sim.register(p2);
                    break;

                case 3:
                    AssemblyParcel3 p3 = new AssemblyParcel3( Parcel.builder(ap, factory.nextDeliveryPoint())
                            .serviceDuration(SERVICE_DURATION)
                            .neededCapacity(1 + sim.getRandomGenerator().nextInt(5))
                            .buildDTO());
                    sim.register(p3);
                    break;

                case 4:
                    AssemblyParcel4 p4 = new AssemblyParcel4( Parcel.builder(ap, factory.nextDeliveryPoint())
                            .serviceDuration(SERVICE_DURATION)
                            .neededCapacity(1 + sim.getRandomGenerator().nextInt(5))
                            .buildDTO());
                    sim.register(p4);
                    break;

                case 5:
                    AssemblyParcel5 p5 = new AssemblyParcel5( Parcel.builder(ap, factory.nextDeliveryPoint())
                            .serviceDuration(SERVICE_DURATION)
                            .neededCapacity(1 + sim.getRandomGenerator().nextInt(5))
                            .buildDTO());
                    sim.register(p5);
                    break;
            }

        }

        ArrayList<Crossroad> crossroads = factory.getCrossroads();
        for(Crossroad cr : crossroads){
            boolean added = false;
            for (AssemblyPoint as : factory.getAssemblyPoints()){
                if(Point.distance(as,cr) == 2){
                    cr.setAssemblyPoint(as);
                    cr.setPheromone(new Pheromone(as.getStationNr()));
                    factory.addConnect(cr);
                    added = true;
                }
            }
            if(!added){
                factory.addConnect(cr);
            }
        }



        factory.buildConnects();

        Iterator itCr = factory.getCrossroads().iterator();
        while (itCr.hasNext()){
            Crossroad cr = (Crossroad)itCr.next();
            if(!cr.assemblyPointPresent()){
                int size = factory.findConnections(cr).size();
                if(size !=0){
                    cr.setPheromone(new Pheromone(5));
                }
            }
        }



        AGV agv = new AGV(new Point(2,0),10,factory,sim.getRandomGenerator(),factory.getCrossroadsStation(0));
        sim.register(agv);

        /*
        for(Connect c: factory.getConnections()){
            System.out.print(c.getCrossroad());
            System.out.print("  ");
            System.out.println(c.getCoupled());
        }
        */

        sim.addTickListener(new TickListener() {
            @Override
            public void tick(TimeLapse time) {

                if (time.getStartTime() > 100000000) {
                    sim.stop();
                } else if (sim.getRandomGenerator().nextDouble() < 1) {
                    Iterator it = factory.getInboundPoints().iterator();
                    ArrayList<InboundPoint> temp = new ArrayList<>();
                    while(it.hasNext()){
                        InboundPoint ip = (InboundPoint)it.next();
                        if(!ip.getStored()){
                            temp.add(ip);
                        }
                        if(temp.size() > 0){
                            int i = sim.getRandomGenerator().nextInt(temp.size());
                            InboundPoint emptyIP =  temp.get(i);
                            ip.setStored(true);
                            Task t = new Task(Parcel
                                            .builder(emptyIP,factory.nextDeliveryPoint())
                                            .serviceDuration(SERVICE_DURATION)
                                            .neededCapacity(1 + sim.getRandomGenerator().nextInt(5))
                                            .buildDTO());
                            t.setTasks(factory.taskGenerator());
                            sim.register(t);

                            return;
                        }
                    }
                }
            }

            @Override
            public void afterTick(TimeLapse timeLapse) {}
        });
        sim.start();
        System.out.println("sim start");
    }





}
