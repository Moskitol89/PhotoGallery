package com.example.moskitol.photogallery;

import java.util.List;

public class Photos {

    private int page;
    private int pages;
    private int perpage;
    private int total;
    private List<GalleryItem> photo;

    public Photos() {
    }

    public List<GalleryItem> getPhoto() {
        return photo;
    }

}
