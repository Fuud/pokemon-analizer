package fuud;

import POGOProtos.Enums.PokemonFamilyIdOuterClass;
import POGOProtos.Enums.PokemonIdOuterClass;
import POGOProtos.Networking.Requests.Messages.EvolvePokemonMessageOuterClass;
import POGOProtos.Networking.Requests.Messages.ReleasePokemonMessageOuterClass;
import POGOProtos.Networking.Requests.RequestTypeOuterClass;
import POGOProtos.Networking.Responses.EvolvePokemonResponseOuterClass;
import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.CandyJar;
import com.pokegoapi.api.inventory.Inventories;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.api.pokemon.PokemonDetails;
import com.pokegoapi.api.pokemon.PokemonMeta;
import com.pokegoapi.api.pokemon.PokemonMetaRegistry;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.main.ServerRequest;
import fuud.dto.HttpResult;
import j2html.tags.ContainerTag;
import j2html.tags.EmptyTag;
import j2html.tags.Tag;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static j2html.TagCreator.*;

@RestController
public class Endpoint {
    private final OkHttpClient httpClient = new OkHttpClient();

    @RequestMapping(method = RequestMethod.GET, value = "get-refresh-token")
    public RedirectView getRefreshToken(@RequestParam(value = "token") String token) throws Exception {
        final GoogleUserCredentialProvider provider = new GoogleUserCredentialProvider(httpClient);
        provider.login(token);
        final String withToken = "/pokemon-list?refreshToken=" + provider.getRefreshToken();
        return new RedirectView(withToken);
    }

    @RequestMapping(method = RequestMethod.GET, value = "pokemon-list", produces = MediaType.TEXT_HTML_VALUE)
    public String pokemonList(@RequestParam(value = "refreshToken") String refreshToken) throws Exception {
        PokemonGo go = new PokemonGo(new GoogleUserCredentialProvider(httpClient, refreshToken), httpClient);

        // After this you can access the api from the PokemonGo instance :
        final Inventories inventories = go.getInventories();// to get all his inventories (Pokemon, backpack, egg, incubator)

        final List<Pokemon> pokemons = inventories.getPokebank().getPokemons();
        final CandyJar candyjar = inventories.getCandyjar();


        final long todayCount = pokemons.stream().filter(this::isToday).count();
        final long yesterdayCount = pokemons.stream().filter(this::isYesterday).count();

        return html().with(
                head().with(
                        link().withRel("stylesheet").withHref("style.css"),
                        script().withType("text/javascript").withSrc("http://code.jquery.com/jquery.min.js"),
                        script().withType("text/javascript").withSrc("app-scripts.js")
                ),
                body().with(
                        h1("Pokemons " + pokemons.size() + " (today: " + todayCount + "; yesterday: " + yesterdayCount+")"),
                        br(),
                        input().withType("button").withValue("Enable actions").attr("onClick", "enableActions()"),
                        br(),
                        br(),
                        table().with(
                                generatePokemonRows(pokemons, candyjar, refreshToken)
                        )
                )
        ).render();
    }

