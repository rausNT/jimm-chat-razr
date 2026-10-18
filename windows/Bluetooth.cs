// Adapted 2026-10-18. GPL-2.0-or-later.
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
public static class RazrBluetoothInventory {
    [StructLayout(LayoutKind.Sequential)]
    public struct SystemTime {
        public ushort Year, Month, DayOfWeek, Day, Hour, Minute, Second, Millisecond;
        public override string ToString() {
            return String.Format("{0:D4}-{1:D2}-{2:D2} {3:D2}:{4:D2}:{5:D2}", Year, Month, Day, Hour, Minute, Second);
        }
    }
    [StructLayout(LayoutKind.Sequential, CharSet=CharSet.Unicode)]
    public struct Device {
        public override string ToString(){return Name+"  ["+Address.ToString("X12")+"]";}
        public uint Size;
        public ulong Address;
        public uint Class;
        [MarshalAs(UnmanagedType.Bool)] public bool Connected;
        [MarshalAs(UnmanagedType.Bool)] public bool Remembered;
        [MarshalAs(UnmanagedType.Bool)] public bool Authenticated;
        public SystemTime LastSeen, LastUsed;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst=248)] public string Name;
    }
    [StructLayout(LayoutKind.Sequential)]
    public struct Search {
        public uint Size;
        [MarshalAs(UnmanagedType.Bool)] public bool Authenticated;
        [MarshalAs(UnmanagedType.Bool)] public bool Remembered;
        [MarshalAs(UnmanagedType.Bool)] public bool Unknown;
        [MarshalAs(UnmanagedType.Bool)] public bool Connected;
        [MarshalAs(UnmanagedType.Bool)] public bool Inquiry;
        public byte Timeout;
        public IntPtr Radio;
    }
    [DllImport("bthprops.cpl",SetLastError=true)]
    static extern IntPtr BluetoothFindFirstDevice(ref Search search, ref Device device);
    [DllImport("bthprops.cpl",SetLastError=true)]
    [return:MarshalAs(UnmanagedType.Bool)]
    static extern bool BluetoothFindNextDevice(IntPtr handle, ref Device device);
    [DllImport("bthprops.cpl")]
    [return:MarshalAs(UnmanagedType.Bool)]
    static extern bool BluetoothFindDeviceClose(IntPtr handle);
    public static Device[] Enumerate(bool inquiry) {
        var search = new Search { Size=(uint)Marshal.SizeOf(typeof(Search)), Authenticated=true,
            Remembered=true, Unknown=true, Connected=true, Inquiry=inquiry, Timeout=8 };
        var device = new Device { Size=(uint)Marshal.SizeOf(typeof(Device)) };
        var result = new List<Device>();
        var handle = BluetoothFindFirstDevice(ref search, ref device);
        if(handle==IntPtr.Zero) {
            int error=Marshal.GetLastWin32Error();
            if(error==259) return result.ToArray();
            throw new System.ComponentModel.Win32Exception(error);
        }
        try {
            do { result.Add(device); device.Size=(uint)Marshal.SizeOf(typeof(Device)); }
            while(BluetoothFindNextDevice(handle,ref device));
        } finally { BluetoothFindDeviceClose(handle); }
        return result.ToArray();
    }
}