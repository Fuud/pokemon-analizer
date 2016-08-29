package fuud;

import POGOProtos.Data.PlayerDataOuterClass;
import POGOProtos.Data.PokemonDataOuterClass;
import POGOProtos.Enums.PokemonFamilyIdOuterClass;
import POGOProtos.Enums.PokemonIdOuterClass;
import POGOProtos.Inventory.InventoryItemDataOuterClass;
import POGOProtos.Inventory.InventoryItemOuterClass;
import POGOProtos.Inventory.Item.ItemDataOuterClass;
import POGOProtos.Inventory.Item.ItemIdOuterClass;
import POGOProtos.Networking.Requests.Messages.*;
import POGOProtos.Networking.Requests.RequestTypeOuterClass;
import POGOProtos.Networking.Responses.*;
import com.google.protobuf.InvalidProtocolBufferException;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.*;
import com.pokegoapi.api.pokemon.*;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.main.ServerRequest;
import fuud.copied.GoogleUserCredentialProvider;
import fuud.copied.PokemonCpUtils;
import fuud.copied.RequestHandler;
import fuud.dto.*;
import fuud.dto.Inventories;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static POGOProtos.Networking.Responses.SetFavoritePokemonResponseOuterClass.SetFavoritePokemonResponse.Result.SUCCESS;

@RestController
public class Endpoint {
    private static final Logger logger = LoggerFactory.getLogger(Endpoint.class);
    private final OkHttpClient httpClient;
    private final CredentialProviderHolder credentialProviderHolder;
    private final RequestHandlerHolder requestHandlerHolder;

    @Autowired
    public Endpoint(OkHttpClient httpClient, CredentialProviderHolder credentialProviderHolder, RequestHandlerHolder requestHandlerHolder) {
        this.httpClient = httpClient;
        this.credentialProviderHolder = credentialProviderHolder;
        this.requestHandlerHolder = requestHandlerHolder;
    }


    @RequestMapping(method = RequestMethod.GET, value = "get-refresh-token")
    public RedirectView getRefreshToken(@RequestParam(value = "token") String authcode) throws Exception {
        final GoogleUserCredentialProvider provider = GoogleUserCredentialProvider.login(httpClient, authcode);
        final String withToken = "/pokemons.html?refreshToken=" + provider.getRefreshToken();
        return new RedirectView(withToken);
    }

    @RequestMapping(method = RequestMethod.GET, value = "pokemon-list")
    public RedirectView pokemonList(@RequestParam(value = "refreshToken") String refreshToken) throws Exception {
        final String withToken = "/pokemons.html?refreshToken=" + refreshToken;
        return new RedirectView(withToken);
    }

