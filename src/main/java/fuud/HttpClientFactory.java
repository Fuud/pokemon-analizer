package fuud;

import com.pokegoapi.util.ClientInterceptor;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class HttpClientFactory {

    @Bean
    OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .addNetworkInterceptor(new ClientInterceptor())
                .build();
    }
}
