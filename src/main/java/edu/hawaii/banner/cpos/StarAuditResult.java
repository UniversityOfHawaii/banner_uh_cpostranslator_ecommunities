package edu.hawaii.banner.cpos;
import java.util.List;

/* object that represent a Star degree audit result
 * 
 */
public class StarAuditResult {
	public String getBannerId() {
		return bannerId;
	}
	public void setBannerId(String bannerId) {
		this.bannerId = bannerId;
	}
	public String getAuditId() {
		return auditId;
	}
	public void setAuditId(String auditId) {
		this.auditId = auditId;
	}
	public String getAuditSource() {
		return auditSource;
	}
	public void setAuditSource(String auditSource) {
		this.auditSource = auditSource;
	}
	public String getDegree() {
		return degree;
	}
	public void setDegree(String degree) {
		this.degree = degree;
	}
	public String getMajor() {
		return major;
	}
	public void setMajor(String major) {
		this.major = major;
	}
	public String getMinor() {
		return minor;
	}
	public void setMinor(String minor) {
		this.minor = minor;
	}
	public String getConc() {
		return conc;
	}
	public void setConc(String conc) {
		this.conc = conc;
	}
	public String getCatyr() {
		return catyr;
	}
	public void setCatyr(String catyr) {
		this.catyr = catyr;
	}
	public String getHomeCampus() {
		return homeCampus;
	}
	public void setHomeCampus(String homeCampus) {
		this.homeCampus = homeCampus;
	}
	public List<ClassInfo> getClassInfoList() {
		return classInfoList;
	}
	public void setClassInfoList(List<ClassInfo> classInfoList) {
		this.classInfoList = classInfoList;
	}
	
	private String bannerId;
	private String auditId;
	private String auditSource;
	private String degree;
	private String major;
	private String minor;
	private String conc;
	private String catyr;
	private String homeCampus;
	private List<ClassInfo> classInfoList;
	
}
