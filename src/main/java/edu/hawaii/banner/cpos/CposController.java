package edu.hawaii.banner.cpos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CposController {
	@Value("${star.user}")
	String starUser;
	@Value("${star.cred}")
	String starCred;
	@Value("${star.url}")
	String starUrl;

	private static final Logger logger = LogManager.getLogger(CposController.class);
	// testLogger is used to output just the response xml to banner for
	// verification purposes
	private static final Logger tempLogger = LogManager.getLogger("testLogger");

	private static final SimpleDateFormat auditIdSdf = new SimpleDateFormat("yyyyMMddHHmmss");
	private static final SimpleDateFormat freezeDateSdf = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
	private static final String degreeAuditVersion = "System=MyEDU-Degree-Audit Release=4.1.5";

	// our test method just to see if things are working
	@RequestMapping(value = "/test1", method = RequestMethod.GET, produces = "application/json")
	public String test1(@RequestParam(value = "campus", defaultValue = "NONE") String campus) {
		logger.debug("entering test1, campus: " + campus);
		tempLogger.debug("I'm in /test1 method");
		return "welcome to our " + campus + " Campus";
	}

	/**
	 * @param dataStream
	 *          - the request xml from banner
	 * @param campus
	 *          - campus we are running for
	 * @param term
	 *          - array of terms to process
	 * @return - json representation of a student's audit result from STAR
	 * @throws Exception
	 */
	@RequestMapping(value = "/runAudit", method = RequestMethod.POST, produces = "application/json")
	public String runAudit(InputStream dataStream, @RequestParam(value = "campus") String campus,
			@RequestParam("term") String[] term) throws Exception {
		logger
				.debug("********************************************************************************");
		StringBuffer sbTerm = new StringBuffer();
		if (term.length == 0) {
			logger.error("no term code specified for this request, aborting, returning blank xml");
			return "";
		}

		for (int i = 0; i < term.length; i++) {
			sbTerm.append("term ").append(i).append(":").append(term[i]).append(",");
		}

		logger.debug("entering runAudit, campus: " + campus + ", terms: " + sbTerm);
		logger.debug("starUrl: " + this.starUrl + ", starUser: " + this.starUser);

		String auditId = null;
		String freezeDate = freezeDateSdf.format(new Date());
		String bannerId_temp = null;

		// TODO: look into this some more
		// generate a random AuditId that will be sent back, i.e.
		// Audit_id="A0000001"
		// for now, we're just using date + last 4 of bannerid;

		InputStreamReader isr = new InputStreamReader(dataStream);
		SAXReader reader = new SAXReader();
		Document document = reader.read(isr);

		// we need to remove namespaces because in the request xml from banner, this
		// part
		// <WhatIfAuditRequest xmlns= messes up dom4j and can't parse the xml
		removeAllNamespaces(document);

		// pull out important data we need from the xml and store them into a json
		// object for later
		JSONObject studentInfo = parseRequestXml(document);

		// We'll use date plus the last 4 of bannerId to create the audit_id
		bannerId_temp = ((String) studentInfo.get("bannerId")).substring(4);

		// generate an audit id
		auditId = auditIdSdf.format(new Date()) + "_" + bannerId_temp;

		// call our own controller for now as a POC
		// http://localhost:8081/cpos/StarAuditTest?bannerId=12345678&homeCampus=KAP&degree=AS&major=BIOL&minor=&conc=&catyr=201930

		ArrayList<String> starAuditJsonList = new ArrayList<String>();

		// call STAR for each term. we will then merge the results into one that
		// contains all the terms' classInfo in convertAuditResult()
		JSONParser parser = new JSONParser();
		JSONObject starAuditObject = new JSONObject();
		String starAuditJson = null;
		for (int i = 0; i < term.length; i++) {
			starAuditJson = callStarAudit(this.starUrl, 
			                              this.starUser,
			                              this.starCred,
			                              studentInfo,
			                              campus,
			                              term[i]);
			if (starAuditJson != null) {
				if (starAuditJson.trim().length() > 0) {
					starAuditObject = (JSONObject) parser.parse(starAuditJson);
					String bannerId = (String) starAuditObject.get("bannerId");
					if (bannerId != null) {
						if (bannerId.length() > 0) {
							starAuditJsonList.add(starAuditJson);
							logger.debug("starAuditJson: " + starAuditJson);
						} else {
							logger.error("found blank bannerId for term: " + term[i] + "for "
									+ studentInfo.get("bannerId"));
						}
					} else {
						logger.error(
								"found null bannerId for term: " + term[i] + "for " + studentInfo.get("bannerId"));
					}
				} else {
					logger.error("found empty json result for term: " + term[i] + "for "
							+ studentInfo.get("bannerId"));
				}
			} else {
				logger.error(
						"found null json result for term: " + term[i] + "for " + studentInfo.get("bannerId"));
			}
		}

		// convert json reponse from Star to xml and also merge multiple terms into one
		// printAuditResult(starAuditJson, xmlInfo);
		String returnXml = null;
		if (starAuditJsonList.size() > 0) {
			returnXml = convertAuditResult(starAuditJsonList, studentInfo, auditId, freezeDate);
			logger.debug("xml from convertAuditResult:");
			logger.debug(returnXml);
			tempLogger.debug(returnXml);
		} else {
			logger.debug(
					"json result empty, returning a well-formed empty xml to avoid SAXParser Errors in SFPCPOS java process");
			returnXml = createEmptyAuditResult(studentInfo, auditId, freezeDate);
			tempLogger.debug(returnXml);
		}

		logger.debug("leaving runAudit");
		return returnXml;
	}

	/*
	 * creates an empty well-formed xml audit result so that banner does not puke
	 * 
	 * @param bannerInfo - json that contains student info, parsed from banner
	 * request xml
	 * 
	 * @param auditId - auditID; generated using date plus last 4 of banner id
	 * 
	 * @param freezeDate - audit freezeDate
	 * 
	 * @return
	 */
	private String createEmptyAuditResult(JSONObject bannerInfo, String auditId, String freezeDate) {
		logger.debug("entering createEmptyAuditResult for " + bannerInfo.get("bannerId"));

		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
		sb.append("<Report xmlns=\"urn:net:hedtech:degreeworks:audit:v1.0.0\">\n");
		sb.append("  <Audit>\n");
		sb.append("    <AuditHeader Audit_id=\"");
		sb.append(auditId);
		sb.append("\"");
		sb.append(" FreezeDate=\"");
		sb.append(freezeDate);
		sb.append("\"");
		sb.append(" FreezeType=\"");
		sb.append("FREEZE");
		sb.append("\"");
		sb.append(" FreezeTypeDescription=\"");
		sb.append(bannerInfo.get("auditDesc"));
		sb.append("\"");
		sb.append(" FreezeUserName=\"");
		sb.append("");
		sb.append("\"");
		sb.append(" In_progress=\"");
		sb.append("Y");
		sb.append("\"");
		sb.append(" Stu_id=\"");
		sb.append(bannerInfo.get("bannerId"));
		sb.append("\"");
		sb.append(" Version=\"");
		sb.append(degreeAuditVersion);
		sb.append("\"/>\n");

		try {
			/*
			 * skip the course info part, don't think it's needed
			 * sb.append("    <Clsinfo>\n"); sb.append("    </Clsinfo>\n");
			 * 
			 * sb.append("    <OTL Classes=\"\" Credits=\"\" Noncourses=\"\"></OTL>\n"
			 * ); sb.
			 * append("    <Fallthrough Classes=\"\" Credits=\"\" Noncourses=\"\"></Fallthrough>\n"
			 * ); sb.
			 * append("    <Insufficient Classes=\"\" Credits=\"\" Noncourses=\"\"></Insufficient>\n"
			 * ); sb.
			 * append("    <FitList Classes=\"\" Credits=\"\" Noncourses=\"\"></FitList>\n"
			 * );
			 */
			sb.append("    <Deginfo>\n");
			sb.append("      <DegreeData Degree=\"");
			sb.append(bannerInfo.get("program"));
			sb.append("\"");
			sb.append(" Cat_yr=\"");
			sb.append(bannerInfo.get("catyr"));
			sb.append("\"");
			sb.append(" Stu_level=\"");
			sb.append(bannerInfo.get("school"));
			sb.append("\"/>\n");

			sb.append("      <Goal Cat_yr=\"");
			sb.append(bannerInfo.get("catyr"));
			sb.append("\"");
			sb.append(" Value=\"");
			sb.append(bannerInfo.get("major"));
			sb.append("\"");
			sb.append(" Code=\"");
			sb.append("MAJOR");
			sb.append("\"/>\n");

			// add minor and conc here
			if (bannerInfo.get("minor") != null) {
				sb.append("      <Goal Cat_yr=\"");
				sb.append(bannerInfo.get("catyr"));
				sb.append("\"");
				sb.append(" Value=\"");
				sb.append(bannerInfo.get("minor"));
				sb.append("\"");
				sb.append(" Code=\"");
				sb.append("MINOR");
				sb.append("\"/>\n");
			}
			if (bannerInfo.get("conc") != null) {
				sb.append("      <Goal Cat_yr=\"");
				sb.append(bannerInfo.get("catyr"));
				sb.append("\"");
				sb.append(" Value=\"");
				sb.append(bannerInfo.get("conc"));
				sb.append("\"");
				sb.append(" Code=\"");
				sb.append("CONC");
				sb.append("\"/>\n");
			}

			sb.append("    </Deginfo>\n");
			sb.append("    <ExceptionList/>\n");
			sb.append("  </Audit>\n");
			sb.append("</Report>");
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.debug("leaving createEmptyAuditResult");
		return sb.toString();

	}

	/*
	 * converts audit result from json to xml to return to banner
	 */
	/*
	 * This is the newer version takes an arraylist to handle as many terms(jsons)
	 * as we need
	 * 
	 * @param starAuditJson - returned arraylist of result json from STAR
	 * 
	 * @param bannerInfo - json that contains student info, parsed from banner
	 * request xml
	 * 
	 * @param auditId - auditID; generated using date plus last 4 of banner id
	 * 
	 * @param freezeDate - audit freezeDate
	 * 
	 * @return xml to send back to banner
	 */
	private String convertAuditResult(ArrayList<String> starAuditJsonList, JSONObject bannerInfo,
			String auditId, String freezeDate) {
		logger.debug("entering convertAuditResult new with json array parameter");

		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
		sb.append("<Report xmlns=\"urn:net:hedtech:degreeworks:audit:v1.0.0\">\n");
		sb.append("  <Audit>\n");
		sb.append("    <AuditHeader Audit_id=\"");
		sb.append(auditId);
		sb.append("\"");
		sb.append(" FreezeDate=\"");
		sb.append(freezeDate);
		sb.append("\"");
		sb.append(" FreezeType=\"");
		sb.append("FREEZE");
		sb.append("\"");
		sb.append(" FreezeTypeDescription=\"");
		sb.append(bannerInfo.get("auditDesc"));
		sb.append("\"");
		sb.append(" FreezeUserName=\"");
		sb.append("");
		sb.append("\"");
		sb.append(" In_progress=\"");
		sb.append("Y");
		sb.append("\"");
		sb.append(" Stu_id=\"");
		sb.append(bannerInfo.get("bannerId"));
		sb.append("\"");
		sb.append(" Version=\"");
		sb.append(degreeAuditVersion);
		sb.append("\"/>\n");

		JSONParser parser = new JSONParser();
		
		// this will hold our first json string parsed into a json object
		JSONObject starAuditObjectBase = new JSONObject();
		// this will hold our next and subsequent json string parsed into a json
		// object
		JSONObject starAuditObjectNext = new JSONObject();

		// the classListInfo from our first json string
		JSONArray classListArrayBase = new JSONArray();
		// the classListInfo from the next & subsequent json string
		JSONArray classListArrayNext = new JSONArray();

		try {
			starAuditObjectBase = (JSONObject) parser.parse(starAuditJsonList.get(0));
			logger.debug("bannerId: " + starAuditObjectBase.get("bannerId"));
			logger.debug("auditId: " + starAuditObjectBase.get("auditID"));

			classListArrayBase = (JSONArray) starAuditObjectBase.get("ClassInfo");

			// start at 2nd arraylist element (basically 2nd term's classes and on)
			// and merge into our base clssListArray
			for (int i = 1; i < starAuditJsonList.size(); i++) {
				starAuditObjectNext = (JSONObject) parser.parse(starAuditJsonList.get(i));
				classListArrayNext = (JSONArray) starAuditObjectNext.get("ClassInfo");

				logger.debug("classListArrayNext.size(): " + classListArrayNext.size());

				for (int j = 0; j < classListArrayNext.size(); j++) {
					JSONObject jsonObject = (JSONObject) classListArrayNext.get(j);
					classListArrayBase.add((JSONObject) jsonObject);
				}
			}

			// json object to hold one class element as we loop through
			// classListArrayBase
			JSONObject classObject = new JSONObject();

			if (classListArrayBase.size() > 0) {
				sb.append("    <Clsinfo>\n");
				for (int i = 0; i < classListArrayBase.size(); i++) {
					classObject = (JSONObject) classListArrayBase.get(i);
					sb.append("      <Class Discipline=\"");
					sb.append(classObject.get("subj"));
					sb.append("\"");
					sb.append(" Number=\"");
					sb.append(classObject.get("number"));
					sb.append("\"");
					sb.append(" Id_num=\"");
					sb.append(i + 1);
					sb.append("\"");
					sb.append(" Term=\"");
					sb.append(classObject.get("term"));
					sb.append("\"");
					sb.append(" Force_insuff=\"");
					sb.append("");
					sb.append("\"");
					sb.append(" In_progress=\"");
					sb.append("");
					sb.append("\"");
					sb.append(" Reason_insuff=\"");
					sb.append("");
					sb.append("\"");
					sb.append(" Status=\"");
					sb.append("");
					sb.append("\">\n");

					sb.append("        <Attribute Code=\"DWSISKEY\"");
					sb.append(" Value=\"");
					sb.append(classObject.get("crn"));
					sb.append("\"/>\n");
					sb.append("      </Class>\n");

					logger.debug("subj: " + classObject.get("subj"));
					logger.debug("numb: " + classObject.get("number"));
					logger.debug("crn: " + classObject.get("crn"));
					logger.debug("doesClassCount: " + classObject.get("doesClassCount"));

				}
				sb.append("    </Clsinfo>\n");
			}

			sb.append("    <OTL Classes=\"\" Credits=\"\" Noncourses=\"\"></OTL>\n");
			sb.append("    <Fallthrough Classes=\"\" Credits=\"\" Noncourses=\"\"></Fallthrough>\n");

			sb.append("    <Insufficient Classes=\"\" Credits=\"\" Noncourses=\"\">\n");
			if (classListArrayBase.size() > 0) {
				for (int i = 0; i < classListArrayBase.size(); i++) {
					classObject = (JSONObject) classListArrayBase.get(i);
					String sDoesItCount = (String) classObject.get("doesClassCount");
					if (!Boolean.valueOf(sDoesItCount).booleanValue()) {
						sb.append("      <Class Discipline=\"");
						sb.append(classObject.get("subj"));
						sb.append("\"");
						sb.append(" Number=\"");
						sb.append(classObject.get("number"));
						sb.append("\"");
						sb.append(" Id_num=\"");
						sb.append(i + 1);
						sb.append("\"/>\n");
					}
				}
			}
			sb.append("    </Insufficient>\n");

			sb.append("    <FitList Classes=\"\" Credits=\"\" Noncourses=\"\">\n");
			if (classListArrayBase.size() > 0) {
				for (int i = 0; i < classListArrayBase.size(); i++) {
					classObject = (JSONObject) classListArrayBase.get(i);
					String sDoesItCount = (String) classObject.get("doesClassCount");
					if (Boolean.valueOf(sDoesItCount).booleanValue()) {
						sb.append("        <Class Discipline=\"");
						sb.append(classObject.get("subj"));
						sb.append("\"");
						sb.append(" Number=\"");
						sb.append(classObject.get("number"));
						sb.append("\"");
						sb.append(" Id_num=\"");
						sb.append(i + 1);
						sb.append("\"/>\n");
					}
				}
			}
			sb.append("    </FitList>\n");

			// ???? need to decide if we use the degree info from request xml, or
			// ???? we send these to STAR and use what they return back. however, it
			// seems that,
			// ???? they should be identical.
			sb.append("    <Deginfo>\n");
			sb.append("      <DegreeData Degree=\"");
			sb.append(bannerInfo.get("program"));
			sb.append("\"");
			sb.append(" Cat_yr=\"");
			sb.append(bannerInfo.get("catyr"));
			sb.append("\"");
			sb.append(" Stu_level=\"");
			sb.append(bannerInfo.get("school"));
			sb.append("\"/>\n");

			sb.append("      <Goal Cat_yr=\"");
			sb.append(bannerInfo.get("catyr"));
			sb.append("\"");
			sb.append(" Value=\"");
			sb.append(bannerInfo.get("major"));
			sb.append("\"");
			sb.append(" Code=\"");
			sb.append("MAJOR");
			sb.append("\"/>\n");

			// add minor and conc here
			if (bannerInfo.get("minor") != null) {
				sb.append("      <Goal Cat_yr=\"");
				sb.append(bannerInfo.get("catyr"));
				sb.append("\"");
				sb.append(" Value=\"");
				sb.append(bannerInfo.get("minor"));
				sb.append("\"");
				sb.append(" Code=\"");
				sb.append("MINOR");
				sb.append("\"/>\n");
			}
			if (bannerInfo.get("conc") != null) {
				sb.append("      <Goal Cat_yr=\"");
				sb.append(bannerInfo.get("catyr"));
				sb.append("\"");
				sb.append(" Value=\"");
				sb.append(bannerInfo.get("conc"));
				sb.append("\"");
				sb.append(" Code=\"");
				sb.append("CONC");
				sb.append("\"/>\n");
			}

			sb.append("    </Deginfo>\n");
			sb.append("    <ExceptionList/>\n");
			sb.append("  </Audit>\n");
			sb.append("</Report>");
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.debug("leaving convertAuditResult with json array as parameter");
		return sb.toString();
	}

	// merge two json objects into one. each represents one semester
	private String mergeJson(String starAuditJson1, String starAuditJson2) {
		// TODO Auto-generated method stub
		return null;
	}

	private void printAuditResult(String starAuditResult, JSONObject xmlInfo) {
		logger.debug("entering printAuditResult");

		JSONParser parser = new JSONParser();
		JSONObject jsonObject = new JSONObject();
		JSONArray classListArray = new JSONArray();
		try {
			// jsonObject = (JSONObject)parser.parse(new
			// FileReader("D:\\data\\projects\\banner\\CPOS\\star_mock1.json"));
			jsonObject = (JSONObject) parser.parse(starAuditResult);

			logger.debug("bannerId: " + jsonObject.get("bannerId"));
			logger.debug("auditId: " + jsonObject.get("auditId"));
			// logger.debug("degree: " + jsonObject.get("degree"));
			// logger.debug("major: " + jsonObject.get("major"));

			classListArray = (JSONArray) jsonObject.get("ClassInfo");

			for (int i = 0; i < classListArray.size(); i++) {
				jsonObject = (JSONObject) classListArray.get(i);
				logger.debug("classListArray index:: " + i);
				logger.debug("subj: " + jsonObject.get("subj"));
				logger.debug("numb: " + jsonObject.get("number"));
				logger.debug("crn: " + jsonObject.get("crn"));
				String sDoesItCount = (String) jsonObject.get("doesClassCount");
				// logger.debug("doesClassCount: " + jsonObject.get("doesClassCount"));
				logger.debug("doesClassCount: " + Boolean.valueOf(sDoesItCount).booleanValue());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.debug("leaving printAuditResult");

	}

	private String callStarAudit(String sUrl, String username, String cred, JSONObject studentInfo,
			String campus, String termCode) {
		logger.debug("entering callStarAudit");
		logger.debug("campus is: " + campus);

		String USER_AGENT = "Mozilla/5.0";
		String bannerId = (String) studentInfo.get("bannerId");

		StringBuffer response = new StringBuffer();
		try {
				String fullStarUrl = sUrl
						+ "?institution=" + campus
						+ "&TermCode=" + termCode
						+ "&BannerId=" + bannerId
						+ "&SecurityKey=" + cred ;
				URL url = new URL(fullStarUrl);
						
				//actual call to the degree audit is something like this:
				// https://degreeaudit.institution.edu/api/classcount?institution=<MEP>&TermCode=201930&BannerId=123456789&SecurityKey=<SecurityKey>


			java.net.HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", USER_AGENT);

			int responseCode = con.getResponseCode();
			logger.debug("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.debug("leaving callStarAudit");
		return response.toString();
	}

	// parse the xml and store it in a json object
	// this is so we can access some of the student data easily
	private JSONObject parseRequestXml(Document document) {
		logger.debug("entering parseRequestXml");
		logger.debug("incoming banner xml:");
		logger.debug(document.asXML());
		
		String auditDesc = null;
		String bannerId = null;
		String degree = null;
		// program is something like: BS-BIOL-BMD1, when composing response, it'll be element "degree"
		String program = null;
		// school is level
		String school = null;
		String major = null;
		String minor = null;
		String conc = null;
		String catyr = null;

		JSONObject jsonObject = new JSONObject();
		Element rootElement = document.getRootElement();
		logger.debug("Root element :" + rootElement.getName());

		List<Node> nodes = document.selectNodes("/WhatIfAuditRequest/RequestData/Person/PersonID");

		bannerId = nodes.get(0).selectSingleNode("PersonIdCode").getText();

		nodes = document.selectNodes("/WhatIfAuditRequest/RequestConfiguration/SaveAudit");
		auditDesc = nodes.get(0).selectSingleNode("Description").getText();

		nodes = document.selectNodes("/WhatIfAuditRequest/RequestData/AcademicProgram");
		program = nodes.get(0).selectSingleNode("Program").getText();
		school = nodes.get(0).selectSingleNode("School").getText();
		degree = nodes.get(0).selectSingleNode("Degree").getText();
		catyr = nodes.get(0).selectSingleNode("CatalogYear").getText();

		nodes = document.selectNodes("/WhatIfAuditRequest/RequestData/Goals/Goal");
		for (Node node : nodes) {
			if (node.selectSingleNode("Type").getText().equalsIgnoreCase("MAJOR")) {
				major = node.selectSingleNode("Name").getText();
			}
			if (node.selectSingleNode("Type").getText().equalsIgnoreCase("MINOR")) {
				minor = node.selectSingleNode("Name").getText();
			}
			if (node.selectSingleNode("Type").getText().equalsIgnoreCase("CONC")) {
				conc = node.selectSingleNode("Name").getText();
			}
		}
		jsonObject.put("bannerId", bannerId);
		jsonObject.put("degree", degree);
		jsonObject.put("school", school);
		jsonObject.put("program", program);
		jsonObject.put("major", major);
		jsonObject.put("minor", minor);
		jsonObject.put("conc", conc);
		jsonObject.put("catyr", catyr);
		jsonObject.put("auditDesc", auditDesc);

		logger.debug("auditDesc: " + auditDesc);
		logger.debug("bannerId: " + bannerId);
		logger.debug("program: " + program);
		logger.debug("school: " + school);
		logger.debug("degree: " + degree);
		logger.debug("major: " + major);
		logger.debug("minor: " + minor);
		logger.debug("conc: " + conc);
		logger.debug("catyr: " + catyr);

		logger.debug("leaving parseRequestXml");
		return jsonObject;
	}

	// For testing only
	// this method mimicks what STAR audit does and returns a temporary json object
	// that we can test
	@RequestMapping(value = "/StarAuditTest", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<StarAuditResult> getStarAuditResult(
			@RequestParam(value = "bannerId") String bannerId,
			@RequestParam(value = "homeCampus", required = false) String homeCampus,
			@RequestParam(value = "degree") String degree,
			@RequestParam(value = "major") String major,
			@RequestParam(value = "minor", required = false) String minor,
			@RequestParam(value = "conc", required = false) String conc,
			@RequestParam(value = "catyr") String catyr)
	{

		logger.debug("entering getStarAuditResult, bannerId: " + bannerId);
		logger.debug("entering getStarAuditResult, homeCampus: " + homeCampus);

		logger.debug("entering getStarAuditResult, catyr: " + catyr);
		logger.debug("entering getStarAuditResult, bannerId: " + bannerId);

		ResponseEntity<StarAuditResult> respBody;

		List<ClassInfo> classInfoList = buildClassInfoList();
		StarAuditResult starAuditResult = buildStarAuditResult(
			classInfoList,
			bannerId,
			homeCampus,
			degree,
			major,
			minor,
			conc,
			catyr
		);

		boolean hasError = false;
		if (!hasError) {
			respBody = ResponseEntity.ok().body(starAuditResult);
		} else {
			respBody = ResponseEntity.badRequest().body(starAuditResult);
		}

		logger.debug("leaving getStarAuditResult");
		logger.debug("leaving getStarAuditResult");
		logger.debug("respBody.toString(): " + respBody.toString());

		return respBody;
	}

	// for testing only
	private StarAuditResult buildStarAuditResult(List<ClassInfo> classInfo, String bannerId,
			String homeCampus, String degree, String major, String minor, String conc, String catyr) {
		StarAuditResult starAuditResult = new StarAuditResult();
		starAuditResult.setClassInfoList(classInfo);
		starAuditResult.setBannerId(bannerId);
		starAuditResult.setHomeCampus(homeCampus);
		starAuditResult.setDegree(degree);
		starAuditResult.setMajor(major);
		starAuditResult.setMinor(minor);
		starAuditResult.setConc(conc);
		starAuditResult.setCatyr(catyr);
		starAuditResult.setAuditId("A12345");

		return starAuditResult;
	}

	// for testing only
	private List<ClassInfo> buildClassInfoList() {
		List<ClassInfo> classInfoList = new ArrayList<>();

		ClassInfo classInfo = new ClassInfo();
		classInfo.setDoesClassCount(true);
		classInfo.setSubj("Math");
		classInfo.setNumb("205");
		classInfo.setCrn("12345");
		classInfo.setTerm("201930");
		classInfo.setVpdi("KAP");
		classInfoList.add(classInfo);

		classInfo = new ClassInfo();
		classInfo.setDoesClassCount(false);
		classInfo.setSubj("Art");
		classInfo.setNumb("101");
		classInfo.setCrn("24102");
		classInfo.setTerm("201930");
		classInfo.setVpdi("KAP");
		classInfoList.add(classInfo);

		return classInfoList;
	}

	public static void removeAllNamespaces(Document doc) {
		Element root = doc.getRootElement();
		if (root.getNamespace() != Namespace.NO_NAMESPACE) {
			removeNamespaces(root.content());
		}
	}

	public static void unfixNamespaces(Document doc, Namespace original) {
		Element root = doc.getRootElement();
		if (original != null) {
			setNamespaces(root.content(), original);
		}
	}

	public static void setNamespace(Element elem, Namespace ns) {

		elem.setQName(QName.get(elem.getName(), ns, elem.getQualifiedName()));
	}

	/**
	 * Recursively removes the namespace of the element and all its children: sets
	 * to Namespace.NO_NAMESPACE
	 */
	public static void removeNamespaces(Element elem) {
		setNamespaces(elem, Namespace.NO_NAMESPACE);
	}

	/**
	 * Recursively removes the namespace of the list and all its children: sets to
	 * Namespace.NO_NAMESPACE
	 */
	public static void removeNamespaces(List l) {
		setNamespaces(l, Namespace.NO_NAMESPACE);
	}

	/**
	 * Recursively sets the namespace of the element and all its children.
	 */
	public static void setNamespaces(Element elem, Namespace ns) {
		setNamespace(elem, ns);
		setNamespaces(elem.content(), ns);
	}

	/**
	 * Recursively sets the namespace of the List and all children if the current
	 * namespace is match
	 */
	public static void setNamespaces(List l, Namespace ns) {
		Node n = null;
		for (int i = 0; i < l.size(); i++) {
			n = (Node) l.get(i);

			if (n.getNodeType() == Node.ATTRIBUTE_NODE) {
				((Attribute) n).setNamespace(ns);
			}
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				setNamespaces((Element) n, ns);
			}
		}
	}

}