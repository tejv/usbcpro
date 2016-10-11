/*
 * Copyright (C) 2016 teju <tejendersheoran@gmail.com>
 * See license file for more information
*/

package org.ykc.usbcpro;

import java.awt.EventQueue;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import javax.swing.JTable;
import javax.usb.UsbDevice;
import javax.usb.UsbException;

import org.ykc.usbmanager.USBManEvent;
import org.ykc.usbmanager.USBManListener;
import org.ykc.usbmanager.USBManager;
import org.ykc.gutils.*;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.Toolkit;
import java.awt.SystemColor;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.DefaultCaret;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.JTree;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;


public class USBCPro implements USBManListener{

	private JFrame frmUsbcproPdAnalyzer;

	static private USBTransferHandler ccDataTask;
	static private DataLoad dataLoadTask;
	private boolean isAttached = false;
	private boolean isCapturing = false;
	private boolean isLongLogging = true;

    //private TableRowSorter<DefaultTableModel>  sorter = new TableRowSorter<DefaultTableModel>(dTableModel);

    private JTable tableDataView;
    private JCheckBoxMenuItem chckbxmntmLongLogging;
    private JCheckBoxMenuItem chckbxmntmAdvancedOptions;
    private JSplitPane splitPane;
	private JCheckBoxMenuItem chckbxmntmAttachdetachEnable;
	private JTextArea txtrLog;
	private JTextField textFieldTrgStart;
	private JTextField textFieldTrgEnd;
	private JTextField textFieldTrgCount;
	private JTextField textFieldTrgMsgId;
	private JComboBox comboBoxMsgClass;
	private JComboBox comboBoxMsgType;
	private JCheckBox chckbxTrgStart;
	private JCheckBox chckbxTrgEnd;
	private JCheckBox chckbxTrgSop;
	private JCheckBox chckbxTrgMsgClass;
	private JCheckBox chckbxTrgMsgType;
	private JCheckBox chckbxTrgCount;
	private JCheckBox chckbxTrgMsgId;
	private JComboBox comboBoxSopType;
	private JComboBox comboBoxDevice;
	private USBManager usbm;
	private UsbDevice dev;
	private ArrayList<UsbDevice> devList;
	private int comboBoxDeviceSelIdx = -1;
	private JTextField textFieldDetachDebounce;
	private ArrayList<JLabel> filterLabelList = new ArrayList<JLabel>();
	private ArrayList<JTextField> filterTextBoxList = new ArrayList<JTextField>();
	private JPanel panel_filter;
	private JScrollPane scrollPaneFilter;
	private JPanel panel_filter_actual;
	private JButton setFilter;
	private JButton clearFilter;
	private FilterKeyListener  filterListener = new FilterKeyListener();
	JTabbedPane tabbedPane;
	private JScrollPane scrollPaneData;
	private JTextArea textAreaData;
	private JScrollPane scrollPaneMsgTree;
	private JTree tree;
	private static long startTime = 0;
	private JLabel lblStartDelta;
	private DefaultTableModel tableModel;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {

		USBCPro window = null;
		try {
			window = new USBCPro();
		} catch (UsbException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		window.frmUsbcproPdAnalyzer.setVisible(true);

		Thread ccThread = new Thread(ccDataTask);
		ccThread.start();
		dataLoadTask = new DataLoad(window.tableDataView, ccDataTask.getDataQueue());

		if(args.length > 0) {
            File file = new File(args[0]);
            if(FileConversionUtils.isFileUC(file) ==  true)
            {
            	if(FileConversionUtils.importFromUC((DefaultTableModel) window.tableDataView.getModel(), args[0]) == true)
            	{
            		window.txtrLog.append("File Open success\n");
            	}
            	else
            	{
            		window.txtrLog.append("File Open failed\n");
            	}
            }
            else
            {
            	window.txtrLog.append("File not a UC file\n");
            }
        }
		Thread dataLoadThread = new Thread(dataLoadTask);
		dataLoadThread.start();

		while(true);
	}

	/**
	 * Create the application.
	 * @throws UsbException
	 */
	public USBCPro() throws UsbException {
//		setUIFont (new javax.swing.plaf.FontUIResource("Serif",Font.PLAIN,14));
//		setUIFont (new javax.swing.plaf.FontUIResource("Arial",Font.PLAIN,12));
		initialize();
//		form_filter_interface();
		//tableDataView.setRowSorter(sorter);
		tableDataView.setRowSelectionAllowed(true);
		DefaultCaret caret = (DefaultCaret)txtrLog.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		ccDataTask = new USBTransferHandler();
		usbm = USBManager.getInstance();
		updateDeviceList();
		usbm.addUSBManListener(this);




		autoScrollToLastJtable(tableDataView);

		KeyboardFocusManager keyManager;

		keyManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		keyManager.addKeyEventDispatcher(new KeyEventDispatcher() {

		  @Override
		  public boolean dispatchKeyEvent(KeyEvent e) {
			boolean result = false;
			if(e.isControlDown() == true)
			{
				if(e.getID()== KeyEvent.KEY_RELEASED) {
					result = true;
			    	switch(e.getKeyCode())
			    	{
			    	case KeyEvent.VK_R:
			    		resetClick();
			    		break;
			    	case KeyEvent.VK_C:
			    		clearDataTable(tableDataView.getModel());
			    		break;
			    	case KeyEvent.VK_D:
			    		putDevInBoot();
			    		break;
			    	case KeyEvent.VK_V:
			    		getVersion();
			    		break;
			    	case KeyEvent.VK_L:
			    		scrollToLastJtable(tableDataView);
			    		break;
			    	case KeyEvent.VK_F:
			    		scrollToFirstJtable(tableDataView);
			    		break;
			    	case KeyEvent.VK_X:
			    		startStopClick();
			    		break;
			    	case KeyEvent.VK_O:
			    		openFile();
			    		break;
			    	case KeyEvent.VK_S:
			    		saveFile();
			    		break;
			    	default:
			    		result = false;
			    		break;

			    	}
				}
			}

		    return result;
		  }
		  });

		/* File Drop */
		new FileDrop( null, splitPane, /*dragBorder,*/ new FileDrop.Listener()
        {
			public void filesDropped( File[] files )
            {
				for( int i = 0; i < files.length; i++ )
                {
            		if(FileConversionUtils.isFileUC(files[i]) == true)
            		{
            			try
            			{
							if(FileConversionUtils.importFromUC((DefaultTableModel) tableDataView.getModel(), files[i].getCanonicalPath()) == true)
							{
								txtrLog.append("File Open success\n");
								break;
							}
							else
							{
								txtrLog.append("File Open failed\n");
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

            		}
                }
            }
        });

	}

	/**
	 * Initialize the contents of the frame.
	 */
	@SuppressWarnings({ "serial", "rawtypes" })
	private void initialize() {
		frmUsbcproPdAnalyzer = new JFrame();
		//frmUsbcproPdAnalyzer.setIconImage(Toolkit.getDefaultToolkit().getImage(USBCPro.class.getResource("icons/1459622839_arrow_right.ico_128x128.png")));
		frmUsbcproPdAnalyzer.setTitle("USBCPro PD Analyzer");
		frmUsbcproPdAnalyzer.setBounds(100, 100, 1185, 724);
		frmUsbcproPdAnalyzer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JMenuBar menuBar = new JMenuBar();
		menuBar.setForeground(SystemColor.text);
		menuBar.setBackground(Color.WHITE);
		frmUsbcproPdAnalyzer.setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmOpen = new JMenuItem("Open         'Ctrl-O'");
		mntmOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				openFile();
			}
		});
		mnFile.add(mntmOpen);

		JMenuItem mntmSave = new JMenuItem("Save          'Ctrl-S'");
		mntmSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				saveFile();
			}
		});
		mnFile.add(mntmSave);

		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				/* TODO: Safely disable threads and any open files */
				System.exit(0);
			}
		});
		mnFile.add(mntmExit);

		JMenu mnView = new JMenu("View");
		menuBar.add(mnView);

		chckbxmntmAdvancedOptions = new JCheckBoxMenuItem("Advanced Options");
		chckbxmntmAdvancedOptions.setSelected(true);
		chckbxmntmAdvancedOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(chckbxmntmAdvancedOptions.isSelected() == true)
				{
					splitPane.setDividerLocation(0.7d);
				}
				else
				{
					splitPane.setDividerLocation(1.0d);
				}

			}
		});
		mnView.add(chckbxmntmAdvancedOptions);

		chckbxmntmLongLogging = new JCheckBoxMenuItem("Long Logging");
		chckbxmntmLongLogging.setSelected(true);
		chckbxmntmLongLogging.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				isLongLogging = chckbxmntmLongLogging.isSelected();
				dataLoadTask.setLongLogging(isLongLogging);
			}
		});
		mnView.add(chckbxmntmLongLogging);

		chckbxmntmAttachdetachEnable = new JCheckBoxMenuItem("Attach/Detach Enable");
		mnView.add(chckbxmntmAttachdetachEnable);

		JMenu mnActions = new JMenu("Actions");
		menuBar.add(mnActions);

		JMenuItem mntmStartstopCapture = new JMenuItem("Start/Stop Capture   'Ctrl-X'");
		mntmStartstopCapture.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				startStopClick();

			}
		});
		mnActions.add(mntmStartstopCapture);

		JMenuItem mntmReset = new JMenuItem("Reset                           'Ctrl-R'");
		mntmReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				resetClick();
			}
		});
		mnActions.add(mntmReset);

		JMenuItem mntmDownloadFw = new JMenuItem("Download FW            'Ctrl-D'");
		mntmDownloadFw.setEnabled(false);
		mntmDownloadFw.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				putDevInBoot();
			}
		});
		mnActions.add(mntmDownloadFw);

		JMenuItem mntmGetHwfwVer = new JMenuItem("Get HW/FW Ver         'Ctrl-V'");
		mntmGetHwfwVer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				getVersion();
			}
		});
		mnActions.add(mntmGetHwfwVer);

		JMenuItem mntmClearData = new JMenuItem("Clear Data                  'Ctrl-C'");
		mntmClearData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				clearDataTable(tableDataView.getModel());
			}
		});
		mnActions.add(mntmClearData);

		JMenuItem mntmScrollToLast = new JMenuItem("Scroll to Last             'Ctrl-L'");
		mntmScrollToLast.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				scrollToLastJtable(tableDataView);
			}
		});
		mnActions.add(mntmScrollToLast);

		JMenuItem mntmScrollToFirst = new JMenuItem("Scroll to First             'Ctrl-F'");
		mntmScrollToFirst.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				scrollToFirstJtable(tableDataView);
			}
		});
		mnActions.add(mntmScrollToFirst);

		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JOptionPane.showMessageDialog(frmUsbcproPdAnalyzer, "USBCPro v2.0.0.0\nAuthor: Tejender Sheoran\nEmail: tejendersheoran@gmail.com \nCopyright(C) (2016) Tejender Sheoran\nThis program is free software:\nYou can redistribute it and/or modify it under the terms of the\nGNU General Public License Ver 3<http://www.gnu.org/licenses/>","About Me",
					    JOptionPane.INFORMATION_MESSAGE);
			}
		});
		mnHelp.add(mntmAbout);

		comboBoxDevice = new JComboBox();
		Dimension dm = comboBoxDevice.getPreferredSize();
		dm.width = 230;
		comboBoxDevice.setMaximumSize(dm );
		comboBoxDevice.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int selIdx = comboBoxDevice.getSelectedIndex();

				if((selIdx >= 0) &&(selIdx != comboBoxDeviceSelIdx))
				{
					comboBoxDeviceSelIdx = selIdx;

					dev = devList.get(comboBoxDeviceSelIdx);
					txtrLog.append("Device Selection changed to " + dev.toString() + "\n");
					ccDataTask.setDevice(dev);
					txtrLog.append("USBCPro HW Attached \n");
					txtrLog.append("Please make sure to use libusb drivers otherwise further communication with device will fail. \n");
					isAttached = true;

				}
			}
		});
		menuBar.add(comboBoxDevice);

		lblStartDelta = new JLabel("     Start Delta(us): 0");
		menuBar.add(lblStartDelta);

		splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.8);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		frmUsbcproPdAnalyzer.getContentPane().add(splitPane, BorderLayout.CENTER);

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		splitPane.setRightComponent(tabbedPane);

		JPanel panel_log = new JPanel();
		tabbedPane.addTab("Log Window", null, panel_log, null);
		panel_log.setLayout(new BorderLayout(0, 0));

		txtrLog = new JTextArea();
		panel_log.add(txtrLog, BorderLayout.CENTER);

		JScrollPane scrollPaneLog = new JScrollPane(txtrLog);
		panel_log.add(scrollPaneLog, BorderLayout.CENTER);

		JPanel panel_trigger = new JPanel();
		tabbedPane.addTab("Triggers", null, panel_trigger, null);
		panel_trigger.setLayout(null);

		chckbxTrgStart = new JCheckBox("Trg St no");
		chckbxTrgStart.setBounds(6, 7, 97, 23);
		panel_trigger.add(chckbxTrgStart);

		textFieldTrgStart = new JTextField();
		textFieldTrgStart.setText("0");
		textFieldTrgStart.setBounds(114, 8, 86, 20);
		panel_trigger.add(textFieldTrgStart);
		textFieldTrgStart.setColumns(10);

		chckbxTrgEnd = new JCheckBox("Trg End no");
		chckbxTrgEnd.setBounds(218, 7, 97, 23);
		panel_trigger.add(chckbxTrgEnd);

		textFieldTrgEnd = new JTextField();
		textFieldTrgEnd.setText("0");
		textFieldTrgEnd.setBounds(321, 8, 86, 20);
		panel_trigger.add(textFieldTrgEnd);
		textFieldTrgEnd.setColumns(10);

		chckbxTrgSop = new JCheckBox("Trg SOP");
		chckbxTrgSop.setBounds(442, 7, 97, 23);
		panel_trigger.add(chckbxTrgSop);

		chckbxTrgMsgClass = new JCheckBox("Trg Msg Class");
		chckbxTrgMsgClass.setBounds(6, 38, 130, 23);
		panel_trigger.add(chckbxTrgMsgClass);

		comboBoxMsgType = new JComboBox();
		comboBoxMsgType.setModel(new DefaultComboBoxModel(new String[] {"C_RSVD0", "GD_CRC", "GO_TO_MIN", "ACCEPT", "REJECT", "PING", "PS_RDY", "GET_SRC_CAP", "GET_SNK_CAP", "DR_SWAP", "PR_SWAP", "VCONN_SWAP", "WAIT", "SOFT_RESET", "C_RSVD14", "C_RSVD15", "NOT_SUPPORTED", "GET_SRC_CAP_EXT", "GET_SRC_STATUS", "FR_SWAP", "C_RSVD20", "C_RSVD21", "C_RSVD22", "C_RSVD23", "C_RSVD24", "C_RSVD25", "C_RSVD26", "C_RSVD27", "C_RSVD28", "C_RSVD29", "C_RSVD30", "C_RSVD31"}));
		comboBoxMsgType.setBounds(526, 39, 130, 20);
		panel_trigger.add(comboBoxMsgType);


		comboBoxMsgClass = new JComboBox();
		comboBoxMsgClass.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				switch(comboBoxMsgClass.getSelectedItem().toString())
				{
				case "CONTROL":
					comboBoxMsgType.removeAllItems();
					for(int i = 0 ; i < DataLoad.CTRL_MSG_TYPE.length; i++)
					{
						comboBoxMsgType.addItem(DataLoad.CTRL_MSG_TYPE[i]);
					}

					break;
				case "DATA":
					comboBoxMsgType.removeAllItems();
					for(int i = 0 ; i < DataLoad.DATA_MSG_TYPE.length; i++)
					{
						comboBoxMsgType.addItem(DataLoad.DATA_MSG_TYPE[i]);
					}
					break;
				case "EXTENDED":
					comboBoxMsgType.removeAllItems();
					for(int i = 0 ; i < DataLoad.EXTD_MSG_TYPE.length; i++)
					{
						comboBoxMsgType.addItem(DataLoad.EXTD_MSG_TYPE[i]);
					}
					break;
				default:
					break;
				}
			}
		});
		comboBoxMsgClass.setModel(new DefaultComboBoxModel(new String[] {"CONTROL", "DATA", "EXTENDED"}));
		comboBoxMsgClass.setBounds(158, 39, 136, 20);
		panel_trigger.add(comboBoxMsgClass);

		chckbxTrgMsgType = new JCheckBox("Trg Msg Type");
		chckbxTrgMsgType.setBounds(321, 38, 115, 23);
		panel_trigger.add(chckbxTrgMsgType);


		chckbxTrgCount = new JCheckBox("Trg Count");
		chckbxTrgCount.setBounds(6, 73, 97, 23);
		panel_trigger.add(chckbxTrgCount);

		textFieldTrgCount = new JTextField();
		textFieldTrgCount.setText("0");
		textFieldTrgCount.setBounds(114, 74, 86, 20);
		panel_trigger.add(textFieldTrgCount);
		textFieldTrgCount.setColumns(10);

		chckbxTrgMsgId = new JCheckBox("Trg Msg Id");
		chckbxTrgMsgId.setBounds(218, 73, 97, 23);
		panel_trigger.add(chckbxTrgMsgId);

		textFieldTrgMsgId = new JTextField();
		textFieldTrgMsgId.setText("0");
		textFieldTrgMsgId.setBounds(321, 74, 86, 20);
		panel_trigger.add(textFieldTrgMsgId);
		textFieldTrgMsgId.setColumns(10);

		JButton btnSetTrigger = new JButton("Set Trigger");
		btnSetTrigger.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setTrigger();
			}
		});
		btnSetTrigger.setBounds(557, 73, 99, 23);
		panel_trigger.add(btnSetTrigger);

		comboBoxSopType = new JComboBox();
		comboBoxSopType.setModel(new DefaultComboBoxModel(new String[] {"SOP", "SOP_PRIME", "SOP_DRPIME", "SOP_P_DBG", "SOP_DP_DBG", "HARD_RESET", "CABLE_RESET"}));
		comboBoxSopType.setBounds(559, 8, 97, 20);
		panel_trigger.add(comboBoxSopType);

		JLabel lblDetachDebounce = new JLabel("Detach Debounce");
		lblDetachDebounce.setBounds(694, 9, 118, 19);
		panel_trigger.add(lblDetachDebounce);

		textFieldDetachDebounce = new JTextField();
		textFieldDetachDebounce.setText("8000");
		textFieldDetachDebounce.setBounds(822, 8, 86, 20);
		panel_trigger.add(textFieldDetachDebounce);
		textFieldDetachDebounce.setColumns(10);

		panel_filter = new JPanel();
		tabbedPane.addTab("Filters", null, panel_filter, null);
		tabbedPane.setEnabledAt(2, false);
		panel_filter.setLayout(new BorderLayout(0,0));

        panel_filter_actual = new JPanel();

		scrollPaneFilter = new JScrollPane(panel_filter_actual);
		scrollPaneFilter.setEnabled(false);
		panel_filter_actual.setLayout(new GridLayout(8,8,10,10));
		panel_filter_actual.setBorder(BorderFactory.createEmptyBorder(0,10,10,10));
		panel_filter.add(scrollPaneFilter);


		JSplitPane splitPane_1 = new JSplitPane();
		splitPane_1.setResizeWeight(0.8);
		splitPane.setLeftComponent(splitPane_1);

		JSplitPane splitPaneData = new JSplitPane();
		splitPaneData.setResizeWeight(0.8);
		splitPaneData.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPane_1.setRightComponent(splitPaneData);

		scrollPaneData = new JScrollPane();
		splitPaneData.setRightComponent(scrollPaneData);

		textAreaData = new JTextArea();
		textAreaData.setWrapStyleWord(true);
		textAreaData.setLineWrap(true);
		textAreaData.setEnabled(true);
		textAreaData.setEditable(false);
		textAreaData.setForeground(Color.BLACK);
		textAreaData.setFont(new Font("Consolas", Font.PLAIN, 14));
		textAreaData.setBackground(Color.WHITE);
		textAreaData.setText("");
		scrollPaneData.setViewportView(textAreaData);

		scrollPaneMsgTree = new JScrollPane();
		splitPaneData.setLeftComponent(scrollPaneMsgTree);

		tree = new JTree();
		tree.setModel(null);
		scrollPaneMsgTree.setViewportView(tree);

		tableDataView = new JTable()
		{
			 public boolean getScrollableTracksViewportWidth() {
				   return getPreferredSize().width < getParent().getWidth();
				 }
			 @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (isRowSelected(row))
                {
                	return c;
                }

            	c.setBackground(super.getBackground());
                c.setForeground(super.getForeground());

                int modelColumn = convertColumnIndexToModel(col);

                if((modelColumn == DataLoad.DT_OK_IDX) )
                {
	                String status = (String)getValueAt(row, col);

	                switch(status)
	                {
	                case "Ok":
	                    c.setBackground(Color.GREEN);
	                    c.setForeground(Color.BLACK);
	                	break;
	                case "ER_CRC":
	                    c.setBackground(Color.YELLOW);
	                    c.setForeground(Color.BLACK);
	                	break;
	                case "ER":
	                case "ER_CRC_EOP":
	                    c.setBackground(Color.RED);
	                    c.setForeground(Color.WHITE);
	                	break;
	                case "ER_EOP":
	                    c.setBackground(Color.BLUE);
	                    c.setForeground(Color.WHITE);
	                	break;
	                case "VBUS_DN":
	                    c.setBackground(Color.ORANGE);
	                    c.setForeground(Color.BLACK);
	                	break;
	                case "VBUS_UP":
	                    c.setBackground(Color.CYAN);
	                    c.setForeground(Color.BLACK);
	                	break;

	                case "CC1_DEF":
	                case "CC1_1_5A":
	                case "CC1_3A":
	                case "CC2_DEF":
	                case "CC2_1_5A":
	                case "CC2_3A":

	                    c.setBackground(Color.PINK);
	                    c.setForeground(Color.BLACK);
	            		break;
	                case "DETACH":
	                    c.setBackground(Color.DARK_GRAY);
	                    c.setForeground(Color.WHITE);
	                	break;
	                default:
	                }
                }
                else if(modelColumn == DataLoad.DT_MSG_IDX)
                {
                	String status = (String)getValueAt(row, col);
                	if(status.contains("_RSVD"))
        			{
	                    c.setBackground(Color.ORANGE);
	                    c.setForeground(Color.BLACK);
        			}
                }
                return c;
            }
		};
		tableDataView.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();
				if( (keyCode == KeyEvent.VK_UP) || (keyCode == KeyEvent.VK_DOWN))
				{
					int row = tableDataView.getSelectedRow();
			        if (row >= 0 ) {
			            rowClicked(row);
			        }
				}
			}
		});
		tableDataView.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				int row = tableDataView.rowAtPoint(evt.getPoint());
		        int col = tableDataView.columnAtPoint(evt.getPoint());
		        if (row >= 0 && col >= 0) {
		            rowClicked(row);
		        }
			}
		});
