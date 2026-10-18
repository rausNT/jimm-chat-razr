// Created 2026-10-18. GPL-2.0-or-later; see ../COPYING.
using System;
using System.IO;
using System.IO.Compression;
using System.Drawing;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

public sealed class InstallerForm:Form {
    TextBox uin=new TextBox(),password=new TextBox(),host=new TextBox(),port=new TextBox();
    ComboBox devices=new ComboBox();Button refresh=new Button(),send=new Button(),export=new Button(),cancel=new Button();
    Label status=new Label();ProgressBar progress=new ProgressBar();CancellationTokenSource pending;
    TableLayoutPanel layout=new TableLayoutPanel();bool busy;
    public InstallerForm():this(true){}
    public InstallerForm(bool discover){
        Text="Jimm Chat • Настройка Motorola";ClientSize=new Size(600,620);MinimumSize=new Size(570,650);StartPosition=FormStartPosition.CenterScreen;Font=new Font("Segoe UI",10);AutoScaleMode=AutoScaleMode.Dpi;
        layout.Dock=DockStyle.Fill;layout.Padding=new Padding(22);layout.ColumnCount=2;layout.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute,110));layout.ColumnStyles.Add(new ColumnStyle(SizeType.Percent,100));layout.AutoScroll=true;Controls.Add(layout);
        Wide("Jimm Chat для Motorola RAZR • "+Updates.Current,true);
        var updateLink=new LinkLabel{Text="Проверяем обновления…",AutoSize=true,Enabled=false};AddWide(updateLink);
        updateLink.LinkClicked+=delegate{System.Diagnostics.Process.Start("https://github.com/rausNT/jimm-chat-razr/releases");};
        if(discover)Shown+=async delegate{try{string newer=await Task.Run(()=>Updates.Check());updateLink.Text=newer==null?"Версия актуальна · GitHub":"Доступна версия "+newer+" · открыть GitHub (включая preview)";}catch{updateLink.Text="Обновления недоступны · встроенный JAR готов к работе";}updateLink.Enabled=true;};
        Wide("1. Данные подключения",true);
        Field("UIN",uin);Field("Пароль",password);password.UseSystemPasswordChar=true;password.MaxLength=128;uin.MaxLength=10;
        host.Text="icq.jimm.online";port.Text="5190";host.MaxLength=128;port.MaxLength=5;Field("Сервер",host);Field("Порт",port);
        Wide("2. Телефон",true);devices.DropDownStyle=ComboBoxStyle.DropDownList;Field("Bluetooth",devices);
        var buttons=new FlowLayoutPanel{AutoSize=true,Dock=DockStyle.Fill};refresh.Text="Обновить список";refresh.AutoSize=true;buttons.Controls.Add(refresh);
        var pairing=new Button{Text="Сопряжение в Windows",AutoSize=true};buttons.Controls.Add(pairing);AddWide(buttons);
        Wide("Сначала сопряги телефон в Windows. Закрой Jimm на телефоне и включи «Найти меня».",false);
        Wide("3. Передача",true);send.Text="Создать и передать";export.Text="Сохранить комплект…";cancel.Text="Отмена";send.AutoSize=export.AutoSize=cancel.AutoSize=true;cancel.Enabled=false;
        var actions=new FlowLayoutPanel{AutoSize=true,Dock=DockStyle.Fill};actions.Controls.Add(send);actions.Controls.Add(export);actions.Controls.Add(cancel);AddWide(actions);
        progress.Dock=DockStyle.Fill;AddWide(progress);status.Text="Введи данные и выбери телефон. Отправка начнётся только по кнопке.";status.AutoSize=true;status.MaximumSize=new Size(520,0);AddWide(status);
        Wide("Пароль можно извлечь из персонального JAR. Не публикуй его. При передаче файл создаётся только в памяти. Сохранённые на телефоне настройки имеют приоритет.",false);
        refresh.Click+=async delegate{await RefreshPhones();};pairing.Click+=delegate{System.Diagnostics.Process.Start("ms-settings:bluetooth");};
        send.Click+=async delegate{await Send();};export.Click+=delegate{Export();};cancel.Click+=delegate{if(pending!=null)pending.Cancel();};
        FormClosing+=delegate(object sender,FormClosingEventArgs e){if(busy){if(pending!=null)pending.Cancel();e.Cancel=true;status.Text="Останавливаем операцию. Затем окно можно закрыть.";}};
        if(discover)Shown+=async delegate{await RefreshPhones();};
    }
    void AddWide(Control c){int r=layout.RowCount++;layout.Controls.Add(c,0,r);layout.SetColumnSpan(c,2);c.Margin=new Padding(0,5,0,5);}
    void Wide(string text,bool bold){var l=new Label{Text=text,AutoSize=true,MaximumSize=new Size(520,0)};if(bold)l.Font=new Font(Font,FontStyle.Bold);AddWide(l);}
    void Field(string label,Control control){int r=layout.RowCount++;layout.Controls.Add(new Label{Text=label,AutoSize=true,Anchor=AnchorStyles.Left},0,r);control.Dock=DockStyle.Fill;control.Margin=new Padding(0,4,0,4);layout.Controls.Add(control,1,r);}
    void Busy(bool value){busy=value;refresh.Enabled=send.Enabled=export.Enabled=devices.Enabled=uin.Enabled=password.Enabled=host.Enabled=port.Enabled=!value;cancel.Enabled=value&&pending!=null;}
    Settings ReadSettings(){var s=new Settings(uin.Text,password.Text,host.Text,port.Text);s.Validate();return s;}
    async Task RefreshPhones(){
        Busy(true);status.Text="Читаем сопряжённые Bluetooth-устройства…";
        try{var list=await Task.Run(()=>RazrBluetoothInventory.Enumerate(false));devices.Items.Clear();foreach(var d in list)if(d.Authenticated)devices.Items.Add(d);devices.SelectedIndex=-1;status.Text=devices.Items.Count==0?"Сопряжённых устройств нет. Открой «Сопряжение в Windows», затем обнови список.":"Выбери свой телефон из списка.";}
        catch(Exception e){status.Text="Не удалось прочитать Bluetooth: "+e.Message;}finally{Busy(false);}
    }
    async Task Send(){
        byte[] jar=null;
        try{
            var settings=ReadSettings();if(devices.SelectedIndex<0)throw new ArgumentException("Выбери телефон.");var device=(RazrBluetoothInventory.Device)devices.SelectedItem;
            jar=Personalizer.Build(settings);pending=new CancellationTokenSource();Busy(true);progress.Value=0;status.Text="Передача на "+device.Name+". Подтверди приём файла на телефоне.";
            var updates=new Progress<int>(n=>progress.Value=n);var token=pending.Token;
            await Task.Run(()=>RazrBluetoothObex.Send(device.Address,jar,s=>{},n=>((IProgress<int>)updates).Report(n),token));
            progress.Value=100;status.Text="Файл полностью принят телефоном. Подтверди установку и сохранение данных. Это ещё не подтверждение установки.";password.Clear();
        }catch(Exception e){status.Text=pending!=null&&pending.IsCancellationRequested?"Передача отменена. Можно повторить.":"Не передано: "+e.Message+" Закрой Jimm, включи обнаружение и повтори.";}
        finally{if(jar!=null)Array.Clear(jar,0,jar.Length);if(pending!=null){pending.Dispose();pending=null;}Busy(false);}
    }
    void Export(){try{var settings=ReadSettings();using(var dialog=new SaveFileDialog{Filter="Персональный комплект (*.zip)|*.zip",FileName="JimmChat-personal.zip",OverwritePrompt=true}){if(dialog.ShowDialog()!=DialogResult.OK)return;byte[] bytes=Personalizer.Bundle(settings);try{File.WriteAllBytes(dialog.FileName,bytes);}finally{Array.Clear(bytes,0,bytes.Length);}status.Text="Комплект сохранён: JAR, JAD, лицензия и соответствующие исходники. Он содержит пароль — храни его приватно.";}}catch(Exception e){status.Text="Не сохранено: "+e.Message;}}
}
static class Program {
    [STAThread]static int Main(string[] args){
        if(args.Length>0&&args[0]=="--self-test")return SelfTest.Run(args.Length>1?args[1]:"self-test");
        Application.EnableVisualStyles();Application.SetCompatibleTextRenderingDefault(false);Application.Run(new InstallerForm());return 0;
    }
}
