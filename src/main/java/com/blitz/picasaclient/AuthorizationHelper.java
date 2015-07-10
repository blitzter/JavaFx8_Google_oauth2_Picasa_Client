package com.blitz.picasaclient;

/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.util.Preconditions;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;

/**
 * OAuth 2.0 authorization code flow for an installed Java application that persists end-user
 * credentials using embedded javafx browser.
 *
 * Specifically meant to be used with a javafx application and need Application's stage as parent to show a modal on.
 *
 */
public class AuthorizationHelper {
    public static String code = "";

    private WebView webView;
    private WebEngine webEngine;

    /** Authorization code flow. */
    private final AuthorizationCodeFlow flow;

    /** Verification code receiver. */
    //private final VerificationCodeReceiver receiver;

    private static final Logger LOGGER =
            Logger.getLogger(AuthorizationHelper.class.getName());

    /**
     * @param flow authorization code flow
     */
    public AuthorizationHelper(
            AuthorizationCodeFlow flow) {
        this.flow = Preconditions.checkNotNull(flow);
        //this.receiver = Preconditions.checkNotNull(receiver);
    }

    /**
     * Authorizes the installed application to access user's protected data.
     *
     * @param userId user ID or {@code null} if not using a persisted credential store
     * @return credential
     */
    public Credential authorize(String userId, Stage parentStage) throws IOException, GeneralSecurityException {
        Credential credential = flow.loadCredential(userId);
        if (credential != null
                && (credential.getRefreshToken() != null || credential.getExpiresInSeconds() > 60)) {
            credential.refreshToken();
            flow.getCredentialDataStore().set(userId, new StoredCredential(credential));
            return credential;
        }
        // open in browser
        String redirectUri = "urn:ietf:wg:oauth:2.0:oob:auto";
        AuthorizationCodeRequestUrl authorizationUrl =
                flow.newAuthorizationUrl().setRedirectUri(redirectUri);
        //onAuthorization(authorizationUrl);
        AuthorizationHelper.code = "";
        showLogin(parentStage, authorizationUrl.build());
        // receive authorization code and exchange it for an access token

        if(AuthorizationHelper.code.equals("ERROR")) {
            throw new GeneralSecurityException("Not authorized");
        }

        TokenResponse response = flow.newTokenRequest(AuthorizationHelper.code).setRedirectUri(redirectUri).execute();
        // store credential and return it
        return flow.createAndStoreCredential(response, userId);
    }

    private void showLogin(Stage parent, String url) {
        final Stage dialog = new Stage(StageStyle.DECORATED);
        StackPane root = new StackPane();
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.getLoadWorker().stateProperty().addListener(
                new ChangeListener<Worker.State>() {
                    public void changed(ObservableValue ov, Worker.State oldState, Worker.State newState) {
                        if (newState == Worker.State.SUCCEEDED) {
                            String url = webEngine.getLocation();
                            System.out.println(url);
                            if(url.startsWith("https://accounts.google.com/o/oauth2/approval")) {
                                String title = webEngine.getTitle();
                                if(title.startsWith("Success code=")) {
                                    AuthorizationHelper.code = ((title.split("="))[1]).trim();
                                } else {
                                    AuthorizationHelper.code = "ERROR";
                                }
                                dialog.close();
                            }
                        }
                    }
                });
        (root.getChildren()).add(webView);
        // initialize the confirmation dialog
        dialog.initOwner(parent);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setScene(new Scene(root, 600, 400));
        dialog.setTitle("Login to Google");
        dialog.setOnShown(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                webEngine.load(url);
            }
        });
        dialog.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                if(AuthorizationHelper.code.equals("")) {
                    AuthorizationHelper.code = "ERROR";
                }
            }
        });
        // style and show the dialog.
        dialog.showAndWait();
    }

}
