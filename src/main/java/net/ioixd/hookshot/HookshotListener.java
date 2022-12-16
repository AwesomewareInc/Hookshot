package net.ioixd.hookshot;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

public class HookshotListener implements Listener {
    ItemStack chain = new ItemStack(Material.CHAIN);
    HashMap<String, Long> events = new HashMap<String, Long>();
    Hookshot plugin;

    public enum Direction {
        TowardsPositiveX,
        TowardsNegativeX,
        TowardsPositiveZ,
        TowardsNegativeZ,
    }
    public HookshotListener(Hookshot plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
    }

    @EventHandler
    public void rightclick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // this event gets fired twice for left clicks and i have no idea why.
        // probably related to a bad mouse.
        // so here's a manual cooldown
        Long prevTime = events.get(player.getDisplayName());
        if(prevTime != null) {
            Long newTime = Instant.now().toEpochMilli();
            if(newTime <= prevTime+100) {
                return;
            }
        }
        events.put(player.getDisplayName(), Instant.now().toEpochMilli());
        ItemStack mainhand = player.getInventory().getItemInMainHand();
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (
            event.getAction() == Action.LEFT_CLICK_AIR &&
            mainhand.getType() == Material.TNT &&
            offhand.getType() == Material.FLINT_AND_STEEL
        ) {
            throwableTNT(event);
            return;
        }

