package world.bentobox.limits;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.limits.events.LimitsPermCheckEvent;
import world.bentobox.limits.objects.IslandBlockCount;

import java.util.Arrays;
import java.util.Locale;

public class Util {
	/**
	 * Check and set the permissions of the player and how they affect the island limits
	 * @param player - player
	 * @param permissionPrefix - permission prefix for this game mode
	 * @param islandId - island string id
	 * @param gameMode - game mode string doing the checking
	 */
	public static void checkPerms(Player player, String permissionPrefix, String islandId, String gameMode, Limits addon) {
		IslandBlockCount ibc = addon.getBlockLimitListener().getIsland(islandId);
		// Check permissions
		if (ibc != null) {
			// Clear permission limits
			ibc.getEntityLimits().clear();
			ibc.getEntityGroupLimits().clear();
			ibc.getBlockLimits().clear();
		}
		for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
			if (!perms.getValue()
					|| !perms.getPermission().startsWith(permissionPrefix)
					|| badSyntaxCheck(perms, player.getName(), permissionPrefix, addon)) {
				continue;
			}
			// Check formatting
			String[] split = perms.getPermission().split("\\.");
			// Entities & materials
			EntityType et = Arrays.stream(EntityType.values()).filter(t -> t.name().equalsIgnoreCase(split[3])).findFirst().orElse(null);
			Material m = Arrays.stream(Material.values()).filter(t -> t.name().equalsIgnoreCase(split[3])).findFirst().orElse(null);
			Settings.EntityGroup entgroup = addon.getSettings().getGroupLimitDefinitions().stream()
					.filter(t -> t.getName().equalsIgnoreCase(split[3])).findFirst().orElse(null);

			if (entgroup == null && et == null && m == null) {
				logError(player.getName(), perms.getPermission(), split[3].toUpperCase(Locale.ENGLISH) + " is not a valid material or entity type/group.", addon);
				break;
			}
			// Make an ibc if required
			if (ibc == null) {
				ibc = new IslandBlockCount(islandId, gameMode);
			}
			// Get the value
			int value = Integer.parseInt(split[4]);
			// Fire perm check event
			LimitsPermCheckEvent l = new LimitsPermCheckEvent(player, islandId, ibc, entgroup, et, m, value);
			Bukkit.getPluginManager().callEvent(l);
			if (l.isCancelled()) continue;
			// Use event values
			ibc = l.getIbc();
			// Run null checks and set ibc
			runNullCheckAndSet(ibc, l);
		}
		// Check removed permissions

		// If any changes have been made then store it - don't make files unless they are needed
		if (ibc != null) addon.getBlockLimitListener().setIsland(islandId, ibc);
	}

	private static boolean badSyntaxCheck(PermissionAttachmentInfo perms, String name, String permissionPrefix, Limits addon) {
		// No wildcards
		if (perms.getPermission().contains(permissionPrefix + "*")) {
			logError(name, perms.getPermission(), "wildcards are not allowed.", addon);
			return true;
		}
		// Check formatting
		String[] split = perms.getPermission().split("\\.");
		if (split.length != 5) {
			logError(name, perms.getPermission(), "format must be '" + permissionPrefix + "MATERIAL.NUMBER', '" + permissionPrefix + "ENTITY-TYPE.NUMBER', or '" + permissionPrefix + "ENTITY-GROUP.NUMBER'", addon);
			return true;
		}
		// Check value
		if (!NumberUtils.isDigits(split[4])) {
			logError(name, perms.getPermission(), "the last part MUST be a number!", addon);
			return true;
		}

		return false;
	}

	private static void logError(String name, String perm, String error, Limits addon) {
		addon.logError("Player " + name + " has permission: '" + perm + "' but " + error + " Ignoring...");
	}


	private static void runNullCheckAndSet(@Nullable IslandBlockCount ibc, @NonNull LimitsPermCheckEvent l) {
		if (ibc == null) {
			return;
		}
		Settings.EntityGroup entgroup = l.getEntityGroup();
		EntityType et = l.getEntityType();
		Material m = l.getMaterial();
		int value = l.getValue();
		if (entgroup != null) {
			// Entity group limit
			ibc.setEntityGroupLimit(entgroup.getName(), Math.max(ibc.getEntityGroupLimit(entgroup.getName()), value));
		} else if (et != null && m == null) {
			// Entity limit
			ibc.setEntityLimit(et, Math.max(ibc.getEntityLimit(et), value));
		} else if (m != null && et == null) {
			// Material limit
			ibc.setBlockLimit(m, Math.max(ibc.getBlockLimit(m), value));
		} else {
			if (m != null && m.isBlock()) {
				// Material limit
				ibc.setBlockLimit(m, Math.max(ibc.getBlockLimit(m), value));
			} else if (et != null){
				// This is an entity setting
				ibc.setEntityLimit(et, Math.max(ibc.getEntityLimit(et), value));
			}
		}

	}

}
