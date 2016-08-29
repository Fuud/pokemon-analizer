package fuud.dto;

import POGOProtos.Data.PokemonDataOuterClass;
import POGOProtos.Enums.PokemonFamilyIdOuterClass;
import com.pokegoapi.api.inventory.Item;
import com.pokegoapi.api.inventory.Stats;
import com.pokegoapi.api.pokemon.EggPokemon;

import java.util.List;
import java.util.Map;

public class Inventories {
    private final List<PokemonDataOuterClass.PokemonData> pokemons;
    private final List<EggPokemon> eggs;
    private final List<Item> items;
    private final Map<PokemonFamilyIdOuterClass.PokemonFamilyId, Integer> candies;
    private final Stats playerStats;

    public Inventories(List<PokemonDataOuterClass.PokemonData> pokemons, List<EggPokemon> eggs, List<Item> items, Map<PokemonFamilyIdOuterClass.PokemonFamilyId, Integer> candies, Stats playerStats) {
        this.pokemons = pokemons;
        this.eggs = eggs;
        this.items = items;
        this.candies = candies;
        this.playerStats = playerStats;
    }

    public List<PokemonDataOuterClass.PokemonData> getPokemons() {
        return pokemons;
    }

    public List<EggPokemon> getEggs() {
        return eggs;
    }

    public List<Item> getItems() {
        return items;
    }

    public Map<PokemonFamilyIdOuterClass.PokemonFamilyId, Integer> getCandies() {
        return candies;
    }

    public Stats getPlayerStats() {
        return playerStats;
    }
}
