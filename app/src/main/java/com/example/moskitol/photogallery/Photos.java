package com.example.moskitol.photogallery;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Photos {

    int page;
    int pages;
    int perpage;
    int total;
    List<GalleryItem> photo;

    public Photos() {
    }

    public List<GalleryItem> getPhoto() {
        return photo;
    }

}
