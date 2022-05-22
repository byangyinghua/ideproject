package bzl.entity;

import utils.UUIDUtil;

public class RelatedEntity {

	private String uuid = UUIDUtil.getUUID();
	private String cognateId;
	
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getCognateId() {
		return cognateId;
	}
	public void setCognateId(String cognateId) {
		this.cognateId = cognateId;
	}
	
	
}
