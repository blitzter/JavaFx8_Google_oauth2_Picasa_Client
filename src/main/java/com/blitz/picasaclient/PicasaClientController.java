package com.blitz.picasaclient;

import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.util.ServiceException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.ResourceBundle;


public class PicasaClientController implements Initializable {

    @FXML Label loggedInEmail;

    @FXML Button loginButton;

    @FXML ScrollPane loginDialog;

    @FXML ListView<AlbumEntry> listAlbums;

    private ObservableList<AlbumEntry> listAlbumsData = FXCollections.observableArrayList();

    @FXML public void onLoginButtonClicked(ActionEvent event){
        try {
            if(loggedInEmail.getText().isEmpty()) {
                BlitzPicasawebService.login((Stage) loginDialog.getParent().getScene().getWindow());
                loggedInEmail.setText(BlitzPicasawebService.getLoggedIn().getDisplayName());
                loginButton.setText("Logout");
                loadAlbums();
            } else {
                logOut();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    private void loadAlbums() throws IOException, ServiceException {
        ArrayList<AlbumEntry> albums = BlitzPicasawebService.getAlbums();
        listAlbumsData.addAll(albums);
        listAlbums.setItems(listAlbumsData);
    }

    private void logOut() {
        loggedInEmail.setText("");
        listAlbumsData.clear();
        listAlbums.setItems(listAlbumsData);
        loginButton.setText("Login");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listAlbums.setCellFactory( list -> {
                return new ListCell<AlbumEntry>() {
                    @Override
                    protected void updateItem(AlbumEntry item, boolean empty) {
                        super.updateItem(item, empty);
                        if(!empty) {
                            setText(item.getTitle().getPlainText());
                            try {
                                Image fxImage = BlitzPicasawebService.getImage(item);
                                ImageView imageView = new ImageView(fxImage);
                                setGraphic(imageView);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (ServiceException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
        });
    }
}
