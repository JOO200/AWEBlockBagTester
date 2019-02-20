package de.joo.AWEBlockBagTester;

import org.bukkit.plugin.java.JavaPlugin;

public class TestPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("tester").setExecutor(new TestCommands(this));
    }
}
