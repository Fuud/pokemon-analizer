package fuud;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.main.RequestHandler;
import com.pokegoapi.util.Time;
import okhttp3.OkHttpClient;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;

public class CloseablePokemonGo extends PokemonGo implements Closeable{
    public CloseablePokemonGo(CredentialProvider credentialProvider, OkHttpClient client, Time time) throws LoginFailedException, RemoteServerException {
        super(credentialProvider, client, time);
    }

    public CloseablePokemonGo(CredentialProvider credentialProvider, OkHttpClient client) throws LoginFailedException, RemoteServerException {
        super(credentialProvider, client);
    }

    @Override
    public void close() throws IOException {
        try{
            final Field asyncHttpThreadField = RequestHandler.class.getDeclaredField("asyncHttpThread");
            asyncHttpThreadField.setAccessible(true);
            final Object asyncHttpThread = asyncHttpThreadField.get(getRequestHandler());
            Thread.class.cast(asyncHttpThread).interrupt();
        }catch (Exception e){
            throw new IOException(e);
        }
    }
}
