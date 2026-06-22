package videogoose.spaceguard.data.commands;

import api.mod.StarMod;
import api.mod.config.PersistentObjectUtil;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import org.schema.game.common.data.player.PlayerState;
import videogoose.spaceguard.SpaceGuard;
import videogoose.spaceguard.data.PlayerData;
import videogoose.spaceguard.manager.SecurityManager;

public class TrustPlayerCommand implements CommandInterface {
	@Override
	public String getCommand() {
		return "trust_player";
	}

	@Override
	public String[] getAliases() {
		return new String[]{"trust_player", "trust"};
	}

	@Override
	public String getDescription() {
		return "Sets the specified player as trusted, and will exempt them from a specific security check.\n" + "- /%COMMAND% <player> <true|false>: Sets whether the player is trusted and exempted from all security checks.\n" + "- /%COMMAND% <player> <alt|vpn|proxy|tor> <true|false>: Sets whether teh player is trusted and exempted from the specified security check.";
	}

	@Override
	public boolean isAdminOnly() {
		return true;
	}

	@Override
	public boolean onCommand(PlayerState sender, String[] args) {
		if(args != null && args.length >= 2) {
			try {
				PlayerData playerData = SecurityManager.getPlayer(args[0]);
				if(playerData == null)
					PlayerUtils.sendMessage(sender, "[ERROR]: Player \"" + args[0] + "\" not found.");
				else {
					if(args.length == 2) {
						boolean trusted = Boolean.parseBoolean(args[1]);
						playerData.setTrustedAll(trusted);
						PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
						PlayerUtils.sendMessage(sender, "Player \"" + playerData.getPlayerName() + "\" is now " + (trusted ? "trusted" : "untrusted"));
						return true;
					} else if(args.length == 3) {
						String checkType = args[1];
						boolean trusted = Boolean.parseBoolean(args[2]);
						switch(checkType) {
							case "alt":
								playerData.setTrusted(PlayerData.TRUSTED_ALT, trusted);
								break;
							case "vpn":
								playerData.setTrusted(PlayerData.TRUSTED_VPN, trusted);
								break;
							case "proxy":
								playerData.setTrusted(PlayerData.TRUSTED_PROXY, trusted);
								break;
							case "tor":
								playerData.setTrusted(PlayerData.TRUSTED_TOR, trusted);
								break;
							default:
								PlayerUtils.sendMessage(sender, "[ERROR]: Invalid security check type \"" + checkType + "\"");
								return false;
						}
						PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
						PlayerUtils.sendMessage(sender, "Player \"" + playerData.getPlayerName() + "\" is now " + (trusted ? "trusted" : "untrusted") + " for security check \"" + checkType + "\"");
						return true;
					}
				}
			} catch(Exception exception) {
				SpaceGuard.getInstance().logException("An error occurred while attempting to trust player \"" + args[0] + "\"", exception);
				PlayerUtils.sendMessage(sender, "[ERROR]: An error occurred while attempting to trust player \"" + args[0] + "\"");
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
