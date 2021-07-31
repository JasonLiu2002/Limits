package world.bentobox.limits.listeners;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import world.bentobox.limits.Limits;
import world.bentobox.limits.Util;
import world.bentobox.limits.objects.IslandBlockCount;

import java.util.Objects;

public class PermissionListener implements Listener {

	private final Limits addon;
	private final LuckPerms luckperms;

	public PermissionListener(Limits addon, LuckPerms luckperms) {
		this.addon = addon;
		this.luckperms = luckperms;
		this.luckperms.getEventBus().subscribe(this.addon.getPlugin(), NodeAddEvent.class, this::onUserGroupChange);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void onUserGroupChange(NodeAddEvent e) {
		if (!e.isUser()) return;
		User user = (User) e.getTarget();
		Player player = addon.getServer().getPlayer(user.getUniqueId());
		addon.getGameModes().forEach(gm -> {
			if (addon.getIslands().hasIsland(gm.getOverWorld(), user.getUniqueId())) {
				String islandId = Objects.requireNonNull(addon.getIslands().getIsland(gm.getOverWorld(), user.getUniqueId())).getUniqueId();
				Util.checkPerms(player, gm.getPermissionPrefix() + "island.limit.", islandId, gm.getDescription().getName(), addon);
			}
		});
	}
}
