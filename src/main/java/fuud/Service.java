package fuud;

import POGOProtos.Data.PlayerDataOuterClass;
import POGOProtos.Data.PokemonDataOuterClass;
import POGOProtos.Enums.PokemonFamilyIdOuterClass;
import POGOProtos.Enums.PokemonIdOuterClass;
import POGOProtos.Networking.Requests.Messages.EvolvePokemonMessageOuterClass;
import POGOProtos.Networking.Requests.Messages.ReleasePokemonMessageOuterClass;
import POGOProtos.Networking.Requests.Messages.SetFavoritePokemonMessageOuterClass;
import POGOProtos.Networking.Requests.RequestTypeOuterClass;
import POGOProtos.Networking.Responses.EvolvePokemonResponseOuterClass;
import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass;
import POGOProtos.Networking.Responses.SetFavoritePokemonResponseOuterClass;
import com.google.protobuf.InvalidProtocolBufferException;
import com.pokegoapi.api.inventory.CandyJar;
import com.pokegoapi.api.inventory.Inventories;
import com.pokegoapi.api.player.PlayerProfile;
import com.pokegoapi.api.pokemon.*;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.main.ServerRequest;
import fuud.copied.PokemonCpUtils;
import fuud.dto.*;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static POGOProtos.Networking.Responses.SetFavoritePokemonResponseOuterClass.SetFavoritePokemonResponse.Result.SUCCESS;

@Component
public class Service {
    private static final Logger logger = LoggerFactory.getLogger(Endpoint.class);
    private final OkHttpClient httpClient = new OkHttpClient();
    private final RefreshTokenStorage refreshTokenStorage;

    @Autowired
    public Service(RefreshTokenStorage refreshTokenStorage) {
        this.refreshTokenStorage = refreshTokenStorage;
    }

    public String getUserNameByRefreshToken(String refreshToken) throws Exception {
        try (CloseablePokemonGo go = CloseablePokemonGo.createByRefreshToken(refreshToken, refreshTokenStorage, httpClient)) {
            return go.getPlayerProfile().getPlayerData().getUsername();
        }
    }

    public PokemonListData pokemonListJson(String username) throws Exception {
        try (CloseablePokemonGo go = CloseablePokemonGo.createByUserName(username, refreshTokenStorage, httpClient)) {

            final PlayerProfile playerProfile = go.getPlayerProfile();
            final PlayerDataOuterClass.PlayerData playerData = playerProfile.getPlayerData();
            final Inventories inventories = go.getInventories();
            final List<Pokemon> pokemons = inventories.getPokebank().getPokemons();
            final CandyJar candyjar = inventories.getCandyjar();
            final int playerLevel = playerProfile.getStats().getLevel();

            final PokemonListData data = new PokemonListData();

            data.setUserName(playerData.getUsername());
            data.setUserLevel(playerLevel);

            final Map<PokemonIdOuterClass.PokemonId, List<Pokemon>> pokemonsById = groupPokemons(pokemons);

            List<PokemonClassData> pokemonClassDatas = new ArrayList<>();

            for (PokemonIdOuterClass.PokemonId pokemonId : PokemonIdOuterClass.PokemonId.values()) {
                if (pokemonsById.containsKey(pokemonId)) {
                    final PokemonClassData pokemonClassData = generatePokemonClassData(pokemonId, pokemonsById.get(pokemonId), candyjar, playerLevel);
                    pokemonClassDatas.add(pokemonClassData);
                }
            }

            data.setPokemonsByClass(pokemonClassDatas);

            return data;
        }
    }

