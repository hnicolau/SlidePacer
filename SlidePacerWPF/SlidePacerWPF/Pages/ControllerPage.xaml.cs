using FirstFloor.ModernUI.Windows.Controls;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Timers;
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
    /// Interaction logic for PresentationController.xaml
    /// </summary>
    public partial class ControllerPage : UserControl
    {
        // powerpoint application object
        Microsoft.Office.Interop.PowerPoint.Application mApplication;

        // powerpoint presentation object 
        Microsoft.Office.Interop.PowerPoint.Presentation mPresentation;

        // progress bar status
        private enum PROGRESS_STATUS { WAITING_MESSAGE, WAITING_GAZE_TO_SLIDE, WAITING_VIEW_SLIDE, READY };

        // timers
        private Timer mTimerToChangeSlide = new Timer();
        private Timer mTimerToStartSpeaking = new Timer();

        // last command
        private String mLastCommand = "";

        public ControllerPage()
        {
            InitializeComponent();
        }

        private void btnNext_Click(object sender, RoutedEventArgs e)
        {
            if (App.mBTClient == null)
            {
                // there is no connection with the interpreter
                slideshowNextSlide();
            }
            else 
            { 
                // there is a connection with the interpreter
                // send message
                sendMessage("next");

                // disable controls
                updateButtons(false);

                // update progress
                updateProgress(PROGRESS_STATUS.WAITING_MESSAGE); 
            }
        }

        private void btnPrevious_Click(object sender, RoutedEventArgs e)
        {
            if (App.mBTClient == null)
            {
                // there is no connection with the interpreter
                slideshowPreviousSlide();
            }
            else
            {
                // there is a connection with the interpreter
                sendMessage("previous");
                updateButtons(false);

                // update progress
                updateProgress(PROGRESS_STATUS.WAITING_MESSAGE); 
            }
        }  

        private void UserControl_Loaded(object sender, RoutedEventArgs e)
        {

            if (!checkPowerPointApplication())
            {
                Application.Current.Shutdown();
            }

            // set listener for application close
            Application.Current.MainWindow.Closing += MainWindow_Closing;

            // configure timers
            mTimerToChangeSlide = new Timer();
            mTimerToChangeSlide.Interval = App.mTimeToLook;
            mTimerToChangeSlide.Elapsed += mTimerToChangeSlide_Elapsed;
            mTimerToStartSpeaking = new Timer();
            mTimerToStartSpeaking.Interval = App.mTimeToView;
            mTimerToStartSpeaking.Elapsed += mTimerToStartSpeaking_Elapsed;

            // update progress bar
            updateProgress(PROGRESS_STATUS.READY);

            // register listener for messages from interpreter's device
            App.OnMessageFromInterpreter += App_OnMessageFromInterpreter;
        }

        void MainWindow_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            App.OnMessageFromInterpreter -= App_OnMessageFromInterpreter;
        }

        void mTimerToStartSpeaking_Elapsed(object sender, ElapsedEventArgs e)
        {
            // stops timer
            mTimerToStartSpeaking.Enabled = false;

            // update UI
            updateButtons(true);
            updateProgress(PROGRESS_STATUS.READY);
        }

        // wait for the student to look at the slide
        private void mTimerToChangeSlide_Elapsed(object sender, ElapsedEventArgs e)
        {
            // stop timer
            mTimerToChangeSlide.Enabled = false;

            // perform action
            if (mLastCommand.Equals("next"))
            {
                slideshowNextSlide();
            }
            else if (mLastCommand.Equals("previous"))
            {
                slideshowPreviousSlide();
            }

            updateProgress(PROGRESS_STATUS.WAITING_VIEW_SLIDE);

            // wait for the student to view the next/previous slide
            mTimerToStartSpeaking.Enabled = true;
        }

        void App_OnMessageFromInterpreter(string message)
        {
            if (message.Equals("next") || message.Equals("previous"))
            {
                // if it is a valid command, save it
                mLastCommand = message;

                // update progress bar
                updateProgress(PROGRESS_STATUS.WAITING_GAZE_TO_SLIDE);

                // start timer to change slide
                mTimerToChangeSlide.Enabled = true;
            }
        }

        private bool sendMessage(string message)
        {
            if (App.mBTClient != null && App.mClientStream != null)
            {
                try
                {
                    byte[] messageBytes = GetBytes(message);
                    App.mClientStream.Write(messageBytes, 0, messageBytes.Length);
                }
                catch
                {
                    ModernDialog.ShowMessage("Error while communicating with Interpreter's app", "Communication Error", MessageBoxButton.OK);
                    return false;
                }
                return true;
            }
            return false;
        }

        private static byte[] GetBytes(string str)
        {
            byte[] bytes = new byte[str.Length * sizeof(char)];
            System.Buffer.BlockCopy(str.ToCharArray(), 0, bytes, 0, bytes.Length);
            return bytes;
        }

        private bool checkPowerPointApplication()
        {
            try
            {
                // TODO: limitation - only allow one presentation opened at a time
                // get running slideshow powerpoint application object 
                mApplication = System.Runtime.InteropServices.Marshal.GetActiveObject("PowerPoint.Application") as Microsoft.Office.Interop.PowerPoint.Application;
            }
            catch
            {
                String message = "Please run application within PowerPoint add-in. Also, make sure you only have one presentation opened";
                String title = "PowerPoint is closed";
                ModernDialog.ShowMessage(message, title, MessageBoxButton.OK);
                return false;
            }

            mPresentation = mApplication.ActivePresentation;
            return true;
        }

        private void updateButtons(bool isEnabled)
        {
            Application.Current.Dispatcher.Invoke((Action)delegate
            {
                btnNext.IsEnabled = isEnabled;
                btnPrevious.IsEnabled = isEnabled;
            });
        }

        private void updateProgress(PROGRESS_STATUS status)
        {
            Application.Current.Dispatcher.Invoke((Action)delegate
            {
                switch (status)
                {
                    case PROGRESS_STATUS.READY:
                        progressStatus.IsActive = false;
                        break;
                    case PROGRESS_STATUS.WAITING_MESSAGE:
                        progressStatus.Foreground = Brushes.Red;
                        progressStatus.IsActive = true;
                        break;
                    case PROGRESS_STATUS.WAITING_GAZE_TO_SLIDE:
                        progressStatus.Foreground = Brushes.OrangeRed;
                        progressStatus.IsActive = true;
                        break;
                    case PROGRESS_STATUS.WAITING_VIEW_SLIDE:
                        progressStatus.Foreground = Brushes.Orange;
                        progressStatus.IsActive = true;
                        break;
                }
            });
        }

        private void slideshowNextSlide()
        {
            mPresentation.SlideShowWindow.View.Next();
        }

        private void slideshowPreviousSlide()
        {
            mPresentation.SlideShowWindow.View.Previous();
        }
    }
}
