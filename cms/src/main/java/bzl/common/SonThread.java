package bzl.common;

public class SonThread extends Thread {
	public Callback callback = null;

	public SonThread() {
		this.setName("sonThread");
	}

	public void setCallback(Callback theCallback) {
		callback = theCallback;
	}

	@Override
	public void run() {

		callback.callBack("", null);
	}
}