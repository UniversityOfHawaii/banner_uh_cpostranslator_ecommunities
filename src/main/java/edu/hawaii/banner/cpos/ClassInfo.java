package edu.hawaii.banner.cpos;

//object that represents the classes portion of student's audit result
public class ClassInfo {

	public String getSubj() {
		return subj;
	}

	public void setSubj(String subj) {
		this.subj = subj;
	}

	public String getNumb() {
		return numb;
	}

	public void setNumb(String numb) {
		this.numb = numb;
	}

	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		this.section = section;
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public String getVpdi() {
		return vpdi;
	}

	public void setVpdi(String vpdi) {
		this.vpdi = vpdi;
	}

	public String getCrn() {
		return crn;
	}

	public void setCrn(String crn) {
		this.crn = crn;
	}

	public boolean isDoesClassCount() {
		return doesClassCount;
	}

	public void setDoesClassCount(boolean doesClassCount) {
		this.doesClassCount = doesClassCount;
	}

	public String getFallThrough() {
		return fallThrough;
	}

	public void setFallThrough(String fallThrough) {
		this.fallThrough = fallThrough;
	}

	public String getFallThroughCode() {
		return fallThroughCode;
	}

	public void setFallThroughCode(String fallThroughCode) {
		this.fallThroughCode = fallThroughCode;
	}

	private String subj;
	private String numb;
	private String section;
	private String term;
	private String vpdi;
	private String crn;
	private boolean doesClassCount;
	private String fallThrough;
	private String fallThroughCode;
}
