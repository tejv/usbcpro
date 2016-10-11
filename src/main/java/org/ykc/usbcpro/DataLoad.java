/*
 * Copyright (C) 2016 teju <tejendersheoran@gmail.com>
 * See license file for more information
*/

package org.ykc.usbcpro;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.ykc.gutils.*;

public class DataLoad implements Runnable{
	/* Table View Indices */
	public static final int DT_SNO_IDX = 0;
	public static final int DT_OK_IDX =  1;
	public static final int DT_SOP_IDX = 2;
	public static final int DT_MSG_IDX = 3;
	public static final int DT_MSGID_IDX = 4;
	public static final int DT_DATA_ROLE_IDX = 5;
	public static final int DT_PWR_ROLE_IDX = 6;
	public static final int DT_CNT_IDX =  7;
	public static final int DT_REV_IDX = 8;
	public static final int DT_DATA_IDX = 9;
	public static final int DT_DURATION_IDX = 10;
	public static final int DT_DELTA_IDX = 11;
	public static final int DT_VBUS_IDX = 12;
	public static final int DT_START_TIME_IDX = 13;
	public static final int DT_END_TIME_IDX = 14;

	/* Indices in raw packet */
	private static final int RAW_SNO_IDX = 0;
	private static final int RAW_HDR_IDX = 1;
	private static final int RAW_DATA_IDX = 2;
	private static final int RAW_VBUS_IDX = 13;
	private static final int RAW_START_TIME_IDX = 14;
	private static final int RAW_END_TIME_IDX = 15;
	private static final int RAW_DATA_PAYLOAD_SIZE = 11;
	private static final int MAX_LONG_LOG_COUNT = 10000;
	private static final int MAX_LONG_LOG_OFFSET = 4000;
	private static final int MAX_ROW_IN_ONE_MODEL = 5000;

	public static final String[] CTRL_MSG_TYPE = {"C_RSVD0", "GD_CRC", "GO_TO_MIN", "ACCEPT", "REJECT", "PING", "PS_RDY", "GET_SRC_CAP", "GET_SNK_CAP", "DR_SWAP", "PR_SWAP", "VCONN_SWAP", "WAIT", "SOFT_RESET", "C_RSVD14", "C_RSVD15", "NOT_SUPPORTED", "GET_SRC_CAP_EXT", "GET_SRC_STATUS", "FR_SWAP", "C_RSVD20", "C_RSVD21", "C_RSVD22", "C_RSVD23", "C_RSVD24", "C_RSVD25", "C_RSVD26", "C_RSVD27", "C_RSVD28", "C_RSVD29", "C_RSVD30", "C_RSVD31"};
	public static final String[] DATA_MSG_TYPE = {"D_RSVD0 ", "SRC_CAP", "REQUEST", "BIST", "SNK_CAP", "BAT_STATUS", "SRC_ALERT", "D_RSVD7", "D_RSVD8", "D_RSVD9", "D_RSVD10", "D_RSVD11", "D_RSVD12", "D_RSVD13", "D_RSVD14", "VDM", "D_RSVD16", "D_RSVD17", "D_RSVD18", "D_RSVD19", "D_RSVD20", "D_RSVD21", "D_RSVD22", "D_RSVD23", "D_RSVD24", "D_RSVD25", "D_RSVD26", "D_RSVD27", "D_RSVD28", "D_RSVD29", "D_RSVD30", "D_RSVD31" };
	public static final String[] EXTD_MSG_TYPE = {"E_RSVD0", "SRC_CAP_EXT", "SRC_STATUS", "GET_BAT_CAP", "GET_BAT_STATUS", "BAT_CAP", "GET_MANU_INFO", "MANU_INFO", "SECURITY_REQ", "SECURITY_RESP", "E_RSVD9", "E_RSVD10", "E_RSVD11", "E_RSVD12", "E_RSVD13", "E_RSVD14", "E_RSVD15", "E_RSVD16", "E_RSVD17", "E_RSVD19", "E_RSVD20", "E_RSVD21", "E_RSVD22", "E_RSVD23", "E_RSVD24", "E_RSVD25", "E_RSVD26", "E_RSVD27", "E_RSVD28", "E_RSVD29", "E_RSVD30", "E_RSVD31" };

