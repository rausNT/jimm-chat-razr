/* Jimm Chat modification notice (GPL-2.0-or-later).
 * 2026-10-17: forwarded incoming/outgoing messages to the new persistent chat UI.
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
 File: src/jimm/ChatHistory.java
 Version: 0.5.1  Date: 2006/10/31
 Author(s): Andreas Rossbacher, Artyomov Denis, Dmitry Tunin
 *******************************************************************************/

package jimm;

import java.util.Enumeration;
import java.util.*;
import javax.microedition.lcdui.*;

import jimm.comm.Message;
import jimm.comm.PlainMessage;
import jimm.comm.SystemNotice;
import jimm.comm.UrlMessage;
import jimm.comm.Util;
import jimm.util.ResourceBundle;
import DrawControls.TextList;
import DrawControls.VirtualList;
import DrawControls.VirtualListCommands;


class MessData
{
	private long time;
	private int rowData;
	
	public MessData(boolean incoming, long time, int textOffset, boolean contains_url)
	{
		this.time    = time;
		this.rowData = (textOffset&0xFFFFFF)|(contains_url ? 0x8000000 : 0)|(incoming ? 0x4000000 : 0);
	}
		
	public boolean getIncoming() { return (rowData&0x4000000) != 0; }
	public long getTime() { return time; }
	public int getOffset() { return (rowData&0xFFFFFF); }
	public boolean isURL() { return (rowData&0x8000000) != 0; }
}


class ChatTextList implements VirtualListCommands
{
	
	TextList textList;
	public String ChatName, uin;
	private Vector messData = new Vector();
	private int messTotalCounter = 0;
	
	ChatTextList(String name, String uin)
	{
		
		textList = new TextList(null);
		
		textList.setCursorMode(TextList.SEL_NONE);
		textList.setFontSize
		(
			Options.getBoolean(Options.OPTION_CHAT_SMALL_FONT)
				   ? TextList.SMALL_FONT : TextList.MEDIUM_FONT
		);
		
		this.uin = uin;
		ChatName = name;
		JimmUI.setColorScheme(textList);
		
		textList.setVLCommands(this);
	}
	
	public Displayable getDisplayable()
	{
		Displayable result;
		textList.setForcedSize(textList.getWidth(), textList.getHeight());
		result = textList;
		
		return result;
	}
	
	static int getInOutColor(boolean incoming)
	{
		return incoming ? 
		       0xFF0000 : 
		       Options.getSchemeColor(Options.CLRSCHHEME_BLUE);
	}

	Vector getMessData()
	{
		return messData; 
	}

	public void onCursorMove(VirtualList sender) 
	{
		sender.removeCommand(ContactListContactItem.gotourlCommand);
		int messIndex = textList.getCurrTextIndex();
		if (messIndex != -1)
		{
			MessData md = (MessData)getMessData().elementAt(messIndex);
			if ( md.isURL() )
				sender.addCommand(ContactListContactItem.gotourlCommand);
		}
	}
	
	public void onItemSelected(VirtualList sender) {}
	public void onKeyPress(VirtualList sender, int keyCode,int type)
	{
		try // getGameAction can raise exception
		{
			if (type == VirtualList.KEY_PRESSED)
			{
				String currUin;
				switch (sender.getGameAction(keyCode))
				{
				case Canvas.LEFT:
					currUin = ContactList.showNextPrevChat(false);
					ChatHistory.calcCounter(currUin);
					return;
				
				case Canvas.RIGHT:
					currUin = ContactList.showNextPrevChat(true);
					ChatHistory.calcCounter(currUin);
					return;
				}
			}
			
			JimmUI.execHotKey(ContactList.getItembyUIN(uin), keyCode, type);
		}
		catch(Exception e)
		{
			// do nothing
		}
	}
	
	private boolean typing = false;
	public void BeginTyping(boolean type)
	{
		typing = type;
		textList.repaint();
	}
	
	void addTextToForm(String from, String message, String url, long time, boolean red, boolean offline)
	{
		int texOffset;
		
		textList.lock();
		int lastSize = textList.getSize();
		textList.addBigText
		(
			from + " (" + Util.getDateString(!offline, time) + "): ",
			getInOutColor(red), 
			Font.STYLE_BOLD,
			messTotalCounter
		);
		textList.doCRLF(messTotalCounter);

		if (url.length() > 0)
		{
			textList.addBigText
			(
				ResourceBundle.getString("url")+": "+url, 
				0x00FF00, 
				Font.STYLE_PLAIN, messTotalCounter
			);
		}
		
		texOffset = textList.getSize()-lastSize;
		
		JimmUI.addMessageText(textList, message, messTotalCounter);
		
		boolean contains_url = false;
		if ( Util.parseMessageForURL(message) != null ) 
		{
			contains_url = true;
			if ( texOffset == 1) textList.addCommand(ContactListContactItem.gotourlCommand);
		}
		getMessData().addElement( new MessData(red, time, texOffset, contains_url) );
		messTotalCounter++;
		
		textList.setTopItem(lastSize);		
		textList.unlock();
	}
	
