package android.hugonicolau.androidbluetoothlibrary.client;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Sends messages as <code>Strings</code>. Messages are separated by
 * <code>'\n'</code> characters.
 * 
 * @author Joshua Brown
 * 
 */
public class MessageSender {

	private OutputStream mOutputStream;

	/**
	 * Class constructor
	 */
	public MessageSender() {
	}

	public void setOutputStream(OutputStream output) {
		mOutputStream = output;
	}

	public void sendMessage(String message) {
		if (mOutputStream == null) {
			throw new IllegalStateException(
					"sendMessage() called with no OutputStream set");
		}
		//message = message + '\u001a';
		try {
			mOutputStream.write(message.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}