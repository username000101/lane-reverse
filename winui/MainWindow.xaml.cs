using Microsoft.UI.Xaml;
using Windows.Graphics;

namespace LaneWinUI;

public sealed partial class MainWindow : Window
{
    public MainWindow()
    {
        InitializeComponent();

        ExtendsContentIntoTitleBar = true;
        SetTitleBar(AppTitleBar);

        // Center and set standard desktop window dimensions
        AppWindow.Resize(new SizeInt32(1180, 780));

        RootFrame.Navigate(typeof(MainPage));
    }
}
