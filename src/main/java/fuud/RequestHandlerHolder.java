package fuud;

import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import fuud.copied.GoogleUserCredentialProvider;
import fuud.copied.RequestHandler;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class RequestHandlerHolder {

    private final OkHttpClient okHttpClient;
    private final CredentialProviderHolder credentialProviderHolder;

    @Autowired
    public RequestHandlerHolder(OkHttpClient okHttpClient, CredentialProviderHolder credentialProviderHolder) {
        this.okHttpClient = okHttpClient;
        this.credentialProviderHolder = credentialProviderHolder;
    }

    @Cacheable(cacheNames = "requestHandler", sync = true)
    public RequestHandler byRefreshToken(String refreshToken) throws LoginFailedException, RemoteServerException {
        return new RequestHandler(okHttpClient, credentialProviderHolder.byRefreshToken(refreshToken));
    }
}
