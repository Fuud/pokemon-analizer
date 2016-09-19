var app = angular.module('pokemons', [], function ($locationProvider) {
    $locationProvider.html5Mode({
        enabled: true,
        requireBase: false
    });
});

app.factory('notAuthorizedInterceptor', function ($q, $window) {
    return {
        'responseError': function (rejection) {
            console.log(rejection)
            if (rejection.status == 401) {
                $window.location.href = "/";
            }
            return $q.reject(rejection)
        }
    }
});

app.config(['$httpProvider', function ($httpProvider) {
    $httpProvider.interceptors.push('notAuthorizedInterceptor');
}]);

app.service('endpointService', function ($http, $location, $window) {
    var refreshToken = $location.search()['refreshToken'];
    if (refreshToken) {
        console.log("Will use refresh token: " + refreshToken);
    } else {
        // no refresh token - let's autorize
        $window.location = "/";
    }

    this.pokemonList = function () {
        return $http.get('/pokemon-list-json?refreshToken=' + refreshToken)
    };

    this.setFavorite = function (pokemonId, isFavorite) {
        return $http.get("/favoritize?pokemonId=" + pokemonId + "&favorite=" + isFavorite + "&refreshToken=" + refreshToken)
    };

    this.transfer = function (pokemonId) {
        return $http.get("/transfer?pokemonId=" + pokemonId + "&refreshToken=" + refreshToken)
    };

    this.evolve = function (pokemonId, playerLevel) {
        return $http.get("/evolve?pokemonId=" + pokemonId + "&playerLevel=" + playerLevel + "&refreshToken=" + refreshToken)
    };
});

