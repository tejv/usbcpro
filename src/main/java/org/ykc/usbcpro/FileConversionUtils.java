/*
 * Copyright (C) 2016 teju <tejendersheoran@gmail.com>
 * See license file for more information
*/

package org.ykc.usbcpro;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.ykc.gutils.*;
import org.ykc.gutils.FileUtils.CancelledException;

public class FileConversionUtils {

	public static boolean exportToUC(DefaultTableModel model) {
		File newFile;
		try {
			newFile = FileUtils.getFile(FileUtils.SaveOpen.SAVE, "uc");
		} catch (CancelledException e) {
			return false;
		}

		FileWriter out = null;
		try {
			out = new FileWriter(newFile.getAbsolutePath());

			for(int i=0; i < model.getColumnCount(); i++) {
			    out.write(model.getColumnName(i) + "@");
			}
			out.write("\n");
			for(int i=0; i< model.getRowCount(); i++) {
			    for(int j=0; j < model.getColumnCount(); j++) {
			        out.write( model.getValueAt(i,j).toString() +"@");
			    }
			    out.write("\n");
			}
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		finally{
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }

	public static boolean isFileUC(File file)
	{
		String name = file.getName();
		String[] parts = name.split("\\.");

		if( (parts.length >= 2) && ( parts[parts.length - 1].equalsIgnoreCase("uc") ))
		{
			return true;
		}
		return false;
	}


	public static boolean importFromUC(DefaultTableModel tableModel, String ucFilePath) {

		BufferedReader bufReader = null;
		try {
			tableModel.setRowCount(0);
			bufReader = new BufferedReader(new FileReader(ucFilePath));
			int rowCount = 0;
			while(bufReader.ready())
			{
				String[] tableRow;
				String line = bufReader.readLine();
				if(rowCount == 0)
				{
					rowCount++;
					continue;
				}
				tableRow = line.split("@");
				tableModel.addRow(tableRow);

			}
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		finally{
			try {
				bufReader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static boolean importFromUC(DefaultTableModel tableModel) {
		String ucFilePath = "";

		JFileChooser chooser;

        FileNameExtensionFilter filter = new FileNameExtensionFilter("UC Format (uc)", "uc");

        chooser = new JFileChooser();
        chooser.setDialogTitle("Open UC File");
        chooser.setFileFilter(filter);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int rval = chooser.showOpenDialog((Component)null);

        if(rval == JFileChooser.APPROVE_OPTION) {
        	ucFilePath = chooser.getSelectedFile().getAbsolutePath();
        }
        else
        {
        	return false;
        }
		return importFromUC(tableModel, ucFilePath);

	}




}
