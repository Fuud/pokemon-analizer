package fuud;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.main.RequestHandler;
import okhttp3.OkHttpClient;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class CloseablePokemonGo extends PokemonGo implements Closeable {
    private static final AtomicInteger threadCounter = new AtomicInteger();
    private final Runnable callbackOnClose;

    private CloseablePokemonGo(OkHttpClient client, Runnable callbackOnClose) throws LoginFailedException, RemoteServerException {
        super(client);
        this.callbackOnClose = callbackOnClose;
    }

    public static CloseablePokemonGo createByRefreshToken(String refreshToken, RefreshTokenStorage storage, OkHttpClient client, Runnable callbackOnClose) throws Exception {
        final GoogleUserCredentialProvider credentialProvider = new GoogleUserCredentialProvider(client, refreshToken);
        final CloseablePokemonGo closeablePokemonGo = new CloseablePokemonGo(client, callbackOnClose);
        try {
            closeablePokemonGo.login(credentialProvider);
            final String username = closeablePokemonGo.getPlayerProfile().getPlayerData().getUsername();
            storage.storeRefreshToken(username, refreshToken);
        } catch (Throwable t) {
            closeablePokemonGo.close();
            throw t;
        }

        return closeablePokemonGo;
    }

    public static CloseablePokemonGo createByUserName(String userName, RefreshTokenStorage storage, OkHttpClient client, Runnable callbackOnClose) throws Exception {
        final String refreshToken = storage.getRefreshToken(userName);
        return createByRefreshToken(refreshToken, storage, client, callbackOnClose);
    }

    @Override
    public void close() {
        callbackOnClose.run();
    }

    public void stop() {
        try {
            final Thread thread = getThread();
            thread.interrupt();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private Thread getThread() throws NoSuchFieldException, IllegalAccessException {
        final Field asyncHttpThreadField = RequestHandler.class.getDeclaredField("asyncHttpThread");
        asyncHttpThreadField.setAccessible(true);
        final Object asyncHttpThread = asyncHttpThreadField.get(getRequestHandler());
        return Thread.class.cast(asyncHttpThread);
    }
}
