package cacophonia.runtime;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import cacophonia.Constants;

/**
 * Notifies the remote UI, running in a different process, that a call is made between two plugins.
 * 
 * See {@link cacophonia.ui.UI} for the implementation of the remote UI itself.
 *
 */
class RemoteUI {
	Socket socket;
	DataInputStream inputStream;
	DataOutputStream outputStream;

	public RemoteUI() {
		this.setupListener();
	}

	DataInputStream getInputStream() {
		if (inputStream != null) return inputStream;
		try {
			if (socket == null) {
				socket = new Socket("localhost",6666);
			}
			inputStream = new DataInputStream(socket.getInputStream());  
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}  
		return inputStream;
	}

	DataOutputStream getOutputStream() {
		if (outputStream != null) return outputStream;
		try {
			if (socket == null) {
				socket = new Socket("localhost",6666);
			}
			outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));  
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}  
		return outputStream;
	}

	private void setupListener() {
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(2000);
						int command = getInputStream().readInt();
						String details = (String)getInputStream().readUTF();
						switch (command) {
						case Constants.EVENT_INSPECT_PLUGIN:
							for (String name : details.split(" ")) {
								Method.trace(name, true);
							}
							break;
						case Constants.EVENT_UN_INSPECT_PLUGIN:
							for (String name : details.split(" ")) {
								Method.trace(name, false);
							}
							break;
						case Constants.EVENT_IMPORT_PLUGIN_FROM_SOURCE:
						case Constants.EVENT_IMPORT_PLUGIN_FROM_REPOSITORY:
							System.err.println("Plugin import not implemented. Manually import " + details);
							break;
						case Constants.EVENT_EXIT:
							System.exit(0);
							break;
						}
					} catch (Exception e) {
						System.out.println("UI went away");
						System.exit(0);
					}
				}
			}
		}).start();
	}
	
	public void sendEvent(int type, String message) {
		try {
			synchronized (socket) {
				DataOutputStream stream = getOutputStream();
				stream.writeInt(type);
				stream.writeUTF(message);
				stream.flush(); 
			}
		} catch (Exception e) {
			// ignore - UI went away - will exit soon
		}  
	}
	
	void close() {
		try {
			getInputStream().close();
			getOutputStream().close();
			socket.close();   
		} catch (Exception e) {
			e.printStackTrace();
		}  
	}
}