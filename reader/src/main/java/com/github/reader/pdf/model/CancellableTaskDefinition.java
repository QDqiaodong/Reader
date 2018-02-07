package com.github.reader.pdf.model;

public interface CancellableTaskDefinition <Params, Result>
{
	public Result doInBackground(Params... params);
	public void doCancel();
	public void doCleanup();
}
