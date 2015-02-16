using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Microsoft.Office.Tools.Ribbon;

namespace SlidePacerAddIn
{
    public partial class SlidePacerRibbon
    {
        private void Ribbon1_Load(object sender, RibbonUIEventArgs e)
        {

        }

        private void checkBox1_Click(object sender, RibbonControlEventArgs e)
        {
            Globals.ThisAddIn.mEnabled = cbEnabled.Checked;
        }
    }
}
