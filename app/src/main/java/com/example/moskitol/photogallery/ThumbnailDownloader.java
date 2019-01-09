package com.example.moskitol.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {

    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private ConcurrentMap<T,String> mRequestMap = new ConcurrentHashMap<>();
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    private LruCache<String, Bitmap> mLruCache;
    private ConcurrentLinkedQueue<GalleryItem> cache;

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
        mLruCache = new LruCache<>(1024 * 1024);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> mThumbnailDownloadListener) {
        this.mThumbnailDownloadListener = mThumbnailDownloadListener;
    }

    public LruCache<String, Bitmap> getCache() {
        return mLruCache;
    }

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap bitmap);
     }


    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T target, String url, ConcurrentLinkedQueue<GalleryItem> cache) {
        Log.i(TAG,"Got a URL: " + url);
        this.cache = cache;
        if(url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mLruCache.evictAll();
        mRequestMap.clear();
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    private void handleRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);

            if(url == null) {
                return;
            }

            final Bitmap bitmap;

            if(mLruCache.get(url) != null) {
                bitmap = mLruCache.get(url);
            }else {
                byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                bitmap = BitmapFactory
                        .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                Log.i(TAG,"Bitmap created");

            }

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                if(!(mRequestMap.get(target) == url) || mHasQuit) {
                    return;
                }
                mLruCache.put(url,bitmap);
                mRequestMap.remove(target);
                mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                }
            });

             Thread t =  new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (GalleryItem galleryItem : cache) {
                            FlickrFetchr flickrFetchr = new FlickrFetchr();
                            String urlForCache = galleryItem.getmUrl();
                            if (urlForCache != null && mLruCache.get(urlForCache) == null) {
                                byte[] bitmapBytes = new byte[0];

                                bitmapBytes = flickrFetchr.getUrlBytes(urlForCache);

                                mLruCache.put(urlForCache, BitmapFactory
                                        .decodeByteArray(bitmapBytes, 0, bitmapBytes.length));
                            }
                        }
                        interrupt();
                    } catch (IOException e) {
                        Log.e(TAG, "Error downloading image for cache", e);
                    }
                }
            });
             t.start();

        }catch (IOException e) {
            Log.e(TAG, "Error downloading image", e);
        }
    }
}
