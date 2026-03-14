package com.quietpages.quietpages;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    private static HelloApplication instance;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        instance     = this;
        primaryStage = stage;

        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 750);

        String css = HelloApplication.class
                .getResource("library.css").toExternalForm();

        // Add to scene — covers all nodes in main window
        scene.getStylesheets().add(css);

        stage.setTitle("QuietPages");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    public static HelloApplication getInstance() { return instance; }
    public static Stage getPrimaryStage()        { return primaryStage; }

    public static void main(String[] args) {
        launch();
    }
}