# Jimm Chat Setup for Windows

A portable Windows Forms companion (.NET Framework 4.8 recommended on Windows 10/11). Java and Python are **not needed by end users**. No administrator rights or driver changes are required by the application. The EXE is not code-signed.

1. Pair the phone using Windows Bluetooth settings.
2. Run `JimmChatSetup.exe`, enter your UIN/password. Server `icq.jimm.online` and port `5190` are prefilled and editable.
3. Refresh the list and explicitly select your paired phone (name and address).
4. Close Jimm on the phone, enable discoverability, click Create and transfer. Accept the file and Java installation on the handset.

The application does not register accounts. Obtain yours at https://jimm.online/ . It does not automatically pair phones or use a fixed PIN. Classic Bluetooth Object Push is required; BLE-only devices are not supported. A final OBEX acknowledgement confirms file reception, **not installation**. Transfer cancellation closes the connection; already received handset bytes cannot be recalled.

The personalized JAR is generated in memory during transfer. Passwords are not logged or saved to preferences; they remain extractable from the JAR received by the phone. Explicit export saves a private ZIP with JAR, corrected-size JAD, GPL and corresponding source, including the same `resources/personal.dat`. Do not publish this ZIP. Existing phone RMS settings override installer defaults: edit My account to change an installed account without deleting its history.

## Build and verify

From PowerShell with JDK 17 and Python 3.10+ available:

```powershell
./windows/build.ps1
./windows/out/JimmChatSetup.exe --self-test ./windows/out/self-test
```

Use `-Offline` when the pinned Java build dependencies are cached. The build uses the Windows .NET Framework C# compiler and embeds a freshly built **public** JAR and corresponding source. It refuses private defaults/configuration. Output is `windows/out/JimmChatSetup.exe`.

Self-tests use synthetic credentials and do not send files. They check ZIP content preservation, validation, source inclusion, JAD size, and construct/render the form. Hardware Bluetooth/installation testing is a separate step.

Configuration format: `personal.dat` starts with big-endian `0x4A434631`, then four Java `DataInputStream.readUTF` strings: UIN, password, host, port. The Java reader validates the complete record before applying it; existing saved settings load afterwards.

GPL-2.0-or-later. See `../COPYING`. The Windows companion and resource reader were created/adapted on 2026-10-18. Microsoft API references: [BluetoothFindFirstDevice](https://learn.microsoft.com/windows/win32/api/bluetoothapis/nf-bluetoothapis-bluetoothfindfirstdevice), [ZipArchiveEntry](https://learn.microsoft.com/dotnet/api/system.io.compression.ziparchiveentry).
