package com.github.reader.pdf.model;

import android.net.Uri;

public abstract class FilePicker {
	private final FilePickerSupport support;

	public FilePicker(FilePickerSupport _support) {
		support = _support;
	}
	public abstract void onPick(Uri uri);

	public void pick() {
		support.performPickFor(this);
	}

	public interface FilePickerSupport {
		void performPickFor(FilePicker picker);
	}
}
