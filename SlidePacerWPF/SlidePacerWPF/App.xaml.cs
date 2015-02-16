using InTheHand.Net.Sockets;
using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data;
using System.Linq;
using System.Threading.Tasks;
using System.Windows;

namespace SlidePacerWPF
{
    /// <summary>
    /// Interaction logic for App.xaml
    /// </summary>
    public partial class App : Application
    {
        // GLOBAL Variables

        // bluetooth connection with interpreter
        public static BluetoothClient mBTClient = null;

        // bluetooth stram with interperter
        public static System.IO.Stream mClientStream = null;

        // receivers when message comes from interpreter
        public static event MessageReceived OnMessageFromInterpreter;
        public delegate void MessageReceived(String message);

        // time to look at the slide
        public static double mTimeToLook = 2000;

        // time to view the slide
        public static double mTimeToView = 3000;

        public static void forwardMessageFromInterpreter(String message)
        {
            OnMessageFromInterpreter(message);
        }
    }
}
