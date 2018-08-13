import com.github.rinde.rinsim.geom.*;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import java.util.ArrayList;

public class Layout {
    private static final int ROWS = 4;
    private static final int COLUMNS = 5;


    static ListenableGraph<LengthData> getGraph() {
        final Graph<LengthData> g = new TableGraph<>();

        ArrayList<InboundPoint> ibs = new ArrayList<>();
        ArrayList<Crossroad> crs1 = new ArrayList<>();
        ArrayList<DeliveryPoint> dps = new ArrayList<>();
        ArrayList<Crossroad> crs2 = new ArrayList<>();

        for(int i = 0; i < 10; i++){
            ibs.add(new InboundPoint(i+10,3));
            crs1.add(new Crossroad(i+10,5));
        }

        for(int i =0; i < 4; i++){
            dps.add(new DeliveryPoint(i+25,3));
            crs2.add(new Crossroad(i+25,5));
        }


        final Table<Integer, Integer, Point> bounds = createBounds(ibs,crs1,dps,crs2);
        final Table<Integer, Integer, Point> assembly = createAssemblyLine(new Point(10,7));

        // Bound
        for(int i = 0; i < 14;i++){
            Graphs.addBiPath(g,bounds.get(i,0),bounds.get(i,1));
        }
        for(int i = 0; i < 13;i++){
            Graphs.addPath(g,bounds.get(i+1,1),bounds.get(i,1));
        }

        //Bound + assembly
        Graphs.addPath(g,assembly.get(6,0),bounds.get(13,1));
        Graphs.addPath(g,bounds.get(0,1),assembly.get(-1,0));

        //Assembly
        Graphs.addPath(g,assembly.get(0,0),assembly.get(1,0));
        Graphs.addPath(g,assembly.get(0,1),assembly.get(1,0));
        Graphs.addPath(g,assembly.get(0,1),assembly.get(1,1));
        Graphs.addPath(g,assembly.get(0,2),assembly.get(1,2));
        Graphs.addPath(g,assembly.get(0,3),assembly.get(1,3));
        Graphs.addPath(g,assembly.get(0,2),assembly.get(1,3));

        Graphs.addPath(g,assembly.get(1,0),assembly.get(3,0));

        Graphs.addPath(g,assembly.get(1,0),assembly.get(3,1));
        Graphs.addPath(g,assembly.get(3,0),assembly.get(3,1));

        Graphs.addPath(g,assembly.get(1,1),assembly.get(3,1));
        Graphs.addPath(g,assembly.get(1,2),assembly.get(1,1));
        Graphs.addPath(g,assembly.get(1,1), assembly.get(2,2));

        Graphs.addPath(g,assembly.get(1,2),assembly.get(2,2));
        Graphs.addPath(g,assembly.get(2,2),assembly.get(3,2));

        Graphs.addPath(g,assembly.get(1,2),assembly.get(3,3));
        Graphs.addPath(g,assembly.get(1,3),assembly.get(3,3));
        Graphs.addPath(g,assembly.get(3,3),assembly.get(3,2));

        Graphs.addPath(g,assembly.get(3,0),assembly.get(5,0));
        Graphs.addPath(g,assembly.get(3,1),assembly.get(5,0));
        Graphs.addPath(g,assembly.get(3,1),assembly.get(4,1));
        Graphs.addPath(g,assembly.get(4,1),assembly.get(5,1));
        Graphs.addPath(g,assembly.get(5,0),assembly.get(5,1));

        Graphs.addPath(g,assembly.get(4,2),assembly.get(4,1));
        Graphs.addPath(g,assembly.get(3,2),assembly.get(4,2));
        Graphs.addPath(g,assembly.get(4,2),assembly.get(5,2));
        Graphs.addPath(g,assembly.get(3,2),assembly.get(5,3));

        Graphs.addPath(g,assembly.get(5,2),assembly.get(5,3));

        //Assembly
        for(int i = 1; i<5;i++){
            Graphs.addPath(g,assembly.get(-1,i),assembly.get(0,i-1));
        }
        for(int i = 0; i < 4;i++ ){
            Graphs.addPath(g,assembly.get(-1,i),assembly.get(-1,i+1));
        }

        Graphs.addBiPath(g,assembly.get(6,4),assembly.get(6,3));
        Graphs.addBiPath(g,assembly.get(6,3),assembly.get(6,2));
        Graphs.addBiPath(g,assembly.get(6,2),assembly.get(6,1));
        Graphs.addBiPath(g,assembly.get(6,1),assembly.get(6,0));

        for(int i = 1; i<5; i++){
            Graphs.addPath(g,assembly.get(5,i-1),assembly.get(6,i));
        }

        //Connect Assemblers row 0
        Graphs.addBiPath(g,assembly.get(0,0),assembly.get(10,0));
        Graphs.addBiPath(g,assembly.get(0,1),assembly.get(10,1));
        Graphs.addBiPath(g,assembly.get(0,2),assembly.get(10,2));
        Graphs.addBiPath(g,assembly.get(0,3),assembly.get(10,3));

        //Connect Assemblers row 1
        Graphs.addBiPath(g,assembly.get(1,0),assembly.get(11,0));
        Graphs.addBiPath(g,assembly.get(1,1),assembly.get(11,1));
        Graphs.addBiPath(g,assembly.get(1,2),assembly.get(11,2));
        Graphs.addBiPath(g,assembly.get(1,3),assembly.get(11,3));

        //Connect Assemblers row 3
        Graphs.addBiPath(g,assembly.get(3,0),assembly.get(13,0));
        Graphs.addBiPath(g,assembly.get(3,1),assembly.get(13,1));
        Graphs.addBiPath(g,assembly.get(3,2),assembly.get(13,2));
        Graphs.addBiPath(g,assembly.get(3,3),assembly.get(13,3));

        //Connect Assemblers row 5
        Graphs.addBiPath(g,assembly.get(5,0),assembly.get(15,0));
        Graphs.addBiPath(g,assembly.get(5,1),assembly.get(15,1));
        Graphs.addBiPath(g,assembly.get(5,2),assembly.get(15,2));
        Graphs.addBiPath(g,assembly.get(5,3),assembly.get(15,3));


        return new ListenableGraph<>(g);
    }

