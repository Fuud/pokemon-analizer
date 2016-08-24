package fuud.dto;

public class FavoritizeResult {
    private final boolean success;
    private final String reason;

    private FavoritizeResult(boolean success, String reason) {
        this.success = success;
        this.reason = reason;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReason() {
        return reason;
    }

    public static FavoritizeResult success(){
        return new FavoritizeResult(true, null);
    }

    public static FavoritizeResult failed(String reason){
        return new FavoritizeResult(false, reason);
    }
}
