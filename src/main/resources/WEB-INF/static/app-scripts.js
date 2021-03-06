function evolvePokemon(button, pokemonId, refreshToken) {
    button.disabled = true;
    button.value = "Evolving...";
    $.ajax({
        url: "evolve?pokemonId=" + pokemonId + "&refreshToken=" + refreshToken,
        success: function (data) {
            if (data.result) {
                button.value = "Already evolved";
            } else {
                alert(data.reason);
                button.disabled = false;
                button.value = "Done";
            }
        },
        error: function (xmlHttpRequest, textStatus, errorThrown) {
            alert(xmlHttpRequest.status + " : " + textStatus);
            button.disabled = false;
            button.value = "Evolve";
        }
    });
}

function transferPokemon(button, pokemonId, refreshToken) {
    button.disabled = true;
    button.value = "Transferring...";
    $.ajax({
        url: "transfer?pokemonId=" + pokemonId + "&refreshToken=" + refreshToken,
        success: function (data) {
            if (data.result) {
                button.value = "Done";
            } else {
                alert(data.reason);
                button.disabled = false;
                button.value = "Transfer";
            }
        },
        error: function (xmlHttpRequest, textStatus, errorThrown) {
            alert(xmlHttpRequest.status + " : " + textStatus);
            button.disabled = false;
            button.value = "Transfer";
        }
    });
}

function setFavorite(image, pokemonId, favorite, refreshToken) {
    $.ajax({
        url: "favoritize?pokemonId=" + pokemonId + "&favorite="+favorite+"&refreshToken=" + refreshToken,
        success: function (data) {
            if (data.result) {
                if (favorite){
                    image.src = "/sprites/favorite.png";
                }else {
                    image.src = "/sprites/non-favorite.png";
                }
            } else {
                alert(data.reason);
            }
        },
        error: function (xmlHttpRequest, textStatus, errorThrown) {
            alert(xmlHttpRequest.status + " : " + textStatus);
        }
    });
}

function enableActions() {
    $("input[type=button]").removeAttr("disabled", "disabled");
}