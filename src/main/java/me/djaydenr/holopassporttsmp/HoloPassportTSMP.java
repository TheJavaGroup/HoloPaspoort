package me.djaydenr.holopassporttsmp;

import me.angeschossen.lands.api.integration.LandsIntegration;
import me.djaydenr.holopassporttsmp.commands.Setgeslacht;
import me.djaydenr.holopassporttsmp.commands.Setleeftijd;
import me.djaydenr.holopassporttsmp.commands.Setnaam;
import me.djaydenr.holopassporttsmp.controller.PlayerController;
import me.djaydenr.holopassporttsmp.dao.PlayerDAO;
import me.djaydenr.holopassporttsmp.events.Paspoort;
import me.djaydenr.holopassporttsmp.events.PlayerJoin;
import me.djaydenr.holopassporttsmp.tabcompleters.SetLeeftijdTabCompleter;
import me.djaydenr.holopassporttsmp.tabcompleters.SetNaamTabCompleter;
import me.djaydenr.holopassporttsmp.tabcompleters.SetgeslachtTabCompleter;
import me.djaydenr.holopassporttsmp.utils.HologramManager;
import me.djaydenr.holopassporttsmp.utils.FileEditor;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class HoloPassportTSMP extends JavaPlugin {

    private Connection conn = null;
    private PlayerController playerController;
    private HologramManager hologramManager;
    private LandsIntegration landsIntegration;

    @Override
    public void onEnable() {
        System.out.println("Plugin geactiveerd by DjaydenR");

        int pluginId = 13557;
        Metrics metrics = new Metrics(this, pluginId);

        getConfig().options().copyDefaults();
        saveDefaultConfig();

        checkHologramPlugin();

        activateSQL();

        initialize();

        // events
        getServer().getPluginManager().registerEvents(new PlayerJoin(playerController), this);
        getServer().getPluginManager().registerEvents(new Paspoort(hologramManager), this);

        // commands
        getCommand("setnaam").setExecutor(new Setnaam(this, playerController));
        getCommand("setgeslacht").setExecutor(new Setgeslacht(playerController));
        getCommand("setleeftijd").setExecutor(new Setleeftijd(playerController));

        // tabCompleter
        Bukkit.getServer().getPluginCommand("setgeslacht").setTabCompleter(new SetgeslachtTabCompleter());
        Bukkit.getServer().getPluginCommand("setnaam").setTabCompleter(new SetNaamTabCompleter());
        Bukkit.getServer().getPluginCommand("setleeftijd").setTabCompleter(new SetLeeftijdTabCompleter());

        hologramManager.startCheckHologramsForDelete();
    }

    @Override
    public void onDisable() {
        hologramManager.deleteAllActiveHolograms();
        System.out.println("Plugin gedeactiveerd by DjaydenR");
    }

    private void checkHologramPlugin() {
        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            getLogger().severe("*** HolographicDisplays is not installed or not enabled. ***");
            getLogger().severe("*** This plugin will be disabled. ***");
            this.setEnabled(false);
        }
    }

    private void activateSQL() {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            String database = getConfig().getString("databank");
            String host = getConfig().getString("ipadres");
            String username = getConfig().getString("gebruikersnaam");
            String password = getConfig().getString("wachtwoord");
            conn = DriverManager.getConnection("jdbc:mysql://" + host + "/" + database + "?user=" + username + "&password=" + password);

            System.out.println("De plugin werkt met databank: " + conn.getMetaData().getDatabaseProductName());

            // Do something with the Connection

            InputStream inputStream = this.getClass().getResourceAsStream("/" + "create.sql");
            File file = FileEditor.copyInputStreamToFile(inputStream);

            System.out.println(file.getName());

            FileEditor.modifyFile("./create.sql", "database", database);

            //Initialize the script runner
            ScriptRunner sr = new ScriptRunner(conn);
            //Creating a reader object
            Reader reader = new BufferedReader(new FileReader(file));


            //Running the script
            sr.runScript(reader);
        } catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void initialize() {
        playerController = new PlayerController(new PlayerDAO(conn));
        this.landsIntegration = new LandsIntegration(this);
        hologramManager = new HologramManager(this, playerController, landsIntegration);
    }

}
