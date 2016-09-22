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
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Service service;
    private final RefreshTokenStorage refreshTokenStorage;

    @Autowired
    public Endpoint(Service service, RefreshTokenStorage refreshTokenStorage) {
        this.service = service;
        this.refreshTokenStorage = refreshTokenStorage;
    }

    @RequestMapping(method = RequestMethod.GET, value = "users")
    public List<String> listUsers() {
        return refreshTokenStorage.getUserNames();
    }

    @RequestMapping(method = RequestMethod.GET, value = "get-refresh-token")
    public RedirectView getUserName(@RequestParam(value = "token") String token) throws Exception {
        final GoogleUserCredentialProvider provider = new GoogleUserCredentialProvider(httpClient);
        provider.login(token);

        String userName = null;
        for (int i = 0; i < 10; i++) {
            try {
                userName = service.getUserNameByRefreshToken(provider.getRefreshToken());
                break;
            } catch (LoginFailedException e) {
                return new RedirectView("/");
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                Thread.sleep(1000);
            }
        }

        if (userName == null) {
            return new RedirectView("/");
        } else {
            return new RedirectView("/pokemons.html?username=" + userName);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "pokemon-list-json")
    public PokemonListData pokemonListJson(@RequestParam(value = "username") String username) throws Exception {
        return service.pokemonListJson(username);
    }

    @RequestMapping(method = RequestMethod.GET, value = "evolve")
    public EvolveResult evolve(@RequestParam("pokemonId") long pokemonId,
                               @RequestParam("playerLevel") int playerLevel,
                               @RequestParam(value = "username") String userName) throws Exception {
        return service.evolve(pokemonId, playerLevel, userName);
    }

    @RequestMapping(method = RequestMethod.GET, value = "transfer")
    public TransferResult transfer(@RequestParam("pokemonId") long pokemonId,
                                   @RequestParam(value = "username") String userName) throws Exception {
        return service.transfer(pokemonId, userName);
    }

    @RequestMapping(method = RequestMethod.GET, value = "favoritize")
    public FavoritizeResult favoritize(@RequestParam("pokemonId") long pokemonId,
                                       @RequestParam(value = "favorite") boolean favorite,
                                       @RequestParam(value = "username") String userName) throws Exception {
        return service.favoritize(pokemonId, favorite, userName);
    }
}
