package com.chteuchteu.munin.ui;

public interface IImportExportActivity {
	void onExportSuccess(String pswd);
	void onExportError();

	void onImportSuccess();
	void onImportError();
}
