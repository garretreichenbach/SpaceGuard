package videogoose.spaceguard.data;

import org.json.JSONObject;

public interface JsonSerializer {

	JSONObject serialize();

	void deserialize(JSONObject data);
}
