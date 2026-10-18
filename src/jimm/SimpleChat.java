/* Modified 2026-10-18: expose chat actions directly in native command menu. GPL-2.0-or-later. */
/* Modified 2026-10-18: editable RMS phrases, numeric picker and caret insertion. GPL-2.0-or-later. */
/* Modified 2026-10-18: central/menu key opens three chat actions; left Write remains direct. GPL-2.0-or-later. */
/* Jimm Chat modification notice (GPL-2.0-or-later).
 * 2026-10-17: new chat UI, persistence and compact menu. 2026-10-18: quick replies, smile picker and license viewer.
 * 2026-10-18: added this dated notice; original authors and license retained.
 * See COPYING, NOTICE.txt and CHANGES.md. No warranty.
 */
/* Jimm Chat UI, GPL-2.0-or-later, based on Jimm 0.5.1. */
package jimm;
import javax.microedition.lcdui.*;
import javax.microedition.rms.*;
import java.util.*;
import java.io.*;
import jimm.comm.*;
import jimm.util.ResourceBundle;

public final class SimpleChat extends Canvas implements CommandListener {
 static SimpleChat singleton;
 final Vector chats=new Vector();
 ThreadData active;
 int selected, scroll;
 final Font normal=Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_SMALL);
 final Font bold=Font.getFont(Font.FACE_SYSTEM,Font.STYLE_BOLD,Font.SIZE_SMALL);
 final Command open=new Command("Открыть",Command.STOP,2);
 final Command write=new Command("Написать",Command.STOP,2);
 final Command chatWrite=new Command("Написать",Command.SCREEN,2);
 final Command menu=new Command("Меню",Command.SCREEN,3);
 final Command back=new Command("Назад",Command.BACK,1);
 final Command send=new Command("Отправить",Command.STOP,2);
 final Command ok=new Command("Открыть",Command.STOP,2);
 final Command phrases=new Command("Быстрый ответ",Command.SCREEN,3);
 final Command smiles=new Command("Смайлики",Command.SCREEN,4);
 static final String[] PHRASES={"Да","Нет","Не знаю","Возможно","Ты тут?","Привет!","Спасибо!","Сейчас занят","Напишу позже"};
 static final String[] SMILES={":)",";)",":D",":(",":P",":O",":/",":'(","<3"};
 static final String[] SMILE_LABELS={":)  Улыбка",";)  Подмигнуть",":D  Смех",":(  Грусть",":P  Язык",":O  Удивление",":/  Сомнение",":'(  Слёзы","<3  Сердце"};
 QuickPicker quickPicker;boolean pickingSmiles;
 final String[] ownPhrases=new String[9];
 int insertionPoint;
 TextBox phraseEditor;
 final Command editPhrase=new Command("Изменить",Command.SCREEN,3);
 final Command savePhrase=new Command("Сохранить",Command.STOP,2);
 TextBox editor,number;
 List actions,contacts,newChatMenu,appMenu,chatMenu;
 Form account,helpScreen,licenseScreen;
 final Command licenseCommand=new Command("Лицензия",Command.SCREEN,3);
 ContactListContactItem[] people;
 String storageError;
 static class Msg {String text; boolean mine; long time; Msg(String s,boolean m,long t){text=s;mine=m;time=t;}}
 static class ThreadData {String uin,name,draft="";int unread;Vector messages=new Vector();ThreadData(String u,String n){uin=u;name=n;}}
 public static SimpleChat instance(){if(singleton==null)singleton=new SimpleChat();return singleton;}
 public SimpleChat(){loadPhrases();load();setCommandListener(this);buttons();}
 static public void home(){SimpleChat c=instance();c.syncContacts();c.active=null;c.scroll=0;c.buttons();Jimm.display.setCurrent(c);c.repaint();}
 void buttons(){removeCommand(open);removeCommand(write);removeCommand(back);removeCommand(menu);removeCommand(chatWrite);removeCommand(phrases);removeCommand(smiles);if(active==null){addCommand(open);addCommand(menu);}else{addCommand(write);addCommand(back);addCommand(chatWrite);addCommand(phrases);addCommand(smiles);}}
 ThreadData get(String u,String name){for(int i=0;i<chats.size();i++){ThreadData t=(ThreadData)chats.elementAt(i);if(t.uin.equals(u))return t;}ThreadData t=new ThreadData(u,name);chats.addElement(t);return t;}
 static public void received(String u,String from,String text,long time,boolean mine){
  SimpleChat c=instance();synchronized(c){
   ContactListContactItem person=ContactList.getItembyUIN(u);
   ThreadData t=c.get(u,person==null?u:person.getStringValue(ContactListContactItem.CONTACTITEM_NAME));
   t.messages.addElement(new Msg(text,mine,time));
   while(t.messages.size()>40)t.messages.removeElementAt(0);
   if(!mine && !(c.isShown()&&c.active==t))t.unread++;
   if(c.active==t)c.scroll=0;
   chatsToFront(c,t);c.save();
  }c.repaint();
 }
 static void chatsToFront(SimpleChat c,ThreadData t){c.chats.removeElement(t);c.chats.insertElementAt(t,0);c.selected=0;}
 void syncContacts(){
  ContactListContactItem[] items=ContactList.getContactItems();boolean changed=false;
  for(int i=0;i<items.length;i++){
   String u=items[i].getStringValue(0),name=items[i].getStringValue(1);
   int before=chats.size();ThreadData t=get(u,name);
   if(chats.size()!=before)changed=true;
   if(name!=null&&!name.equals(t.name)){t.name=name;changed=true;}
  }
  if(changed)save();
 }
 void openThread(ThreadData t){active=t;t.unread=0;scroll=0;ContactListContactItem p=ContactList.getItembyUIN(t.uin);if(p!=null)p.resetUnreadMessages();buttons();save();Jimm.display.setCurrent(this);repaint();}
 void compose(){if(active==null)return;editor=new TextBox("Сообщение",active.draft,1024,TextField.ANY);editor.addCommand(send);editor.addCommand(phrases);editor.addCommand(smiles);editor.addCommand(back);editor.setCommandListener(this);Jimm.display.setCurrent(editor);}
 void pickQuick(boolean smile){if(editor==null)return;insertionPoint=editor.getCaretPosition();active.draft=editor.getString();save();pickingSmiles=smile;quickPicker=new QuickPicker();Jimm.display.setCurrent(quickPicker);}
 void insertQuick(String text){String old=editor.getString();int pos=Math.max(0,Math.min(insertionPoint,old.length()));String before=pos>0&&old.charAt(pos-1)!=' '&&old.charAt(pos-1)!='\n'?" ":"";String after=pos<old.length()&&old.charAt(pos)!=' '&&old.charAt(pos)!='\n'?" ":"";String added=before+text+after;if(old.length()+added.length()>editor.getMaxSize()){Alert a=new Alert("Сообщение заполнено","Сократи текст: максимум "+editor.getMaxSize()+" символа.",null,AlertType.INFO);a.setTimeout(Alert.FOREVER);Jimm.display.setCurrent(a,editor);return;}editor.insert(added,pos);active.draft=editor.getString();save();Jimm.display.setCurrent(editor);}
 final class QuickPicker extends Canvas {
  int selected;
  QuickPicker(){addCommand(open);addCommand(back);if(!pickingSmiles)addCommand(editPhrase);setCommandListener(SimpleChat.this);}
  int getSelectedIndex(){return selected;}
  protected void keyPressed(int key){if(key>=KEY_NUM1&&key<=KEY_NUM9){selected=key-KEY_NUM1;commandAction(open,this);return;}int a=0;try{a=getGameAction(key);}catch(Exception e){}if(a==FIRE){commandAction(open,this);return;}if(a==UP||a==DOWN){selected=(selected+(a==UP?8:1))%9;repaint();}}
  protected void keyRepeated(int key){if(key<KEY_NUM0||key>KEY_NUM9)keyPressed(key);}
  protected void paint(Graphics g){int w=getWidth(),h=getHeight(),row=normal.getHeight()+4,top=bold.getHeight()+7;g.setColor(0xF3F6FA);g.fillRect(0,0,w,h);g.setColor(0x2487B9);g.fillRect(0,0,w,top);g.setColor(0xFFFFFF);g.setFont(bold);g.drawString(pickingSmiles?"Смайлики 1–9":"Фразы 1–9",6,3,0);g.setFont(normal);int visible=Math.max(1,(h-top)/row),first=selected>=visible?selected-visible+1:0;for(int i=first;i<9;i++){int y=top+(i-first)*row;if(y+row>h)break;g.setColor(i==selected?0xDDEEF9:0xFFFFFF);g.fillRect(0,y,w,row-1);g.setColor(0x172B40);g.drawString((i+1)+" "+fit(pickingSmiles?SMILE_LABELS[i]:ownPhrases[i],w-26),6,y+2,0);}}
 }
 void loadPhrases(){for(int i=0;i<9;i++)ownPhrases[i]=PHRASES[i];RecordStore rs=null;try{rs=RecordStore.openRecordStore("chat-phrases-v1",false);DataInputStream in=new DataInputStream(new ByteArrayInputStream(rs.getRecord(1)));if(in.readInt()!=1)return;String[] loaded=new String[9];for(int i=0;i<9;i++){loaded[i]=in.readUTF();if(loaded[i].length()==0||loaded[i].length()>160)throw new IOException();}for(int i=0;i<9;i++)ownPhrases[i]=loaded[i];}catch(Exception e){}finally{try{if(rs!=null)rs.closeRecordStore();}catch(Exception e){}}}
 boolean storePhrase(int slot,String value){RecordStore rs=null;try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(1);for(int i=0;i<9;i++)out.writeUTF(i==slot?value:ownPhrases[i]);out.close();byte[] data=bytes.toByteArray();rs=RecordStore.openRecordStore("chat-phrases-v1",true);if(rs.getNumRecords()==0)rs.addRecord(data,0,data.length);else rs.setRecord(1,data,0,data.length);ownPhrases[slot]=value;return true;}catch(Exception e){Alert a=new Alert("Не сохранено","Не удалось сохранить фразу. Попробуй ещё раз.",null,AlertType.INFO);a.setTimeout(Alert.FOREVER);Jimm.display.setCurrent(a,phraseEditor);return false;}finally{try{if(rs!=null)rs.closeRecordStore();}catch(Exception e){}}}
 List makeMenu(String title,String[] labels){List list=new List(title,List.IMPLICIT);for(int i=0;i<labels.length;i++)list.append(labels[i],null);list.addCommand(back);list.setCommandListener(this);return list;}
 void showChatMenu(){chatMenu=makeMenu("Сообщение",new String[]{"Написать","Быстрый ответ","Смайлики"});Jimm.display.setCurrent(chatMenu);}
 void showMenu(){actions=makeMenu("Меню",new String[]{"Моя учётка","Новый чат","Приложение"});Jimm.display.setCurrent(actions);}
 void showAccount(){account=new Form("Моя учётка");account.append("Мой UIN: "+Options.getString(Options.OPTION_UIN));account.append(Icq.isConnected()?"Статус: в сети":"Статус: нет соединения");account.append("Сервис: Jimm.online");account.append("Сервер: "+Options.getString(Options.OPTION_SRV_HOST)+":"+Options.getString(Options.OPTION_SRV_PORT));account.append("Пароль подключения сохранён.");account.addCommand(back);account.setCommandListener(this);Jimm.display.setCurrent(account);}
 void showNewChat(){newChatMenu=makeMenu("Новый чат",new String[]{"Из контактов","По UIN"});Jimm.display.setCurrent(newChatMenu);}
 void showAppMenu(){appMenu=makeMenu("Приложение",new String[]{Icq.isConnected()?"Переподключиться":"Подключиться","Справка","Выйти"});Jimm.display.setCurrent(appMenu);}
 void info(String title,String text){Alert a=new Alert(title,text,null,AlertType.INFO);a.setTimeout(Alert.FOREVER);Jimm.display.setCurrent(a,this);}
 void showLicense(){licenseScreen=new Form("GNU GPL v2");InputStream in=null;try{in=getClass().getResourceAsStream("/COPYING");if(in==null)throw new IOException();StringBuffer chunk=new StringBuffer();int c;while((c=in.read())!=-1){chunk.append((char)c);if(chunk.length()>=900&&c=='\n'){licenseScreen.append(chunk.toString());chunk.setLength(0);}}if(chunk.length()>0)licenseScreen.append(chunk.toString());}catch(Exception e){licenseScreen.append("Полный текст: COPYING в комплекте исходников.");}finally{try{if(in!=null)in.close();}catch(Exception e){}}licenseScreen.addCommand(back);licenseScreen.setCommandListener(this);Jimm.display.setCurrent(licenseScreen);}
 public void commandAction(Command c,Displayable d){
  if(d==chatMenu){if(c==back){Jimm.display.setCurrent(this);return;}int choice=chatMenu.getSelectedIndex();compose();if(choice==1)pickQuick(false);else if(choice==2)pickQuick(true);return;}
  if(d==licenseScreen){if(c==back)Jimm.display.setCurrent(helpScreen);return;}
  if(d==helpScreen){if(c==back)Jimm.display.setCurrent(appMenu);else if(c==licenseCommand)showLicense();return;}
  if(d==phraseEditor){if(c==back){Jimm.display.setCurrent(quickPicker);return;}if(c==savePhrase){String value=phraseEditor.getString().trim();if(value.length()==0){Alert a=new Alert("Пустая фраза","Введи текст фразы.",null,AlertType.INFO);a.setTimeout(Alert.FOREVER);Jimm.display.setCurrent(a,phraseEditor);return;}if(storePhrase(quickPicker.selected,value))Jimm.display.setCurrent(quickPicker);}return;}
  if(d==quickPicker){if(c==editPhrase&&!pickingSmiles){phraseEditor=new TextBox("Фраза "+(quickPicker.selected+1),ownPhrases[quickPicker.selected],160,TextField.ANY);phraseEditor.addCommand(savePhrase);phraseEditor.addCommand(back);phraseEditor.setCommandListener(this);Jimm.display.setCurrent(phraseEditor);return;}if(c==back){Jimm.display.setCurrent(editor);return;}int i=quickPicker.getSelectedIndex();String[] choices=pickingSmiles?SMILES:ownPhrases;if(i>=0&&i<choices.length)insertQuick(choices[i]);return;}
  if(d==editor){if(c==phrases){pickQuick(false);return;}if(c==smiles){pickQuick(true);return;}active.draft=editor.getString();save();if(c==back){Jimm.display.setCurrent(this);return;}
   if(c==send){if(active.draft.trim().length()==0)return;if(!Icq.isConnected()){info("Нет соединения","Черновик сохранён. Меню → Приложение → Подключиться. Затем повтори отправку.");return;}
    ContactListContactItem p=ContactList.getItembyUIN(active.uin);if(p==null)p=new ContactListContactItem(0,0,active.uin,active.name,false,true);
    PlainMessage msg=new PlainMessage(Options.getString(Options.OPTION_UIN),p,Message.MESSAGE_TYPE_NORM,Util.createCurrentDate(false),active.draft);
    try{Icq.requestAction(new SendMessageAction(msg));}catch(JimmException e){info("Не отправлено","Черновик сохранён. Проверь соединение.");return;}
    ChatHistory.addMyMessage(active.uin,active.draft,msg.getNewDate(),active.name);active.draft="";save();Jimm.display.setCurrent(this);repaint();return;
   }
  }
  if(d==number){if(c==back){Jimm.display.setCurrent(newChatMenu);return;}String u=number.getString().trim();try{long v=Long.parseLong(u);if(v<10000 || v>2147483647L)throw new Exception();}catch(Exception e){info("Нужен UIN","Введи числовой UIN собеседника (до 2147483647).");return;}openThread(get(u,u));return;}
  if(d==contacts){if(c==back){Jimm.display.setCurrent(newChatMenu);return;}int i=contacts.getSelectedIndex();if(i>=0&&i<people.length)openThread(get(people[i].getStringValue(0),people[i].getStringValue(1)));return;}
  if(d==account){if(c==back)Jimm.display.setCurrent(actions);return;}
  if(d==actions){if(c==back){Jimm.display.setCurrent(this);return;}switch(actions.getSelectedIndex()){case 0:showAccount();break;case 1:showNewChat();break;case 2:showAppMenu();break;}return;}
  if(d==newChatMenu){if(c==back){Jimm.display.setCurrent(actions);return;}if(newChatMenu.getSelectedIndex()==1){number=new TextBox("UIN собеседника","",10,TextField.NUMERIC);number.addCommand(ok);number.addCommand(back);number.setCommandListener(this);Jimm.display.setCurrent(number);}else{people=ContactList.getContactItems();if(people.length==0){Alert a=new Alert("Контактов пока нет","Выбери «По UIN», чтобы начать переписку.",null,AlertType.INFO);a.setTimeout(Alert.FOREVER);Jimm.display.setCurrent(a,newChatMenu);return;}contacts=new List("Контакты",List.IMPLICIT);for(int i=0;i<people.length;i++)contacts.append(people[i].getStringValue(1),null);contacts.addCommand(back);contacts.setCommandListener(this);Jimm.display.setCurrent(contacts);}return;}
  if(d==appMenu){if(c==back){Jimm.display.setCurrent(actions);return;}switch(appMenu.getSelectedIndex()){
   case 0:if(Icq.isConnected())Icq.disconnect();ContactList.beforeConnect();Icq.connect();break;
   case 1:helpScreen=new Form("Jimm Chat 0.1.7");helpScreen.append("На основе Jimm 0.5.1. Copyright (C) 2003-06 Jimm Project. Независимая модификация от 17–18.10.2026. GNU GPL v2 или более поздняя. Разрешены изменение и распространение по GPL. БЕЗ КАКИХ-ЛИБО ГАРАНТИЙ. Полный текст — кнопка «Лицензия», исходники — в комплекте этой сборки.");helpScreen.append("Центр/5 — меню сообщения: написать, фразы, смайлики. 1 — фразы, 3 — смайлики. В списке фраз/смайликов цифры 1–9 вставляют у курсора. «Изменить» сохраняет свою фразу. Вверх/вниз — прокрутка. Моя учётка — первый пункт меню. Хранятся последние 40 сообщений на чат, до 96 КБ. Свой пузырёк означает постановку на отправку, не подтверждение доставки.");if(storageError!=null)helpScreen.append(storageError);helpScreen.addCommand(licenseCommand);helpScreen.addCommand(back);helpScreen.setCommandListener(this);Jimm.display.setCurrent(helpScreen);break;
   case 2:save();try{Jimm.jimm.destroyApp(true);}catch(Exception e){}break;
  }return;}
  if(c==menu){if(active==null)showMenu();else showChatMenu();return;}if(c==back){home();return;}if(c==write||c==chatWrite){compose();return;}if(d==this&&active!=null&&(c==phrases||c==smiles)){compose();pickQuick(c==smiles);return;}if(c==open){if(chats.size()==0)showMenu();else openThread((ThreadData)chats.elementAt(selected));}
 }
 protected void keyPressed(int key){if(active!=null&&(key==KEY_NUM1||key==KEY_NUM3)){compose();pickQuick(key==KEY_NUM3);return;}int a=0;try{a=getGameAction(key);}catch(Exception e){}if(a==FIRE||key==KEY_NUM5){if(active==null)commandAction(open,this);else showChatMenu();return;}if(a==UP||a==DOWN){int dir=a==UP?-1:1;if(active==null){selected=Math.max(0,Math.min(chats.size()-1,selected+dir));}else scroll=Math.max(0,scroll-dir*normal.getHeight()*2);repaint();}}
 protected void keyRepeated(int key){keyPressed(key);}
 String fit(String s,int w){if(s==null)return "";s=s.replace('\n',' ').replace('\r',' ');if(normal.stringWidth(s)<=w)return s;while(s.length()>0&&normal.stringWidth(s+"...")>w)s=s.substring(0,s.length()-1);return s+"...";}
 static Vector wrap(String s,Font f,int width){Vector lines=new Vector();int pos=0;while(pos<s.length()){int end=pos,lastSpace=-1;while(end<s.length()&&s.charAt(end)!='\n'&&f.stringWidth(s.substring(pos,end+1))<=width){if(s.charAt(end)==' ')lastSpace=end;end++;}if(end==pos)end++;else if(end<s.length()&&s.charAt(end)!='\n'&&lastSpace>pos)end=lastSpace;lines.addElement(s.substring(pos,end).replace('\r',' '));pos=end;if(pos<s.length()&&(s.charAt(pos)==' '||s.charAt(pos)=='\n'))pos++;}if(lines.size()==0)lines.addElement("");return lines;}
 String time(long seconds){return Util.getDateString(true,seconds);}
 protected synchronized void paint(Graphics g){int w=getWidth(),h=getHeight(),fh=normal.getHeight(),header=bold.getHeight()+6,top=header+3;g.setColor(0xF3F6FA);g.fillRect(0,0,w,h);g.setFont(normal);
  if(active==null){int row=47,first=selected/Math.max(1,(h-top)/row)*Math.max(1,(h-top)/row);if(chats.size()==0){g.setColor(0x256B93);g.setFont(bold);g.drawString("Здесь будут чаты",12,top+20,0);g.setFont(normal);g.setColor(0x64748B);g.drawString("Меню → Новый чат",12,top+43,0);g.drawString("или выбери контакт",12,top+43+fh+3,0);}
   for(int i=first;i<chats.size();i++){int y=top+(i-first)*row;if(y>=h)break;ThreadData t=(ThreadData)chats.elementAt(i);g.setColor(i==selected?0xDDEEF9:0xFFFFFF);g.fillRect(0,y,w,row-1);g.setColor(0x529BC3);g.fillRoundRect(7,y+8,29,29,14,14);g.setColor(0xFFFFFF);g.setFont(bold);g.drawString(t.name.length()==0?"?":t.name.substring(0,1).toUpperCase(),17,y+13,0);g.setColor(0x172B40);g.drawString(fit(t.name,w-80),43,y+5,0);g.setFont(normal);String preview=t.draft.length()>0?"Черновик: "+t.draft:t.messages.size()==0?"Написать сообщение":((Msg)t.messages.lastElement()).text;g.setColor(t.draft.length()>0?0xA64D21:0x63758A);g.drawString(fit(preview,w-55),43,y+23,0);if(t.unread>0){g.setColor(0x218CCA);g.fillRoundRect(w-30,y+6,25,17,12,12);g.setColor(0xFFFFFF);g.drawString(t.unread>99?"99+":""+t.unread,w-27,y+7,0);}}
  }else {int total=0;Vector wraps=new Vector();for(int i=0;i<active.messages.size();i++){Vector ls=wrap(((Msg)active.messages.elementAt(i)).text,normal,w-38);wraps.addElement(ls);total+=ls.size()*fh+fh+18;}scroll=Math.min(scroll,Math.max(0,total-(h-top-6)));int y=(total>h-top-6?h-6-total:top)+scroll;g.setClip(0,header,w,h-header);for(int i=0;i<active.messages.size();i++){Msg m=(Msg)active.messages.elementAt(i);Vector ls=(Vector)wraps.elementAt(i);int bh=ls.size()*fh+fh+12,x=m.mine?21:6;g.setColor(m.mine?0xD5EDFC:0xFFFFFF);g.fillRoundRect(x,y,w-27,bh,10,10);g.setColor(0x152D42);for(int k=0;k<ls.size();k++)g.drawString((String)ls.elementAt(k),x+6,y+5+k*fh,0);g.setColor(0x607D92);g.drawString(time(m.time),x+w-33,y+bh-fh-1,Graphics.TOP|Graphics.RIGHT);y+=bh+6;}if(active.messages.size()==0){g.setColor(0x64748B);g.drawString("Напиши первое сообщение",8,top+20,0);}g.setClip(0,0,w,h);}
  g.setColor(0x2487B9);g.fillRect(0,0,w,header);g.setColor(0xFFFFFF);g.setFont(bold);String title=active==null?"Чаты":active.name;boolean shortened=false;while(title.length()>0&&bold.stringWidth(title+(shortened?"...":""))>w-32){title=title.substring(0,title.length()-1);shortened=true;}g.drawString(title+(shortened?"...":""),8,3,0);g.setColor(Icq.isConnected()?0x99F5BF:0xFFE0A0);g.fillArc(w-15,(header-7)/2,7,7,0,360);
 }
 int utfSize(String s){int n=2;for(int i=0;i<s.length();i++){char c=s.charAt(i);n+=c>=1&&c<=127?1:c<=2047?2:3;}return n;}
 int archiveSize(){int n=8;for(int i=0;i<chats.size();i++){ThreadData t=(ThreadData)chats.elementAt(i);n+=utfSize(t.uin)+utfSize(t.name)+utfSize(t.draft)+8;for(int j=0;j<t.messages.size();j++){Msg m=(Msg)t.messages.elementAt(j);n+=utfSize(m.text)+9;}}return n;}
 synchronized void save(){RecordStore rs=null;try{
 while(archiveSize()>98304){boolean removed=false;for(int i=chats.size()-1;i>=0;i--){ThreadData t=(ThreadData)chats.elementAt(i);if(t.messages.size()>1){t.messages.removeElementAt(0);removed=true;break;}}if(!removed){storageError="Архив заполнен";return;}}
 ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);out.writeInt(1);out.writeInt(chats.size());for(int i=0;i<chats.size();i++){ThreadData t=(ThreadData)chats.elementAt(i);out.writeUTF(t.uin);out.writeUTF(t.name);out.writeUTF(t.draft);out.writeInt(t.unread);out.writeInt(t.messages.size());for(int j=0;j<t.messages.size();j++){Msg m=(Msg)t.messages.elementAt(j);out.writeUTF(m.text);out.writeBoolean(m.mine);out.writeLong(m.time);}}out.close();byte[] data=b.toByteArray();rs=RecordStore.openRecordStore("chat-ui-v1",true);if(rs.getNumRecords()==0)rs.addRecord(data,0,data.length);else rs.setRecord(1,data,0,data.length);storageError=null;}catch(Exception e){storageError="Не удалось сохранить историю";}finally{try{if(rs!=null)rs.closeRecordStore();}catch(Exception e){}}}
 void load(){RecordStore rs=null;try{rs=RecordStore.openRecordStore("chat-ui-v1",false);DataInputStream in=new DataInputStream(new ByteArrayInputStream(rs.getRecord(1)));if(in.readInt()!=1)return;int count=in.readInt();if(count<0||count>1000)throw new IOException();for(int i=0;i<count;i++){ThreadData t=new ThreadData(in.readUTF(),in.readUTF());t.draft=in.readUTF();t.unread=in.readInt();int n=in.readInt();if(n<0||n>40)throw new IOException();for(int j=0;j<n;j++){String s=in.readUTF();boolean mine=in.readBoolean();long tm=in.readLong();t.messages.addElement(new Msg(s,mine,tm));}chats.addElement(t);}}catch(RecordStoreNotFoundException e){}catch(Exception e){storageError="Не удалось прочитать историю";}finally{try{if(rs!=null)rs.closeRecordStore();}catch(Exception e){}}}
}