    @RequestMapping(method = RequestMethod.GET, value = "evolve")
    public HttpResult evolve(@RequestParam("pokemonId") long pokemonId, @RequestParam(value = "refreshToken") String refreshToken) throws Exception {
        try {
            PokemonGo go = new PokemonGo(new GoogleUserCredentialProvider(httpClient, refreshToken), httpClient);

            EvolvePokemonMessageOuterClass.EvolvePokemonMessage reqMsg =
                    EvolvePokemonMessageOuterClass.EvolvePokemonMessage.newBuilder().
                            setPokemonId(pokemonId).
                            build();

            ServerRequest serverRequest = new ServerRequest(RequestTypeOuterClass.RequestType.EVOLVE_POKEMON, reqMsg);
            go.getRequestHandler().sendServerRequests(serverRequest);

            EvolvePokemonResponseOuterClass.EvolvePokemonResponse response =
                    EvolvePokemonResponseOuterClass.EvolvePokemonResponse.parseFrom(serverRequest.getData());

            if (response.getResult() == EvolvePokemonResponseOuterClass.EvolvePokemonResponse.Result.SUCCESS) {
                String message = "Pokemon successfully evolved.\n" +
                        "New pokemon: " + response.getEvolvedPokemonData().getPokemonId().name() + ";\n" +
                        "Experience awarded: " + response.getExperienceAwarded() + ";\n" +
                        "Candy awarded: " + response.getCandyAwarded();
                return new HttpResult(true, message);
            } else {
                return new HttpResult(false, "Can not evolve pokemon because of " + response.getResult());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new HttpResult(false, "Can not evolve pokemon because of " + e.getMessage());
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "transfer")
    public HttpResult transfer(@RequestParam("pokemonId") long pokemonId, @RequestParam(value = "refreshToken") String refreshToken) {
        try {
            PokemonGo go = new PokemonGo(new GoogleUserCredentialProvider(httpClient, refreshToken), httpClient);
            ReleasePokemonMessageOuterClass.ReleasePokemonMessage reqMsg =
                    ReleasePokemonMessageOuterClass.ReleasePokemonMessage.
                            newBuilder().
                            setPokemonId(pokemonId).build();

            ServerRequest serverRequest = new ServerRequest(RequestTypeOuterClass.RequestType.RELEASE_POKEMON, reqMsg);
            go.getRequestHandler().sendServerRequests(serverRequest);

            ReleasePokemonResponseOuterClass.ReleasePokemonResponse response;
            response = ReleasePokemonResponseOuterClass.ReleasePokemonResponse.parseFrom(serverRequest.getData());

            if (response.getResult() == ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result.SUCCESS) {
                return new HttpResult(true,
                        "Pokemon successfully transferred.\n" +
                                "Candy awarded: " + response.getCandyAwarded());
            } else {
                return new HttpResult(false, "Can not transfer pokemon because of " + response.getResult());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new HttpResult(false, "Can not transfer pokemon because of " + e.getMessage());
        }
    }

    private ContainerTag generateHeader() {
        return tr().with(
                th("cp"),
                th("hp"),
                th("level"),
                th("attack"),
                th("defence"),
                th("stamina"),
                th("norm cp"),
                th("remaining dust"),
                th()
        );
    }

    private List<Tag> generatePokemonRows(List<Pokemon> pokemons, CandyJar candyjar, String refreshToken) {
        final Map<PokemonIdOuterClass.PokemonId, List<Pokemon>> pokemonsById = pokemons.stream().collect(Collectors.toMap(
                PokemonDetails::getPokemonId,
                Collections::singletonList,
                (pokemonsLeft, pokemonsRight) ->
                        Stream.
                                of(pokemonsLeft, pokemonsRight).
                                flatMap(Collection::stream).
                                sorted((p1, p2) -> -(p1.getCp() - p2.getCp())).
                                collect(Collectors.toList())
        ));

        List<Tag> result = new ArrayList<>();

        Arrays.stream(PokemonIdOuterClass.PokemonId.values())
                .filter(pokemonsById::containsKey)
                .forEachOrdered(
                        pokemonId -> {
                            final PokemonMeta pokemonMeta = PokemonMetaRegistry.getMeta(pokemonId);
                            final int totalCandies = candyjar.getCandies(pokemonMeta.getFamily());
                            final int candyToEvolve = pokemonMeta.getCandyToEvolve();
                            final boolean evolutionAvailable = candyToEvolve != 0;
                            final int availableEvolutionCount;
                            final int elapsedCandiesAfterAllEvol;
                            final String evolutionString;

                            if (evolutionAvailable) {
                                availableEvolutionCount = totalCandies / candyToEvolve;
                                elapsedCandiesAfterAllEvol = totalCandies % candyToEvolve;
                                evolutionString = "(" + totalCandies + " == " + availableEvolutionCount + " evolutions and " + elapsedCandiesAfterAllEvol + " will left)";
                            } else {
                                evolutionString = "(no evolution)";
                                availableEvolutionCount = 0;
                                elapsedCandiesAfterAllEvol = 0;
                            }


                            result.add(
                                    tr().with(
                                            td().attr("colspan", "13").with(
                                                    img().withSrc("/sprites/" + imageFor(pokemonId.getNumber())).attr("width", "32px").attr("height", "32px"),
                                                    text(pokemonId.name() + " " + evolutionString + " absMaxCp: " + getAbsoluteMaxCp(pokemonId))
                                            )
                                    )
                            );

                            result.add(generateHeader());

                            final List<Pokemon> pokemonsOfType = pokemonsById.get(pokemonId);
                            for (int i = 0; i < pokemonsOfType.size(); i++) {
                                final Pokemon pokemon = pokemonsOfType.get(i);

                                final boolean isSuperFresh = isToday(pokemon);
                                final boolean isFresh = isYesterday(pokemon);

                                final String freshImage;
                                if (isSuperFresh) {
                                    freshImage = "new_green.png";
                                } else if (isFresh) {
                                    freshImage = "new_red.png";
                                } else {
                                    freshImage = "blank.png";
                                }

                                boolean isCool = pokemon.getIndividualAttack() >= 14 && pokemon.getIndividualDefense() >= 14 && pokemon.getIndividualStamina() >= 14;
                                String coolBg = isCool ? "yellow" : "transparent";


                                final boolean candidateForEvolve = i < availableEvolutionCount;
                                result.add(
                                        tr().with(
                                                td("" + pokemon.getCp()),
                                                td("" + pokemon.getStamina()),
                                                td("" + pokemon.getLevel()),
                                                td("" + pokemon.getIndividualAttack()).attr("bgcolor", coolBg),
                                                td("" + pokemon.getIndividualDefense()).attr("bgcolor", coolBg),
                                                td("" + pokemon.getIndividualStamina()).attr("bgcolor", coolBg),
                                                td("" + getNormalizedMaxCp(pokemon)),
                                                td("" + getRemindingDust(pokemon.getLevel())),
                                                td(" ").attr("bgcolor", !candidateForEvolve ? "green" : "transparent"),
                                                td().with(
                                                        createTransferButton(pokemon.getId(), refreshToken)
                                                ),

                                                td(" ").attr("bgcolor", candidateForEvolve ? "green" : "transparent"),
                                                td().with(
                                                        createEvolveButton(pokemon.getId(), refreshToken)
                                                ),
                                                td().with(
                                                        img().withSrc("/sprites/" + freshImage).attr("width", "16px").attr("height", "16px").attr("title", new Date(pokemon.getCreationTimeMs()).toString())
                                                )
                                        )
                                );
                            }
                        }
                );

        return result;
    }

    private boolean isToday(Pokemon pokemon) {
        return DateUtils.isSameDay(new Date(), new Date(pokemon.getCreationTimeMs()));
    }

    private boolean isYesterday(Pokemon pokemon) {
        Date creationDate = new Date(pokemon.getCreationTimeMs());
        final Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, -1);
        return DateUtils.isSameDay(calendar.getTime(), creationDate);
    }

    private int getRemindingDust(float level) {
        int sum = 0;
        for (; level < 22; level += 0.5) {
            sum += getStartdustCostsForPowerup(level);
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

    private String getNormalizedMaxCp(Pokemon pokemon) {
        final int individualAttack = pokemon.getIndividualAttack();
        final int individualDefense = pokemon.getIndividualDefense();
        final int individualStamina = pokemon.getIndividualStamina();
        final PokemonFamilyIdOuterClass.PokemonFamilyId pokemonFamily = pokemon.getPokemonFamily();
        if (pokemonFamily != PokemonFamilyIdOuterClass.PokemonFamilyId.FAMILY_EEVEE) {
            final PokemonIdOuterClass.PokemonId highestForFamily = PokemonMetaRegistry.getHighestForFamily().get(pokemonFamily);
            final int max = getNormalizedMaxCp(individualAttack, individualDefense, individualStamina, highestForFamily);
            return "" + max;
        } else {
            return "V" + getNormalizedMaxCp(individualAttack, individualDefense, individualStamina, PokemonIdOuterClass.PokemonId.VAPOREON) +
                    " / " +
                    "J" + getNormalizedMaxCp(individualAttack, individualDefense, individualStamina, PokemonIdOuterClass.PokemonId.JOLTEON) +
                    " / " +
                    "F" + getNormalizedMaxCp(individualAttack, individualDefense, individualStamina, PokemonIdOuterClass.PokemonId.FLAREON);

        }
    }

    private String getAbsoluteMaxCp(PokemonIdOuterClass.PokemonId pokemon) {
        final int individualAttack = 15;
        final int individualDefense = 15;
        final int individualStamina = 15;
        final PokemonFamilyIdOuterClass.PokemonFamilyId pokemonFamily = PokemonMetaRegistry.getMeta(pokemon).getFamily();
        if (pokemonFamily != PokemonFamilyIdOuterClass.PokemonFamilyId.FAMILY_EEVEE) {
            final PokemonIdOuterClass.PokemonId highestForFamily = PokemonMetaRegistry.getHighestForFamily().get(pokemonFamily);
            final int max = getNormalizedMaxCp(individualAttack, individualDefense, individualStamina, highestForFamily);
            return "" + max;
        } else {
            return "V" + getNormalizedMaxCp(individualAttack, individualDefense, individualStamina, PokemonIdOuterClass.PokemonId.VAPOREON) +
                    " " +
                    "J" + getNormalizedMaxCp(individualAttack, individualDefense, individualStamina, PokemonIdOuterClass.PokemonId.JOLTEON) +
                    " " +
                    "F" + getNormalizedMaxCp(individualAttack, individualDefense, individualStamina, PokemonIdOuterClass.PokemonId.FLAREON);

        }
    }

    private int getNormalizedMaxCp(int individualAttack, int individualDefense, int individualStamina, PokemonIdOuterClass.PokemonId highestForFamily) {
        final PokemonMeta highestPokemonMeta = PokemonMetaRegistry.getMeta(highestForFamily);
        final int attack = individualAttack + highestPokemonMeta.getBaseAttack();
        final int defense = individualDefense + highestPokemonMeta.getBaseDefense();
        final int stamina = individualStamina + highestPokemonMeta.getBaseStamina();
//        return (int) (attack * Math.pow(defense, 0.5) * Math.pow(stamina, 0.5) * Math.pow(0.79030001f, 2) / 10f);
        return (int) (attack * Math.pow(defense, 0.5) * Math.pow(stamina, 0.5) * Math.pow(0.62656713f, 2) / 10f);
    }

    private EmptyTag createEvolveButton(long id, String refreshToken) {
        return input().withType("button").withValue("Evolve").attr("disabled", "disabled").attr("onClick", "evolvePokemon(this, \"" + id + "\", \"" + refreshToken + "\")");
    }

    private EmptyTag createTransferButton(long id, String refreshToken) {
        return input().withType("button").withValue("Transfer").attr("disabled", "disabled").attr("onClick", "transferPokemon(this, \"" + id + "\", \"" + refreshToken + "\")");
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
