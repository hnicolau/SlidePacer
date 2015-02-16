using FirstFloor.ModernUI.Windows.Controls;
using InTheHand.Net;
using InTheHand.Net.Bluetooth;
using InTheHand.Net.Sockets;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;

namespace SlidePacerWPF.Pages
{
    /// <summary>
    /// Interaction logic for Home.xaml
    /// </summary>
    public partial class ConnectionPage : UserControl
    {
        // BT client
        private Guid SERVICE = new Guid("{05f2934c-1e81-4554-bb08-44aa761afbfb}");
        
        
        private bool mIsListening = true;
        private System.Threading.Thread mListeningThread = null;
        private System.Threading.Thread mAttemptConnection = null;
        private String mLastReceivedCommand = "";

        public ConnectionPage()
        {
            InitializeComponent();
        }

        private void btnConnect_Click(object sender, RoutedEventArgs e)
        {
             if (btnConnect.Content.Equals("Connect"))
            {
                // connect without blocking UI
                //mAttemptConnection = new System.Threading.Thread(new System.Threading.ThreadStart(attemptConnection));
                //mAttemptConnection.Start();

                btnConnect.IsEnabled = false;

                // create BT client
                App.mBTClient = new BluetoothClient();
                String address = tbAddress.Text;

                try
                {
                    App.mBTClient.Connect((new BluetoothEndPoint(BluetoothAddress.Parse(formatAddress(address)), SERVICE)));
                }
                catch (Exception ex)
                {
                    ModernDialog.ShowMessage("Could not connect to Interpreter's app. " + ex.Message, "Connection Error", MessageBoxButton.OK);
                    btnConnect.IsEnabled = true;
                    App.mBTClient = null;
                    return;
                }

                // store stream
                App.mClientStream = App.mBTClient.GetStream();

                // monitor stream
                mIsListening = true;
                mListeningThread = new System.Threading.Thread(new System.Threading.ThreadStart(ListenLoop));
                mListeningThread.Start();

                tbAddress.IsEnabled = false;     
                btnConnect.IsEnabled = true;
                btnConnect.Content = "Disconnect";
            }
            else
            {
                // disconnect
                disconnect();
            }
        }

        private void UserControl_Loaded(object sender, RoutedEventArgs e)
        {
            // set listener for application close
            Application.Current.MainWindow.Closing += MainWindow_Closing;

            // configure BT
            // turn on bt radio
            BluetoothRadio radio = BluetoothRadio.PrimaryRadio;
            if (radio == null)
            {
                ModernDialog.ShowMessage("No supported Bluetooth radio/stack found", "Bluetooth", MessageBoxButton.OK);
                Application.Current.Shutdown();
            }
            else if (radio != null && radio.Mode == RadioMode.PowerOff)
            {
                BluetoothRadio.PrimaryRadio.Mode = RadioMode.Connectable;
            }
        }

        void MainWindow_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            mIsListening = false;
            if (mListeningThread != null)
            {
                mListeningThread.Abort();
                mListeningThread = null;
            }

            if (App.mClientStream != null)
            {
                App.mClientStream.Close();
                App.mClientStream = null;
            }

            if (App.mBTClient != null)
            {
                App.mBTClient.Close();
                App.mBTClient = null;
            }
        }

        public void disconnect()
        {
            mIsListening = false;
            if (mListeningThread != null)
            {
                mListeningThread.Abort();
                mListeningThread = null;
            }

            if (App.mClientStream != null)
            {
                App.mClientStream.Close();
                App.mClientStream = null;
            }

            if (App.mBTClient != null)
            {
                App.mBTClient.Close();
                App.mBTClient = null;
            }

            tbAddress.IsEnabled = true;
            btnConnect.Content = "Connect";
        }

        private String formatAddress(String address)
        {
            String ret = "";

            foreach (char c in address)
            {
                if (c != ':')
                    ret += c;
            }
            return ret;
        }
        

        private void ListenLoop()
        {
            byte[] buffer = new byte[1024];
            int received = 0;

            //keep connection open
            while (mIsListening)
            {
                try
                {
                    received = App.mClientStream.Read(buffer, 0, 1024);
                }
                catch
                {
                    //MessageBox.Show(ex.Message);
                    try
                    {
                        disconnect();
                    }
                    catch
                    { }

                    return;
                }

                if (received > 0)
                {
                    string command = GetString(buffer);

                    // process command
                    //MessageBox.Show("Message received: " + command + " lenght: " + command.Length);

                    if (command.Equals("next") || command.Equals("previous"))
                    {
                        mLastReceivedCommand = command;

                        // comunicates with controller
                        App.forwardMessageFromInterpreter(command);
                    }

                    buffer = new byte[1024];
                }
                else
                {
                    //connection lost
                    MessageBox.Show("connection lost");
                    try
                    {
                        disconnect();
                    }
                    catch
                    { }
                    return;
                }
            }
        }    

        private static string GetString(byte[] bytes)
        {
            char[] chars = new char[bytes.Length / sizeof(char)];
            System.Buffer.BlockCopy(bytes, 0, chars, 0, bytes.Length);
            string ret = new string(chars);
            int i = ret.IndexOf('\0');
            ret = ret.Remove(i);
            return ret;
        }
    }
}
