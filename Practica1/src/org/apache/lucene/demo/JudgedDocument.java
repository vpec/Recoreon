package org.apache.lucene.demo;

public class JudgedDocument {
	private String docId;
	private boolean relevant;
	
	public JudgedDocument(String docId, boolean relevant) {
		this.docId = docId;
		this.relevant = relevant;
	}

	public String getDocId() {
		return docId;
	}

	public void setDocId(String docId) {
		this.docId = docId;
	}

	public boolean isRelevant() {
		return relevant;
	}

	public void setRelevant(boolean relevant) {
		this.relevant = relevant;
	}
	
}