        if (
                event.getAction() == Action.LEFT_CLICK_AIR && (
                        mainhand.getType() == Material.ANVIL ||
                        mainhand.getType() == Material.CHIPPED_ANVIL ||
                        mainhand.getType() == Material.DAMAGED_ANVIL
                )
        ) {
            event.setCancelled(true);
            throwableGeneric(event);
            return;
        }


    }

    public void hookshot(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        Location pos1 = player.getLocation();
        Location pos2 = player.getTargetBlock(null, 20).getLocation();
        double yaw = pos1.getYaw();
        double pitch = pos1.getPitch();
        double slopeX = (pos2.getY() - pos1.getY()) / (pos2.getX() - pos1.getX());
        double slopeZ = (pos2.getY() - pos1.getY()) / (pos2.getZ() - pos1.getZ());

        BlockData data = Bukkit.createBlockData(Material.SAND);
        FallingBlock block = world.spawnFallingBlock(pos1, data);
        double distance = 100.0;
        block.setVelocity(player.getLocation().getDirection().multiply(2));
        /*
        int num = 0;
        boolean done_x = false;
        boolean done_z = false;
        double newYaw = 0.0;
        while(!done_x && !done_z) {
            Location loc = new Location(world, curX, curY, curZ, (float)yaw, 90.0F);
            ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
            stand.setInvisible(true);
            stand.setGravity(false);
            Objects.requireNonNull(stand.getEquipment()).setHelmet(chain);
            switch(dir) {
                case TowardsPositiveZ:
                    // newYaw = -0.35;
                    newYaw = yaw/100.0;
                    if(Math.abs(newYaw) >= 0.1) {
                        newYaw /= 1+(yaw/1000.0);
                    }
                    //if(newYaw >= 1.0) newYaw -= Math.round(newYaw);
                    curX -= newYaw;
                    curZ += slopeZ;
                    break;
                case TowardsPositiveX:
                    newYaw = Math.abs((yaw+45)/100.0);
                    if(newYaw >= 1.0) newYaw -= Math.round(newYaw);
                    curX += slopeX;
                    curZ -= slopeZ+newYaw;
                    break;
                case TowardsNegativeX:
                    newYaw = Math.abs((yaw+135)/100.0);
                    if(newYaw >= 1.0) newYaw -= Math.round(newYaw);
                    curX -= slopeX;
                    curZ += slopeZ+newYaw;
                    break;
                case TowardsNegativeZ:
                    newYaw = Math.abs((yaw+135)/100.0);
                    if(newYaw >= 1.0) newYaw -= Math.round(newYaw);
                    curX -= slopeX+newYaw;
                    curZ -= slopeZ;
                    break;
            }
            curY -= pitch/100.0;
            num++;
            if(num >= 25) {
                break;
            }
        }*/
        player.sendMessage(String.format("%.2f, %.2f",pitch,yaw));
   }

    public void throwableTNT(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        Location pos1 = player.getLocation();

        TNTPrimed tnt = world.spawn(pos1, TNTPrimed.class);
        tnt.setFuseTicks(1000000);
        tnt.setVelocity(player.getLocation().getDirection().multiply(4));

        ItemStack mainhand = player.getInventory().getItemInMainHand();
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if(player.getGameMode() != GameMode.CREATIVE) {
            int amount = mainhand.getAmount();
            if(amount > 1) {
                mainhand.setAmount(amount-1);
            } else {
                player.getInventory().remove(mainhand);
                player.updateInventory();
            }
            ItemMeta meta = offhand.getItemMeta();
            Damageable dmg = ((Damageable)meta);
            int dmgLevel = dmg.getDamage();
            if(dmgLevel >= 64) {
                player.playSound(player, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                ItemStack data = new ItemStack(Material.AIR);
                player.getInventory().setItemInOffHand(data);
            } else {
                dmg.setDamage(dmgLevel+16);
                offhand.setItemMeta(meta);
                player.getInventory().setItemInOffHand(offhand);

            }

        }

        // the rest of the code has to be on new threads because blocking this function results in
        // the tnt not being launched.

        // watch the tnt until its landed, then explode.
        Thread t1 = new Thread(() -> {
            Location pos = tnt.getLocation();

            long time = Instant.now().toEpochMilli();
            long sameTicks = 0;
            while(sameTicks <= 1000) {
                // is it in the same position as it was last time we checked?
                if(tnt.getLocation().getX() == pos.getX() &&
                        tnt.getLocation().getY() == pos.getY() &&
                        tnt.getLocation().getZ() == pos.getZ()) {
                    // update the time that it's been at the same spot
                    sameTicks = Instant.now().toEpochMilli() - time;
                } else {
                    // reset the time
                    sameTicks = 0;
                    time = Instant.now().toEpochMilli();
                    // reset the last position we knew the tnt was at
                    pos = tnt.getLocation();
                }
            }
            tnt.setFuseTicks(-1000);
        });
        t1.start();

        // we also need to check for nearby entities.
        // but we can't do that from a new thread because bukkit doesn't let us call getNearbyEntities from a new thread but then we can't run this synchrnously or else the function or hang and there's no alternative because fuck you
        // so run it every 2 ticks
        BukkitRunnable t2 = new BukkitRunnable() {
            @Override
            public void run() {
                List<Entity> nearbyEntities = tnt.getNearbyEntities(1, 1, 1);
                if (nearbyEntities.size() >= 1) {
                    // if at least one of the entities is a mob (players don't count)
                    // special exceptions for projectiles because throwing against those sounds funny
                    for(Entity entity : nearbyEntities) {
                        if(entity instanceof Monster || entity instanceof Projectile) {
                            // only activate if the tnt has been primed for a quarter second
                            if(tnt.getFuseTicks() <= 1000000-10) {
                                tnt.setFuseTicks(-1000);
                                return;
                            }
                        }
                    }
                }
                // special exceptions:
                // broaden the range but only activate if a flying mob is nearby
                nearbyEntities = tnt.getNearbyEntities(2, 2, 2);
                if (nearbyEntities.size() >= 1) {
                    // if at least one of the entities is a mob (players don't count)
                    for(Entity entity : nearbyEntities) {
                        if(entity instanceof Flying || entity instanceof Wither) {
                            if(tnt.getFuseTicks() <= 1000000-10) {
                                tnt.setFuseTicks(-1000);
                                return;
                            }
                        }
                    }
                }
            }
        };
        t2.runTaskTimer(this.plugin,0,2);
   }

    public void throwableGeneric(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        Location pos1 = player.getLocation();

        BlockData data = Bukkit.createBlockData(player.getInventory().getItemInMainHand().getType());
        FallingBlock anvil = world.spawnFallingBlock(pos1,data);
        anvil.setVelocity(player.getLocation().getDirection().multiply(4));

        ItemStack mainhand = player.getInventory().getItemInMainHand();
        if(player.getGameMode() != GameMode.CREATIVE) {
            int amount = mainhand.getAmount();
            if(amount > 1) {
                mainhand.setAmount(amount-1);
            } else {
                player.getInventory().remove(mainhand);
            }
        }
    }

}
