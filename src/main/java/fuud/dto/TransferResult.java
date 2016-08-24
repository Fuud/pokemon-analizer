package fuud.dto;

public class TransferResult {
    private final boolean success;
    private final String reason;
    private final int candyAwarded;

    private TransferResult(boolean success, String reason, int candyAwarded) {
        this.success = success;
        this.reason = reason;
        this.candyAwarded = candyAwarded;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReason() {
        return reason;
    }

    public int getCandyAwarded() {
        return candyAwarded;
    }

    public static TransferResult failed(String reason) {
        return new TransferResult(false, reason, 0);
    }

    public static TransferResult success(int candyAwarded) {
        return new TransferResult(true, "success", candyAwarded);
    }
}
