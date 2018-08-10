package kie.com.soundtube;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;


public class PlayListManager {

    private static PlayListManager INSTANCE;
    private Context context;
    HashMap<String, PlaylistHolder> playlists;
    ArrayList<String> playlistNames;


    public static PlayListManager getInstance(Context context) {

        if (INSTANCE == null) {
            PlayListManager manager = read(context);
            if (manager != null) {
                INSTANCE = manager;
            } else {
                INSTANCE = new PlayListManager(context);
            }

        }
        return INSTANCE;
    }

    private PlayListManager(Context context) {
        this.context = context;
        playlists = new HashMap<>();
        playlistNames = new ArrayList<>();

    }

    public void write() {

        try {
            OutputStream os = new FileOutputStream(new File(context.getFilesDir(), "PlayListManager.ser"));
            ObjectOutputStream oo = new ObjectOutputStream(os);
            oo.writeObject(this);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static PlayListManager read(Context context) {
        PlayListManager manager = null;

        try {
            InputStream in = new FileInputStream(new File(context.getFilesDir(), "PlayListManager.ser"));
            ObjectInputStream oi = new ObjectInputStream(in);
            manager = (PlayListManager) oi.readObject();
            in.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return manager;
    }

}
