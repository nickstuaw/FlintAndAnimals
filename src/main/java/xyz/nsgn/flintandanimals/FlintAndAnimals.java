package xyz.nsgn.flintandanimals;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public final class FlintAndAnimals extends JavaPlugin implements Listener, CommandExecutor {
    private final String prefix = "[F&A] ";

    private int fireTicks;

    private boolean fireCharge, flintAndSteel, permissionRequired;

    private void log(String s) {
        getLogger().info(prefix + s);
    }

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        updateSettings();
        if(getConfig().getBoolean("startup.show-settings")) {
            String version = "1.2";
            log(ChatColor.GREEN + "FlintAndAnimals v" + version + ChatColor.DARK_GREEN + " by nsgw");
            log(ChatColor.DARK_GREEN + "Set players and mobs on fire with one click.");
            log(ChatColor.DARK_GREEN + "Loaded!");
            log(ChatColor.YELLOW + "" + fireTicks + " fire ticks");
            log(ChatColor.DARK_GREEN + "Flint & steel " + (flintAndSteel ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
            log(ChatColor.DARK_GREEN + "Fire charge " + (fireCharge ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        }
        Objects.requireNonNull(getCommand("reloadflintandanimals")).setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void updateSettings() {
        fireTicks = getConfig().getInt("entity-lighting.fire-ticks");
        fireCharge = getConfig().getBoolean("entity-lighting.fire-charge");
        flintAndSteel = getConfig().getBoolean("entity-lighting.flint-and-steel");
        permissionRequired = getConfig().getBoolean("entity-lighting.permission-required");
    }

    private boolean usingFlintAndSteel(Material mat) {
        return flintAndSteel && mat.equals(Material.FLINT_AND_STEEL);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!sender.hasPermission("flintandanimals.reload")) return true;
        reloadConfig();
        updateSettings();
        sender.sendMessage(ChatColor.DARK_GREEN + prefix + "Reloaded config!" +
                "\n" + ChatColor.YELLOW + fireTicks + " fire ticks" + ChatColor.DARK_GREEN +
                "\nFlint & steel " + (flintAndSteel ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.DARK_GREEN +
                "\nFire charge " + (fireCharge ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        return true;
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onLightEvent(PlayerInteractEntityEvent e) {
        try
        {
            ItemStack item = (ItemStack) getVersionCompatibleGetMethod().invoke(e.getPlayer().getInventory());
            Material mat = item.getType();
            if(!(isFireCharge(mat) || usingFlintAndSteel(mat))) return;
            if(permissionRequired) {
                if (!e.getPlayer().hasPermission("flintandanimals.use")) return;
            }
            if(e.getRightClicked() instanceof LivingEntity) {
                if(e.getRightClicked().getFireTicks() > 0) return;
                e.getRightClicked().setFireTicks(fireTicks);
                if(Bukkit.getVersion().contains("1.18")) {
                    e.getPlayer().getWorld().playSound(e.getRightClicked().getLocation(), Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 10f, 1f);
                }
                if(e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) return;
                if(isFireCharge(mat)){
                    item.setAmount(item.getAmount() - 1);
                    getVersionCompatibleSetMethod().invoke(e.getPlayer().getInventory(), item);
                }
            }
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException ignored) {

        }


    }

    /**
     * Gets the version compatible getItemInHand method
     * @return the method
     * @throws NoSuchMethodException ignored
     */
    public Method getVersionCompatibleGetMethod() throws NoSuchMethodException {
        try
        {
            return PlayerInventory.class.getMethod("getItemInMainHand");
        } catch (NoSuchMethodException e) {
            return PlayerInventory.class.getMethod("getItemInHand");
        }
    }

    /**
     * Gets the version compatible setItemInHand method
     * @return the method
     * @throws NoSuchMethodException ignored
     */
    public Method getVersionCompatibleSetMethod() throws NoSuchMethodException {
        try
        {
            return PlayerInventory.class.getMethod("setItemInMainHand", ItemStack.class);
        } catch (NoSuchMethodException e) {
            return PlayerInventory.class.getMethod("setItemInHand", ItemStack.class);
        }
    }

    /**
     * Checks the material regardless of the version used.
     * @param mat The material to check.
     * @return Whether the material is a fire charge.
     */
    public boolean isFireCharge(Material mat) {
        return mat.toString().equals("FIRE_CHARGE") || mat.toString().equals("FIREBALL");
    }
}
