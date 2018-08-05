import com.github.rinde.rinsim.core.model.comm.MessageContents;

public class ExploreMessage implements MessageContents {
    private String message;
    private ExploreInfo info;
    private AssemblyPoint sender;

    public ExploreMessage(String message, ExploreInfo info){
        this.message = message;
        this.info = info;
        this.sender = sender;
    }

    public String getMessage(){
        return message;
    }

    public ExploreInfo getInfo() {
        return info;
    }


}
