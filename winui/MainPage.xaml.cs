using System.Collections.ObjectModel;
using Windows.ApplicationModel.DataTransfer;
using Windows.Storage.Streams;
using Windows.System;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media.Imaging;
using QRCoder;
using LaneWinUI.Core;

namespace LaneWinUI;

public sealed partial class MainPage : Page
{
    private string _authDeviceId = string.Empty;
    private DispatcherTimer? _authPollTimer;
    private readonly ObservableCollection<LanePlaylist> _playlists = new();
    private readonly ObservableCollection<LaneTrack> _displayedTracks = new();
    private List<LaneTrack> _allTracks = new();
    private LanePlaylist? _selectedPlaylist;
    private bool _isScrubbing = false;

    public MainPage()
    {
        InitializeComponent();

        PlaylistsListView.ItemsSource = _playlists;
        TracksListView.ItemsSource = _displayedTracks;

        // Subscribe to PlayerService events
        App.Player.PlaybackStateChanged += Player_PlaybackStateChanged;
        App.Player.PositionChanged += Player_PositionChanged;
        App.Player.TrackChanged += Player_TrackChanged;

        Loaded += MainPage_Loaded;
    }

    private async void MainPage_Loaded(object sender, RoutedEventArgs e)
    {
        var session = SessionStorage.Load();
        if (session != null && !string.IsNullOrEmpty(session.Token))
        {
            App.CurrentSession = session;
            App.ApiClient.Token = session.Token;
            App.ApiClient.DeviceId = session.DeviceId;

            ShowAppView();
            await LoadUserProfileAndPlaylistsAsync();
        }
        else
        {
            ShowAuthView();
            StartAuthFlow();
        }
    }

    #region Authentication Flow

    private void ShowAuthView()
    {
        AuthContainer.Visibility = Visibility.Visible;
        AppContainer.Visibility = Visibility.Collapsed;
    }

    private void ShowAppView()
    {
        AuthContainer.Visibility = Visibility.Collapsed;
        AppContainer.Visibility = Visibility.Visible;
    }

    private async void StartAuthFlow()
    {
        _authDeviceId = Bnit.RandomDeviceId();
        DeviceIdText.Text = $"Устройство: {_authDeviceId}";

        string botUrl = $"https://t.me/lane_music_bot?start=auth{_authDeviceId}";

        // Generate high-resolution QR code PNG in memory
        try
        {
            using var generator = new QRCodeGenerator();
            using var data = generator.CreateQrCode(botUrl, QRCodeGenerator.ECCLevel.Q);
            var qr = new PngByteQRCode(data);
            byte[] bytes = qr.GetGraphic(20);

            var stream = new InMemoryRandomAccessStream();
            using (var writer = new DataWriter(stream.GetOutputStreamAt(0)))
            {
                writer.WriteBytes(bytes);
                await writer.StoreAsync();
            }

            var bmp = new BitmapImage();
            await bmp.SetSourceAsync(stream);
            QrCodeImage.Source = bmp;
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"QR generation error: {ex.Message}");
        }

