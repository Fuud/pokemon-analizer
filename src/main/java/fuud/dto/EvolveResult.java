package fuud.dto;

public class EvolveResult {
    private final boolean success;
    private final String reason;
    private final int candyAwarded;
    private final String newPokemonClassId;
    private final PokemonData newPokemonData;

    private EvolveResult(boolean success, String reason, int candyAwarded, String newPokemonClassId, PokemonData newPokemonData) {
        this.success = success;
        this.reason = reason;
        this.candyAwarded = candyAwarded;
        this.newPokemonClassId = newPokemonClassId;
        this.newPokemonData = newPokemonData;
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

    public String getNewPokemonClassId() {
        return newPokemonClassId;
    }

    public PokemonData getNewPokemonData() {
        return newPokemonData;
    }

    public static EvolveResult failed(String reason) {
        return new EvolveResult(false, reason, 0, null, null);
    }

    public static EvolveResult success(int candyAwarded, String newPokemonClassId, PokemonData newPokemonData) {
        return new EvolveResult(true, "success", candyAwarded, newPokemonClassId, newPokemonData);
    }
}
