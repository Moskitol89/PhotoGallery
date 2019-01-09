package com.example.moskitol.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private int mPageNumber = 1;
    private int mLastElementIndex = 0;
    private int mSpanCount = 1;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemsTask().execute();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder target, Bitmap bitmap) {
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        target.bindDrawable(drawable);
                    }
                }
        );

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG,"Background thread started");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mRecyclerView = view.findViewById(R.id.photo_recycler_view);
        final GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), mSpanCount);
        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                       mSpanCount = view.getWidth() / 160;
                       layoutManager.setSpanCount(mSpanCount);
                    }
                });
        mRecyclerView.setLayoutManager(layoutManager);
        setupAdapter();

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(!recyclerView.canScrollVertically(1)) {
                    ++mPageNumber;
                    new FetchItemsTask().execute();
                }
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    private void setupAdapter() {
        if (isAdded()) {
            mRecyclerView.setAdapter(new PhotoAdapter(mItems));
            mRecyclerView.scrollToPosition(mLastElementIndex);
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mImageView;

        public PhotoHolder(@NonNull View itemView) {
            super(itemView);

            mImageView = itemView.findViewById(R.id.item_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            if(mThumbnailDownloader.getCache().maxSize() > 200) {
                mThumbnailDownloader.getCache().trimToSize(30);
            }
            mImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @NonNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, viewGroup,false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder photoHolder, int i) {
            int cursor = i;
            ConcurrentLinkedQueue<GalleryItem> galleryItems = new ConcurrentLinkedQueue<>();
            GalleryItem galleryItem = mGalleryItems.get(i);
            for (int k = 0; k < 10; k++) {
                if (cursor > 1 && cursor < mGalleryItems.size()) {
                    galleryItems.add(mGalleryItems.get(--cursor));
                }
            }
            cursor = i;
            for (int k = 0; k < 10; k++) {
                if (cursor < mGalleryItems.size() - 2 && cursor >=0) {
                    galleryItems.add(mGalleryItems.get(++cursor));
                }
            }
            Drawable placeholder = getResources().getDrawable(
                    R.drawable.bill_up_close
            );
            photoHolder.bindDrawable(placeholder);
            if ((photoHolder.getAdapterPosition() + 1) % 100 == 0) {
                mLastElementIndex = photoHolder.getAdapterPosition();
            }
             mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getmUrl(), galleryItems);

        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }


    private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            String query = "boobs";

            if(query == null) {
                return new FlickrFetchr().fetchRecentPhotos(mPageNumber);
            } else {
                return new FlickrFetchr().searchPhotos(query,mPageNumber);
            }

        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            if(mItems == null) {
                mItems = galleryItems;
            } else mItems.addAll(galleryItems);
            setupAdapter();
        }
    }
}