app.controller('pokemonsController', function ($scope, $interval, endpointService) {
    $scope.initialized = false;

    var init = function () {
        endpointService.pokemonList()
            .success(function (data) {
                $scope.pokemon_data = data;
                $scope.lastUpdateDate = new Date();
                $scope.initialized = true;
            })
            .error(function (data) {
                setTimeout(init, 1000);
            });
    };

    init();

    $interval(init, 10000);

    $scope.pokemonCount = function () {
        var count = 0;

        for (var i = 0; i < $scope.pokemon_data.pokemonsByClass.length; i++) {
            var pokemonByClass = $scope.pokemon_data.pokemonsByClass[i];
            count = count + pokemonByClass.pokemons.length;
        }

        return count;
    };

    $scope.todayPokemonCount = function () {
        var count = 0;

        for (var i = 0; i < $scope.pokemon_data.pokemonsByClass.length; i++) {
            var pokemonByClass = $scope.pokemon_data.pokemonsByClass[i];
            var pokemons = pokemonByClass.pokemons;
            for (var j = 0; j < pokemons.length; j++) {
                if ($scope.isToday(pokemons[j].creationTimeMs)) {
                    count = count + 1;
                }
            }
        }

        return count;
    };

    $scope.yesterdayPokemonCount = function () {
        var count = 0;

        for (var i = 0; i < $scope.pokemon_data.pokemonsByClass.length; i++) {
            var pokemonByClass = $scope.pokemon_data.pokemonsByClass[i];
            var pokemons = pokemonByClass.pokemons;
            for (var j = 0; j < pokemons.length; j++) {
                if ($scope.isYesterday(pokemons[j].creationTimeMs)) {
                    count = count + 1;
                }
            }
        }

        return count;
    };

    $scope.actionsDisabled = true;

    $scope.isGoodPokemon = function (pokemonData) {
        return pokemonData.individualAttack >= 13 && pokemonData.individualDefence >= 13 && pokemonData.individualStamina >= 13;
    };

    $scope.isToday = function (dateInMs) {
        var now = moment();
        return moment(dateInMs).isSame(now, 'day')
    };

    $scope.isYesterday = function (dateInMs) {
        var yesterday = moment().subtract(1, 'day');
        return moment(dateInMs).isSame(yesterday, 'd')
    };

    $scope.evolvable = function (pokemonClass) {
        return pokemonClass.candyToEvole != 0;
    };

    $scope.evolutionCount = function (pokemonClass) {
        if (pokemonClass.candyToEvole == 0) {
            return 0;
        } else {
            return Math.floor(pokemonClass.totalCandies / pokemonClass.candyToEvole)
        }
    };

    $scope.candiesLeft = function (pokemonClass) {
        if (pokemonClass.candyToEvole == 0) {
            return 0;
        } else {
            return Math.floor(pokemonClass.totalCandies % pokemonClass.candyToEvole)
        }
    };

    $scope.removePokemon = function (pokemonClass, pokemonId) {
        var pokemons = pokemonClass.pokemons;
        for (var i = 0; i < pokemons.length; i++) {
            if (pokemons[i].pokemonId == pokemonId) {
                pokemons.splice(i, 1); // remove pokemon
                return;
            }
        }
    };

    $scope.awardCandies = function (familyId, candyAwarded) {
        var pokemonClasses = $scope.pokemon_data.pokemonsByClass;
        for (var i = 0; i < pokemonClasses.length; i++) {
            var pokemonClass = pokemonClasses[i];
            if (pokemonClass.pokemonFamilyId == familyId) {
                pokemonClass.totalCandies = pokemonClass.totalCandies + candyAwarded;
            }
        }
    };

    $scope.addPokemon = function (newPokemonClassId, pokemonData) {
        var pokemonClasses = $scope.pokemon_data.pokemonsByClass;
        for (var i = 0; i < pokemonClasses.length; i++) {
            var pokemonClass = pokemonClasses[i];
            if (pokemonClass.pokemonClassId == newPokemonClassId) {
                pokemonClass.pokemons.push(pokemonData);
                return;
            }
        }

        // no pokemon class found

        init();
    };

    $scope.setFavorite = function (pokemonClass, pokemon, isFavorite) {
        endpointService.setFavorite(pokemon.pokemonId, isFavorite)
            .success(function (data) {
                if (data.success) {
                    pokemon.favorite = isFavorite;
                } else {
                    alert("Can not set/remove favorite flag for pokemon " + pokemonClass.pokemonClassId + " with cp " + pokemon.cp + " because of " + data.reason)
                }
            })
            .error(function (data) {
                alert("Can not set/remove favorite flag for pokemon with cp " + pokemon.cp)
            });
    };

    $scope.transfer = function (pokemonClass, pokemon) {
        if (pokemon.favorite) {
            alert("Can not transfer favorite pokemon");
            return;
        }
        endpointService.transfer(pokemon.pokemonId)
            .success(function (data) {
                if (data.success) {
                    $scope.awardCandies(pokemonClass.pokemonFamilyId, data.candyAwarded);
                    $scope.removePokemon(pokemonClass, pokemon.pokemonId)
                } else {
                    alert("Can not transfer pokemon " + pokemonClass.pokemonClassId + " with cp " + pokemon.cp + " because of " + data.reason)
                }
            })
            .error(function (data) {
                alert("Can not transfer pokemon " + pokemonClass.pokemonClassId + " with cp " + pokemon.cp)
            });
    };

    $scope.evolve = function (pokemonClass, pokemon) {
        if (pokemon.favorite) {
            alert("Can not evolve favorite pokemon");
            return;
        }
        endpointService.evolve(pokemon.pokemonId, $scope.pokemon_data.userLevel)
            .success(function (data) {
                if (data.success) {
                    $scope.awardCandies(pokemonClass.pokemonFamilyId, data.candyAwarded);
                    $scope.removePokemon(pokemonClass, pokemon.pokemonId);
                    $scope.addPokemon(data.newPokemonClassId, data.newPokemonData)
                } else {
                    alert("Can not evolve pokemon " + pokemonClass.pokemonClassId + " with cp " + pokemon.cp + " because of " + data.reason)
                }
            })
            .error(function (data) {
                alert("Can not evolve pokemon " + pokemonClass.pokemonClassId + " with cp " + pokemon.cp)
            });
    };

});