    public EvolveResult evolve(long pokemonId, int playerLevel, String userName) throws LoginFailedException {
        try (CloseablePokemonGo go = CloseablePokemonGo.createByUserName(userName, refreshTokenStorage, httpClient)) {


            EvolvePokemonMessageOuterClass.EvolvePokemonMessage reqMsg =
                    EvolvePokemonMessageOuterClass.EvolvePokemonMessage.newBuilder().
                            setPokemonId(pokemonId).
                            build();

            ServerRequest serverRequest = new ServerRequest(RequestTypeOuterClass.RequestType.EVOLVE_POKEMON, reqMsg);
            go.getRequestHandler().sendServerRequests(serverRequest);

            EvolvePokemonResponseOuterClass.EvolvePokemonResponse response =
                    EvolvePokemonResponseOuterClass.EvolvePokemonResponse.parseFrom(serverRequest.getData());

            if (response.getResult() == EvolvePokemonResponseOuterClass.EvolvePokemonResponse.Result.SUCCESS) {
                return EvolveResult.success(
                        response.getCandyAwarded(),
                        response.getEvolvedPokemonData().getPokemonId().name(),
                        convertToPokemonData(response.getEvolvedPokemonData(), playerLevel)
                );
            } else {
                return EvolveResult.failed("Can not evolve pokemon because of " + response.getResult());
            }
        } catch (LoginFailedException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return EvolveResult.failed("Can not evolve pokemon because of " + e.getMessage());
        }
    }

