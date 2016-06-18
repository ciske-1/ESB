/**
 * @author Malaka Silva
 *  Sales force my SQL sink GUI interface design
 * 
 * */
package org.wso2.datamapper.poc;

/*
 * HelloWorldSwing.java requires no other files. 
 */
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

class LoginPage extends JFrame implements ActionListener{
 JButton SUBMIT,Exit;
 JPanel panel;
 JFrame frame;
 JLabel label1,label2,label3;
 final JTextField  text1,text2,text3;
 LoginPage(){
	 
	 Properties properties = BackupSalesforceData.getPropertyFile();
	 
	 label1 = new JLabel();
	 label1.setText("Username:");
	 text1 = new JTextField(40);
	 text1.setText(properties.getProperty("salesforce.source.username"));
	 
	 label2 = new JLabel();
	 label2.setText("Password:");
     text2 = new JPasswordField(40);
     text2.setText(properties.getProperty("salesforce.source.password"));
 
	 label3 = new JLabel();
	 label3.setText("Session Token:");
     text3 = new JPasswordField(40);
     text3.setText(properties.getProperty("salesforce.source.sessiontoken"));     
     
     SUBMIT=new JButton("SUBMIT");
     Exit=new JButton("EXIT");
    panel=new JPanel(new GridLayout(4,1));
    panel.add(label1);
    panel.add(text1);
    panel.add(label2);
    panel.add(text2);
    panel.add(label3);
    panel.add(text3);
    
    panel.add(SUBMIT);
    panel.add(Exit);
	add(panel,BorderLayout.CENTER);
    SUBMIT.addActionListener(this);
    Exit.addActionListener(this);
    
    setTitle("Salesforce Login Page");
    
    frame = this;
    frame.setSize(400,150);
    frame.setVisible(true);
    
  }
   public void actionPerformed(ActionEvent ae){
	   try{
		   if (ae.getActionCommand().equals("SUBMIT")) {
			   String salesforceUserName = text1.getText();
			   String salesforcePassword = text2.getText();
			   String salesforceSessionToken = text3.getText();
			   
			   
			   BackupSalesforceData backupSalesforceData = new BackupSalesforceData(salesforceUserName,salesforcePassword,salesforceSessionToken);

				   BackupSalesforceGUI helloWorldSwing = new BackupSalesforceGUI(backupSalesforceData);
				   helloWorldSwing.createAndShowGUI();
				   frame.setVisible(false);

		   }else{
			   System.exit(0);
		   }

		   
	   }catch(Exception e){
		   e.printStackTrace();
		   JOptionPane.showMessageDialog(this,"Incorrect login or password",
		            "Error",JOptionPane.ERROR_MESSAGE);
	   }
   }
}

public class BackupSalesforceGUI {
	private BackupSalesforceData salesforceClient = null;
    private JList listAll;
    private JList listSel;
    private JPanel panel,mainPanel;
    private DefaultListModel listModelAll = null;
    private DefaultListModel listModelSel = null;
    
    private JCheckBox c1,c2;    
    private JTextField t1,t2;
    
    private static JFrame frame;
    
    public BackupSalesforceGUI(BackupSalesforceData salesforceClient){
    	this.salesforceClient = salesforceClient;
    }
    
	private void hideAll(){
		mainPanel.removeAll();
		frame.repaint();
	}
	   
	class GenerateButtonListener extends JFrame implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("<<")) {
				for(Object object:listSel.getSelectedValues()){
					listModelSel.removeElement(object);
					listModelAll.addElement(object);
				}
			}else if (e.getActionCommand().equals(">>")) {
				for(Object object:listAll.getSelectedValues()){
					listModelSel.addElement(object);						
					listModelAll.removeElement(object);
				}
			}else if (e.getActionCommand().equals("query")) {
				if(listModelSel.isEmpty()){
					JOptionPane.showMessageDialog(this,"Please select Object(s)",
				            "Error",JOptionPane.ERROR_MESSAGE);				
				}else{
					List <String> sObjectNames = new ArrayList<String>();
					for(int i=0;i < listModelSel.size();i++){
						sObjectNames.add(listModelSel.get(i).toString());
					}
					salesforceClient.generateQuerySchema(sObjectNames);

		
						JOptionPane.showMessageDialog(this,"Schema Sucessfully Created",
					            "Success",JOptionPane.INFORMATION_MESSAGE);

				}
			}else if (e.getActionCommand().equals("upsert")) {
				if(listModelSel.isEmpty()){
					JOptionPane.showMessageDialog(this,"Please select Object(s)",
				            "Error",JOptionPane.ERROR_MESSAGE);
				}else{
					List <String> sObjectNames = new ArrayList<String>();
					for(int i=0;i < listModelSel.size();i++){
						sObjectNames.add(listModelSel.get(i).toString());
					}
					
					salesforceClient.generateUpsertSchema(sObjectNames);
	
						JOptionPane.showMessageDialog(this,"Schema Sucessfully Created",
					            "Success",JOptionPane.INFORMATION_MESSAGE);
				}	
			}
		}
	}	

	
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    public void createAndShowGUI() {
	    
	    
	    frame = new JFrame("POC - Generate Datamapper salesforce schema");
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    
	    frame.setUndecorated(true);
	    frame.getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
	    
    	mainPanel = new JPanel(); 
    	
    	frame.add(mainPanel);
	    //frame.setResizable(false);
	    frame.setSize(800,400);
	    frame.setVisible(true);
	    showGenerateGUI();
    }

    public void showGenerateGUI() {
    	 
    	listModelAll = new DefaultListModel();
    	listModelSel = new DefaultListModel();
    	
    	List <String> lObjects = salesforceClient.listMetadata();
    	if(lObjects != null && lObjects.size() > 0){
    		for(int i=0;i < lObjects.size();i++){
    			String strVal = lObjects.get(i);
    			listModelAll.addElement(strVal);    			
    		}
    	}

	    panel = new JPanel(new GridLayout(2,0));
	    listAll = new JList(listModelAll);
	    listSel = new JList(listModelSel);
	    
		JScrollPane paneAll = new JScrollPane();
		paneAll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		paneAll.getViewport().add(listAll);
	    
		JScrollPane paneSel = new JScrollPane();
		paneSel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		paneSel.getViewport().add(listSel);
		
	    JPanel panelTop = new JPanel();
	    panelTop.add(paneAll);

	    JButton buttonRemove = new JButton("<<");
	    buttonRemove.addActionListener(new GenerateButtonListener());
	    panelTop.add(buttonRemove);
	    
	    JButton buttonAdd = new JButton(">>");
	    buttonAdd.addActionListener(new GenerateButtonListener());
	    panelTop.add(buttonAdd);
	    
	    panelTop.add(paneSel);
	    panel.add(panelTop);
	    
	    JButton buttonCreate = new JButton("query");
	    buttonCreate.addActionListener(new GenerateButtonListener());
	    JPanel panelMiddle = new JPanel();
	    panelMiddle.add(buttonCreate);
	    
	    JButton buttonCreateData = new JButton("upsert");
	    buttonCreateData.addActionListener(new GenerateButtonListener());
	    panelMiddle.add(buttonCreateData);
	    
	    panel.add(panelMiddle);
	    
	    mainPanel.add(panel);
	    frame.add(mainPanel);
	    mainPanel.revalidate();
    }
    

    
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createLogin() {
    	LoginPage loginPage = new LoginPage();
    }
    
 
    
	public static void main(String[] args) {

		try {
			new LoginPage();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