    private static ImmutableTable<Integer, Integer, Point> createAssemblyLine(Point p) {
        final ImmutableTable.Builder<Integer, Integer, Point> builder =
                ImmutableTable.builder();
        Point offset = new Point(p.x,p.y+2);
        Crossroad cr1 = new Crossroad(5,9);
        Crossroad cr2 = new Crossroad(5,19);
        Crossroad cr3 = new Crossroad(5,29);
        Crossroad cr4 = new Crossroad(5,39);
        cr1.setFunction(10);
        cr2.setFunction(10);
        cr3.setFunction(10);
        cr4.setFunction(10);
        //Row -
        builder.put(-1,0,new Point(cr1.x,cr1.y-4));
        builder.put(-1,1,cr1);
        builder.put(-1,2,cr2);
        builder.put(-1,3,cr3);
        builder.put(-1,4,cr4);


        //Row 0
        builder.put(0,0,new Crossroad(0+offset.x,0+offset.y));
        builder.put(0,1,new Crossroad(0+offset.x,10+offset.y));
        builder.put(0,2,new Crossroad(0+offset.x,20+offset.y));
        builder.put(0,3,new Crossroad(0+offset.x,30+offset.y));

        //Assemblers 0
        AssemblyPoint p01 = new AssemblyPoint(0+offset.x,0+offset.y-2,0);
        AssemblyPoint p02 = new AssemblyPoint(0+offset.x,10+offset.y+2,0);
        AssemblyPoint p03 = new AssemblyPoint(0+offset.x,20+offset.y-2,0);
        AssemblyPoint p04 = new AssemblyPoint(0+offset.x,30+offset.y+2,0);

        builder.put(10,0,p01);
        builder.put(10,1,p02);
        builder.put(10,2,p03);
        builder.put(10,3,p04);

        //Row 1
        builder.put(1,0,new Crossroad(10+offset.x,0+offset.y));
        builder.put(1,1,new Crossroad(10+offset.x,10+offset.y));
        builder.put(1,2,new Crossroad(10+offset.x,20+offset.y));
        builder.put(1,3,new Crossroad(10+offset.x,30+offset.y));

        //Assemblers 1
        AssemblyPoint p11 = new AssemblyPoint(10+offset.x,0+offset.y-2,1);
        AssemblyPoint p12 = new AssemblyPoint(10+offset.x,10+offset.y-2,2);
        AssemblyPoint p13 = new AssemblyPoint(10+offset.x,20+offset.y+2,1);
        AssemblyPoint p14 = new AssemblyPoint(10+offset.x,30+offset.y+2,1);

        builder.put(11,0,p11);
        builder.put(11,1,p12);
        builder.put(11,2,p13);
        builder.put(11,3,p14);

        //Row 2
        builder.put(2,0,new Crossroad(15+offset.x,offset.y));
        builder.put(2,2,new Crossroad(15+offset.x,20+offset.y));

        //Row 3
        builder.put(3,0,new Crossroad(20+offset.x,0+offset.y));
        builder.put(3,1,new Crossroad(20+offset.x,10+offset.y));
        builder.put(3,2,new Crossroad(20+offset.x,20+offset.y));
        builder.put(3,3,new Crossroad(20+offset.x,30+offset.y));

        //Assembler 3
        AssemblyPoint p21 = new AssemblyPoint(20+offset.x,0+offset.y-2,2);
        AssemblyPoint p22 = new AssemblyPoint(20+offset.x,10+offset.y+2,3);
        AssemblyPoint p23 = new AssemblyPoint(20+offset.x,20+offset.y-2,3);
        AssemblyPoint p24 = new AssemblyPoint(20+offset.x,30+offset.y+2,2);

        builder.put(13,0,p21);
        builder.put(13,1,p22);
        builder.put(13,2,p23);
        builder.put(13,3,p24);

        //Row 4
        builder.put(4,1,new Crossroad(25+offset.x,10+offset.y));
        builder.put(4,2,new Crossroad(25+offset.x,20+offset.y));

        //Row 5
        builder.put(5,0,new Crossroad(30+offset.x,0+offset.y));
        builder.put(5,1,new Crossroad(30+offset.x,10+offset.y));
        builder.put(5,2,new Crossroad(30+offset.x,20+offset.y));
        builder.put(5,3,new Crossroad(30+offset.x,30+offset.y));

        //Assembler 5
        AssemblyPoint p31 = new AssemblyPoint(30+offset.x,0+offset.y-2,4);
        AssemblyPoint p32 = new AssemblyPoint(30+offset.x,10+offset.y+2,5);
        AssemblyPoint p33 = new AssemblyPoint(30+offset.x,20+offset.y-2,4);
        AssemblyPoint p34 = new AssemblyPoint(30+offset.x,30+offset.y+2,5);

        builder.put(15,0,p31);
        builder.put(15,1,p32);
        builder.put(15,2,p33);
        builder.put(15,3,p34);



        //Row 6
        builder.put(6,0,new Point(35+offset.x,0+p.y-2));
        builder.put(6,1,new Crossroad(35+offset.x,0+p.y+2));
        builder.put(6,2,new Crossroad(35+offset.x,12+p.y));
        builder.put(6,3,new Crossroad(35+offset.x,22+p.y));
        builder.put(6,4,new Crossroad(35+offset.x,0+p.y+32));


        p31.addBackwardsReachable(p21);
        p31.addBackwardsReachable(p22);

        p32.addBackwardsReachable(p31);
        p32.addBackwardsReachable(p22);

        p33.addBackwardsReachable(p22);
        p33.addBackwardsReachable(p23);

        p34.addBackwardsReachable(p33);
        p34.addBackwardsReachable(p23);

        p21.addBackwardsReachable(p11);

        p22.addBackwardsReachable(p11);
        p22.addBackwardsReachable(p21);
        p22.addBackwardsReachable(p12);

        p23.addBackwardsReachable(p12);
        p23.addBackwardsReachable(p24);
        p23.addBackwardsReachable(p13);

        p24.addBackwardsReachable(p13);
        p24.addBackwardsReachable(p14);

        p11.addBackwardsReachable(p01);
        p11.addBackwardsReachable(p02);

        p12.addBackwardsReachable(p02);
        p12.addBackwardsReachable(p13);

        p13.addBackwardsReachable(p03);

        p14.addBackwardsReachable(p03);
        p14.addBackwardsReachable(p04);

        p01.addBackwardsReachable(cr1);
        p02.addBackwardsReachable(cr2);
        p03.addBackwardsReachable(cr3);
        p04.addBackwardsReachable(cr4);



        return builder.build();
    }


    private static ImmutableTable<Integer, Integer, Point> createBounds(ArrayList<InboundPoint> ibs, ArrayList<Crossroad> crs1,
                                                                        ArrayList<DeliveryPoint> dps, ArrayList<Crossroad> crs2) {
        final ImmutableTable.Builder<Integer, Integer, Point> builder =
                ImmutableTable.builder();

        for(int i = 0; i<ibs.size();i++){
            builder.put(i,0,ibs.get(i));
            builder.put(i,1,crs1.get(i));
        }

        for(int i = 0; i<dps.size();i++){
            builder.put(10+i,0,dps.get(i));
            builder.put(10+i,1,crs2.get(i));
        }

        return builder.build();

    }



}