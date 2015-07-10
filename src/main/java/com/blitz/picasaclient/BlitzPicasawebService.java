package com.blitz.picasaclient;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Person;
import com.google.gdata.client.AuthTokenFactory;
import com.google.gdata.client.GoogleAuthTokenFactory;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.BaseEntry;
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.media.MediaSource;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by blitzter on 6/12/15.
 */
public class BlitzPicasawebService {

    private static final String APPLICATION_NAME = "BlitzPicasaClient";

    private static final String APPLICATION_DIR = System.getProperty("user.home")+".blitzPicasaData/";

    private static final String DB_FILE = "blitzDB";

    /** Global instance of the HTTP transport. */
    private static HttpTransport httpTransport;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static PicasawebService myService;

    private static Plus plusService;

    private static Person loggedIn;


    private static void testDb(){
        Connection conn = null;
        try {
            Class.forName("org.h2.Driver");
            conn = DriverManager.
                    getConnection("jdbc:h2:" + APPLICATION_DIR+DB_FILE, "blitz", "blitz");
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        // add application code here

    }

    public static void login(Stage primaryStage) throws IOException, GeneralSecurityException {
        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        // authorization
        Credential credential = CredentialsProvider.authorize(httpTransport, JSON_FACTORY, primaryStage);

        // set up global Oauth2 instance
        myService = new PicasawebService(APPLICATION_NAME);
        myService.setOAuth2Credentials(credential);
        myService.setAuthTokenFactory(new GoogleAuthTokenFactory(APPLICATION_NAME, APPLICATION_NAME, new AuthTokenFactory.TokenListener() {
            @Override
            public void tokenChanged(AuthTokenFactory.AuthToken newToken) {
                System.out.println("Token Changed ="+newToken.toString());
            }
        }));
        plusService = new Plus.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        loggedIn = plusService.people().get("me").execute();
    }

    public static ArrayList<AlbumEntry> getAlbums() throws IOException, ServiceException {
        ArrayList<AlbumEntry> albums = new ArrayList<AlbumEntry>();
        URL feedUrl = null;
        try {
            feedUrl = new URL("https://picasaweb.google.com/data/feed/api/user/default?v=2&kind=album");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        UserFeed myUserFeed = myService.getFeed(feedUrl, UserFeed.class);
        for (Object myAlbum : myUserFeed.getEntries()) {
            AlbumEntry album = new AlbumEntry((BaseEntry<AlbumEntry>) myAlbum);
            album.getXmlBlob();
            System.out.println(album.getTitle().getPlainText());
            albums.add(album);
        }
        return albums;
    }

    public static Image getImage(AlbumEntry album) throws IOException, ServiceException {
        String url = album.getMediaThumbnails().get(0).getUrl();
        MediaContent mc = (new MediaContent());
        mc.setUri(url);
        MediaSource ms = myService.getMedia(mc);
        return new Image(ms.getInputStream(),40,40, false, false);
    }

    public static Person getLoggedIn(){
        return loggedIn;
    }

}
