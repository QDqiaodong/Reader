package com.github.reader.app.model.manager;

import com.github.reader.pdf.model.OutlineItem;

public class OutlineManager {
	public OutlineItem items[];
	public int         position;
	static private OutlineManager singleton;

	static public void set(OutlineManager d) {
		singleton = d;
	}

	static public OutlineManager get() {
		if (singleton == null)
			singleton = new OutlineManager();
		return singleton;
	}
}
