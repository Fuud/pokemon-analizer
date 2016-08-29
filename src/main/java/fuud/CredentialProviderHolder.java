package fuud;

import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import fuud.copied.GoogleUserCredentialProvider;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class CredentialProviderHolder {

    private final OkHttpClient okHttpClient;

    @Autowired
    public CredentialProviderHolder(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    @Cacheable(cacheNames = "credProvider", sync = true)
    public CredentialProvider byRefreshToken(String refreshToken) throws LoginFailedException, RemoteServerException {
        return new GoogleUserCredentialProvider(okHttpClient, refreshToken);
    }
}