    public TransferResult transfer(long pokemonId, String userName) {
        try (CloseablePokemonGo go = CloseablePokemonGo.createByUserName(userName, refreshTokenStorage, httpClient)) {
            ReleasePokemonMessageOuterClass.ReleasePokemonMessage reqMsg =
                    ReleasePokemonMessageOuterClass.ReleasePokemonMessage.
                            newBuilder().
                            setPokemonId(pokemonId).build();

            ServerRequest serverRequest = new ServerRequest(RequestTypeOuterClass.RequestType.RELEASE_POKEMON, reqMsg);
            go.getRequestHandler().sendServerRequests(serverRequest);

            ReleasePokemonResponseOuterClass.ReleasePokemonResponse response;
            response = ReleasePokemonResponseOuterClass.ReleasePokemonResponse.parseFrom(serverRequest.getData());

            response.getCandyAwarded();

            if (response.getResult() == ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result.SUCCESS) {
                return TransferResult.success(response.getCandyAwarded());
            } else {
                return TransferResult.failed("Can not transfer pokemon because of " + response.getResult());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return TransferResult.failed("Can not transfer pokemon because of " + e.getMessage());
        }
    }

    public FavoritizeResult favoritize(long pokemonId, boolean favorite, String userName) throws LoginFailedException {
        try (CloseablePokemonGo go = CloseablePokemonGo.createByUserName(userName, refreshTokenStorage, httpClient)) {
            SetFavoritePokemonMessageOuterClass.SetFavoritePokemonMessage reqMsg = SetFavoritePokemonMessageOuterClass.SetFavoritePokemonMessage.newBuilder()
                    .setPokemonId(pokemonId)
                    .setIsFavorite(favorite)
                    .build();

            ServerRequest serverRequest = new ServerRequest(RequestTypeOuterClass.RequestType.SET_FAVORITE_POKEMON, reqMsg);
            go.getRequestHandler().sendServerRequests(serverRequest);

            SetFavoritePokemonResponseOuterClass.SetFavoritePokemonResponse response;
            try {
                response = SetFavoritePokemonResponseOuterClass.SetFavoritePokemonResponse.parseFrom(serverRequest.getData());
            } catch (InvalidProtocolBufferException e) {
                throw new RemoteServerException(e);
            }

            if (response.getResult() == SUCCESS) {
                return FavoritizeResult.success();
            } else {
                return FavoritizeResult.failed("Can not change favorite because if " + response.getResult());
            }
        } catch (LoginFailedException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return FavoritizeResult.failed("Can not favoritize pokemon because of " + e.getMessage());
        }
    }

    private PokemonClassData generatePokemonClassData(PokemonIdOuterClass.PokemonId pokemonClassId, List<Pokemon> pokemons, CandyJar candyjar, int playerLevel) {

        final PokemonMeta pokemonMeta = PokemonMetaRegistry.getMeta(pokemonClassId);
        final int totalCandies = candyjar.getCandies(pokemonMeta.getFamily());
        final int candyToEvolve = pokemonMeta.getCandyToEvolve();

        final PokemonClassData pokemonClassData = new PokemonClassData();
        pokemonClassData.setPokemonClassId(pokemonClassId.name());
        pokemonClassData.setPokemonFamilyId(pokemonMeta.getFamily().name());
        pokemonClassData.setTotalCandies(totalCandies);
        pokemonClassData.setCandyToEvole(candyToEvolve);
        pokemonClassData.setPokemonClassImage(imageFor(pokemonClassId.getNumber()));

        pokemonClassData.setBaseAttack(pokemonMeta.getBaseAttack());
        pokemonClassData.setBaseDefence(pokemonMeta.getBaseDefense());
        pokemonClassData.setBaseStamina(pokemonMeta.getBaseStamina());

        final List<PokemonData> pokemonDatas = pokemons.
                stream().
                map(pokemon -> convertToPokemonData(pokemon.getProto(), playerLevel)).
                collect(Collectors.toList());

        pokemonClassData.setPokemons(pokemonDatas);
        return pokemonClassData;
    }

    private PokemonData convertToPokemonData(PokemonDataOuterClass.PokemonData pokemon, int playerLevel) {
        final PokemonData pokemonData = new PokemonData();

        final PokemonIdOuterClass.PokemonId pokemonClassId = pokemon.getPokemonId();
        PokemonMeta pokemonMeta = PokemonMetaRegistry.getMeta(pokemonClassId);

        pokemonData.setPokemonId("" + pokemon.getId());
        pokemonData.setCp(pokemon.getCp());
        pokemonData.setHp(pokemon.getStaminaMax());

        final float level = PokemonCpUtils.getLevelFromCpMultiplier(pokemon.getCpMultiplier() + pokemon.getAdditionalCpMultiplier());
        pokemonData.setLevel(level);

        pokemonData.setIndividualAttack(pokemon.getIndividualAttack());
        pokemonData.setIndividualDefence(pokemon.getIndividualDefense());
        pokemonData.setIndividualStamina(pokemon.getIndividualStamina());

        pokemonData.setAttack(pokemon.getIndividualAttack() + pokemonMeta.getBaseAttack());
        pokemonData.setDefence(pokemon.getIndividualDefense() + pokemonMeta.getBaseDefense());
        pokemonData.setStamina(pokemon.getIndividualStamina() + pokemonMeta.getBaseStamina());

        pokemonData.setFavorite(pokemon.getFavorite() > 0);

        final String maxCpForPlayer = getCpForLastEvolution(pokemonData.getIndividualAttack(), pokemon.getIndividualDefense(), pokemonData.getIndividualStamina(), pokemonClassId, playerLevel);
        pokemonData.setMaxCpForYourLevel(maxCpForPlayer);

        final String maxCp = getCpForLastEvolution(pokemonData.getIndividualAttack(), pokemon.getIndividualDefense(), pokemonData.getIndividualStamina(), pokemonClassId, 40);
        pokemonData.setMaxCpForMaxLevel(maxCp);

        pokemonData.setRemindingDustForYourLevel(getRemindingDust(level, Math.min(40f, playerLevel + 1.5f)));
        pokemonData.setRemindingDustForMaxLevel(getRemindingDust(level, 40));

        pokemonData.setRemindingCandiesForYourLevel(getRemindingCandies(level, Math.min(40f, playerLevel + 1.5f)));
        pokemonData.setRemindingCandiesForMaxLevel(getRemindingCandies(level, 40));

        pokemonData.setCreationTimeMs(pokemon.getCreationTimeMs());

        final PokemonType firstAttackType = PokemonMoveMetaRegistry.getMeta(pokemon.getMove1()).getType();
        final PokemonType secondAttackType = PokemonMoveMetaRegistry.getMeta(pokemon.getMove2()).getType();
        pokemonData.setFirstAttackMatch(pokemonMeta.getType1() == firstAttackType || pokemonMeta.getType2() == firstAttackType);
        pokemonData.setSecondAttackMatch(pokemonMeta.getType1() == secondAttackType || pokemonMeta.getType2() == secondAttackType);

        return pokemonData;
    }

    private String getCpForLastEvolution(int individualAttack, int individualDefense, int individualStamina, PokemonIdOuterClass.PokemonId pokemonId, int playerLevel) {
        final PokemonFamilyIdOuterClass.PokemonFamilyId pokemonFamily = PokemonMetaRegistry.getMeta(pokemonId).getFamily();

        if (pokemonFamily != PokemonFamilyIdOuterClass.PokemonFamilyId.FAMILY_EEVEE) {
            final PokemonIdOuterClass.PokemonId highestForFamily = PokemonMetaRegistry.getHighestForFamily().get(pokemonFamily);
            return "" + getCp(individualAttack, individualDefense, individualStamina, playerLevel, highestForFamily);
        } else if (pokemonId == PokemonIdOuterClass.PokemonId.EEVEE) {
            return "V" + getCp(individualAttack, individualDefense, individualStamina, playerLevel, PokemonIdOuterClass.PokemonId.VAPOREON) +
                    " " +
                    "J" + getCp(individualAttack, individualDefense, individualStamina, playerLevel, PokemonIdOuterClass.PokemonId.JOLTEON) +
                    " " +
                    "F" + getCp(individualAttack, individualDefense, individualStamina, playerLevel, PokemonIdOuterClass.PokemonId.FLAREON);

        } else {
            return "" + getCp(individualAttack, individualDefense, individualStamina, playerLevel, pokemonId);
        }
    }

    private int getCp(int individualAttack, int individualDefence, int individualStamina, int playerLevel, PokemonIdOuterClass.PokemonId pokemonId) {
        final PokemonMeta pokemonMeta = PokemonMetaRegistry.getMeta(pokemonId);
        return PokemonCpUtils.getMaxCpForPlayer(
                pokemonMeta.getBaseAttack() + individualAttack,
                pokemonMeta.getBaseDefense() + individualDefence,
                pokemonMeta.getBaseStamina() + individualStamina,
                playerLevel);
    }

    private Map<PokemonIdOuterClass.PokemonId, List<Pokemon>> groupPokemons(List<Pokemon> pokemons) {
        return pokemons.stream().collect(Collectors.toMap(
                PokemonDetails::getPokemonId,
                Collections::singletonList,
                (pokemonsLeft, pokemonsRight) ->
                        Stream.
                                of(pokemonsLeft, pokemonsRight).
                                flatMap(Collection::stream).
                                sorted((p1, p2) -> -(p1.getCp() - p2.getCp())).
                                collect(Collectors.toList())
        ));
    }

    private int getRemindingDust(float currentLevel, float maxLevel) {
        int sum = 0;
        for (; currentLevel < maxLevel; currentLevel += 0.5) {
            sum += getStartdustCostsForPowerup(currentLevel);
        }
        return sum;
    }

    private int getRemindingCandies(float currentLevel, float maxLevel) {
        int sum = 0;
        for (; currentLevel < maxLevel; currentLevel += 0.5) {
            sum += getCandyCostsForPowerup(currentLevel);
        }
        return sum;
    }

    public static int getStartdustCostsForPowerup(float level) {
        // Based on http://pokemongo.gamepress.gg/power-up-costs
        int powerups = 0;
        if (level < 3) {
            return 200;
        }
        if (level < 4) {
            return 400;
        }
        if (level < 7) {
            return 600;
        }
        if (level < 8) {
            return 800;
        }
        if (level < 11) {
            return 1000;
        }
        if (level < 13) {
            return 1300;
        }
        if (level < 15) {
            return 1600;
        }
        if (level < 17) {
            return 1900;
        }
        if (level < 19) {
            return 2200;
        }
        if (level < 21) {
            return 2500;
        }
        if (level < 23) {
            return 3000;
        }
        if (level < 25) {
            return 3500;
        }
        if (level < 27) {
            return 4000;
        }
        if (level < 29) {
            return 4500;
        }
        if (level < 31) {
            return 5000;
        }
        if (level < 33) {
            return 6000;
        }
        if (level < 35) {
            return 7000;
        }
        if (level < 37) {
            return 8000;
        }
        if (level < 39) {
            return 9000;
        }
        return 10000;
    }

    public static int getCandyCostsForPowerup(float level) {
        if (level < 13) {
            return 1;
        }
        if (level < 21) {
            return 2;
        }
        if (level < 31) {
            return 3;
        }
        return 4;
    }

    private String imageFor(int pokemonIdNumber) {
        if (pokemonIdNumber < 10) {
            return "00" + pokemonIdNumber + ".png";
        }
        if (pokemonIdNumber < 100) {
            return "0" + pokemonIdNumber + ".png";
        }
        return pokemonIdNumber + ".png";
    }
}
