/*
 * Copyright (C) 2016 teju <tejendersheoran@gmail.com>
 * See license file for more information
*/
package org.ykc.usbcpro;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.usb.UsbDevice;
import javax.usb.UsbException;

import org.ykc.usbmanager.*;
import org.ykc.gutils.*;

public class USBTransferHandler implements Runnable{
	public static  final short VID = 0x04B4;
	public static  final short PID = 0x0072;
	static  final byte IN_EP_CC_DATA = (byte)0x81;
	static  final byte OUT_EP_CMD = (byte)0x3;
	static  final byte IN_EP_CMD_RESP = (byte)0x84;
	static  final int MAX_READ_SIZE = 65535;
	static  final int CC_PACKET_SIZE = 64;
	static  final int CC_PACKET_WORDS = CC_PACKET_SIZE / 4;
	private final int SHORT_PKT_SIZE = 8;
	private byte[] tempDataArray = new byte[MAX_READ_SIZE];
	private boolean isRunning = false;
	private ConcurrentLinkedQueue<Integer[]> dataQueue = new ConcurrentLinkedQueue<Integer[]>();
	private UsbDevice dev = null;
	private boolean dataTransferStopped = true;

	USBTransferHandler()
	{

	}

	USBTransferHandler(UsbDevice newDev)
	{
		dev = newDev;
	}

	public void setDevice(UsbDevice newDev)
	{
		dev = newDev;
	}

	public UsbDevice getDevice()
	{
		return dev;
	}

	public boolean start(boolean attachDetachEnable, long detachDebounce)
	{
		byte[] command = new byte[8];

		command[0] = 6;
		if(attachDetachEnable == true)
		{
			command[4] = 1;
		}
		command[6] = GenericHelper.uint32_get_b0(detachDebounce);
		command[7] = GenericHelper.uint32_get_b1(detachDebounce);


		if(USBManager.epXfer(dev, OUT_EP_CMD, command) > 0)
		{
			return true;
		}
		return false;
	}

	public boolean stop()
	{
		byte[] command = new byte[4];
		command[0] = 3;

		if(USBManager.epXfer(dev, OUT_EP_CMD, command) > 0)
		{
			return true;
		}
		return false;

	}

	public boolean setTigger(byte[] cmd)
	{
		if(USBManager.epXfer(dev, OUT_EP_CMD, cmd) > 0)
		{
			return true;
		}
		return false;
	}

	public boolean getVersion(byte[] readBuf)
	{
		byte[] command = new byte[4];
		command[0] = 4;

		if(USBManager.epXfer(dev, OUT_EP_CMD, command) <= 0)
		{
			return false;
		}

		if(USBManager.epXfer(dev, IN_EP_CMD_RESP, readBuf) == 4)
		{
			return true;
		}
		return false;
	}

	public boolean putDeviceInBootloadMode()
	{
		byte[] command = new byte[4];
		command[0] = 0x10;

		if(USBManager.epXfer(dev, OUT_EP_CMD, command) == 4)
		{
			return true;
		}
		return false;
	}

	private boolean checkInTransfer()
	{
		byte[] command = new byte[MAX_READ_SIZE];

		if(USBManager.epXfer(dev, IN_EP_CC_DATA, command) > 0)
		{
			return true;
		}
		return false;

	}

	public Queue<Integer[]> getDataQueue(){
		return dataQueue;
	}

	public void pause()
	{
		isRunning = false;
	}

	public void resume()
	{
		isRunning = true;
		dataTransferStopped = false;
	}

	@Override
	public void run() {

		try {
			Thread.sleep(10);
		} catch (InterruptedException e1) {
		}

		while(true)
		{
			if(isRunning)
			{
				try {
					int result = USBManager.epXfer(dev, IN_EP_CC_DATA, tempDataArray);
					if (result > SHORT_PKT_SIZE)
					{
						/* Store Data in list */
						storePkt(result);
						continue;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			else
			{
				dataTransferStopped = true;
			}

			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
			}

		}
	}

	public boolean isTransferStopped()
	{
		return dataTransferStopped;
	}

	private void storePkt(int length)
	{
		int pkt_count = length / CC_PACKET_SIZE;
        for (int i = 0; i < pkt_count; i++)
        {
            Integer[] tempMsg = new Integer[CC_PACKET_WORDS];
            extractPkt(tempDataArray, tempMsg, i* CC_PACKET_SIZE);
            dataQueue.add(tempMsg);
        }
	}

	private void extractPkt(byte[] mArray, Integer[] tempMsg, int idx)
	{
		for(int i = 0; i < CC_PACKET_WORDS; i++)
		{
			int index = idx + (i * 4);
			tempMsg[i] = GenericHelper.byteToInt(mArray[index], mArray[index + 1], mArray[index + 2], mArray[index + 3]);
		}
	}





}
