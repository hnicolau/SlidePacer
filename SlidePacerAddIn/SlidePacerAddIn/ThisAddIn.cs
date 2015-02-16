using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Xml.Linq;
using PowerPoint = Microsoft.Office.Interop.PowerPoint;
using Office = Microsoft.Office.Core;
using System.Diagnostics;

namespace SlidePacerAddIn
{

    public partial class ThisAddIn
    {
        public bool mEnabled = false;
        private Process mSlidePacer = null;

        private void ThisAddIn_Startup(object sender, System.EventArgs e)
        {
            this.Application.SlideShowBegin += Application_SlideShowBegin;
            this.Application.SlideShowEnd += Application_SlideShowEnd;
        }

        private void ThisAddIn_Shutdown(object sender, System.EventArgs e)
        {
        }

        private void Application_SlideShowEnd(PowerPoint.Presentation Pres)
        {
            Debug.WriteLine("Slideshow End");
            if (mSlidePacer != null)
            {
                try
                {
                    // closes external slideshow controller
                    mSlidePacer.CloseMainWindow();
                    mSlidePacer.Close();
                    mSlidePacer = null;
                }
                catch
                {
                    // Slide Pacer application was already closed
                }
            }
        }

        private void Application_SlideShowBegin(PowerPoint.SlideShowWindow Wn)
        {
            Debug.WriteLine("Slideshow Begin");

            // lauch external slideshow controller
            if (mEnabled)
            {
                mSlidePacer = Process.Start("C:\\WinProjects\\SlidePacerWPF\\SlidePacerWPF\\bin\\Debug\\SlidePacerWPF.exe");
            }
        }


        #region VSTO generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InternalStartup()
        {
            this.Startup += new System.EventHandler(ThisAddIn_Startup);
            this.Shutdown += new System.EventHandler(ThisAddIn_Shutdown);
        }
        
        #endregion
    }
}
