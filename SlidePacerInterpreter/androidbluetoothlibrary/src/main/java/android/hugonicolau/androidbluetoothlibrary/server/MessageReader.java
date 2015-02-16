package android.hugonicolau.androidbluetoothlibrary.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.util.Log;

/**
 * Reads messages and converts them to <code>Strings</code>. Messages are
 * separated by <code>'\n'</code> characters.
 * 
 * @author Joshua Brown
 * 
 */
public class MessageReader {

	private BufferedReader r;

	/**
	 * Class constructor
	 */
	public MessageReader() {
	}

	public void setInputStream(InputStream input) {
		r = new BufferedReader(new InputStreamReader(input));
	}

	public String read() {
		if (r == null) {
			throw new IllegalStateException(
					"read() called with no InputStream set");
		}
		//final String string;
		StringBuilder sb = new StringBuilder();
		try {
			
			/*String size = r.readLine();
			char[] buff = new char[Integer.valueOf(size)];
			r.read(buff);*/
			// TODO: limitation, it only deals with 1024 character at a time
			char[] buff = new char[1024];
			
			int n = r.read(buff);
			sb.append(buff);
			sb.setLength(n);
			
			Log.d("AndroidBluetoothLibrary", "message completed: " + sb.toString());
			//sb.deleteCharAt(sb.length() - 1);
			//string = r.readLine(); 
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return sb.toString();

	}

}