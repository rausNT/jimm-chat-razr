// Modified 2026-10-17/18: Jimm Chat UI/RMS tests. GPL-2.0-or-later; no warranty.
package jimm;
import javax.microedition.lcdui.*;
import javax.microedition.rms.RecordStore;
import jimm.comm.Icq;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
public class ChatTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void render(SimpleChat c,String name)throws Exception{BufferedImage im=new BufferedImage(176,200,BufferedImage.TYPE_INT_RGB);c.paint(new Graphics(im.createGraphics(),176,200));ImageIO.write(im,"png",new File("dist/"+name+".png"));}
 public static void main(String[] args)throws Exception{
  SimpleChat.home();SimpleChat c=SimpleChat.instance();check(c.chats.size()==5,"Starter chats restored");check(((SimpleChat.ThreadData)c.chats.elementAt(0)).name.equals("Jimm Online")&&((SimpleChat.ThreadData)c.chats.elementAt(0)).messages.size()==0,"Starter chat is local and has no fabricated message");render(c,"starter-preview");
  SimpleChat.received("123456","Алексей","Привет! Как твоя Motorola?",1000,false);
  SimpleChat.ThreadData a=(SimpleChat.ThreadData)c.chats.elementAt(0);a.name="Алексей";check(a.unread==1,"Unread increment");
  c.openThread(a);check(a.unread==0,"Read on opening");
  SimpleChat.received("123456","Алексей","Работает! Даже новый мессенджер есть.",1001,true);
  SimpleChat.received("123456","Алексей","Класс, вечером спишемся",1002,false);
  check(a.unread==0,"Visible chat stays read");render(c,"conversation-preview");
  c.compose();c.editor.setString("Мой черновик");c.commandAction(c.back,c.editor);check(a.draft.equals("Мой черновик"),"Draft saved");
  Icq.online=false;c.compose();c.commandAction(c.send,c.editor);check(Icq.sends==0&&a.draft.length()>0,"Offline retains draft without sending");
  Icq.online=true;Icq.fail=true;c.compose();c.commandAction(c.send,c.editor);check(Icq.sends==0&&a.draft.length()>0,"Failure retains draft");
  Icq.fail=false;c.compose();c.commandAction(c.send,c.editor);check(Icq.sends==1&&a.draft.length()==0,"Accepted send clears draft");
  SimpleChat.home();SimpleChat.received("654321","Мария","Увидимся завтра",1003,false);((SimpleChat.ThreadData)c.chats.elementAt(0)).name="Мария";render(c,"chats-preview");
  c.save();SimpleChat loaded=new SimpleChat();check(loaded.chats.size()==c.chats.size(),"RMS chats round trip keeps starter chats");check(((SimpleChat.ThreadData)loaded.chats.elementAt(0)).unread==1,"Unread persisted");
  for(int i=0;i<60;i++)SimpleChat.received("654321","Мария","Сообщение "+i,1000+i,false);check(((SimpleChat.ThreadData)c.chats.elementAt(0)).messages.size()==40,"History bounded");
  check(SimpleChat.wrap("длинноесловобезпробелов1234567890123456789",c.normal,40).size()>1,"Long words wrap");
  c.openThread((SimpleChat.ThreadData)c.chats.elementAt(0));c.scroll=100000;c.paint(new Graphics(new BufferedImage(176,200,1).createGraphics(),176,200));check(c.scroll<100000,"Scroll bounded");
  check(c.commands.contains(c.write)&&!c.commands.contains(c.open),"Single chat primary command");
  Icq.online=false;SimpleChat.home();check(c.commands.contains(c.open)&&c.commands.contains(c.connectNow)&&!c.commands.contains(c.write),"Offline home exposes direct Connect");
  int connectsBefore=Icq.connects;c.commandAction(c.connectNow,c);check(Icq.connects==connectsBefore+1&&Icq.online,"Direct Connect starts the platform network request");
  SimpleChat.home();check(c.commands.contains(c.open)&&!c.commands.contains(c.connectNow),"Online home hides Connect");
  check(RecordStore.data.length<=98304,"Archive bounded");
  ContactList.items=new ContactListContactItem[]{new ContactListContactItem(0,0,"77771","Roster one",false,true),new ContactListContactItem(0,0,"77772","Roster two",false,true)};
  int before=c.chats.size(),sent=Icq.sends;SimpleChat.home();check(c.chats.size()==before+2,"Roster contacts appear without messages");
  SimpleChat.ThreadData bot=c.get("77771","");check(bot.name.equals("Roster one")&&bot.messages.size()==0&&bot.unread==0,"Real roster name, no fabricated messages");
  SimpleChat.home();check(c.chats.size()==before+2,"Roster refresh idempotent");check(Icq.sends==sent,"Roster import never sends messages");
  bot.draft="draft";bot.messages.addElement(new SimpleChat.Msg("saved",true,1000));ContactList.items[0]=new ContactListContactItem(0,0,"77771","New name",false,true);SimpleChat.home();check(bot.name.equals("New name")&&bot.draft.equals("draft")&&bot.messages.size()==1,"Roster rename preserves history and drafts");
  System.out.println("PASS: real roster import, empty conversations visible, deduplication, ZERO sends, rename preserves history.");
  c.showMenu();check(c.actions.labels.size()==3&&c.actions.labels.get(0).equals("Моя учётка"),"Three top sections, account first");
  c.actions.selected=0;c.commandAction(c.open,c.actions);check(Display.current==c.account,"Account opens directly");c.commandAction(c.back,c.account);check(Display.current==c.actions,"Account back to menu");
  c.actions.selected=1;c.commandAction(c.open,c.actions);check(c.newChatMenu.labels.size()==2,"Two new chat choices");c.newChatMenu.selected=1;c.commandAction(c.open,c.newChatMenu);check(Display.current==c.number,"Direct UIN entry");c.commandAction(c.back,c.number);check(Display.current==c.newChatMenu,"UIN back to New chat");c.commandAction(c.back,c.newChatMenu);check(Display.current==c.actions,"New chat back to menu");
  c.actions.selected=2;c.commandAction(c.open,c.actions);check(c.appMenu.labels.size()==3,"Three app actions");c.commandAction(c.back,c.appMenu);check(Display.current==c.actions,"App back to menu");c.commandAction(c.back,c.actions);check(Display.current==c,"Menu back to current screen");
  System.out.println("PASS: three root sections, account first/direct, two new-chat choices, three app actions, consistent parent navigation.");
  c.openThread(bot);bot.draft="";int sendsBefore=Icq.sends;c.keyPressed(49);check(Display.current==c.quickPicker&&!c.pickingSmiles,"Key 1 opens phrases");c.quickPicker.selected=0;c.commandAction(c.open,c.quickPicker);check(c.editor.getString().equals("Да")&&Display.current==c.editor,"Phrase inserted in editor");
  c.commandAction(c.smiles,c.editor);c.quickPicker.selected=1;c.commandAction(c.open,c.quickPicker);check(c.editor.getString().equals("Да ;)"),"Smile appended without losing draft");check(Icq.sends==sendsBefore,"Quick choices never send");
  c.commandAction(c.phrases,c.editor);c.commandAction(c.back,c.quickPicker);check(Display.current==c.editor&&c.editor.getString().equals("Да ;)"),"Cancel preserves draft and returns editor");
  StringBuffer full=new StringBuffer();for(int i=0;i<1024;i++)full.append('x');c.editor.setString(full.toString());c.insertQuick(":)");check(c.editor.getString().length()==1024&&Icq.sends==sendsBefore,"Full draft not truncated or sent");
  bot.draft="";c.keyPressed(51);check(c.pickingSmiles&&Display.current==c.quickPicker,"Key 3 opens smile picker");check(c.actions.labels.size()==3,"Root menu remains three sections");
  System.out.println("PASS: phrases, smile insertion, draft preservation, cancellation, overflow, shortcuts, ZERO automatic sends.");
  c.showAppMenu();c.appMenu.selected=1;c.commandAction(c.open,c.appMenu);check(Display.current==c.helpScreen,"Help exposes legal notice");c.commandAction(c.licenseCommand,c.helpScreen);check(Display.current==c.licenseScreen,"License opens");StringBuffer licenseText=new StringBuffer();for(String part:c.licenseScreen.text)licenseText.append(part);check(licenseText.toString().contains("GNU GENERAL PUBLIC LICENSE")&&licenseText.toString().contains("END OF TERMS AND CONDITIONS"),"Full GPL text loaded");c.commandAction(c.back,c.licenseScreen);check(Display.current==c.helpScreen,"License back to help");
  System.out.println("PASS: attribution/help, full embedded GPL text, back navigation.");
  c.openThread(bot);int menuSends=Icq.sends;c.keyPressed(53);check(Display.current==c.chatMenu&&c.chatMenu.labels.size()==3,"Center opens three chat actions");
  c.chatMenu.selected=1;c.commandAction(c.open,c.chatMenu);check(Display.current==c.quickPicker&&!c.pickingSmiles,"Center menu quick replies");c.commandAction(c.back,c.quickPicker);c.commandAction(c.back,c.editor);
  c.commandAction(c.menu,c);check(Display.current==c.chatMenu,"Motorola menu command opens chat actions");c.chatMenu.selected=2;c.commandAction(c.open,c.chatMenu);check(c.pickingSmiles&&Display.current==c.quickPicker,"Center menu smileys");
  c.commandAction(c.back,c.quickPicker);c.commandAction(c.back,c.editor);c.showChatMenu();c.commandAction(c.back,c.chatMenu);check(Display.current==c,"Chat menu Back returns conversation");c.commandAction(c.write,c);check(Display.current==c.editor,"Left Write still direct");check(Icq.sends==menuSends,"Chat menu never sends automatically");
  c.openThread(bot);check(!c.commands.contains(c.menu)&&c.commands.contains(c.chatWrite)&&c.commands.contains(c.phrases)&&c.commands.contains(c.smiles),"Native chat menu has direct actions and no nested Menu");
  c.commandAction(c.phrases,c);check(Display.current==c.quickPicker&&!c.pickingSmiles,"Native phrase action opens picker directly");c.commandAction(c.back,c.quickPicker);c.commandAction(c.back,c.editor);
  c.commandAction(c.smiles,c);check(Display.current==c.quickPicker&&c.pickingSmiles,"Native smile action opens picker directly");c.commandAction(c.back,c.quickPicker);c.commandAction(c.back,c.editor);
  c.commandAction(c.chatWrite,c);check(Display.current==c.editor,"Native Write opens editor");SimpleChat.home();check(c.commands.contains(c.menu)&&!c.commands.contains(c.phrases)&&!c.commands.contains(c.chatWrite),"Home removes chat commands");c.openThread(bot);c.compose();check(Icq.sends==menuSends,"Native commands never send");
  System.out.println("PASS: central/5/menu chat actions, phrases/smileys, back, direct left Write, ZERO sends.");
  c.editor.setString("До после");c.editor.caret=3;c.pickQuick(false);c.quickPicker.keyPressed(53);check(c.editor.getString().equals("До Ты тут? после"),"Digit 5 chooses fifth phrase at captured caret");
  c.editor.setString("хвост");c.editor.caret=0;c.pickQuick(true);c.quickPicker.keyPressed(49);check(c.editor.getString().equals(":) хвост"),"Insert at start preserves suffix");
  c.editor.setString("конец");c.pickQuick(false);c.quickPicker.keyPressed(57);check(c.editor.getString().equals("конец Напишу позже"),"Digit 9 inserts at end");
  c.pickQuick(false);c.quickPicker.selected=2;c.commandAction(c.editPhrase,c.quickPicker);c.phraseEditor.setString("Буду через 5 минут");c.commandAction(c.savePhrase,c.phraseEditor);check(Display.current==c.quickPicker&&c.ownPhrases[2].equals("Буду через 5 минут"),"Edit saves in same picker");
  SimpleChat restoredPhrases=new SimpleChat();check(restoredPhrases.ownPhrases[2].equals("Буду через 5 минут"),"Custom phrase survives restart");check(restoredPhrases.chats.size()==c.chats.size(),"Phrase store separate from history");
  c.commandAction(c.editPhrase,c.quickPicker);c.phraseEditor.setString(" ");c.commandAction(c.savePhrase,c.phraseEditor);check(c.ownPhrases[2].equals("Буду через 5 минут"),"Empty edit does not erase phrase");c.commandAction(c.back,c.phraseEditor);
  c.commandAction(c.editPhrase,c.quickPicker);c.phraseEditor.setString("Отмена");c.commandAction(c.back,c.phraseEditor);check(c.ownPhrases[2].equals("Буду через 5 минут"),"Cancel edit preserves phrase");
  c.editor.setString("");c.pickQuick(false);c.quickPicker.keyPressed(51);check(c.editor.getString().equals("Буду через 5 минут"),"Digit 3 inserts custom phrase");check(Icq.sends==menuSends,"All new actions never send");
  c.pickQuick(false);BufferedImage pickerImage=new BufferedImage(176,200,BufferedImage.TYPE_INT_RGB);Graphics pickerGraphics=new Graphics(pickerImage.createGraphics(),176,200);c.quickPicker.paint(pickerGraphics);check(pickerGraphics.clippedText==0,"Picker text fits small display");ImageIO.write(pickerImage,"png",new File("dist/phrases-preview.png"));
  System.out.println("PASS: numeric 1/3/5/9, start/middle/end caret, custom phrase persistence, separate RMS, empty/cancel edit, ZERO sends.");
  Options.current=1;c.showAccount();String previous=Options.getString(14);c.accountUin.setString("7654321");c.commandAction(c.back,c.account);check(Options.getString(14).equals(previous),"Cancel does not change account");
  c.showAccount();c.accountUin.setString("7654321");c.accountPassword.setString("test-only");c.accountHost.setString("icq.jimm.online");c.accountPort.setString("70000");int saved=Options.saves;c.commandAction(c.saveAccount,c.account);check(Options.saves==saved,"Invalid port rejected");c.accountPort.setString("5190");Options.failSave=true;c.commandAction(c.saveAccount,c.account);check(Options.getString(14).equals(previous),"Save failure rolls settings back");Options.failSave=false;c.commandAction(c.saveAccount,c.account);check(Options.getString(14).equals("7654321")&&Options.getString(229).equals("test-only")&&Options.saves==saved+1,"Selected account saved, not account one");c.showAccount();check(c.accountUin.getString().equals("7654321")&&c.accountHost.getString().equals("icq.jimm.online"),"Fields reload saved settings");check(Icq.sends==menuSends,"Settings send no messages");
  System.out.println("PASS: editable account fields, cancellation, validation, save rollback and active slot.");
  System.out.println("PASS: empty state, unread, visible read, drafts, offline/failure/success send, persistence, history cap, word wrap, scroll bounds, commands. Mock transport only; no real messages sent.");
 }
}
