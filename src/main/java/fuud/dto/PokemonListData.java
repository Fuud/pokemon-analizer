package fuud.dto;

import java.util.List;

public class PokemonListData {
    private String userName;
    private List<PokemonClassData> pokemonsByClass;
    private int userLevel;

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setPokemonsByClass(List<PokemonClassData> pokemonsByClass) {
        this.pokemonsByClass = pokemonsByClass;
    }

    public List<PokemonClassData> getPokemonsByClass() {
        return pokemonsByClass;
    }

    public void setUserLevel(int userLevel) {
        this.userLevel = userLevel;
    }

    public int getUserLevel() {
        return userLevel;
    }
}
