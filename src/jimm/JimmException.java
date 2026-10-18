/* Jimm Chat modification notice (GPL-2.0-or-later).
 * 2026-10-17: mechanically preprocessed upstream source for MOTOROLA, without optional modules; no intentional functional edit.
 * 2026-10-18: added this dated notice; original authors and license retained.
 * See COPYING, NOTICE.txt and CHANGES.md. No warranty.
 */
/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/jimm/JimmException.java
 Version: 0.5.1  Date: 2006/10/31
 Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/


package jimm;


import jimm.util.ResourceBundle;
import jimm.comm.Icq;

import javax.microedition.lcdui.*;

public class JimmException extends Exception
			implements CommandListener
{


	// Returns the error description for the given error code
	public static String getErrDesc(int errCode, int extErrCode)
	{
		String errDesc = ResourceBundle.getString("error_" + errCode);
		int ext = errDesc.indexOf("EXT");
		if (ext != -1) return (errDesc.substring(0, ext) + extErrCode + errDesc.substring(ext + 3));
		return errDesc;
	}


	/****************************************************************************/


	// True, if this is a critial exception
	protected boolean critical;


	// True, if an error message should be presented to the user
	protected boolean displayMsg;
	
	// True, if this is an exceptuion for an peer connection
	protected boolean peer;


	// Constructs a critical JimmException
	public JimmException(int errCode, int extErrCode)
	{
		super(JimmException.getErrDesc(errCode, extErrCode));
		this.critical = true;
		this.displayMsg = true;
	}


	// Constructs a non-critical JimmException
	public JimmException(int errCode, int extErrCode, boolean displayMsg)
	{
		super(JimmException.getErrDesc(errCode, extErrCode));
		this.critical = false;
		this.displayMsg = displayMsg;
	}
	


	// Returns true if an error message should be presented to the user
	public boolean isDisplayMsg()
	{
		return (this.displayMsg);
	}


	// Returns true if this is a critical exception
	public boolean isCritical()
	{
		return (this.critical);
	}
	


	// Exception handler
	public synchronized static Alert handleException(JimmException e)
	{

		// Critical exception
	    if (e.isCritical())
		{

			// Reset comm. subsystem
			Icq.resetServerCon();
			
			// Set offline status for all contacts and reset online counters 
			ContactList.setStatusesOffline();
			SplashCanvas.setStatusToDraw(JimmUI.getStatusImageIndex(ContactList.STATUS_OFFLINE));
			DrawControls.VirtualList.setLEDmode(DrawControls.VirtualList.BKLT_TYPE_LIGHTING, -1, 0xFF0000);
			
			// Unlock splash (if locked)
			if (SplashCanvas.locked()) SplashCanvas.unlock(true);

			// Display error message
			Alert errorMsg = new Alert(ResourceBundle.getString("error"), e.getMessage(), null, AlertType.ERROR);
			errorMsg.setTimeout(Alert.FOREVER);
			errorMsg.setCommandListener(e);
			MainMenu.activate(errorMsg);
			return(errorMsg);

		}
		// Non-critical exception
		else
		{

			// Display error message, if required
			if (e.isDisplayMsg())
			{
				Alert errorMsg = new Alert(ResourceBundle.getString("warning"), e.getMessage(), null, AlertType.WARNING);
				errorMsg.setTimeout(Alert.FOREVER);
				
				SplashCanvas.unlock(false);
				
				if (Icq.isConnected()) ContactList.activate(errorMsg);
				else MainMenu.activate(errorMsg);
				return(errorMsg);
			}
			return(null);
		}

	}

	public void commandAction(Command c, Displayable d)
	{
		DrawControls.VirtualList.disableLED();
		MainMenu.activate();
	}


}
