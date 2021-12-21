package kevin.utils;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;

public final class ServerUtils extends Mc {

    public static ServerData serverData;

    public static void connectToLastServer() {
        if(serverData == null)
            return;

        getMc().displayGuiScreen(new GuiConnecting(new GuiMultiplayer(new GuiMainMenu()), getMc(), serverData));
    }

    public static String getRemoteIp() {
        String serverIp = "Singleplayer";

        if (getMc().world.isRemote) {
            final ServerData serverData = getMc().getCurrentServerData();

            if(serverData != null)
                serverIp = serverData.serverIP;
        }

        return serverIp;
    }
}