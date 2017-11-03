package com.chteuchteu.munin.adptr;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.chteuchteu.munin.hlpr.I18nHelper;

import java.util.List;

public class Adapter_Languages extends ArrayAdapter<String> {
    private I18nHelper.AppLanguage[] languages;

    public Adapter_Languages(@NonNull Context context, I18nHelper.AppLanguage[] languages, List<String> labels) {
        super(context, android.R.layout.simple_spinner_item, labels);
        this.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        this.languages = languages;
    }

    @Override
    public boolean isEnabled(int position) {
        I18nHelper.AppLanguage language = this.languages[position];
        boolean isSupported = I18nHelper.isLanguageSupportedByDevice(language);

        return isSupported;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        TextView textView = (TextView) view;

        textView.setTextColor(
            this.isEnabled(position)
                ? Color.BLACK
                : Color.GRAY
        );

        return view;
    }
}
