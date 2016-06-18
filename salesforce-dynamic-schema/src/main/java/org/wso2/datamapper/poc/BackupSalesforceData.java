package org.wso2.datamapper.poc;
/**
 * @author Malaka Silva
 *  Sales force my SQL sink GUI interface design
 * 
 * */

import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.ws.BindingProvider;

import org.apache.xerces.dom.ElementNSImpl;

import com.absi.sfdc.partner.DescribeGlobalResult;
import com.absi.sfdc.partner.DescribeGlobalSObjectResult;
import com.absi.sfdc.partner.DescribeSObjectResult;
import com.absi.sfdc.partner.DescribeSObjects;
import com.absi.sfdc.partner.Field;
import com.absi.sfdc.partner.LoginResult;
import com.absi.sfdc.partner.Logout;
import com.absi.sfdc.partner.QueryResult;
import com.absi.sfdc.partner.SObject;
import com.absi.sfdc.partner.SessionHeader;
import com.absi.sfdc.partner.SforceService;
import com.absi.sfdc.partner.Soap;
import com.sun.xml.bind.api.JAXBRIContext;
import com.sun.xml.ws.api.message.Headers;
import com.sun.xml.ws.developer.WSBindingProvider;

public class BackupSalesforceData {
	
	
	private String SALESFORCE_SOURCE_USERNAME = "";
	private String SALESFORCE_SOURCE_PASSWORD = "";
	private String SALESFORCE_SOURCE_SECURITYTOKEN = "";
	private int AXIS_TIMEOUT = 5;
	private int BATCH_SIZE = 2000;
	private boolean LOCAL_ATTACHMENT_UPDATE = false;

	private final static String PROPERTY_FILE_NAME = System.getProperty("user.dir") + "/config.properties";

	private Properties properties = null;
	
	private boolean isSourceSandbox = false;

	private StringBuilder sb = null;	


	public BackupSalesforceData(String SALESFORCE_USERNAME,String SALESFORCE_PASSWORD,String SALESFORCE_SECURITYTOKEN){
		setupClient();
		
		this.SALESFORCE_SOURCE_USERNAME = SALESFORCE_USERNAME;
		this.SALESFORCE_SOURCE_PASSWORD = SALESFORCE_PASSWORD;
		this.SALESFORCE_SOURCE_SECURITYTOKEN = SALESFORCE_SECURITYTOKEN;
		
		loginSource();
	}
	
	public BackupSalesforceData(){
		setupClient();
		loginSource();
	}
	
	private void setupClient(){
		loadPropertyFile();	
	}
	
	public List<String> listMetadata(){
		try{
	        //Get all SObjects			
	        Soap serviceStub = getSourceServiceStub();
	        List <String> lObjects = new ArrayList<String>();	
	        DescribeGlobalResult gResult = serviceStub.describeGlobal();
	        for(DescribeGlobalSObjectResult describeGlobalSObjectResult :gResult.getSobjects()){
	        	if(describeGlobalSObjectResult.isQueryable()){
	        		lObjects.add(describeGlobalSObjectResult.getName());
	        	}
	        }
			return lObjects;
	        
			/*MetadataService metadataServiceStub = new MetadataService();
			MetadataPortType soap = metadataServiceStub.getMetadata();
			List<ListMetadataQuery> listMetadata = new ArrayList<ListMetadataQuery>();
			ListMetadataQuery listMetadataQuery = new ListMetadataQuery();
			listMetadataQuery.setType("CustomObject");
			listMetadata.add(listMetadataQuery);
	        WSBindingProvider bindingProvider = (WSBindingProvider)soap;
			
			Map<String, Object> ctxt = bindingProvider.getRequestContext();
	        ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, loginResult.getMetadataServerUrl());
	        
			com.absi.sfdc.metadata.SessionHeader sh = new com.absi.sfdc.metadata.SessionHeader();
	        sh.setSessionId(loginResult.getSessionId());
	        JAXBContext jc = JAXBContext.newInstance("com.absi.sfdc.metadata");
	        bindingProvider.setOutboundHeaders(Headers.create((JAXBRIContext) jc, sh));
			List<FileProperties> fileProperties = soap.listMetadata(listMetadata, 22d);
			
			List <String> lObjects = new ArrayList<String>();		
			for(FileProperties fileProperties2:fileProperties){		
				String strFullName = fileProperties2.getFullName();
				if(fileProperties2.getManageableState() == null 
						|| (fileProperties2.getManageableState().value().equals("unmanaged") 
								&& strFullName.endsWith("__c"))){
					lObjects.add(strFullName);
				}
			}
			iAPICtr++;
			return lObjects;*/
		}catch(Exception e){
			throw new RuntimeException("Unable to get meta data");
		}
	}
	
