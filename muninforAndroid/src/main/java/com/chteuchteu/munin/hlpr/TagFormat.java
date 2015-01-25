package com.chteuchteu.munin.hlpr;

import android.content.Context;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Easily use templates strings (about / premium pages) using tags:
 * <p>{this_is_replaced_at_runtime}</p>
 */
public class TagFormat {
	private int templateResId;
	private final Map<String, Integer> tags = new LinkedHashMap<>();
	private Context context;

	public static TagFormat from(Context context, int templateResId) {
		return new TagFormat(context, templateResId);
	}

	private TagFormat(Context context, int templateResId) {
		this.templateResId = templateResId;
		this.context = context;
	}

	public TagFormat with(String key, int valueResId) {
		tags.put("\\{" + key + "\\}", valueResId);
		return this;
	}

	public String format() {
		String formatted = context.getString(templateResId);

		for (Map.Entry<String, Integer> tag : tags.entrySet())
			formatted = formatted.replaceAll(tag.getKey(), context.getString(tag.getValue()));

		return formatted;
	}
}
