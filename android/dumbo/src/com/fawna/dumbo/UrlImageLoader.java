package com.fawna.dumbo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.HashMap;

public class UrlImageLoader {

  private static HashMap<String, Bitmap> imageCache = new HashMap<String, Bitmap>();

  public static void loadImage(ImageView image, String url, int scale) {
    DownloadImageTask imgTask = new DownloadImageTask(image, scale);
    imgTask.execute(url);
  }


  private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;
    int scale;

    public DownloadImageTask(ImageView bmImage, int scale) {
      this.bmImage = bmImage;
      this.scale = scale;
    }

    protected Bitmap doInBackground(String... urls) {
      String urldisplay = urls[0];
      if (imageCache.containsKey(urldisplay)) {
        return imageCache.get(urldisplay);
      }
      Bitmap mIcon11 = null;
      try {
        InputStream in = new java.net.URL(urldisplay).openStream();
        mIcon11 = BitmapFactory.decodeStream(in);
      } catch (Exception e) {
        Log.e("Error", e.getMessage());
        e.printStackTrace();
      }
      imageCache.put(urldisplay, mIcon11);
      return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
      bmImage.setImageBitmap(result);
      scaleImageToFitWidth(this.bmImage, scale);
    }

    private void scaleImageToFitWidth(ImageView image, int intendedWidth) {
      Drawable d = image.getDrawable();
      int originalWidth = d.getIntrinsicWidth();
      int originalHeight = d.getIntrinsicHeight();
      float scale = (float)intendedWidth / originalWidth;
      int newHeight = Math.round(originalHeight * scale);
      image.getLayoutParams().width = intendedWidth;
      image.getLayoutParams().height = newHeight;
    }
  }

}
