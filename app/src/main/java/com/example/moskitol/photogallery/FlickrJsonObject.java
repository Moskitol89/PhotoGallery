package com.example.moskitol.photogallery;

import java.util.List;

public class FlickrJsonObject {
    private Photos photos;
    public List<GalleryItem> getGalleryItems() {
        return photos.getPhoto();
    }
}
