package com.team8020.opencvapplication1;

import java.io.Serializable;

public class Cell implements Serializable {

    private String title, path;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }
}
