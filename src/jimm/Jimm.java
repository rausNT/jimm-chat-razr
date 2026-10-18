/* Modified 2026-10-18: first-run account setup for public variant. GPL-2.0-or-later. */
/* Jimm Chat modification notice (GPL-2.0-or-later).
 * 2026-10-17: routed application entry and return to SimpleChat.
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
 File: src/jimm/Jimm.java
 Version: 0.5.1  Date: 2006/10/31
 Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/


package jimm;


import jimm.comm.Icq;
import jimm.util.ResourceBundle;
import jimm.JimmUI;

import java.util.Timer;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.lcdui.Displayable;

public class Jimm extends MIDlet
{


	// Version
	public static String VERSION;


	// Application main object
	public static Jimm jimm;


	// Display object
	public static Display display;


	/****************************************************************************/


	// ICQ object
	private Icq icq;

	// Options container 	 
    private Options o;

	// Main menu object
	private MainMenu mm;


	// Contact list object
	private ContactList cl;

	// Chat history object
	private ChatHistory ch;
	
	static public final int funlight_device_type;
	
	public static final int FUNLIGHT_DEVICE_E380 = 2;
	public static final int FUNLIGHT_DEVICE_E390 = 3;

	// Timer object
	private Timer t;




	// Splash canvas object
	private SplashCanvas sc;
	
	// Storage for messages

	
	private JimmUI ui;

	static
	{

		String funlightsProduct = System.getProperty("funlights.product");
		System.out.println("funlights.product = " + funlightsProduct);
		if (funlightsProduct == null)
		{
			funlight_device_type = -1;
		}
		else if (funlightsProduct.equals("E380"))
		{
			funlight_device_type = FUNLIGHT_DEVICE_E380;
		}
		else
		{
			funlight_device_type = FUNLIGHT_DEVICE_E390;
		}
	}

	// Start Jimm
	public void startApp() throws MIDletStateChangeException
	{
		RunnableImpl.setMidlet(this);
		
		// Return if MIDlet has already been initialized
		if (Jimm.jimm != null)
		{
			showWorkScreen();
			return;
		}
		
		// Save MIDlet reference
		Jimm.jimm = this;
		
		// Get Jimm version
		Jimm.VERSION = this.getAppProperty("Jimm-Version");
		if (Jimm.VERSION == null) Jimm.VERSION = "0.5.1";
		
		// Create options container 	 
        this.o = new Options();

		// Create splash canvas object
		this.sc = new SplashCanvas(ResourceBundle.getString("loading"));

		// Check available heap memory, display warning if less then 250 KB
		if (Runtime.getRuntime().totalMemory() < 256000)
		{
			Alert errorMsg = new Alert(ResourceBundle.getString("warning"), JimmException.getErrDesc(170, 0), null, AlertType.WARNING);
			errorMsg.setTimeout(Alert.FOREVER);
			Display.getDisplay(this).setCurrent(errorMsg, this.sc);
			try
			{
				Thread.sleep(3000);
			}
			catch (InterruptedException e)
			{
				// Do nothing
			}
		}
		// Display splash canvas
		else
		{
			Display.getDisplay(this).setCurrent(this.sc);
		}
	
		// Get display object (and update progress indicator)
		Jimm.display = Display.getDisplay(this);
		SplashCanvas.setProgress(10);

		// Create ICQ object (and update progress indicator)
		this.icq = new Icq();
		SplashCanvas.setStatusToDraw(JimmUI.getStatusImageIndex(Icq.getCurrentStatus()));
		SplashCanvas.setProgress(20);
		
		// Create object for text storage (and update progress indicator)


		// Initialize main menu object (and update progress indicator)
		this.mm = new MainMenu();
		SplashCanvas.setProgress(40);

		
		// Create contact list object (and update progress indicator)
		this.cl = new ContactList();
		ContactList.beforeConnect();
		SplashCanvas.setProgress(60);
		
		// Create chat hisotry object (and update progress indicator)
		this.ch = new ChatHistory();
		SplashCanvas.setProgress(70);

		// Create timer object (and update progress indicator)
		this.t = new Timer();
		SplashCanvas.setProgress(80);
		
		// Create and load emotion icons
		
		new Templates();
		
		ui = new JimmUI();
		
		// set color scheme for all forms
		JimmUI.setColorScheme();
		
		if (Options.getString(Options.OPTION_UIN).length() == 0) { Options.editOptions(); return; }
		if (Options.getBoolean(Options.OPTION_AUTO_CONNECT))
        {
		    // Connect
            ContactList.beforeConnect();
            Icq.connect();
        } else
        {
            // Activate main menu
            SimpleChat.home();
        }
	}


	// Pause
	public void pauseApp()
	{
		// Do nothing
	}


	// Destroy Jimm
	public void destroyApp(boolean unconditional) throws MIDletStateChangeException
	{
        // Disconnect
        Icq.disconnect();       
        
        // Save traffic
		
		Jimm.display.setCurrent(null);
		this.notifyDestroyed();
	}


	// Returns a reference to ICQ object
	public Icq getIcqRef()
	{
		return (this.icq);
	}
	
    // Returns a reference to options container 	 
    public Options getOptionsRef() 	 
    { 	 
            return (this.o); 	 
    }

	// Returns a reference to the main menu object
	public MainMenu getMainMenuRef()
	{
		return (this.mm);
	}


	// Returns a reference to the contact list object
	public ContactList getContactListRef()
	{
		return (this.cl);
	}
	
	// Returns a reference to the chat history list object
	public ChatHistory getChatHistoryRef()
	{
		return (this.ch);
	}


	// Returns a reference to the timer object
	public Timer getTimerRef()
	{
		return (this.t);
	}
	
	// Cancels the timer and makes a new one
	public void cancelTimer()
	{
		this.t.cancel();
		this.t = new Timer();
	}


	// Returns a reference to splash canvas object
	public SplashCanvas getSplashCanvasRef()
	{
		return (this.sc);
	}


	
	public JimmUI getUIRef()
	{
		return ui;
	}
	
	static public void showWorkScreen()
	{
		if (SplashCanvas.locked()) SplashCanvas.show();
		else if (Icq.isConnected()) ContactList.activate();
		else SimpleChat.home();
	}
	
}

