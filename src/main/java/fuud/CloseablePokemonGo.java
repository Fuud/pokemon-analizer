package fuud;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.main.RequestHandler;
import com.pokegoapi.util.Time;
import okhttp3.OkHttpClient;

import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

public class CloseablePokemonGo extends PokemonGo implements Closeable {
    private static final AtomicInteger threadCounter = new AtomicInteger();

    public CloseablePokemonGo(CredentialProvider credentialProvider, OkHttpClient client) throws LoginFailedException, RemoteServerException {
        super(client);

        try{
            login(credentialProvider);
        }catch (Throwable t){
            close();
            throw t;
        }
    }

    @Override
    public void close() {
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
