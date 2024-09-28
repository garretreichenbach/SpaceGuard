/**
 * <H1>Project R<H1>
 * <p/>
 * <p/>
 * <H2>Login</H2>
 * <H3>org.schema.schine.network.commands</H3>
 * Login.java
 * <HR>
 * Description goes here. If you see this message, please contact me and the
 * description will be filled.<BR>
 * <BR>
 *
 * @author Robin Promesberger (schema)
 * @mail <A HREF="mailto:schemaxx@gmail.com">schemaxx@gmail.com</A>
 * @site <A
 * HREF="http://www.the-schema.com/">http://www.the-schema.com/</A>
 * @project JnJ / VIR / Project R
 * @homepage <A
 * HREF="http://www.the-schema.com/JnJ">
 * http://www.the-schema.com/JnJ</A>
 * @copyright Copyright � 2004-2010 Robin Promesberger (schema)
 * @licence Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.schema.schine.network.commands;

import org.schema.schine.network.Command;
import org.schema.schine.network.IdGen;
import org.schema.schine.network.NetworkProcessor;
import org.schema.schine.network.client.ClientStateInterface;
import org.schema.schine.network.server.ServerProcessor;
import org.schema.schine.network.server.ServerStateInterface;

import java.io.IOException;

/**
 * The Class Login.
 * <p/>
 * This command always has the ID 0
 */
public class Login extends Command {

	public static final int NOT_LOGGED_IN = -4242;
	private long started;

	/**
	 * Instantiates a new login.
	 */
	public Login() {
		mode = MODE_RETURN;
	}

	@Override
	public void clientAnswerProcess(Object[] returnValues, ClientStateInterface stateI, short packetId) {
		int code = (Integer) returnValues[0];
		String version = returnValues[3].toString();

		String extraReason = "";

		if(returnValues.length > 4) {
			extraReason = (String) returnValues[4];
		}

		stateI.setServerVersion(version);
		stateI.setExtraLoginFailReason(extraReason);
		if(code < 0) {
			LoginCode lCode = LoginCode.getById(code);
			switch(lCode) {
				case ERROR_VPN:
					System.err.println("[Client] [LOGIN]: ERROR: VPN detected " + extraReason);
					stateI.setId(code);
					break;
				case ERROR_TOR:
					System.err.println("[Client] [LOGIN]: ERROR: TOR detected " + extraReason);
					stateI.setId(code);
					break;
				case ERROR_PROXY:
					System.err.println("[Client] [LOGIN]: ERROR: Proxy detected " + extraReason);
					stateI.setId(code);
					break;
				case ERROR_NO_ALTS:
					System.err.println("[Client] [LOGIN]: ERROR: No alternative accounts allowed " + extraReason);
					stateI.setId(code);
					break;
				case ERROR_ALREADY_LOGGED_IN:
					System.err.println("[Client] [LOGIN]: ERROR: Already logged in " + extraReason);
					stateI.setId(code);
					break;
				case ERROR_AUTHENTICATION_FAILED:
					System.err.println("[Client] [LOGIN]: ERROR: Authentication Failed " + extraReason);
					stateI.setId(code);
					break;
				case ERROR_ACCESS_DENIED:
					System.err.println("[Client] [LOGIN]: ERROR: Access Denied " + extraReason);
					stateI.setId(code);
					break;
				case ERROR_GENRAL_ERROR:
					System.err.println("[Client] [LOGIN]: ERROR: General Error " + extraReason);
					stateI.setId(code);
					break;
				case ERROR_WRONG_CLIENT_VERSION:
					System.err.println("[Client] [LOGIN]: ERROR: The version of your client is not equal to the server. Try updating with the StarMade-Starter. (client version: " + stateI.getClientVersion() + "; server version: " + version + ") " + extraReason);
					stateI.setId(code);
					break;
				case ERROR_SERVER_FULL:
					System.err.println("[Client] [LOGIN]: ERROR: Server FULL Error " + extraReason);
					stateI.setId(code);
					break;
				case ERROR_YOU_ARE_BANNED:
					System.err.println("[Client] [LOGIN]: ERROR: You are banned from this server " + extraReason);
					stateI.setId(code);
					break;
				case ERROR_NOT_ON_WHITELIST:
					System.err.println("[Client] [LOGIN]: ERROR: You are not whitelisted on this server " + extraReason);
					stateI.setId(code);
					break;
				case ERROR_INVALID_USERNAME:
					System.err.println("[Client] [LOGIN]: ERROR: The username is not accepted " + extraReason);
					stateI.setId(code);
					break;
				case ERROR_AUTHENTICATION_FAILED_REQUIRED:
					System.err.println("[Client] [LOGIN]: ERROR: Authentication required but not delivered " + extraReason);
					stateI.setId(code);
					break;
				case ERROR_NOT_ADMIN:
					System.err.println("[Client] [LOGIN]: ERROR: Not admin " + extraReason);
					stateI.setId(code);
					break;
				default:
					assert (false) : "something went wrong: " + code;
					stateI.setId(code);
					throw new IllegalArgumentException("Unknown login return code. Your client might be out of date. Please update!");
			}

		} else {
			stateI.setId((Integer) returnValues[1]);

			long ended = System.currentTimeMillis();
			long roundTripTime = ended - started;

			// set the server time as the time
			// returned by sever
			// plus half of one round trip time
			// to compensate for delay
			stateI.setServerTimeOnLogin((Long) returnValues[2] + roundTripTime / 2);

			System.err.println("[Client] [LOGIN]: Client sucessfully registered with id: " + stateI.getId() + "; Time Difference: " + stateI.getServerTimeDifference() + " (W/O RTT " + returnValues[2] + "); (RTT: " + roundTripTime + ")");
		}
	}

