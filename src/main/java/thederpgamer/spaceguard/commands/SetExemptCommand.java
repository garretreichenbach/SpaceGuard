package thederpgamer.spaceguard.commands;

import api.mod.StarMod;
import api.utils.game.chat.CommandInterface;
import org.jetbrains.annotations.Nullable;
import org.schema.game.common.data.player.PlayerState;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class SetExemptCommand implements CommandInterface {
	@Override
	public String getCommand() {
		return "";
	}

	@Override
	public String[] getAliases() {
		return new String[0];
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public boolean isAdminOnly() {
		return false;
	}

	@Override
	public boolean onCommand(PlayerState playerState, String[] strings) {
		return false;
	}

	@Override
	public void serverAction(@Nullable PlayerState playerState, String[] strings) {

	}

	@Override
	public StarMod getMod() {
		return null;
	}
}
