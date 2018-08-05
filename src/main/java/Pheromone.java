import java.util.ArrayList;
import java.util.Iterator;

public class Pheromone {
    private ArrayList<boolean[]>  tasks;
    private ArrayList<Integer> values;
    private double evaporateRate = 0.1;

    public Pheromone(){
        tasks = new ArrayList<>();
        values = new ArrayList<>();
        createLists();
    }

    public void createLists(){
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    for (int l = 0; l < 2; l++) {
                        for (int m = 0; m < 2; m++) {
                            for (int n = 0; n < 2; n++) {
                                boolean[] ba = new boolean[6];
                                ba[0] = toBoolean(i);
                                ba[1] = toBoolean(j);
                                ba[2] = toBoolean(k);
                                ba[3] = toBoolean(l);
                                ba[4] = toBoolean(m);
                                ba[5] = toBoolean(n);
                                tasks.add(ba);
                                values.add(0);
                            }}}}}}
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