//		tableDataView.setFont(new Font("Consolas", Font.PLAIN, 14));
		tableDataView.setFont(new javax.swing.plaf.FontUIResource("Consolas",Font.PLAIN,14));

		tableDataView.setForeground(Color.WHITE);
		tableDataView.setBackground(Color.DARK_GRAY);
		tableDataView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tableDataView.setPreferredScrollableViewportSize(tableDataView.getPreferredSize());

		JScrollPane scrollPaneDataTable = new JScrollPane(tableDataView);
		splitPane_1.setLeftComponent(scrollPaneDataTable);
	}

	private void start()
	{
		clearFilters();
		tabbedPane.setEnabledAt(2, false);
		tabbedPane.setSelectedIndex(0);

		boolean result;
		clearDataTable(tableDataView.getModel());
		Long detachDebounce = (long) 6000;
		try {
			detachDebounce= Long.parseLong(textFieldDetachDebounce.getText());
		} catch (NumberFormatException e) {
			detachDebounce = (long) 6000;
		}

		result = ccDataTask.start(chckbxmntmAttachdetachEnable.isSelected(), detachDebounce);
		if(result == true)
		{
			isCapturing = true;
			ccDataTask.resume();
			txtrLog.append("Start Command Success \n");
		}
		else
		{
			txtrLog.append("Start Command Fail \n");
		}
	}

	private void stop()
	{
		ccDataTask.pause();
		while(true)
		{
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
			if(ccDataTask.isTransferStopped() == true)
			{
				boolean result;
				result = ccDataTask.stop();

				if(result == true)
				{
					isCapturing = false;
					tabbedPane.setEnabledAt(2, true);

					txtrLog.append("Stop Command Success \n");
				}
				else
				{
					txtrLog.append("Stop Command Fail \n");
				}
				break;
			}

		}
	}

	private void startStopClick()
	{
		if(isAttached == true)
		{
			if(isCapturing == false)
			{
				start();
			}
			else
			{
				stop();
			}
		}
		else
		{
			txtrLog.append("USBCPro HW not connected \n");
		}
	}

	private void resetClick()
	{
		if(isAttached == true)
		{
			stop();
			start();
		}
		else
		{
			txtrLog.append("USBCPro HW not connected \n");
		}
	}

	public void autoScrollToLastJtable(final JTable table)
	{
		table.addComponentListener(new ComponentAdapter() {
	        public void componentResized(ComponentEvent e) {
	            int lastIndex = table.getRowCount()-1;
	            table.changeSelection(lastIndex, 0,false,false);
	        }
	    });
	}

	public void scrollToFirstJtable(JTable table)
	{
		table.scrollRectToVisible(table.getCellRect(0, 0, true));
	}

	public void scrollToLastJtable(JTable table)
	{
		table.scrollRectToVisible(table.getCellRect(table.getRowCount()-1, 0, true));
	}

	private void saveFile() {
		stop();
		if(dataLoadTask.save() == true)
		{
			txtrLog.append("File saved \n");
		}
		else
		{
			txtrLog.append("File save failed \n");
		}
	}

	private void openFile() {
		if(FileConversionUtils.importFromUC((DefaultTableModel) tableDataView.getModel()) == true)
		{
			txtrLog.append("File successfully opened \n");
		}
		else
		{
			txtrLog.append("File Open failed \n");
		}
	}

	private void setTrigger()
	{
		try
        {
            if (isAttached == false)
            {
                txtrLog.append("USBCPro HW not Connected\n");
                return;
            }
            byte[] cmd = new byte[64];
            cmd[0] = 2;


            if (chckbxTrgStart.isSelected() == true)
            {
            	cmd[4] = 1;
            }

            if (chckbxTrgEnd.isSelected() == true)
            {
            	cmd[5] = 1;
            }

            if (chckbxTrgSop.isSelected() == true)
            {
            	cmd[6] = 1;
            }

            if (chckbxTrgMsgType.isSelected() == true)
            {
            	cmd[7] = 1;
            }

            if (chckbxTrgCount.isSelected() == true)
            {
            	cmd[8] = 1;
            }

            if (chckbxTrgMsgId.isSelected() == true)
            {
            	cmd[9] = 1;
            }

            cmd[10] = (byte)comboBoxMsgType.getSelectedIndex();
            cmd[11] = (byte)comboBoxMsgClass.getSelectedIndex();

            cmd[12] = (byte)comboBoxSopType.getSelectedIndex();
            cmd[13] = (byte)Integer.parseInt(textFieldTrgCount.getText());
            cmd[14] = (byte)Integer.parseInt(textFieldTrgMsgId.getText());

            Long start_sno = Long.parseLong(textFieldTrgStart.getText());
            cmd[16] = GenericHelper.uint32_get_b0(start_sno);
            cmd[17] = GenericHelper.uint32_get_b1(start_sno);
            cmd[18] = GenericHelper.uint32_get_b2(start_sno);
            cmd[19] = GenericHelper.uint32_get_b3(start_sno);

            Long end_sno = Long.parseLong(textFieldTrgEnd.getText());
            cmd[20] = GenericHelper.uint32_get_b0(end_sno);
            cmd[21] = GenericHelper.uint32_get_b1(end_sno);
            cmd[22] = GenericHelper.uint32_get_b2(end_sno);
            cmd[23] = GenericHelper.uint32_get_b3(end_sno);


            /* Send trigger command */
            if (ccDataTask.setTigger(cmd) == true)
            {
                txtrLog.append("Trigger Set Successful\n");
            }
            else
            {
            	txtrLog.append("Trigger Set Failed. Please check settings\n");
            }
        }
        catch (Exception ex)
        {
        	txtrLog.append("Trigger Set Failed. Hint : Textboxes must have valid numeric value\n");
        }

    }

	private void getVersion()
	{
		if(isAttached == true)
		{
			byte[] ver = new byte[4];
			boolean result;
			result = ccDataTask.getVersion(ver);
			if(result == true)
			{

				txtrLog.append("Version read success: " +
						GenericHelper.getUnsignedInt(ver[3]) + "." +
						GenericHelper.getUnsignedInt(ver[2]) + "." +
						GenericHelper.getUnsignedInt(ver[1]) + "." +
						GenericHelper.getUnsignedInt(ver[0])  +" \n");
			}
			else
			{
				txtrLog.append("Version read fail \n");
			}

		}
		else
		{
			txtrLog.append("USBCPro HW not connected \n");
		}
	}

	private void putDevInBoot()
	{
		if(isAttached == true)
		{
//			boolean result;
//			result = ccDataTask.putDeviceInBootloadMode();
//			if(result == true)
//			{
//				txtrLog.append("Device in Bootload mode\n");
//				try {
//					Process process = new ProcessBuilder(".\\BootloaderHost\\USBBootloaderHost.exe").start();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					txtrLog.append("Failed to find bootload exe\n");
//				}
//				catch(Exception e)
//				{
//					txtrLog.append("Failed to run Exe\n");
//				}
//			}
//			else
//			{
				txtrLog.append("BootLoad Entry failed\n");
//			}

		}
		else
		{
			txtrLog.append("USBCPro HW not connected \n");
		}
	}

	private void clearDataTable(TableModel tableModel)
	{
		((DefaultTableModel) tableModel).setRowCount(0);
	}

	@Override
	public void deviceAttached(USBManEvent arg0) {
		updateDeviceList();
	}

	@Override
	public void deviceDetached(USBManEvent e) {
		if(isAttached == true)
		{
			if(USBManager.isDevicePresent(USBManager.filterDeviceList(e.getDeviceList(), USBTransferHandler.VID, USBTransferHandler.PID), dev) == false)
			{
				ccDataTask.pause();
				isAttached = false;
				isCapturing = false;
				tabbedPane.setEnabledAt(2, true);
				comboBoxDeviceSelIdx = -1;
				txtrLog.append("USBCPro HW Removed \n");
			}
		}
		updateDeviceList();
	}

	private void updateDeviceList()
	{
		comboBoxDevice.removeAllItems();
		devList = usbm.getDeviceList(USBTransferHandler.VID, USBTransferHandler.PID);
		for(UsbDevice device : devList)
		{
			comboBoxDevice.addItem(device.toString());
		}
	}

	private void form_filter_interface()
	{
		for(int i = 0; i < DataLoad.columns.length; i++)
		{
			JLabel label = new JLabel(DataLoad.columns[i].toString());
			filterLabelList.add(label);
			panel_filter_actual.add(label);
			JTextField txt = new JTextField(10);
			txt.addKeyListener(filterListener);
			filterTextBoxList.add(txt);
			panel_filter_actual.add(txt);
		}

		clearFilter = new JButton("Clear Filter");
		clearFilter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				clearFilters();
			}
		});
		panel_filter_actual.add(clearFilter);
	}

	void setFilters()
	{
//		if(dTableModel.getRowCount() == 0)
//		{
//			return;
//		}
//		RowFilter<DefaultTableModel, Object> rf = null;
//		List<RowFilter<Object,Object>> filters = new ArrayList<RowFilter<Object,Object>>(columns.length);
//		for(int i = 0; i < columns.length; i++)
//		{
//			filters.add(RowFilter.regexFilter("(?i)" + filterTextBoxList.get(i).getText(), i));
//		}
//
//		rf = RowFilter.andFilter(filters);
//	    sorter.setRowFilter(rf);
	}

	void clearFilters()
	{
//		for(int i = 0; i < columns.length; i++)
//		{
//			filterTextBoxList.get(i).setText("");
//		}
//
//		if(dTableModel.getRowCount() == 0)
//		{
//			return;
//		}
//		sorter.setRowFilter(null);
	}

	class FilterKeyListener implements KeyListener{

		@Override
		public void keyPressed(KeyEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void keyReleased(KeyEvent arg0) {

			setFilters();
		}

		@Override
		public void keyTyped(KeyEvent arg0) {
			// TODO Auto-generated method stub

		}
	}

//	public static void setUIFont (javax.swing.plaf.FontUIResource f){
//	    java.util.Enumeration keys = UIManager.getDefaults().keys();
//	    while (keys.hasMoreElements()) {
//	      Object key = keys.nextElement();
//	      Object value = UIManager.get (key);
//	      if (value != null && value instanceof javax.swing.plaf.FontUIResource)
//	        UIManager.put (key, f);
//	      }
//	    }

	private void rowClicked(int row)
	{
		int modalRow = tableDataView.convertRowIndexToModel(row);
		textAreaData.setText((String) tableDataView.getModel().getValueAt(modalRow, DataLoad.DT_DATA_IDX));
		processStartDelta(modalRow);
		detailParse();

	}

	void processStartDelta(int modalRow)
	{
		long startDelta;

		String x = (String)tableDataView.getModel().getValueAt(modalRow, DataLoad.DT_START_TIME_IDX);

		long temp;
		try {
			temp = Long.parseLong(x);
		} catch (NumberFormatException e) {
			return;
		}

		if(startTime >= temp)
		{
			startDelta = startTime - temp;
		}
		else
		{
			startDelta = temp - startTime;
		}
		startTime = temp;
		lblStartDelta.setText("     Start Delta(us): " + NumberFormat.getNumberInstance(Locale.US).format(startDelta));
	}

	void detailParse()
	{
		/* TODO */
		/* Use Data window and convert all data in bytes */
		textAreaData.getText();
		/* Call PDParser and populate tree */

	}
}
