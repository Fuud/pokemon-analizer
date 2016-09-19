package fuud;

import com.pokegoapi.exceptions.LoginFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

@Component
public class RefreshTokenStorage {
    public static final String REFRESH_TOKEN_STORAGE_FOLDER_PN = "refresh_token_storage_folder";
    private final File refreshTokenStorageFile;

    @Autowired
    public RefreshTokenStorage(@Value(REFRESH_TOKEN_STORAGE_FOLDER_PN) File refreshTokenStorageFolder) {
        this.refreshTokenStorageFile = refreshTokenStorageFolder;
        if (!refreshTokenStorageFolder.exists()) {
            if (!refreshTokenStorageFolder.mkdirs()) {
                throw new IllegalStateException("Can not create directories [" + refreshTokenStorageFolder + "]");
            }
        }
        if (!refreshTokenStorageFolder.isDirectory()) {
            throw new IllegalStateException("[" + refreshTokenStorageFolder + "] is not directory");
        }
    }

    public synchronized void storeRefreshToken(String username, String refreshToken) {
        final File file = new File(refreshTokenStorageFile, username);
        try (PrintWriter printWriter = new PrintWriter(file)) {
            printWriter.write(refreshToken);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Can not create file", e);
        }
    }

    public synchronized String getRefreshToken(String username) throws LoginFailedException {
        final File file = new File(refreshTokenStorageFile, username);
        try (Scanner scanner = new Scanner(file)) {

            final String refreshToken = scanner.nextLine();
            if (!StringUtils.isEmpty(refreshToken)) {
                return refreshToken;
            } else {
                throw new LoginFailedException("refresh token is not registered");
            }
        } catch (FileNotFoundException e) {
            throw new LoginFailedException("refresh token is not registered", e);
        }
    }
}
