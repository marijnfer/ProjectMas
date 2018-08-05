
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.geom.Point;

class Test extends Parcel {



    Test(Point o, Point d, long duration) {
        super(
                Parcel.builder(o, d)
                        .serviceDuration(duration)
                        .neededCapacity(1d)
                        .buildDTO());
    }

}
