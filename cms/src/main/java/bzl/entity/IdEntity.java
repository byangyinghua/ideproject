package bzl.entity;

import java.util.Date;

public class IdEntity
{

  private String id;
  private Date addTime;
  private boolean deleteStatus;


	public java.lang.String getId(){
		return this.id;
	}

  public void setId(String id) {
    this.id = id;
  }

  public Date getAddTime() {
    return this.addTime;
  }

  public void setAddTime(Date addTime) {
    this.addTime = addTime;
  }

  public boolean isDeleteStatus() {
    return this.deleteStatus;
  }

  public void setDeleteStatus(boolean deleteStatus) {
    this.deleteStatus = deleteStatus;
  }
}