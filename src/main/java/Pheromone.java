import java.util.ArrayList;
import java.util.Iterator;

public class Pheromone {
    private ArrayList<boolean[]>  tasks;
    private ArrayList<Integer> values;
    private double evaporateRate = 0.1;
    private int stationNr;

    public Pheromone(int stationNr){
        tasks = new ArrayList<>();
        values = new ArrayList<>();
        this.stationNr = stationNr;
        createLists(stationNr);
    }

    public void createLists(int stationNr) {
        int amount = 5 - stationNr;
        boolean[] ba = new boolean[amount];
        switch (amount) {
            case 1:
                for (int i = 0; i < 2; i++) {
                    ba[0] = toBoolean(i);
                }
                break;
            case 2:

                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        ba[0] = toBoolean(i);
                        ba[1] = toBoolean(j);
                    }
                }
                break;
            case 3:
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        for (int k = 0; k < 2; k++) {
                            ba[0] = toBoolean(i);
                            ba[1] = toBoolean(j);
                            ba[2] = toBoolean(k);
                        }
                    }
                }
                break;
            case 4:
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        for (int k = 0; k < 2; k++) {
                            for (int l = 0; l < 2; l++) {
                                ba[0] = toBoolean(i);
                                ba[1] = toBoolean(j);
                                ba[2] = toBoolean(k);
                                ba[3] = toBoolean(l);
                            }
                        }
                    }
                }
                break;
            case 5:
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        for (int k = 0; k < 2; k++) {
                            for (int l = 0; l < 2; l++) {
                                for (int m = 0; m < 2; m++) {
                                    ba[0] = toBoolean(i);
                                    ba[1] = toBoolean(j);
                                    ba[2] = toBoolean(k);
                                    ba[3] = toBoolean(l);
                                    ba[4] = toBoolean(m);
                                    break;
                                }
                            }
                        }
                    }
                }

        }
    }


    private boolean toBoolean(int i){
        if(i == 1){return true;}
        else{ return false; }
    }

    public  void evaporate(){
        Iterator it = values.iterator();
        while (it.hasNext()){
            int v = (Integer)it.next();
            if(v > 0){
                v -= evaporateRate;
            }
        }
    }
}
