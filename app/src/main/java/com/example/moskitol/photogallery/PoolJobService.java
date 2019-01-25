//package com.example.moskitol.photogallery;
//
//import android.app.Notification;
//import android.app.PendingIntent;
//import android.app.job.JobParameters;
//import android.app.job.JobService;
//import android.content.Intent;
//import android.content.res.Resources;
//import android.os.AsyncTask;
//import android.os.Build;
//import android.support.annotation.RequiresApi;
//import android.support.v4.app.NotificationCompat;
//import android.support.v4.app.NotificationManagerCompat;
//import android.util.Log;
//
//import java.util.List;
//
//@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//public class PoolJobService extends JobService {
//    private static final String TAG = "PoolJobService";
//    private PollTask mCurrentTusk;
//    @Override
//    public boolean onStartJob(JobParameters params) {
//
//        mCurrentTusk = new PollTask();
//        mCurrentTusk.execute(params);
//        return true;
//    }
//
//    @Override
//    public boolean onStopJob(JobParameters params) {
//
//        if (mCurrentTusk != null) {
//            mCurrentTusk.cancel(true);
//        }
//        return true;
//    }
//
//    private class PollTask extends AsyncTask<JobParameters, Void, Void> {
//
//        @Override
//        protected Void doInBackground(JobParameters... jobParameters) {
//            JobParameters jobParams = jobParameters[0];
//            String query = QueryPreferences.getStoredQuery(PoolJobService.this);
//            String lastResultId = QueryPreferences.getLastResultId(PoolJobService.this);
//            List<GalleryItem> items;
//
//            if(query == null) {
//                items = new FlickrFetchr().fetchRecentPhotos(1);
//            } else {
//                items = new FlickrFetchr().searchPhotos(query, 1);
//            }
//
//            if(items.size() == 0) {
//                cancel(true);
//            }
//            String resultId = items.get(0).getId();
//            if(resultId.equals(lastResultId)) {
//                Log.i(TAG, "Got an old result: " + resultId);
//            } else {
//                Log.i(TAG, "Got a new result: " + resultId);
//            }
//
//            Resources resources = getResources();
//            Intent i = PhotoGalleryActivity.newIntent(PoolJobService.this);
//            PendingIntent pi = PendingIntent.getActivity(PoolJobService.this, 0, i, 0);
//            Notification notification = new NotificationCompat.Builder(PoolJobService.this)
//                    .setTicker(resources.getString(R.string.new_pictures_title))
//                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
//                    .setContentTitle(resources.getString(R.string.new_pictures_title))
//                    .setContentText(resources.getString(R.string.new_pictures_text))
//                    .setContentIntent(pi)
//                    .setAutoCancel(true)
//                    .build();
//            NotificationManagerCompat notificationManager =
//                    NotificationManagerCompat.from(PoolJobService.this);
//            notificationManager.notify(0, notification);
//            QueryPreferences.setLastResultId(PoolJobService.this, resultId);
//            jobFinished(jobParams, false);
//            return null;
//        }
//    }
//}
