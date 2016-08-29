/*
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fuud.copied;

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.auth.GoogleAuthTokenJson;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.util.SystemTimeImpl;
import com.pokegoapi.util.Time;
import com.squareup.moshi.Moshi;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class GoogleUserCredentialProvider extends CredentialProvider {
    private static final Logger logger = LoggerFactory.getLogger(GoogleUserCredentialProvider.class);

    private static final String SECRET = "NCjF1TLi2CcY6t5mt0ZveuL7";
    private static final String CLIENT_ID = "848232511240-73ri3t7plvk96pj4f85uj8otdat2alem.apps.googleusercontent.com";
    private static final String OAUTH_TOKEN_ENDPOINT = "https://www.googleapis.com/oauth2/v4/token";
    public static final String LOGIN_URL = "https://accounts.google.com/o/oauth2/auth?client_id=848232511240-73ri3t7plvk96pj4f85uj8otdat2alem.apps.googleusercontent.com&redirect_uri=urn%3Aietf%3Awg%3Aoauth%3A2.0%3Aoob&response_type=code&scope=openid%20email%20https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.email";
    //We try and refresh token 5 minutes before it actually expires
    private static final long REFRESH_TOKEN_BUFFER_TIME = 5 * 60 * 1000;

    private final OkHttpClient client;
    private final String refreshToken;
    private final AtomicReference<TokenHolder> tokenHolderRef;

    public GoogleUserCredentialProvider(OkHttpClient client, String refreshToken)
            throws LoginFailedException, RemoteServerException {
        this(client, refreshToken, obtainToken(client, refreshToken));
    }

    private GoogleUserCredentialProvider(OkHttpClient client, String refreshToken, TokenHolder tokenHolder) {
        this.client = client;
        this.refreshToken = refreshToken;
        this.tokenHolderRef = new AtomicReference<>(tokenHolder);
    }

    /**
     * Given the refresh token fetches a new access token and returns AuthInfo.
     *
     * @param refreshToken Refresh token persisted by the user after initial login
     * @throws LoginFailedException If we fail to get tokenId
     */
    private void refreshTokenIfExpired(String refreshToken) throws LoginFailedException, RemoteServerException {
        if (!tokenHolderRef.get().isTokenIdExpired()){
            return;
        }

        synchronized (this) {
            //recheck
            if (!tokenHolderRef.get().isTokenIdExpired()){
                return;
            }
            final TokenHolder tokenHolder = obtainToken(client, refreshToken);
            tokenHolderRef.set(tokenHolder);
        }
    }

    private static TokenHolder obtainToken(OkHttpClient client, String refreshToken) throws RemoteServerException, LoginFailedException {
        HttpUrl url = HttpUrl.parse(OAUTH_TOKEN_ENDPOINT).newBuilder()
                .addQueryParameter("client_id", CLIENT_ID)
                .addQueryParameter("client_secret", SECRET)
                .addQueryParameter("refresh_token", refreshToken)
                .addQueryParameter("grant_type", "refresh_token")
                .build();
        //Empty request body
        RequestBody reqBody = RequestBody.create(null, new byte[0]);
        Request request = new Request.Builder()
                .url(url)
                .method("POST", reqBody)
                .build();

        Response response;
        try {
            response = client.newCall(request).execute();

        } catch (IOException e) {
            throw new RemoteServerException("Network Request failed to fetch refreshed tokenId", e);
        }
        Moshi moshi = new Moshi.Builder().build();

        GoogleAuthTokenJson googleAuthTokenJson;
        try {
            final String responseBody = response.body().string();
            logger.debug("For refreshToken " + refreshToken + " got response: " + responseBody);
            googleAuthTokenJson = moshi.adapter(GoogleAuthTokenJson.class).fromJson(responseBody);
        } catch (IOException e) {
            throw new RemoteServerException("Failed to unmarshal the Json response to fetch refreshed tokenId", e);
        }
        final TokenHolder tokenHolder;
        if (googleAuthTokenJson.getError() != null) {
            throw new LoginFailedException(googleAuthTokenJson.getError());
        } else {
            long expiresTimestamp = System.currentTimeMillis()
                    + (googleAuthTokenJson.getExpiresIn() * 1000 - REFRESH_TOKEN_BUFFER_TIME);
            String tokenId = googleAuthTokenJson.getIdToken();

            tokenHolder = new TokenHolder(tokenId, expiresTimestamp);
        }
        return tokenHolder;
    }


    /**
     * Uses an access code to login and get tokens
     */
    public static GoogleUserCredentialProvider login(OkHttpClient client, String authcode) throws LoginFailedException, RemoteServerException {

        HttpUrl url = HttpUrl.parse(OAUTH_TOKEN_ENDPOINT).newBuilder()
                .addQueryParameter("code", authcode)
                .addQueryParameter("client_id", CLIENT_ID)
                .addQueryParameter("client_secret", SECRET)
                .addQueryParameter("grant_type", "authorization_code")
                .addQueryParameter("scope", "openid email https://www.googleapis.com/auth/userinfo.email")
                .addQueryParameter("redirect_uri", "urn:ietf:wg:oauth:2.0:oob")
                .build();

        //Create empty body
        RequestBody reqBody = RequestBody.create(null, new byte[0]);

        Request request = new Request.Builder()
                .url(url)
                .method("POST", reqBody)
                .build();
        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new RemoteServerException("Network Request failed to fetch tokenId", e);
        }

        Moshi moshi = new Moshi.Builder().build();

        GoogleAuthTokenJson googleAuth;
        try {
            final String responseBody = response.body().string();
            logger.debug(responseBody);
            googleAuth = moshi.adapter(GoogleAuthTokenJson.class).fromJson(responseBody);
        } catch (IOException e) {
            throw new RemoteServerException("Failed to unmarshell the Json response to fetch tokenId", e);
        }

        Time time = new SystemTimeImpl();

        long expiresTimestamp = time.currentTimeMillis()
                + (googleAuth.getExpiresIn() * 1000 - REFRESH_TOKEN_BUFFER_TIME);
        String tokenId = googleAuth.getIdToken();
        String refreshToken = googleAuth.getRefreshToken();

        return new GoogleUserCredentialProvider(client, refreshToken, new TokenHolder(tokenId, expiresTimestamp));
    }

    @Override
    public String getTokenId() throws LoginFailedException, RemoteServerException {
        refreshTokenIfExpired(refreshToken);

        return tokenHolderRef.get().getTokenId();
    }

    /**
     * Refreshes tokenId if it has expired
     *
     * @return AuthInfo object
     * @throws LoginFailedException When login fails
     */
    @Override
    public AuthInfo getAuthInfo() throws LoginFailedException, RemoteServerException {
        refreshTokenIfExpired(refreshToken);

        AuthInfo.Builder authbuilder = AuthInfo.newBuilder();
        authbuilder.setProvider("google");
        authbuilder.setToken(AuthInfo.JWT.newBuilder().setContents(getTokenId()).setUnknown2(59).build());
        return authbuilder.build();
    }

    @Override
    public boolean isTokenIdExpired() {
        return tokenHolderRef.get().isTokenIdExpired();
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    private static class TokenHolder {
        private final String tokenId;
        private final long expiresTimestamp;

        private TokenHolder(String tokenId, long expiresTimestamp) {
            this.tokenId = tokenId;
            this.expiresTimestamp = expiresTimestamp;
        }

        private String getTokenId() {
            return tokenId;
        }

        private boolean isTokenIdExpired() {
            return System.currentTimeMillis() > expiresTimestamp;
        }
    }
}
