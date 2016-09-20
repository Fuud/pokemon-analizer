package fuud;

import com.pokegoapi.exceptions.LoginFailedException;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class CloseablePokemonGoProvider {
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Map<String, BlockingQueue<CloseablePokemonGo>> pool = new HashMap<>();
    private final RefreshTokenStorage refreshTokenStorage;

    @Autowired
    public CloseablePokemonGoProvider(RefreshTokenStorage refreshTokenStorage) {
        this.refreshTokenStorage = refreshTokenStorage;
        new Timer().schedule(new InvalidatePoolTimerTask(), 0, 60000);
    }

    public CloseablePokemonGo getByUserName(String userName) throws Exception {
        return getByRefreshToken(refreshTokenStorage.getRefreshToken(userName));
    }

    public synchronized CloseablePokemonGo getByRefreshToken(String refreshToken) throws Exception {
        final CloseablePokemonGo impl = getImpl(refreshToken);
        try {
            impl.getInventories().updateInventories(true); //force to fix invalid remove pokemon method
        }catch (Exception ignore){
        }
        return impl;
    }

    private CloseablePokemonGo getImpl(String refreshToken) throws Exception {
        final BlockingQueue<CloseablePokemonGo> closeablePokemonGos = pool.get(refreshToken);
        if (closeablePokemonGos != null) {
            return closeablePokemonGos.take();
        }else {
            return createNew(refreshToken);
        }
    }

    private CloseablePokemonGo createNew(String refreshToken) throws Exception {
        final ArrayBlockingQueue<CloseablePokemonGo> semaphore = new ArrayBlockingQueue<>(1);
        final AtomicReference<CloseablePokemonGo> instance = new AtomicReference<>();
        Runnable onClose = () -> {
            final CloseablePokemonGo closeablePokemonGo = instance.get();
            if (closeablePokemonGo!=null){
                try {
                    semaphore.put(closeablePokemonGo);
                } catch (InterruptedException e) {
                    //ignore - should never happen
                }
            }
        };
        CloseablePokemonGo closeablePokemonGo = CloseablePokemonGo.createByRefreshToken(refreshToken, refreshTokenStorage, httpClient, onClose);
        instance.set(closeablePokemonGo);
        pool.put(refreshToken, semaphore);
        return closeablePokemonGo;
    }

    private class InvalidatePoolTimerTask extends TimerTask {
        @Override
        public void run() {
            synchronized (CloseablePokemonGoProvider.this){
                for (BlockingQueue<CloseablePokemonGo> closeablePokemonGos : pool.values()) {
                    try {
                        final CloseablePokemonGo closeablePokemonGo = closeablePokemonGos.take();
                        closeablePokemonGo.stop();
                    } catch (InterruptedException ignore) {
                    }
                }
                pool.clear();
            }
        }
    }
}
