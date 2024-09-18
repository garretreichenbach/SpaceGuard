package username.modtemplate.element.items;

import api.config.BlockConfig;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.schine.graphicsengine.core.GraphicsContext;
import username.modtemplate.element.ElementManager;

public class ExampleItem extends Item {

	public ExampleItem() {
		super("Example Item", ElementManager.getCategory("Items"));
	}

	@Override
	public void initialize() {
		if(GraphicsContext.initialized) {
			itemInfo.setTextureId(ElementKeyMap.getInfo(341).getTextureIds());
			itemInfo.setBuildIconNum(ElementKeyMap.getInfo(341).getBuildIconNum());
		}
		itemInfo.setDescription("A cool item that does nothing.");
		itemInfo.setInRecipe(true);
		itemInfo.setShoppable(true);
		itemInfo.setPlacable(false);
		itemInfo.setPhysical(false);
		BlockConfig.add(itemInfo);
	}
}