	@Override
	public void serverProcess(ServerProcessor serverProcessor, Object[] parameters, ServerStateInterface state, short packetId)
			throws Exception {
		//
		LoginRequest r = new LoginRequest();

		String playerName = (String) parameters[0];
		String version = parameters.length > 1 ? parameters[1].toString() : "0.0.0";
		String uId = parameters.length > 2 ? (String) parameters[2] : "";
		String login_Code = parameters.length > 3 ? (String) parameters[3] : "";
		byte userAgent = parameters.length > 4 ? (Byte) parameters[4] : (byte) 0;

		int newId = IdGen.getFreeStateId();

		System.err.println("[SERVER][LOGIN] new client connected. given id: " + newId + ": description: " + playerName);

//		System.err.println("READ SESSION: "+sessionId+"; "+sessionName);
		r.state = state;
		r.playerName = playerName;
		r.version = version;
		r.uid = uId;
		r.login_code = login_Code;
		r.id = newId;
		r.serverProcessor = serverProcessor;
		r.packetid = packetId;
		r.login = this;
		r.userAgent = userAgent;

		state.addLoginRequest(r);

		int returnCode = 0;

		System.err.println("[SERVER][LOGIN] return code " + returnCode);

	}

	@Override
	public void writeAndCommitParametriziedCommand(Object[] attribs, int fromId,
	                                               int receiver, short packetId, NetworkProcessor ntProcessor) throws IOException {
		started = System.currentTimeMillis();

		super.writeAndCommitParametriziedCommand(attribs, fromId, receiver, packetId, ntProcessor);
	}

	public enum LoginCode {

		SUCCESS_LOGGED_IN(0, ""),
		ERROR_GENRAL_ERROR(-1, "Server: general error"),
		ERROR_ALREADY_LOGGED_IN(-2, "Server: name already logged in on this server\n" +
				"\n\n\n(If you are retrying after a socket exception,\n" +
				"please wait 3 minutes for the server to time-out your old connection)"),
		ERROR_ACCESS_DENIED(-3, "Server: access denied"),
		ERROR_SERVER_FULL(-4, "Server: this server is full. Please try again later."),
		ERROR_WRONG_CLIENT_VERSION(-5, ""),
		ERROR_YOU_ARE_BANNED(-6, "Server: you are banned from this server"),
		ERROR_AUTHENTICATION_FAILED(-7, "Server Reject: this login name is protected on this server!\n\n" +
				"You either aren't logged in via uplink,\n" +
				"or the protected name belongs to another user!\n\n" +
				"Please use the \"Uplink\" menu to authenticate this name!"),
		ERROR_NOT_ON_WHITELIST(-8, "Server: you are not whitelisted on this server"),
		ERROR_INVALID_USERNAME(-9, "Server: your username is not allowed.\nMust be at least 3 characters.\nOnly letters, numbers, '-' and '_' are allowed."),
		ERROR_AUTHENTICATION_FAILED_REQUIRED(-10, "Server: this server requires authentication via the StarMade registry.\n"
				+ "This requirement is usually turned on by a server admin to deal with trolling/griefing.\n"
				+ "Please use the 'Uplink' button to enter your StarMade Registry credentials.\n"
				+ "\n"
				+ "If you don't have StarMade credentials yet, create one for free on www.star-made.org.\n"
				+ "And if you are playing on steam you can upgrade your account via steam link."),
		ERROR_NOT_ADMIN(-11, "This login can only be made as admin"),
		ERROR_VPN(-12, "Server: VPN detected. Please disable your VPN to connect to this server."),
		ERROR_NO_ALTS(-13, "Server: No alternative accounts allowed. Please use your main account to connect to this server."),
		ERROR_PROXY(-14, "Server: Proxy detected. Please disable your proxy to connect to this server."),
		ERROR_TOR(-15, "Server: TOR detected. Please disable your TOR to connect to this server.");

		public final int code;
		private final String msg;

		LoginCode(int code, String msg) {
			this.code = code;
			this.msg = msg;
		}

		public static LoginCode getById(int id) {
			for(LoginCode c : values()) {
				if(c.code == id) {
					return c;
				}
			}
			throw new IllegalArgumentException("ID: " + id);
		}

		public String errorMessage() {
			return msg;
		}
	}
}