	public static final String[] columns = {"#", "Ok", "SOP", "Message", "Msg ID", "Data Role", "Power Role", "Count", "Rev", "Data","Duration(us)", "Delta(us)", "VBUS(mV)", "Start Time(us)", "End Time(us)"};
	private static  String[][] databaseInfo;

	private JTable table;
	private DefaultTableModel currentModel ;
	private ConcurrentLinkedQueue<DefaultTableModel> modelQueue = new ConcurrentLinkedQueue<DefaultTableModel>();
	private TableColumnManager tColManager;
	private Queue<Integer[]> rawQueue;
	private boolean isRunning = true;
	private boolean isLongLogging = true;
	private int extdByteCount = 0;
	private boolean isExtendedPart;
	private DynamicSaveData saveHandler;

	DataLoad(JTable newTable, Queue<Integer[]> rawQueue)
	{
		this.table = newTable;
		this.rawQueue = rawQueue;
		initTable();
		saveHandler = new DynamicSaveData(modelQueue);
		Thread saveThread = new Thread(saveHandler);
		saveThread.start();
	}

	private void initTable()
	{
		USBCProTableModel currentModel = new USBCProTableModel(databaseInfo, columns);
		table.setModel(currentModel);

		//tableDataView.setAutoCreateRowSorter(true); // Auto sorting but need to override as it do string based sorting
		TableColumnModel columnModel = table.getColumnModel();

		columnModel.getColumn(DT_SNO_IDX).setPreferredWidth(35);
		columnModel.getColumn(DT_OK_IDX).setPreferredWidth(5);
		columnModel.getColumn(DT_SOP_IDX).setPreferredWidth(40);
		columnModel.getColumn(DT_MSG_IDX).setPreferredWidth(50);
		columnModel.getColumn(DT_MSGID_IDX).setPreferredWidth(10);
		columnModel.getColumn(DT_DATA_ROLE_IDX).setPreferredWidth(15);
		columnModel.getColumn(DT_PWR_ROLE_IDX).setPreferredWidth(25);
		columnModel.getColumn(DT_CNT_IDX).setPreferredWidth(5);
		columnModel.getColumn(DT_REV_IDX).setPreferredWidth(10);
		columnModel.getColumn(DT_DATA_IDX).setPreferredWidth(700);
		columnModel.getColumn(DT_DURATION_IDX).setPreferredWidth(20);
		columnModel.getColumn(DT_DELTA_IDX).setPreferredWidth(20);
		columnModel.getColumn(DT_VBUS_IDX).setPreferredWidth(20);
		columnModel.getColumn(DT_START_TIME_IDX).setPreferredWidth(0);
		columnModel.getColumn(DT_END_TIME_IDX).setPreferredWidth(0);

		tColManager = new TableColumnManager(table);
		tColManager.hideColumn(DT_END_TIME_IDX);
		tColManager.hideColumn(DT_START_TIME_IDX);
	}

	public void setLongLogging(boolean isLongLogging)
	{
		this.isLongLogging = isLongLogging;
	}

	public void pause()
	{
		isRunning = false;
	}

	public void resume()
	{
		isRunning = true;
	}

