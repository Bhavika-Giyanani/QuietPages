module com.quietpages.quietpages {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires org.kordamp.ikonli.material2;

    requires java.sql;
    requires org.slf4j;
    requires org.xerial.sqlitejdbc;

    opens com.quietpages.quietpages to javafx.fxml;
    opens com.quietpages.quietpages.controller to javafx.fxml;
    opens com.quietpages.quietpages.model to javafx.base;

    exports com.quietpages.quietpages;
    exports com.quietpages.quietpages.controller;
    exports com.quietpages.quietpages.model;
    exports com.quietpages.quietpages.service;
    exports com.quietpages.quietpages.db;
    exports com.quietpages.quietpages.util;
}