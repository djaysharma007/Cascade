package io.github.djaysharma007.cascade.app;

import javafx.application.Application;

/**
 * Standard main class launcher allowing execution without modular JVM arguments.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(CascadeApplication.class, args);
    }
}
