package thederpgamer.spaceguard.data;

import org.json.JSONObject;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public interface JsonSerializer {

	JSONObject serialize();

	void deserialize(JSONObject data);
}
