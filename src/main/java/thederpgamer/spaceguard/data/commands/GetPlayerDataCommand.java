package thederpgamer.spaceguard.data.commands;

import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.spaceguard.SpaceGuard;
import thederpgamer.spaceguard.data.PlayerData;
import thederpgamer.spaceguard.manager.SecurityManager;

import javax.annotation.Nullable;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class GetPlayerDataCommand implements CommandInterface {
	@Override
	public String getCommand() {
		return "get_player_data";
	}

	@Override
	public String[] getAliases() {
		return new String[]{"get_player_data", "p_data", "pdata", "pd"};
	}

	@Override
	public String getDescription() {
		return "Displays security related data for the specified player.\n" +
				"- /%COMMAND% <player>: Displays security data for the specified player.";
	}

	@Override
	public boolean isAdminOnly() {
		return true;
	}

	@Override
	public boolean onCommand(PlayerState sender, String[] args) {
		if(args != null && args.length == 1) {
			try {
				PlayerData playerData = SecurityManager.getPlayer(args[0]);
				if(playerData == null) PlayerUtils.sendMessage(sender, "[ERROR]: Player \"" + args[0] + "\" not found.");
				else PlayerUtils.sendMessage(sender, "Player Data for \"" + playerData.getPlayerName() + "\":\n" + playerData);
				return true;
			} catch(Exception ignored) {}
		}
		return false;
	}

	@Override
	public void serverAction(@Nullable PlayerState sender, String[] args) {

	}

	@Override
	public StarMod getMod() {
		return SpaceGuard.getInstance();
	}
}
