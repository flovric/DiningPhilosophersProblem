/**
 * Created by Filip on 8.4.2018..
 */
public class Fork {

    private boolean isClean;

    public Fork() {     // init constructor that set fork as dirty
        this.isClean = false;
    }

    public Fork (boolean isClean) { // constructor that automatically cleans fork
        this.isClean = isClean;
    }

    public void soilTheFork() {
        this.isClean = false;
    }

    public void cleanTheFork() {
        this.isClean = true;
    }

    public boolean isClean() {
        return isClean;
    }
}