    @RequestMapping(method = RequestMethod.GET, value = "pokemon-list-json")
    public PokemonListData pokemonListJson(@RequestParam(value = "refreshToken") String refreshToken) throws Exception {
        final RequestHandler requestHandler = requestHandlerHolder.byRefreshToken(refreshToken);


        final PlayerDataOuterClass.PlayerData playerData = getPlayerData(requestHandler);
        final Inventories inventories = getInventories(requestHandler);
        final List<PokemonDataOuterClass.PokemonData> pokemons = inventories.getPokemons();
        final Map<PokemonFamilyIdOuterClass.PokemonFamilyId, Integer> candyjar = inventories.getCandies();
        final int playerLevel = inventories.getPlayerStats().getLevel();

        final PokemonListData data = new PokemonListData();

        data.setUserName(playerData.getUsername());
        data.setUserLevel(playerLevel);

        final Map<PokemonIdOuterClass.PokemonId, List<PokemonDataOuterClass.PokemonData>> pokemonsById = groupPokemons(pokemons);

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

    @RequestMapping(method = RequestMethod.GET, value = "evolve")
    public EvolveResult evolve(@RequestParam("pokemonId") long pokemonId, @RequestParam("playerLevel") int playerLevel, @RequestParam(value = "refreshToken") String refreshToken) {
        try {
            final RequestHandler requestHandler = requestHandlerHolder.byRefreshToken(refreshToken);

            EvolvePokemonMessageOuterClass.EvolvePokemonMessage reqMsg =
                    EvolvePokemonMessageOuterClass.EvolvePokemonMessage.newBuilder().
                            setPokemonId(pokemonId).
                            build();

            ServerRequest serverRequest = new ServerRequest(RequestTypeOuterClass.RequestType.EVOLVE_POKEMON, reqMsg);
            requestHandler.sendServerRequests(serverRequest);

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
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return EvolveResult.failed("Can not evolve pokemon because of " + e.getMessage());
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "transfer")
    public TransferResult transfer(@RequestParam("pokemonId") long pokemonId, @RequestParam(value = "refreshToken") String refreshToken) {
        try {
            final RequestHandler requestHandler = requestHandlerHolder.byRefreshToken(refreshToken);

            ReleasePokemonMessageOuterClass.ReleasePokemonMessage reqMsg =
                    ReleasePokemonMessageOuterClass.ReleasePokemonMessage.
                            newBuilder().
                            setPokemonId(pokemonId).build();

            ServerRequest serverRequest = new ServerRequest(RequestTypeOuterClass.RequestType.RELEASE_POKEMON, reqMsg);
            requestHandler.sendServerRequests(serverRequest);

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

    @RequestMapping(method = RequestMethod.GET, value = "favoritize")
    public FavoritizeResult favoritize(@RequestParam("pokemonId") long pokemonId, @RequestParam(value = "favorite") boolean favorite, @RequestParam(value = "refreshToken") String refreshToken) {
        try {
            PokemonGo go = new PokemonGo(credentialProviderHolder.byRefreshToken(refreshToken), httpClient);
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
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return FavoritizeResult.failed("Can not favoritize pokemon because of " + e.getMessage());
        }
    }

    public PlayerDataOuterClass.PlayerData getPlayerData(RequestHandler requestHandler) throws RemoteServerException, LoginFailedException {

        GetPlayerMessageOuterClass.GetPlayerMessage getPlayerReqMsg = GetPlayerMessageOuterClass.GetPlayerMessage.newBuilder().build();
        ServerRequest getPlayerServerRequest = new ServerRequest(RequestTypeOuterClass.RequestType.GET_PLAYER, getPlayerReqMsg);
        requestHandler.sendServerRequests(getPlayerServerRequest);

        GetPlayerResponseOuterClass.GetPlayerResponse playerResponse = null;
        try {
            playerResponse = GetPlayerResponseOuterClass.GetPlayerResponse.parseFrom(getPlayerServerRequest.getData());
        } catch (InvalidProtocolBufferException e) {
            throw new RemoteServerException(e);
        }

        return playerResponse.getPlayerData();
    }

    public Inventories getInventories(RequestHandler requestHandler) throws LoginFailedException, RemoteServerException {
        GetInventoryMessageOuterClass.GetInventoryMessage invReqMsg = GetInventoryMessageOuterClass.GetInventoryMessage.newBuilder()
                .setLastTimestampMs(0)
                .build();
        ServerRequest inventoryRequest = new ServerRequest(RequestTypeOuterClass.RequestType.GET_INVENTORY, invReqMsg);
        requestHandler.sendServerRequests(inventoryRequest);

        GetInventoryResponseOuterClass.GetInventoryResponse response = null;
        try {
            response = GetInventoryResponseOuterClass.GetInventoryResponse.parseFrom(inventoryRequest.getData());
        } catch (InvalidProtocolBufferException e) {
            throw new RemoteServerException(e);
        }

        final List<PokemonDataOuterClass.PokemonData> pokemons = new ArrayList<>();
        final List<EggPokemon> eggs = new ArrayList<>();
        final List<Item> items = new ArrayList<>();
        final Map<PokemonFamilyIdOuterClass.PokemonFamilyId, Integer> candies = new HashMap<>();
        Stats stats = null;

        for (InventoryItemOuterClass.InventoryItem inventoryItem : response.getInventoryDelta().getInventoryItemsList()) {
            InventoryItemDataOuterClass.InventoryItemData itemData = inventoryItem.getInventoryItemData();

            // hatchery
            if (itemData.getPokemonData().getPokemonId() == PokemonIdOuterClass.PokemonId.MISSINGNO && itemData.getPokemonData().getIsEgg()) {
                eggs.add(new EggPokemon(itemData.getPokemonData()));
            }

            // pokebank
            if (itemData.getPokemonData().getPokemonId() != PokemonIdOuterClass.PokemonId.MISSINGNO) {
                pokemons.add(inventoryItem.getInventoryItemData().getPokemonData());
            }

            // items
            if (itemData.getItem().getItemId() != ItemIdOuterClass.ItemId.UNRECOGNIZED
                    && itemData.getItem().getItemId() != ItemIdOuterClass.ItemId.ITEM_UNKNOWN) {
                ItemDataOuterClass.ItemData item = itemData.getItem();
                items.add(new Item(item));
            }

            // candyjar
            if (itemData.getCandy().getFamilyId() != PokemonFamilyIdOuterClass.PokemonFamilyId.UNRECOGNIZED
                    && itemData.getCandy().getFamilyId() != PokemonFamilyIdOuterClass.PokemonFamilyId.FAMILY_UNSET) {
                candies.put(
                        itemData.getCandy().getFamilyId(),
                        itemData.getCandy().getCandy()
                );
            }

            if (itemData.hasPlayerStats()) {
                stats = new Stats(itemData.getPlayerStats());
            }
        }

        return new Inventories(pokemons, eggs, items, candies, stats);
    }

    private PokemonClassData generatePokemonClassData(PokemonIdOuterClass.PokemonId pokemonClassId, List<PokemonDataOuterClass.PokemonData> pokemons, Map<PokemonFamilyIdOuterClass.PokemonFamilyId, Integer> candyjar, int playerLevel) {

        final PokemonMeta pokemonMeta = PokemonMetaRegistry.getMeta(pokemonClassId);
        final int totalCandies = candyjar.get(pokemonMeta.getFamily());
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
                map(pokemon -> convertToPokemonData(pokemon, playerLevel)).
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

        return pokemonData;
    }

    private String getCpForLastEvolution(int individualAttack, int individualDefense, int individualStamina, PokemonIdOuterClass.PokemonId pokemonId, int playerLevel) {
        final PokemonFamilyIdOuterClass.PokemonFamilyId pokemonFamily = PokemonMetaRegistry.getMeta(pokemonId).getFamily();

        if (pokemonFamily != PokemonFamilyIdOuterClass.PokemonFamilyId.FAMILY_EEVEE) {
            final PokemonIdOuterClass.PokemonId highestForFamily = PokemonMetaRegistry.getHighestForFamily().get(pokemonFamily);
            return "" + getCp(individualAttack, individualDefense, individualStamina, playerLevel, highestForFamily);
        } else {
            return "V" + getCp(individualAttack, individualDefense, individualStamina, playerLevel, PokemonIdOuterClass.PokemonId.VAPOREON) +
                    " " +
                    "J" + getCp(individualAttack, individualDefense, individualStamina, playerLevel, PokemonIdOuterClass.PokemonId.JOLTEON) +
                    " " +
                    "F" + getCp(individualAttack, individualDefense, individualStamina, playerLevel, PokemonIdOuterClass.PokemonId.FLAREON);

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

    private Map<PokemonIdOuterClass.PokemonId, List<PokemonDataOuterClass.PokemonData>> groupPokemons(List<PokemonDataOuterClass.PokemonData> pokemons) {
        return pokemons.stream().collect(Collectors.toMap(
                PokemonDataOuterClass.PokemonData::getPokemonId,
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
