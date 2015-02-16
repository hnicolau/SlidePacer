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

namespace SlidePacerWPF.Pages.Settings
{
    /// <summary>
    /// Interaction logic for Timings.xaml
    /// </summary>
    public partial class Timings : UserControl
    {
        public Timings()
        {
            InitializeComponent();
        }

        private void sliderTimeToLook_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            App.mTimeToLook = (e.NewValue == 0 ? 0.1 : e.NewValue) * 1000;
        }

        private void sliderTimeToView_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            App.mTimeToView = (e.NewValue == 0 ? 0.1 : e.NewValue) * 1000;
        }
    }
}
