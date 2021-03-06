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
    private static final int NUM_AGVS =7;
    static final int SERVICE_DURATION = 10000;

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
                                .withVehicleCoordinates()
                        .withDifferentColorsForVehicles()
                        )
                .with(RoadUserRenderer.builder()
                        .withImageAssociation(Task.class, "/inboundPoint.png")
                        .withImageAssociation(AssemblyParcel0.class, "/ap0.png")
                        .withImageAssociation(AssemblyParcel1.class, "/ap1.png")
                        .withImageAssociation(AssemblyParcel2.class, "/ap2.png")
                        .withImageAssociation(AssemblyParcel3.class, "/ap3.png")
                        .withImageAssociation(AssemblyParcel4.class, "/ap4.png")
                        .withImageAssociation(AssemblyParcel5.class, "/ap5.png"));


        if (testing) {
            viewBuilder = viewBuilder
                    //.withSimulatorEndTime(TEST_END_TIME)
                    .withTitleAppendix("Factory")
                    .withSpeedUp(2)
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

        while (it.hasNext()){
            InboundPoint ip = (InboundPoint)it.next();
            ip.setStored(true);
            Task p = new Task(
                    Parcel.builder(ip,factory.nextDeliveryPoint())
                            .serviceDuration(SERVICE_DURATION)
                            .neededCapacity(1 + sim.getRandomGenerator().nextInt(5))
                            .buildDTO());
            p.setTasks(factory.taskGenerator());
            sim.register(p);
        }

        //Add the appropriate parcel to each assembly
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

        //Couple each AssemblyPoint to its connected crossroad
        ArrayList<Crossroad> exploreStart = new ArrayList<>();
        ArrayList<Crossroad> crossroads = factory.getCrossroads();
        for(Crossroad cr : crossroads) {
            boolean added = false;
            if (cr.y > 5) {
                for (AssemblyPoint as : factory.getAssemblyPoints()) {
                    if (Point.distance(as, cr) == 2) {
                        cr.setAssemblyPoint(as);
                        factory.addConnect(cr);
                        added = true;
                    }
                }
                if (!added) {
                    factory.addConnect(cr);
                }
                if (cr.getFunction() == 10) {
                    exploreStart.add(cr);
                }
                sim.register(cr);
            }
        }

        factory.buildConnects();
        factory.buildAllconnections();

        ArrayList<Point> spawnPoints = factory.getSpawnPoints();
        int j =0;
        for(int i = 0; i< NUM_AGVS;i++){
            AGV agv = new AGV(spawnPoints.get(i),10,factory,sim.getRandomGenerator(),exploreStart,j);
            j++;
            sim.register(agv);
        }

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
    }
}
