package com.chteuchteu.munin.adptr;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.async.BitmapFetcher;
import com.chteuchteu.munin.async.LightBitmapFetcher;
import com.chteuchteu.munin.obj.MuninPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Adapter_GraphList extends ArrayAdapter<MuninPlugin> {
    private Context context;
    private List<MuninPlugin> plugins;
    private static final int rowLayout = R.layout.adapter_imageview;
    private HashMap<MuninPlugin, Bitmap> bitmaps;

    public Adapter_GraphList(Context context, List<MuninPlugin> plugins) {
        super(context, rowLayout, plugins);
        this.context = context;
        this.plugins = plugins;
        this.bitmaps = new HashMap<>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(context).inflate(rowLayout, parent, false);

        final MuninPlugin plugin = plugins.get(position);

        final ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
        ((TextView) convertView.findViewById(R.id.tv)).setText(plugin.getName());

        MuninFoo.log(plugin.getName() + " is downloaded ?: " + bitmaps.keySet().contains(plugin));

        if (bitmaps.keySet().contains(plugin))
            imageView.setImageBitmap(bitmaps.get(plugin));
        else {
            new LightBitmapFetcher(null, MuninPlugin.Period.DAY, plugin, new LightBitmapFetcher.OnBitmapDownloaded() {
                @Override
                public void onBitmapDownloaded(Bitmap bitmap) {
                    bitmaps.put(plugin, bitmap);
                    imageView.setImageBitmap(bitmap);
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        return convertView;
    }
}
