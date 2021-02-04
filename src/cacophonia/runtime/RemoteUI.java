package cacophonia.runtime;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
		try {
			socket = new Socket("localhost",6666);
			inputStream = new DataInputStream(socket.getInputStream());  
			outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));  
			this.setupListener();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}  
	}
	
	private void setupListener() {
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(1000);
						int command = inputStream.readInt();
						String details = (String)inputStream.readUTF();
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
				outputStream.writeInt(type);
				outputStream.writeUTF(message);
				outputStream.flush(); 
			}
		} catch (Exception e) {
			// ignore - UI went away - will exit soon
		}  
	}
	
	void close() {
		try {
			outputStream.close();
			socket.close();   
		} catch (Exception e) {
			e.printStackTrace();
		}  
	}
}