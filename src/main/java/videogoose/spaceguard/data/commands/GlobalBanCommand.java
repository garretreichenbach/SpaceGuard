package videogoose.spaceguard.data.commands;

import api.common.GameServer;
import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import org.schema.game.common.data.player.PlayerState;
import videogoose.spaceguard.SpaceGuard;
import videogoose.spaceguard.data.PlayerData;
import videogoose.spaceguard.manager.SecurityManager;

public class GlobalBanCommand implements CommandInterface {
	@Override
	public String getCommand() {
		return "global_ban";
	}

	@Override
	public String[] getAliases() {
		return new String[]{"global_ban", "gban"};
	}

	@Override
	public String getDescription() {
		return "Bans a Player's account, IP, and HID, as well as any alts they might have from the server.\n" + "- /%COMMAND% <player> : Bans the specified player globally.";
	}

	@Override
	public boolean isAdminOnly() {
		return true;
	}

	@Override
	public boolean onCommand(PlayerState sender, String[] args) {
		if(args != null && args.length == 1) {
			try {
				PlayerState target = GameServer.getServerState().getPlayerFromName(args[0]);
				if(target == null) {
					PlayerData playerData = SecurityManager.getPlayer(args[0]);
					SecurityManager.globalBanPlayer(playerData);
					PlayerUtils.sendMessage(sender, "Successfully banned player \"" + args[0] + "\" globally.");
				} else {
					if(target.isAdmin()) {
						PlayerUtils.sendMessage(sender, "[ERROR]: You cannot ban an admin.");
					} else {
						PlayerData playerData = SecurityManager.getPlayer(target);
						SecurityManager.globalBanPlayer(playerData);
						PlayerUtils.sendMessage(sender, "Successfully banned player \"" + target.getName() + "\" globally.");
					}
				}
				return true;
			} catch(Exception ignored) {
			}
		}
		return false;
	}

	@Override
	public void serverAction(PlayerState sender, String[] args) {

	}

	@Override
	public StarMod getMod() {
		return SpaceGuard.getInstance();
	}
}