	public boolean save()
	{
		/* TODO stop run task */
//		modelQueue.add(currentModel);
//		initTable();
//		try {
//			Thread.sleep(500);
//		} catch (InterruptedException e) {
//		}
		if(FileConversionUtils.exportToUC(currentModel) == true)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public void run() {
		int sleep_counter = 0;
		while(true)
		{
			if(isRunning)
			{
				while(rawQueue.isEmpty() == false)
				{
					sleep_counter++;
					if(sleep_counter > 1000)
					{
						sleep_counter = 0;
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
						}
					}
					try {
						handleLongLogging();

						Integer[] tRow;
						tRow = rawQueue.peek();
						addToModel(tRow);
						rawQueue.remove();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			try {
				Thread.sleep(350);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
			}
		}
	}

	void handleLongLogging()
	{
		if(isLongLogging == true)
		{
			DefaultTableModel x = (DefaultTableModel) table.getModel();
			int rowCount = x.getRowCount();
			if(rowCount >= MAX_LONG_LOG_COUNT)
			{
				for(int i = 0; i < (rowCount - MAX_LONG_LOG_OFFSET); i++)
				{
					x.removeRow(0);
				}
			}
		}
	}

	void addToModel(Integer[] cRow)
	{
		Object[] tempRow;
		try {
			DefaultTableModel x = (DefaultTableModel) table.getModel();
			tempRow = parsePkt(cRow);
			/* TODO: One bug here if extended message get the boundary */
			if(x.getRowCount() <= MAX_ROW_IN_ONE_MODEL)
			{
				x.addRow(tempRow);
			}
			else
			{
				modelQueue.add(x);
				initTable();
			}
		} catch (NoLoadException e) {

		}
	}

	String[] parsePkt(Integer[] row) throws NoLoadException
	{
		String[] tempRow = new String[]{"", "", "", "", "", "", "", "", "", "", "", "0", "", "", "" };

		/* Start Time */
		Long startTime = GenericHelper.getUnsignedInt(row[RAW_START_TIME_IDX]);
		tempRow[DT_START_TIME_IDX] = Long.toString(startTime);

		/* Delta */
		int mTableRowCount = table.getModel().getRowCount();
		if(mTableRowCount != 0)
		{
			Long endTime;
			try
			{
				endTime = Long.parseLong((String)table.getModel().getValueAt((mTableRowCount - 1), DT_END_TIME_IDX));
			}
			catch(NumberFormatException ex)
			{
				endTime = 0L;
			}
			Long delta = startTime - endTime;
			tempRow[DT_DELTA_IDX] = NumberFormat.getNumberInstance(Locale.US).format(delta);
		}

		/* End Time */
		tempRow[DT_END_TIME_IDX] = Long.toString(GenericHelper.getUnsignedInt(row[RAW_END_TIME_IDX]));

		/* Duration */
		Long duration = GenericHelper.getUnsignedInt(row[RAW_END_TIME_IDX]) - GenericHelper.getUnsignedInt(row[RAW_START_TIME_IDX]) ;
		tempRow[DT_DURATION_IDX] = NumberFormat.getNumberInstance(Locale.US).format(duration);

		/* VBus */
		Long vbus =  GenericHelper.getUnsignedInt(row[RAW_VBUS_IDX]) ;
		vbus = (vbus  * 5000 * 11) / 255;
		tempRow[DT_VBUS_IDX] = NumberFormat.getNumberInstance(Locale.US).format(vbus);

		/* Sno */
		Long hdr = GenericHelper.getUnsignedInt(row[RAW_HDR_IDX]);
		Long sno = GenericHelper.getUnsignedInt(row[RAW_SNO_IDX]);
		if(PDHelper.get_field_pkt_type(hdr) == PDHelper.enumPktType.VOLT_PKT)
		{
			PDHelper.enumVoltPktEvt pevt =  PDHelper.enumVoltPktEvt.values()[row[RAW_SNO_IDX]];
			tempRow[DT_OK_IDX] = pevt.name();
			return tempRow;
		}

		tempRow[DT_SNO_IDX] = Long.toString(sno);

		/* Ok */
		String ok = "Ok";
		if(PDHelper.get_field_ok(hdr) == false)
		{
			ok = "ER";
			if(PDHelper.get_field_crc_error(hdr) == true)
			{
				ok = ok + "_CRC";
			}
			if(PDHelper.get_field_eop_error(hdr) == true)
			{
				ok = ok + "_EOP";
			}
		}
		tempRow[DT_OK_IDX] = ok;

		/* SOP */
		PDHelper.enumSOPType sopType =  PDHelper.get_field_sop_type(hdr);
		tempRow[DT_SOP_IDX] = sopType.name();

		if((sopType == PDHelper.enumSOPType.HARD_RESET) || (sopType == PDHelper.enumSOPType.CABLE_RESET))
		{
			return tempRow;
		}

		/* Data */
		isExtendedPart = false;
		tempRow[DT_DATA_IDX] = getData(hdr, row);

		if(isExtendedPart == true)
		{
			DefaultTableModel m = (DefaultTableModel) table.getModel();
			try {
				String x = (String) m.getValueAt(m.getRowCount() - 1, DT_DATA_IDX);
				x = x + tempRow[DT_DATA_IDX];
				m.setValueAt(x, m.getRowCount() - 1, DT_DATA_IDX);
			} catch (Exception e) {
				throw new NoLoadException();
			}

			throw new NoLoadException();
		}
		/* Message */
		tempRow[DT_MSG_IDX] = PDHelper.get_field_msg_type(hdr);

		/* Message ID */
		tempRow[DT_MSGID_IDX] = Integer.toString((PDHelper.get_field_msg_id(hdr)));

		/* Data Role */
		tempRow[DT_DATA_ROLE_IDX] = PDHelper.get_field_data_role(hdr);

		/* Power role */
		tempRow[DT_PWR_ROLE_IDX] = PDHelper.get_field_power_role(hdr);

		/* Count */
		tempRow[DT_CNT_IDX] = Integer.toString((PDHelper.get_field_msg_count(hdr)));

		/* Rev */
		tempRow[DT_REV_IDX] = (PDHelper.get_field_spec_rev(hdr)).name();


		return tempRow;
	}

	private String getData(Long hdr, Integer[] pRow)
	{
        /* Add PD Header */
        StringBuilder dat_str = new StringBuilder("");
        int chunk_no = PDHelper.get_field_chunk_no(hdr);

        int ext_hdr = PDHelper.get_extended_hdr(pRow[RAW_DATA_IDX]);

        boolean is_extended = PDHelper.get_field_extended(hdr);
        boolean is_chunked = PDHelper.get_field_is_chunked(ext_hdr);

        if (((is_extended == false) || ((is_extended == true) && (is_chunked == true))) && (chunk_no == 0))
        {
            extdByteCount = 0;
            dat_str.append("0x" + (Integer.toHexString(PDHelper.get_pd_hdr(hdr)).toUpperCase()) + " ");
            int msg_count = PDHelper.get_field_msg_count(hdr);
            for (int i = 0; i < msg_count; i++)
            {
                dat_str.append("0x" + Long.toHexString(GenericHelper.getUnsignedInt(pRow[i + RAW_DATA_IDX])).toUpperCase() + " ");
            }
        }
        else
        {

            if (chunk_no == 0)
            {
                dat_str.append("0x" + (Integer.toHexString(PDHelper.get_pd_hdr(hdr)).toUpperCase()) + " ");
                extdByteCount = PDHelper.get_field_extended_count(ext_hdr) + 2;
            }
            else
            {
            	isExtendedPart = true;
            }

            String temp = "";
            for (int i = 0; i < RAW_DATA_PAYLOAD_SIZE; i++)
            {
                if (extdByteCount == 0)
                {
                    break;
                }

                Long x = GenericHelper.getUnsignedInt(pRow[i + RAW_DATA_IDX]);
                dat_str.append("0x");

                temp = "";

                for (int j = 0; j < 4; j++)
                {
                	String byteHolder = Long.toHexString(((x >>> (j << 3)) & 0xFF)).toUpperCase();
                	int len = byteHolder.length();

                	if(len == 1)
                	{
                		byteHolder = "0" + byteHolder;
                	}
                    temp = byteHolder + temp;

                    extdByteCount--;

                    if (extdByteCount == 0)
                    {
                        dat_str.append(temp);
                        temp = "";
                        break;
                    }
                }
                dat_str.append(temp + " ");
            }

        }


        return dat_str.toString();
	}

	class NoLoadException extends Exception{

	}

	class USBCProTableModel extends DefaultTableModel implements Serializable {
		private static final long serialVersionUID = 1L;
		public USBCProTableModel(String[][] databaseInfo, String[] columns) {
			super(databaseInfo,columns);
		}

		@Override
        public Class getColumnClass(int column) {
            Class returnValue;
           // Verifying that the column exists (index > 0 && index < number of columns

            if ((column >= 0) && (column < getColumnCount())) {
              returnValue = getValueAt(0, column).getClass();
            } else {

            // Returns the class for the item in the column
            	returnValue = Object.class;
            }
            return returnValue;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
           //all cells false
           return false;
        }

	}

}
