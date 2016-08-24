package fuud.dto;

import POGOProtos.Enums.PokemonIdOuterClass;

import java.util.List;

public class PokemonClassData {
    private String pokemonClassId;
    private String pokemonFamilyId;
    private List<PokemonData> pokemons;
    private int totalCandies;
    private int candyToEvole;
    private String pokemonClassImage;

    private int baseAttack;
    private int baseDefence;
    private int baseStamina;

    public void setPokemonClassId(String pokemonClassId) {
        this.pokemonClassId = pokemonClassId;
    }

    public String getPokemonClassId() {
        return pokemonClassId;
    }

    public void setPokemons(List<PokemonData> pokemons) {
        this.pokemons = pokemons;
    }

    public List<PokemonData> getPokemons() {
        return pokemons;
    }

    public void setTotalCandies(int totalCandies) {
        this.totalCandies = totalCandies;
    }

    public int getTotalCandies() {
        return totalCandies;
    }

    public void setCandyToEvole(int candyToEvole) {
        this.candyToEvole = candyToEvole;
    }

    public int getCandyToEvole() {
        return candyToEvole;
    }

    public void setPokemonClassImage(String pokemonClassImage) {
        this.pokemonClassImage = pokemonClassImage;
    }

    public String getPokemonClassImage() {
        return pokemonClassImage;
    }

    public void setBaseAttack(int baseAttack) {
        this.baseAttack = baseAttack;
    }

    public int getBaseAttack() {
        return baseAttack;
    }


    public void setBaseDefence(int baseDefence) {
        this.baseDefence = baseDefence;
    }

    public int getBaseDefence() {
        return baseDefence;
    }

    public void setBaseStamina(int baseStamina) {
        this.baseStamina = baseStamina;
    }

    public int getBaseStamina() {
        return baseStamina;
    }

    public String getPokemonFamilyId() {
        return pokemonFamilyId;
    }

    public void setPokemonFamilyId(String pokemonFamilyId) {
        this.pokemonFamilyId = pokemonFamilyId;
    }
}
