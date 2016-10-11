/*
 * Copyright (C) 2016 teju <tejendersheoran@gmail.com>
 * See license file for more information
*/

package org.ykc.usbcpro;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.table.DefaultTableModel;

public class DynamicSaveData implements Runnable{
	private File tempFile;
	private FileOutputStream fos;
	private ObjectOutputStream oos;
	private ConcurrentLinkedQueue<DefaultTableModel> modelQueue;
	private boolean isRunning = true;

	DynamicSaveData(ConcurrentLinkedQueue<DefaultTableModel> newModelQueue)
	{
		modelQueue = newModelQueue;
        try {
    		tempFile = new File("log.temp");
    		fos= new FileOutputStream(tempFile);
			oos  = new ObjectOutputStream(fos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public File getTempFile()
	{
		isRunning = false;
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		try {
			oos.close();
		} catch (IOException e) {
		}
		return tempFile;
	}

	public void pause()
	{
		isRunning = false;
	}

	public void resume()
	{
		isRunning = true;
	}

	@Override
	public void run() {
		while(true)
		{
			if(isRunning)
			{
				if(modelQueue.isEmpty() == false)
				{
					DefaultTableModel x ;
					x = modelQueue.peek();
					try {
						oos.writeObject(x);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					modelQueue.remove();
				}
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
		}

	}

}