	private Map <String, Map <String, Field>> getObjectStructure(List <String> sObjectNames, boolean bSource)throws MalformedURLException,JAXBException{
        //Creating client service stubs
        Soap serviceStub;
        
       	serviceStub = getSourceServiceStub();;

        
        //Get object metadata
        DescribeSObjects describeSObjects = new DescribeSObjects();
        int iSize = sObjectNames.size();        
        int i,j;
        i=0;        
        Map <String, Map <String, Field>> mTableStructure = new HashMap<String, Map <String, Field>>();
        List<DescribeSObjectResult> describeSObjectResults = null;
        while(i<iSize){
        	List<String> strObjectNames = new ArrayList<String>();
            for(j=0;(i<iSize && j < 100);i++,j++){
            	strObjectNames.add(sObjectNames.get(i));
            }
	        try{
	        	describeSObjectResults = serviceStub.describeSObjects(strObjectNames);
	        }catch(Exception e){
	        	System.out.println(e.getMessage());
	        }
	        Map <String, Field> mTableColumns = null;
	        for(DescribeSObjectResult describeSObjectResult:describeSObjectResults){
	        	String strObjectName = describeSObjectResult.getName();
	        	mTableColumns = new HashMap<String, Field>();
	        	for(Field field:describeSObjectResult.getFields()){
	        		mTableColumns.put(field.getName(), field);
	        	}
	        	mTableStructure.put(strObjectName, mTableColumns);
	        }
        }     
        return mTableStructure;
	}
	
	public void generateQuerySchema(List <String> sObjectNames){
		try{
			Map <String, Map <String, Field>> mTableStructure = getObjectStructure(sObjectNames, true);
	        for(String strObjectName:mTableStructure.keySet()){	        	
	        	Map<String, Field> mTableColumns = mTableStructure.get(strObjectName);
		        insertTableData(strObjectName, mTableColumns);	        	
	        }    
		}catch(Exception e){
			e.printStackTrace();
		}
    }

