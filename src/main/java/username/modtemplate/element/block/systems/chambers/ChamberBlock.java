package username.modtemplate.element.block.systems.chambers;

import api.config.BlockConfig;
import org.schema.game.common.data.blockeffects.config.StatusEffectType;
import org.schema.game.common.data.element.ElementInformation;
import username.modtemplate.ModTemplate;
import username.modtemplate.element.ElementManager;

public abstract class ChamberBlock {

	protected ElementInformation blockInfo;

	public ChamberBlock(String name, short generalId, float chamberCapacity, StatusEffectType... statusEffects) {
		blockInfo = BlockConfig.newChamber(ModTemplate.getInstance(), name, generalId, statusEffects);
		blockInfo.chamberCapacity = chamberCapacity;
		ElementManager.addChamber(this);
	}

	public ElementInformation getBlockInfo() {
		return blockInfo;
	}

	public short getId() {
		return blockInfo.getId();
	}

	public void addChildren(ChamberBlock... children) {
		for(ChamberBlock child : children) {
			child.blockInfo.chamberParent = getId();
			child.blockInfo.chamberPrerequisites.add(getId());
			blockInfo.chamberChildren.add(child.getId());
		}
	}

	public void setUpgrade(ChamberBlock upgrade) {
		addChildren(upgrade);
		upgrade.blockInfo.chamberMutuallyExclusive.addAll(blockInfo.chamberMutuallyExclusive);
		blockInfo.chamberUpgradesTo = upgrade.getId();
	}

	public void addExclusives(ChamberBlock... exclusives) {
		for(ChamberBlock exclusive : exclusives) {
			exclusive.blockInfo.chamberMutuallyExclusive.add(getId());
			blockInfo.chamberMutuallyExclusive.add(exclusive.getId());
		}
	}

	public abstract void initialize();
}