        // Start polling loop
        _authPollTimer?.Stop();
        _authPollTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(2) };
        _authPollTimer.Tick += AuthPollTimer_Tick;
        _authPollTimer.Start();
    }

    private async void AuthPollTimer_Tick(object? sender, object e)
    {
        var (success, token, user) = await App.ApiClient.PollAuthTokenAsync(_authDeviceId);
        if (success && !string.IsNullOrEmpty(token))
        {
            _authPollTimer?.Stop();

            var session = new AuthSession
            {
                Token = token,
                DeviceId = _authDeviceId,
                User = user
            };

            SessionStorage.Save(session);
            App.CurrentSession = session;

            ShowAppView();
            await LoadUserProfileAndPlaylistsAsync();
        }
    }

    private async void OpenTelegramButton_Click(object sender, RoutedEventArgs e)
    {
        if (string.IsNullOrEmpty(_authDeviceId)) return;
        string botUrl = $"https://t.me/lane_music_bot?start=auth{_authDeviceId}";
        await Launcher.LaunchUriAsync(new Uri(botUrl));
    }

    #endregion

    #region Library & Playlists

    private async Task LoadUserProfileAndPlaylistsAsync()
    {
        TracksLoadingRing.IsActive = true;

        try
        {
            var user = await App.ApiClient.GetProfileAsync();
            if (user != null)
            {
                UserNameText.Text = user.DisplayName;
                UserVipText.Text = user.IsVip ? "LANE VIP" : "FREE";
            }
            else if (App.CurrentSession?.User != null)
            {
                UserNameText.Text = App.CurrentSession.User.DisplayName;
            }

            var playlists = await App.ApiClient.GetPlaylistsAsync();
            _playlists.Clear();

            LanePlaylist? likesPlaylist = null;
            foreach (var pl in playlists)
            {
                if (pl.RealId == "lane_likes" || pl.RealTitle.Contains("Нравится", StringComparison.OrdinalIgnoreCase))
                {
                    likesPlaylist = pl;
                }
                else
                {
                    _playlists.Add(pl);
                }
            }

            if (likesPlaylist != null)
            {
                FavCountText.Text = likesPlaylist.TotalTracks.ToString();
            }

            // Load favorites by default
            await LoadPlaylistTracksAsync(likesPlaylist?.RealId ?? "lane_likes", "Мне нравится");
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"Failed to load playlists: {ex.Message}");
        }
        finally
        {
            TracksLoadingRing.IsActive = false;
        }
    }

    private async Task LoadPlaylistTracksAsync(string playlistId, string playlistTitle)
    {
        TracksLoadingRing.IsActive = true;
        CurrentPlaylistTitle.Text = playlistTitle;

        try
        {
            var tracks = await App.ApiClient.GetPlaylistTracksAsync(playlistId);
            _allTracks = tracks;

            _displayedTracks.Clear();
            foreach (var t in tracks) _displayedTracks.Add(t);

            CurrentPlaylistSubtitle.Text = $"{tracks.Count} треков • Стриминг {App.Player.Quality}";
            if (playlistId == "lane_likes")
            {
                FavCountText.Text = tracks.Count.ToString();
            }
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"Failed to load tracks: {ex.Message}");
        }
        finally
        {
            TracksLoadingRing.IsActive = false;
        }
    }

    private async void FavoritesNavButton_Click(object sender, RoutedEventArgs e)
    {
        PlaylistsListView.SelectedItem = null;
        await LoadPlaylistTracksAsync("lane_likes", "Мне нравится");
    }

    private async void PlaylistsListView_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (PlaylistsListView.SelectedItem is LanePlaylist selected)
        {
            _selectedPlaylist = selected;
            await LoadPlaylistTracksAsync(selected.RealId, selected.RealTitle);
        }
    }

    private void TrackSearchBox_TextChanged(AutoSuggestBox sender, AutoSuggestBoxTextChangedEventArgs args)
    {
        ApplyTrackFilter(sender.Text);
    }

    private void TrackSearchBox_QuerySubmitted(AutoSuggestBox sender, AutoSuggestBoxQuerySubmittedEventArgs args)
    {
        ApplyTrackFilter(args.QueryText);
    }

    private void ApplyTrackFilter(string query)
    {
        string q = query.Trim().ToLowerInvariant();
        _displayedTracks.Clear();

        if (string.IsNullOrEmpty(q))
        {
            foreach (var t in _allTracks) _displayedTracks.Add(t);
        }
        else
        {
            foreach (var t in _allTracks)
            {
                if (t.Title.ToLowerInvariant().Contains(q) || t.ArtistsDisplayedName.ToLowerInvariant().Contains(q))
                {
                    _displayedTracks.Add(t);
                }
            }
        }
    }

    private async void TracksListView_DoubleTapped(object sender, Microsoft.UI.Xaml.Input.DoubleTappedRoutedEventArgs e)
    {
        if (TracksListView.SelectedItem is LaneTrack track)
        {
            int index = _displayedTracks.IndexOf(track);
            await App.Player.PlayTrackAsync(track, _displayedTracks.ToList(), index);
        }
    }

    private async void PlayAllButton_Click(object sender, RoutedEventArgs e)
    {
        if (_displayedTracks.Count > 0)
        {
            await App.Player.PlayTrackAsync(_displayedTracks[0], _displayedTracks.ToList(), 0);
        }
    }

    #endregion

    #region Player Controls

    private void Player_TrackChanged(object? sender, LaneTrack? track)
    {
        DispatcherQueue.TryEnqueue(() =>
        {
            if (track != null)
            {
                PlayerTrackTitle.Text = track.Title;
                PlayerTrackArtists.Text = track.ArtistsDisplayedName;

                if (!string.IsNullOrEmpty(track.CoverUrl))
                {
                    PlayerCoverImage.Source = new BitmapImage(new Uri(track.CoverUrl));
                }
                else
                {
                    PlayerCoverImage.Source = null;
                }
            }
            else
            {
                PlayerTrackTitle.Text = "Ничего не играет";
                PlayerTrackArtists.Text = "Выберите трек для воспроизведения";
                PlayerCoverImage.Source = null;
            }
        });
    }

    private void Player_PlaybackStateChanged(object? sender, EventArgs e)
    {
        DispatcherQueue.TryEnqueue(() =>
        {
            PlayPauseIcon.Glyph = App.Player.IsPlaying ? "\uE769" : "\uE768";
        });
    }

    private void Player_PositionChanged(object? sender, TimeSpan pos)
    {
        if (_isScrubbing) return;

        DispatcherQueue.TryEnqueue(() =>
        {
            var dur = App.Player.Duration;
            PlayerElapsedText.Text = $"{(int)pos.TotalMinutes}:{pos.Seconds:D2}";
            PlayerTotalText.Text = $"{(int)dur.TotalMinutes}:{dur.Seconds:D2}";

            if (dur.TotalSeconds > 0)
            {
                PlayerScrubberSlider.Maximum = dur.TotalSeconds;
                PlayerScrubberSlider.Value = Math.Min(pos.TotalSeconds, dur.TotalSeconds);
            }
        });
    }

    private void PlayPauseButton_Click(object sender, RoutedEventArgs e)
    {
        App.Player.TogglePlayPause();
    }

    private void NextButton_Click(object sender, RoutedEventArgs e)
    {
        App.Player.PlayNext();
    }

    private void PrevButton_Click(object sender, RoutedEventArgs e)
    {
        App.Player.PlayPrevious();
    }

    private void PlayerScrubberSlider_PointerCaptureLost(object sender, Microsoft.UI.Xaml.Input.PointerRoutedEventArgs e)
    {
        _isScrubbing = false;
        App.Player.Seek(TimeSpan.FromSeconds(PlayerScrubberSlider.Value));
    }

    private void QualityItem_Click(object sender, RoutedEventArgs e)
    {
        if (sender is MenuFlyoutItem item && item.Tag is string q)
        {
            App.Player.Quality = q;
            QualityButton.Content = $"{q} ({(q == "ULTRA" ? "320k" : q == "HIGH" ? "192k" : "128k")})";
            CurrentPlaylistSubtitle.Text = $"{_displayedTracks.Count} треков • Стриминг {q}";
        }
    }

    private void VolumeSlider_ValueChanged(object sender, Microsoft.UI.Xaml.Controls.Primitives.RangeBaseValueChangedEventArgs e)
    {
        if (App.Player != null)
        {
            App.Player.Volume = e.NewValue / 100.0;
        }
    }

    #endregion

    #region Dialogs & Settings

    private async void ConfigButton_Click(object sender, RoutedEventArgs e)
    {
        if (App.CurrentSession == null) return;

        string configJson = SessionStorage.ToLanedJson(App.CurrentSession);

        var dialog = new ContentDialog
        {
            Title = "Конфигурация Lane (laned.json)",
            Content = new ScrollViewer
            {
                Content = new TextBox
                {
                    Text = configJson,
                    IsReadOnly = true,
                    AcceptsReturn = true,
                    FontFamily = new Microsoft.UI.Xaml.Media.FontFamily("Consolas"),
                    FontSize = 12,
                    Height = 260
                }
            },
            PrimaryButtonText = "Скопировать",
            CloseButtonText = "Закрыть",
            DefaultButton = ContentDialogButton.Primary,
            XamlRoot = this.XamlRoot
        };

        var result = await dialog.ShowAsync();
        if (result == ContentDialogResult.Primary)
        {
            var package = new DataPackage();
            package.SetText(configJson);
            Clipboard.SetContent(package);
        }
    }

    private void LogoutButton_Click(object sender, RoutedEventArgs e)
    {
        SessionStorage.Clear();
        App.CurrentSession = null;
        App.ApiClient.Token = null;
        App.ApiClient.DeviceId = null;

        _playlists.Clear();
        _displayedTracks.Clear();
        _allTracks.Clear();

        ShowAuthView();
        StartAuthFlow();
    }

    #endregion
}
