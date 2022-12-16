package net.ioixd.hookshot;

import org.bukkit.plugin.java.JavaPlugin;

public class Hookshot extends JavaPlugin {

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new HookshotListener(this), this);
    }


    public void log(String msg) {
        getLogger().info(msg);
    }
}