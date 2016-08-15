function evolvePokemon(pokemonId, refreshToken) {
    $.get(
        "evolve",
        {
            pokemonId : pokemonId,
            refreshToken : refreshToken
        },
        function(data) {
            alert(data);
            console.log(data);
        }
    );
}

function transferPokemon(pokemonId, refreshToken) {
    $.get(
        "transfer",
        {
            pokemonId : pokemonId,
            refreshToken : refreshToken
        },
        function(data) {
            alert(data);
            console.log(data);
        }
    );
}