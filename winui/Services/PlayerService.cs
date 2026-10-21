using Windows.Media.Core;
using Windows.Media.Playback;
using Windows.Media;
using Microsoft.UI.Xaml;
using LaneWinUI.Core;

namespace LaneWinUI.Services;

public class PlayerService
{
    private readonly MediaPlayer _player;
    private readonly LaneApiClient _apiClient;
    private readonly DispatcherTimer _timer;

    public List<LaneTrack> Queue { get; } = new();
    public int CurrentIndex { get; private set; } = -1;
    public LaneTrack? CurrentTrack => CurrentIndex >= 0 && CurrentIndex < Queue.Count ? Queue[CurrentIndex] : null;

    public string Quality { get; set; } = "ULTRA";
    public bool IsPlaying => _player.PlaybackSession.PlaybackState == MediaPlaybackState.Playing;
    public TimeSpan Position => _player.PlaybackSession.Position;
    public TimeSpan Duration => _player.PlaybackSession.NaturalDuration;

    public double Volume
    {
        get => _player.Volume;
        set => _player.Volume = Math.Clamp(value, 0.0, 1.0);
    }

    public event EventHandler? PlaybackStateChanged;
    public event EventHandler<TimeSpan>? PositionChanged;
    public event EventHandler<LaneTrack?>? TrackChanged;

    public PlayerService(LaneApiClient apiClient)
    {
        _apiClient = apiClient;
        _player = new MediaPlayer();
        _player.PlaybackSession.PlaybackStateChanged += (s, e) => PlaybackStateChanged?.Invoke(this, EventArgs.Empty);
        _player.MediaEnded += (s, e) => PlayNext();

        _timer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(250) };
        _timer.Tick += (s, e) =>
        {
            if (IsPlaying)
            {
                PositionChanged?.Invoke(this, Position);
            }
        };
        _timer.Start();
    }

    public async Task PlayTrackAsync(LaneTrack track, List<LaneTrack>? newQueue = null, int index = -1)
    {
        if (newQueue != null)
        {
            Queue.Clear();
            Queue.AddRange(newQueue);
            CurrentIndex = index >= 0 ? index : Queue.FindIndex(t => t.SongId == track.SongId);
        }
        else if (index >= 0)
        {
            CurrentIndex = index;
        }
        else
        {
            int found = Queue.FindIndex(t => t.SongId == track.SongId);
            if (found >= 0) CurrentIndex = found;
            else
            {
                Queue.Add(track);
                CurrentIndex = Queue.Count - 1;
            }
        }

        TrackChanged?.Invoke(this, CurrentTrack);

        try
        {
            string? streamUrl = await _apiClient.GetTrackStreamUrlAsync(track.SongId, Quality);
            if (string.IsNullOrEmpty(streamUrl))
            {
                throw new Exception("Не удалось получить URL аудиопотока.");
            }

            var source = MediaSource.CreateFromUri(new Uri(streamUrl));
            _player.Source = source;
            _player.Play();

            UpdateSystemMediaControls(track);
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"Playback error: {ex.Message}");
        }
    }

    public void TogglePlayPause()
    {
        if (IsPlaying)
        {
            _player.Pause();
        }
        else
        {
            _player.Play();
        }
        PlaybackStateChanged?.Invoke(this, EventArgs.Empty);
    }

    public void Seek(TimeSpan target)
    {
        _player.PlaybackSession.Position = target;
    }

    public void PlayNext()
    {
        if (Queue.Count == 0) return;
        int next = (CurrentIndex + 1) % Queue.Count;
        _ = PlayTrackAsync(Queue[next], index: next);
    }

    public void PlayPrevious()
    {
        if (Queue.Count == 0) return;
        int prev = (CurrentIndex - 1 + Queue.Count) % Queue.Count;
        _ = PlayTrackAsync(Queue[prev], index: prev);
    }

    private void UpdateSystemMediaControls(LaneTrack track)
    {
        try
        {
            var smtc = _player.SystemMediaTransportControls;
            smtc.IsEnabled = true;
            smtc.IsPlayEnabled = true;
            smtc.IsPauseEnabled = true;
            smtc.IsNextEnabled = true;
            smtc.IsPreviousEnabled = true;

            var updater = smtc.DisplayUpdater;
            updater.Type = MediaPlaybackType.Music;
            updater.MusicProperties.Title = track.Title;
            updater.MusicProperties.Artist = track.ArtistsDisplayedName;
            updater.Update();
        }
        catch
        {
            // Ignore SMTC errors in unpackaged / restricted mode
        }
    }
}