	public void activate(boolean initChat, boolean resetText)
	{
		Jimm.display.setCurrent(textList);
	}
	
}

public class ChatHistory
{
	static private Hashtable historyTable;
	static private int counter;

	// Adds selected message to history

	static
	{
		historyTable = new Hashtable();
		counter = 1;
	}

	// Adds a message to the message display
	static protected synchronized void addMessage(String uin,Message message,ContactListContactItem contact)
	{
		if (!historyTable.containsKey(uin))
			newChatForm(uin,contact.getStringValue(ContactListContactItem.CONTACTITEM_NAME));
		
		ChatTextList chat = (ChatTextList)historyTable.get(uin);

		boolean offline = message.getOffline();

		if (message instanceof PlainMessage)
		{
			PlainMessage plainMsg = (PlainMessage) message;
			
			if (!chat.getDisplayable().isShown()) contact.increaseMessageCount(ContactListContactItem.MESSAGE_PLAIN);
			
			addTextToForm(uin,contact.getStringValue(ContactListContactItem.CONTACTITEM_NAME), plainMsg.getText(), "", plainMsg.getNewDate(), true, offline);
			
			
			if ( !message.getOffline() )
				ContactListContactItem.showPopupWindow(uin, contact.getStringValue(ContactListContactItem.CONTACTITEM_NAME), plainMsg.getText());
		}
		if (message instanceof UrlMessage)
		{
			UrlMessage urlMsg = (UrlMessage) message;
			if (!chat.getDisplayable().isShown()) contact.increaseMessageCount(ContactListContactItem.MESSAGE_URL);
			addTextToForm(uin,contact.getStringValue(ContactListContactItem.CONTACTITEM_NAME), urlMsg.getText(), urlMsg.getUrl(), urlMsg.getNewDate(), false, offline);
		}
		if (message instanceof SystemNotice)
		{
			SystemNotice notice = (SystemNotice) message;
			if (!chat.getDisplayable().isShown()) contact.increaseMessageCount(ContactListContactItem.MESSAGE_SYS_NOTICE);

			if (notice.getSysnotetype() == SystemNotice.SYS_NOTICE_YOUWEREADDED)
			{
				addTextToForm(uin,ResourceBundle.getString("sysnotice"), ResourceBundle.getString("youwereadded")
						+ notice.getSndrUin(), "", notice.getNewDate(), false, offline);
			} else if (notice.getSysnotetype() == SystemNotice.SYS_NOTICE_AUTHREQ)
			{
				contact.increaseMessageCount(ContactListContactItem.MESSAGE_AUTH_REQUEST);
				addTextToForm(uin,ResourceBundle.getString("sysnotice"), notice.getSndrUin()
						+ ResourceBundle.getString("wantsyourauth") + notice.getReason(), "", notice.getNewDate(), false, offline);
			} else if (notice.getSysnotetype() == SystemNotice.SYS_NOTICE_AUTHREPLY)
			{
				if (notice.isAUTH_granted())
				{
					contact.setBooleanValue(ContactListContactItem.CONTACTITEM_NO_AUTH,false);
					addTextToForm(uin,ResourceBundle.getString("sysnotice"), ResourceBundle.getString("grantedby")
							+ notice.getSndrUin() + ".", "", notice.getNewDate(), false, offline);
				} else if (notice.getReason() != null)
					addTextToForm(uin,ResourceBundle.getString("sysnotice"), ResourceBundle.getString("denyedby")
							+ notice.getSndrUin() + ". " + ResourceBundle.getString("reason") + ": " + notice.getReason(),
							"", notice.getNewDate(), false, offline);
				else
					addTextToForm(uin,ResourceBundle.getString("sysnotice"), ResourceBundle.getString("denyedby")
							+ notice.getSndrUin() + ". " + ResourceBundle.getString("noreason"), "", notice.getNewDate(),
							false, offline);
			}
		}
	}
	
	static protected synchronized void addMyMessage(String uin, String message, long time, String ChatName)
	{
		if (!historyTable.containsKey(uin))
			newChatForm(uin,ChatName);

		((ChatTextList)historyTable.get(uin)).addTextToForm(ResourceBundle.getString("me"),message,"",time,false,false);
        SimpleChat.received(uin, ChatName, message, time, true);
	}
	
	// Add text to message form
	static synchronized private void addTextToForm(String uin,String from, String message, String url, long time, boolean red, boolean offline)
	{
		ChatTextList msgDisplay = (ChatTextList) historyTable.get(uin);

		msgDisplay.addTextToForm(from, message, url, time, red, offline);
        SimpleChat.received(uin, from, message + (url.length()>0 ? " " + url : ""), time, false);
	}
	
