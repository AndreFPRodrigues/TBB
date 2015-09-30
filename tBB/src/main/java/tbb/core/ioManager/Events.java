/*
 * Android Event Injector 
 *
 * Copyright (c) 2013 by Radu Motisan , radu.motisan@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * For more information on the GPL, please go to:
 * http://www.gnu.org/copyleft/gpl.html
 *
 */

package tbb.core.ioManager;
  
import java.util.ArrayList; 

public class Events {



	public class InputDevice {

		private int m_nId;
		private String m_szPath, m_szName;
		private boolean m_bOpen;

		InputDevice(int id, String path) {
			m_nId = id;
			m_szPath = path;
		}

		public int InjectEvent() {
			return 0;
		}
	

		public int getPollingEvent() {
			return PollDev(m_nId);
		}

		public int getSuccessfulPollingType() {
			return getType();
		}

		public int getTimeStamp() {
			return getTime();
		}

		public int getSuccessfulPollingCode() {
			return getCode();
		}

		public int getSuccessfulPollingValue() {
			return getValue();
		}

		public boolean getOpen() {

			return m_bOpen;
		}

		public int getId() {
			return m_nId;
		}

		public String getPath() {
			return m_szPath;
		}

		public String getName() {
			return m_szName;
		}

		public void Close() {
			m_bOpen = false;
			RemoveDev(m_nId);
		}

		public void takeOver(boolean value) {
			int state=0;
			if(value)
				state=1;
				
			takeOverDevice(m_nId, state);
		}

		final int EV_KEY = 0x01, EV_REL = 0x02, EV_ABS = 0x03, REL_X = 0x00,
				REL_Y = 0x01, REL_Z = 0x02, BTN_TOUCH = 0x14a;// 330

		public int send(int i, int t, int c, int v) {
			return intSendEvent(i, t, c, v);

		}

		/**
		 * function Open : opens an input event node
		 * 
		 * @param forceOpen
		 *            will try to set permissions and then reopen if first open
		 *            attempt fails
		 * @return true if input event node has been opened
		 */
		public boolean Open(boolean forceOpen) {

			int res = OpenDev(m_nId);

			// if opening fails, we might not have the correct permissions, try
			// changing 660 to 666
			if (res != 0) {
				// possible only if we have root

				if (forceOpen && Shell.isSuAvailable()) {

					// set new permissions
					Shell.runCommand("chmod 666 " + m_szPath);

					// reopen
					res = OpenDev(m_nId);
				}
			}

			m_szName = getDevName(m_nId);
			m_bOpen = (res == 0);
			return m_bOpen;

		}
		
		public int  createVirtualDrive(String touch, int protocol, int absX, int absY) {
			if (Shell.isSuAvailable()) {
				Shell.runCommand("chmod 666 " + "/dev/uinput");
			}

			return createVirtualDevice(touch, protocol, absX, absY);

		}  
 
	}

	
	public static void sendVirtual(int t , int c, int v){
		sendVirtualEvent( t, c, v);
	}


	/**
	 * Scans and returns the list of all internal devices
	 * 
	 * @return ArrayList<InputDevice> - list of internal devices
	 */
	public ArrayList<InputDevice> Init() {
		ArrayList<InputDevice> m_Devs = new ArrayList<InputDevice>();
		// m_Devs.clear();

		int n = ScanFiles(); // return number of devs
 
		for (int i = 0; i < n; i++) {

			m_Devs.add(new InputDevice(i, getDevPath(i)));

			m_Devs.get(i).Open(true);

		}

		return m_Devs;
	}

	// JNI native code interface
	public native static void intEnableDebug(int enable);

	private native static int ScanFiles(); // return number of devs

	private native static int OpenDev(int devid);

	private native static int takeOverDevice(int devid, int value);

	private native static int RemoveDev(int devid);

	private native static String getDevPath(int devid);

	private native static String getDevName(int devid);

	private native static int PollDev(int devid);

	private native static int getType();

	private native static int getCode();

	private native static int getTime();

	private native static int createVirtualDevice(String touch2, int protocol, int absX, int absY);

	private native static int getValue();

	// injector:
	private native static int intSendEvent(int devid, int type, int code,
			int value);
	private native static int sendVirtualEvent( int type, int code,
			int value);

	static { 
		System.loadLibrary("EventInjector");
	}

}
