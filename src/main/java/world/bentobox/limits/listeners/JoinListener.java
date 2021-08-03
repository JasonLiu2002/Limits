package world.bentobox.limits.listeners;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.events.team.TeamSetownerEvent;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.limits.Limits;
import world.bentobox.limits.Settings.EntityGroup;
import world.bentobox.limits.Util;
import world.bentobox.limits.events.LimitsJoinPermCheckEvent;
import world.bentobox.limits.events.LimitsPermCheckEvent;
import world.bentobox.limits.objects.IslandBlockCount;

/**
 * Sets block limits based on player permission
 * @author tastybento
 *
 */
public class JoinListener implements Listener {

    private final Limits addon;

    public JoinListener(Limits addon) {
        this.addon = addon;
    }

    /*
     * Event handling
     */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNewIsland(IslandEvent e) {
        if (!e.getReason().equals(Reason.CREATED)
                && !e.getReason().equals(Reason.RESETTED)
                && !e.getReason().equals(Reason.REGISTERED)) {
            return;
        }
        setOwnerPerms(e.getIsland(), e.getOwner());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerChange(TeamSetownerEvent e) {
        removeOwnerPerms(e.getIsland());
        setOwnerPerms(e.getIsland(), e.getNewOwner());
    }


    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        // Check if player has any islands in the game modes
        addon.getGameModes().forEach(gm -> {
            if (addon.getIslands().hasIsland(gm.getOverWorld(), e.getPlayer().getUniqueId())) {
                String islandId = Objects.requireNonNull(addon.getIslands().getIsland(gm.getOverWorld(), e.getPlayer().getUniqueId())).getUniqueId();
                IslandBlockCount ibc = addon.getBlockLimitListener().getIsland(islandId);
                if (Util.joinEventCheck(e.getPlayer(), islandId, ibc, addon, false)) {
                    return;
                }
                Util.checkPerms(e.getPlayer(), gm.getPermissionPrefix() + "island.limit.", islandId, gm.getDescription().getName(), addon, false);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnregisterIsland(IslandEvent e) {
        if (!e.getReason().equals(Reason.UNREGISTERED)) {
            return;
        }
        removeOwnerPerms(e.getIsland());
    }

    /*
     * Utility methods
     */

    private void removeOwnerPerms(Island island) {
        World world = island.getWorld();
        if (addon.inGameModeWorld(world)) {
            IslandBlockCount ibc = addon.getBlockLimitListener().getIsland(island.getUniqueId());
            if (ibc != null) {
                ibc.getBlockLimits().clear();
            }
        }
    }

    private void setOwnerPerms(Island island, UUID ownerUUID) {
        World world = island.getWorld();
        if (addon.inGameModeWorld(world)) {
            // Check if owner is online
            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
            if (owner.isOnline()) {
                // Set perm-based limits
                String prefix = addon.getGameModePermPrefix(world);
                String name = addon.getGameModeName(world);
                if (!prefix.isEmpty() && !name.isEmpty() && owner.getPlayer() != null) {
                    Util.checkPerms(Objects.requireNonNull(owner.getPlayer()), prefix + "island.limit.", island.getUniqueId(), name, addon, false);
                }
            }
        }
    }

}
