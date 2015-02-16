using FirstFloor.ModernUI.Presentation;
using FirstFloor.ModernUI.Windows.Controls;
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

namespace SlidePacerWPF
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : ModernWindow
    {

        public MainWindow()
        {
            InitializeComponent();
        }


        private void ModernWindow_Initialized(object sender, EventArgs e)
        {
            AppearanceManager.Current.FontSize = FirstFloor.ModernUI.Presentation.FontSize.Small;
        }

        private void ModernWindow_Loaded(object sender, RoutedEventArgs e)
        {
           
        }
    }
}