	static private MessData getCurrentMessData(String uin)
	{
		ChatTextList list = getChatHistoryAt(uin);
		int messIndex = list.textList.getCurrTextIndex();
		if (messIndex == -1) return null;
		MessData md = (MessData)list.getMessData().elementAt(messIndex);
		return md;
	}
	
	static public String getCurrentMessage(String uin)
	{
		return getChatHistoryAt(uin).textList.getCurrText(getCurrentMessData(uin).getOffset(), false);
	}
	
	static public void copyText(String uin, String from)
	{
		ChatTextList list = getChatHistoryAt(uin);
		int messIndex = list.textList.getCurrTextIndex();
		if (messIndex == -1) return;
		MessData md = (MessData)list.getMessData().elementAt(messIndex);
		
		JimmUI.setClipBoardText
		(
			md.getIncoming(),
			Util.getDateString(false, md.getTime()),
			md.getIncoming() ? from : ResourceBundle.getString("me"),
			getCurrentMessage(uin)
		);
	}

	// Returns the chat history form at the given uin
	static public ChatTextList getChatHistoryAt(String uin)
	{
		if (historyTable.containsKey(uin))
			return (ChatTextList) historyTable.get(uin);
		else
			return new ChatTextList("Error", null);
	}
	
	final static public int DEL_TYPE_CURRENT        = 1;
	final static public int DEL_TYPE_ALL_EXCEPT_CUR = 2;
	final static public int DEL_TYPE_ALL            = 3;
	
	static public void chatHistoryDelete(String uin)
	{
		ContactListContactItem cItem = ContactList.getItembyUIN(uin);
		historyTable.remove(uin);
		
		cItem.setBooleanValue(ContactListContactItem.CONTACTITEM_HAS_CHAT, false);
		cItem.setIntValue(ContactListContactItem.CONTACTITEM_PLAINMESSAGES, 0);
		cItem.setIntValue(ContactListContactItem.CONTACTITEM_URLMESSAGES, 0);
		cItem.setIntValue(ContactListContactItem.CONTACTITEM_SYSNOTICES, 0);
	}

	// Delete the chat history for uin
	static public void chatHistoryDelete(String uin, int delType)
	{
		switch (delType)
		{
		case DEL_TYPE_CURRENT:
			chatHistoryDelete(uin);
			break;
			
		case DEL_TYPE_ALL_EXCEPT_CUR:
		case DEL_TYPE_ALL:
			Enumeration AllChats = historyTable.keys();
			while (AllChats.hasMoreElements())
			{
				String key = (String)AllChats.nextElement();
				if ((delType == DEL_TYPE_ALL_EXCEPT_CUR) && (key.equals(uin))) continue;
				chatHistoryDelete(key);
			}
			break;
		}
	}

	// Returns if the chat history at the given number is shown
	static public boolean chatHistoryShown(String uin)
	{
		if (historyTable.containsKey(uin))
		{
			ChatTextList temp = (ChatTextList)historyTable.get(uin);
			return temp.getDisplayable().isShown();
		}
		else
			return false;
	}

	// Returns true if chat history exists for this uin
	static public boolean chatHistoryExists(String uin)
	{
		return historyTable.containsKey(uin);
	}

	
	// Creates a new chat form
	static private void newChatForm(String uin, String name)
	{
		ChatTextList chatForm = new ChatTextList(name, uin);
		historyTable.put(uin,chatForm);
		UpdateCaption(uin);
		ContactList.getItembyUIN(uin).setBooleanValue(ContactListContactItem.CONTACTITEM_HAS_CHAT,true); ///
	}
	
	// fill chat with last history lines
	
	static public void contactRenamed(String uin, String newName)
	{
		ChatTextList temp = (ChatTextList) historyTable.get(uin);
		if (temp == null) return;
		temp.ChatName = newName;
		UpdateCaption(uin);
	}

	static public void UpdateCaption(String uin)
	{
		calcCounter(uin);
		ChatTextList temp = (ChatTextList) historyTable.get(uin);
		// Calculate the title for the chatdisplay.
		String Title = temp.ChatName+" ("+counter+"/"+historyTable.size()+")";
		temp.textList.setCaption(Title);
	}

	static public void setColorScheme()
	{
		Enumeration AllChats = historyTable.elements();
		while (AllChats.hasMoreElements())
			JimmUI.setColorScheme(((ChatTextList)AllChats.nextElement()).textList);
	}
	
	// Sets the counter for the ChatHistory
	static public void calcCounter(String curUin)
    {
		if (curUin == null) return;
		Enumeration AllChats = historyTable.elements();
		Object chat = historyTable.get(curUin);
		counter = 1;
		while (AllChats.hasMoreElements())
		{
			if (AllChats.nextElement() == chat) break;
			counter++;
		}
    }
}
