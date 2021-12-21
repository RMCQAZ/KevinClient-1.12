package kevin.file;

import kevin.KevinClient;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class HudConfig extends FileConfig {

    public HudConfig(final File file) {
        super(file);
    }

    @Override
    public void loadConfig() throws IOException {
        KevinClient.hud.clearElements();
        KevinClient.hud = new Config(FileUtils.readFileToString(getFile())).toHUD();
    }

    @Override
    public void saveConfig() throws IOException {
        final PrintWriter printWriter = new PrintWriter(new FileWriter(getFile()));
        printWriter.println(new Config(KevinClient.hud).toJson());
        printWriter.close();
    }
}
