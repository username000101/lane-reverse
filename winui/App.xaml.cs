using Microsoft.UI.Xaml;
using LaneWinUI.Core;
using LaneWinUI.Services;

namespace LaneWinUI;

public partial class App : Application
{
    private Window? _window;

    public static LaneApiClient ApiClient { get; } = new();
    public static PlayerService Player { get; } = new(ApiClient);
    public static AuthSession? CurrentSession { get; set; }

    public App()
    {
        InitializeComponent();
    }

    protected override void OnLaunched(LaunchActivatedEventArgs args)
    {
        _window = new MainWindow();
        _window.Activate();
    }
}
