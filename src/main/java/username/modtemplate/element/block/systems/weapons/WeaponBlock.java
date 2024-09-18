package username.modtemplate.element.block.systems.weapons;

import api.config.BlockConfig;
import org.schema.game.client.view.cubes.shapes.BlockStyle;
import org.schema.game.common.data.element.ElementInformation;
import username.modtemplate.ModTemplate;

public abstract class WeaponBlock {

	protected ElementInformation computerInfo;
	protected ElementInformation moduleInfo;

	protected WeaponBlock(String computerName, String moduleName) {
		computerInfo = BlockConfig.newElement(ModTemplate.getInstance(), computerName, new short[6]);
		moduleInfo = BlockConfig.newElement(ModTemplate.getInstance(), moduleName, new short[6]);
		computerInfo.setBlockStyle(BlockStyle.NORMAL.id);
		moduleInfo.setBlockStyle(BlockStyle.NORMAL.id);
	}

	public final ElementInformation getComputerInfo() {
		return computerInfo;
	}

	public final ElementInformation getModuleInfo() {
		return moduleInfo;
	}

	public final short getComputerId() {
		return computerInfo.getId();
	}

	public final short getModuleId() {
		return moduleInfo.getId();
	}

	public abstract void initializeComputer();
	public abstract void initializeModule();
}