	public void generateUpsertSchema(List <String> sObjectNames){
		try{
			Map <String, Map <String, Field>> mTableStructure = getObjectStructure(sObjectNames, true);
	        for(String strObjectName:mTableStructure.keySet()){	        	
	        	Map<String, Field> mTableColumns = mTableStructure.get(strObjectName);	        	
		        //Create the table
		        StringBuilder sb = new StringBuilder();
		        sb.append("<sfdc:sObjects xmlns:sfdc='sfdc' type='" + strObjectName + "'>");	
		        sb.append("<sfdc:sObject>");
		        for(String strColumnName:mTableColumns.keySet()){
		        	sb.append("<sfdc:" + strColumnName + ">");
		        	sb.append("value");
		        	sb.append("</sfdc:" + strColumnName + ">");	
		        }
		        sb.append("</sfdc:sObject>");
		        sb.append("</sfdc:sObjects>");
		        System.out.println(sb.toString());
	        }
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void insertTableData(String strObjectName, Map <String, Field> mTableColumns){
		StringBuilder sbOut = new StringBuilder();
		try{
	        //Creating client service stubs
	        Soap serviceStub = getSourceServiceStub();
			
	        //get object data
	        
	        sbOut.append("<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' "
	        		+ "xmlns='urn:partner.soap.sforce.com' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
	        		+ "xmlns:sf='urn:sobject.partner.soap.sforce.com'><soapenv:Body><queryResponse><result xsi:type='QueryResult'>");

	        //get object data
	        StringBuilder sb = new StringBuilder();
	        sb.append("select ");
	        boolean bFirstColumn = true;
	        for(String strColumnName:mTableColumns.keySet()){
	        	if(bFirstColumn){
	        		sb.append(strColumnName);
	        		bFirstColumn = false;
	        	}else{
	        		sb.append(" ," + strColumnName);
	        	}
	        }
	        sb.append(" from " + strObjectName + " limit 1");
	        
	        QueryResult qr = serviceStub.query(sb.toString());
	        
	        List<SObject> sObjects = qr.getRecords();
	        //Save Data
	        if (sObjects != null && !sObjects.isEmpty()) {	        	
		        SObject sObject = sObjects.get(0);	        	
			    sbOut = new StringBuilder(); 		        	
		        Map<String,String> mColumnValue = new HashMap<String, String>();
		        mColumnValue.put("Id", sObject.getId());
		        boolean first = true;
				for(Object obj:sObject.getAny()){
					ElementNSImpl ele = (ElementNSImpl)obj;
					if(first){
						sbOut.append("<records xsi:type='sf:sObject'><sf:type>");
						sbOut.append(sObject.getType());
						sbOut.append("</sf:type>");
						first = false;
					}
	
					if(ele.getFirstChild() != null && ele.getFirstChild().getNodeValue() != null){					
						sbOut.append("<sf:" + ele.getLocalName() + ">");
						sbOut.append(ele.getFirstChild().getNodeValue());
						sbOut.append("</sf:" + ele.getLocalName() + ">");	 
					}else{
						sbOut.append("<sf:" + ele.getLocalName() + ">");
						sbOut.append("</sf:" + ele.getLocalName() + ">"); 
					}
				}       		        
	        }			
		}catch(Exception e){
			e.printStackTrace();
		}
		System.out.println(sbOut.toString());
	}

	
		
	
	public static Properties getPropertyFile(){
		
		try{
			System.out.println("Loading property file from " + PROPERTY_FILE_NAME);
			
			Properties properties = new Properties();
			properties.load(new FileInputStream(PROPERTY_FILE_NAME));
			return properties;
		}catch(Exception e){
			return null;
		}
	}
	
	private void loadPropertyFile(){
		try{
			properties = getPropertyFile();
							
			this.SALESFORCE_SOURCE_USERNAME = properties.getProperty("salesforce.source.username");
			this.SALESFORCE_SOURCE_PASSWORD = properties.getProperty("salesforce.source.password");
			this.SALESFORCE_SOURCE_SECURITYTOKEN = properties.getProperty("salesforce.source.sessiontoken");			
			
			String tmpStr = properties.getProperty("salesforce.source.sandbox");
			if("Y".equals(tmpStr)){
				isSourceSandbox = true;
			}

			try{this.AXIS_TIMEOUT = new Integer(properties.getProperty("axis.timeout"));}catch(Exception e){}
			try{this.BATCH_SIZE = new Integer(properties.getProperty("batch.size"));}catch(Exception e){}
					
		}catch(Exception e){
		}	
	}
	

	private void loginSource(){
		try{
			
			//Login to salesforce
			SforceService sfdcService = new SforceService();
			Soap service = sfdcService.getSoap();
			BindingProvider binding = (BindingProvider)service;
			//HttpHandler hbinding = (HttpHandler)service;
			Map<String, Object> ctxt = binding.getRequestContext();
			for(String strKey:ctxt.keySet()){
				System.out.println(strKey + ":" + ctxt.get(strKey));
				
			}
	        if(isSourceSandbox){
	        	ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "https://test.salesforce.com/services/Soap/u/22.0");
	        }else{
	        	ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "https://login.salesforce.com/services/Soap/u/22.0");
	        }
	        Integer iTimeout = (this.AXIS_TIMEOUT * 60 * 1000);
	        ctxt.put("com.sun.xml.internal.ws.connect.timeout", iTimeout);
	        ctxt.put("com.sun.xml.internal.ws.request.timeout", iTimeout);
	        ctxt.put("com.sun.xml.ws.connect.timeout", iTimeout);
	        ctxt.put("com.sun.xml.ws.request.timeout", iTimeout);	        
	        LoginResult lr = service.login(SALESFORCE_SOURCE_USERNAME, SALESFORCE_SOURCE_PASSWORD + SALESFORCE_SOURCE_SECURITYTOKEN);
			this.loginSourceResult = lr;
	        
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	private LoginResult loginSourceResult = null;
	
	public void logout(){
		try{
			//Login to salesforce
	        SforceService stub = new SforceService();
	        Logout logout = new Logout();
	        SessionHeader sessionHeader = new SessionHeader();
	        sessionHeader.setSessionId(loginSourceResult.getSessionId());
	        //stub.logout(logout,sessionHeader, null);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	

	

    // helper class to decode a "validFor" bitset
    class Bitset {
        byte[] data;

        public Bitset(byte[] data) {
            this.data = data == null ? new byte[0] : data;
        }

        public boolean testBit(int n) {
            return (data[n >> 3] & (0x80 >> n % 8)) != 0;
        }

        public int size() {
            return data.length * 8;
        }
    }
    
    private Soap getSourceServiceStub() throws MalformedURLException,JAXBException{
        SforceService serviceStub = new SforceService();
        Soap soap = serviceStub.getSoap();
        //Set the endpoint
        WSBindingProvider bindingProvider = (WSBindingProvider)soap;
		
		Map<String, Object> ctxt = bindingProvider.getRequestContext();
        Integer iTimeout = (this.AXIS_TIMEOUT * 60 * 1000);
        ctxt.put("com.sun.xml.internal.ws.connect.timeout", iTimeout);
        ctxt.put("com.sun.xml.internal.ws.request.timeout", iTimeout);
        ctxt.put("com.sun.xml.ws.connect.timeout", iTimeout);
        ctxt.put("com.sun.xml.ws.request.timeout", iTimeout);
        ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, loginSourceResult.getServerUrl());
        
		SessionHeader sh = new SessionHeader();
        sh.setSessionId(loginSourceResult.getSessionId());
        JAXBContext jc = JAXBContext.newInstance("com.absi.sfdc.partner");
        bindingProvider.setOutboundHeaders(Headers.create((JAXBRIContext) jc, sh));
		return soap;
	}
}
