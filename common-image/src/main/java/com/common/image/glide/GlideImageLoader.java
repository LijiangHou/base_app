package com.common.image.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.common.image.CommonImageView;
import com.common.image.IImageLoadListener;
import com.common.image.IImageLoader;
import com.common.image.ImageLoadError;
import com.common.image.ImageOptions;
import com.common.utils.AppLog;

import java.io.File;
import java.io.InputStream;
import java.util.Locale;

import okhttp3.OkHttpClient;

/**
 * Created by houlijiang on 16/4/12.
 * 
 * 图片加载的glide实现
 *
 * 目前这个库的问题：
 * ImageView 不能用setTag，因为glide内部用了，否则会抛异常
 * PlaceHolder 被拉伸了，这个暂时可以通过在XML中设置 scaleType=center来解决
 */
public class GlideImageLoader implements IImageLoader {

    private static final String TAG = GlideImageLoader.class.getSimpleName();
    private LoggingListener<String, GlideDrawable> debugListener = new LoggingListener<>();

    @Override
    public void init(Context context, File cacheDir) {
        Glide.get(context).register(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(new OkHttpClient()));
    }

    @Override
    public long getCacheSize() {
        return 0;
    }

    @Override
    public boolean clearCache() {
        return true;
    }

    @Override
    public void displayImage(Uri uri, CommonImageView iv, ImageOptions options, IImageLoadListener listener) {
        com.common.image.glide.CommonImageView imageView = iv;
        // com.common.image.glide.CommonImageView imageView = null;
        RequestManager manager = Glide.with(imageView.getContext());
        if (uri == null) {
            // 显示空的图片的
            if (options != null) {
                if (options.getImageForEmptyUri() != null) {
                    manager.load(options.getImageForEmptyUri()).into(imageView);
                } else if (options.getImageResForEmptyUri() != 0) {
                    manager.load(options.getImageResForEmptyUri()).into(imageView);
                }
            }
            if (listener != null) {
                listener.onFailed("", imageView, new ImageLoadError(ImageLoadError.ERROR_NONE_URI, ""));
            }
            return;
        }
        DrawableTypeRequest request;
        // 特殊处理资源加载
        if ("res".equals(uri.getScheme())) {
            int resId = 0;
            try {
                resId = Integer.parseInt(uri.getLastPathSegment());
            } catch (Exception e) {
                AppLog.e(TAG, "parse res error:" + e.getLocalizedMessage());
            }
            request = manager.load(resId);
        } else {
            request = manager.load(uri);
        }
        if (options != null) {
            if (options.getImageResOnFail() != 0) {
                request.error(options.getImageResOnFail());
            } else if (options.getImageOnFail() != null) {
                request.error(options.getImageOnFail());
            }

            if (options.getImageResOnLoading() != 0) {
                request.placeholder(options.getImageResOnLoading());
            } else if (options.getImageOnLoading() != null) {
                request.placeholder(options.getImageOnLoading());
            }
            if (options.getImageScaleType() != null) {
                switch (options.getImageScaleType()) {
                    case FIT_XY:
                    case FIT_START:
                    case FIT_END:
                    case CENTER:
                    case CENTER_INSIDE:
                    case FOCUS_CROP:
                    case CENTER_CROP: {
                        request.centerCrop();
                        break;
                    }
                    case FIT_CENTER: {
                        request.fitCenter();
                        break;
                    }
                }
            }
            ImageOptions.ImageSize size = options.getImageSize();
            if (size != null && size.width > 0 && size.height > 0) {
                if (iv.getLayoutParams() != null) {
                    iv.getLayoutParams().width = size.width;
                    iv.getLayoutParams().height = size.height;
                }
                request.override(size.width, size.height);
            }
            if (options.getIfGif()) {
                request.asGif();
            }
        }
        Transformation<Bitmap> bit = imageView.createTransformation();
        if (bit != null) {
            request.bitmapTransform(bit);
        }
        // 这里的dontAnimate主要是因为当placeholder和image的scaleType不同时
        // image先显示的是placeholder的scaleType，当再次滑动回来时又用的正常的scaleType显示
        // DiskCacheStrategy。ALL->DiskCacheStrategy.SOURCE 因为jpg的图片当从缓存中取时背景的白色变成了蓝色
        request.dontAnimate().diskCacheStrategy(DiskCacheStrategy.SOURCE).listener(debugListener).into(imageView);

    }

    public class LoggingListener<T, R> implements RequestListener<T, R> {
        @Override
        public boolean onException(Exception e, Object model, Target target, boolean isFirstResource) {
            AppLog.d(TAG, String.format(Locale.ROOT, "onException(%s, %s, %s, %s)", e, model, target, isFirstResource),
                e);
            return false;
        }

        @Override
        public boolean onResourceReady(Object resource, Object model, Target target, boolean isFromMemoryCache,
            boolean isFirstResource) {
            AppLog.d(TAG, String.format(Locale.ROOT, "onResourceReady(%s, %s, %s, %s, %s)", resource, model, target,
                isFromMemoryCache, isFirstResource));
            return false;
        }
    }
}
