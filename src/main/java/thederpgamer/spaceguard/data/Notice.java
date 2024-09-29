package thederpgamer.spaceguard.data;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class Notice {

	public enum NoticeType {
		INFO,
		WARNING,
		ERROR,
		UPDATE,
		DEBUG,
		IMPORTANT;

		public final String prefix;

		NoticeType() {
			prefix = "[" + name() + "]:";
		}
	}

	private final NoticeType type;
//	private final Set<PermissionGroup> targetGroups = new HashSet<>();
	private final int displayCount;
	private final String message;

	public Notice(NoticeType type, int displayCount, String message) {
		this.type = type;
		this.displayCount = displayCount;
		this.message = message;
//		for(PermissionGroup group : targetGroups) this.targetGroups.add(group);
	}
}
