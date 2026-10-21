using System.Text.Json;

namespace LaneWinUI.Core;

public static class SessionStorage
{
    private static readonly string StorageDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "LaneMusic"
    );

    private static readonly string SessionFile = Path.Combine(StorageDir, "session.json");

    public static AuthSession? Load()
    {
        try
        {
            if (!File.Exists(SessionFile)) return null;
            string json = File.ReadAllText(SessionFile);
            return JsonSerializer.Deserialize<AuthSession>(json);
        }
        catch
        {
            return null;
        }
    }

    public static void Save(AuthSession session)
    {
        try
        {
            Directory.CreateDirectory(StorageDir);
            string json = JsonSerializer.Serialize(session, new JsonSerializerOptions { WriteIndented = true });
            File.WriteAllText(SessionFile, json);
        }
        catch
        {
            // Ignore write errors
        }
    }

    public static void Clear()
    {
        try
        {
            if (File.Exists(SessionFile))
            {
                File.Delete(SessionFile);
            }
        }
        catch
        {
            // Ignore
        }
    }

    public static string ToLanedJson(AuthSession session)
    {
        var dict = new Dictionary<string, object?>
        {
            ["deviceId"] = session.DeviceId,
            ["inode"] = Bnit.DefaultInode,
            ["token"] = session.Token,
            ["user"] = session.User
        };
        return JsonSerializer.Serialize(dict, new JsonSerializerOptions { WriteIndented = true });
    }
}
