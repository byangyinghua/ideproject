package utils;

public class Column {
//table_Name,column_name,data_type,character_maximum_length,numeric_precision,numeric_scale,
	//is_nullable,CASE WHEN extra = 'auto_increment'  THEN 1 ELSE 0 END AS increment,column_default,column_comment
	private String table_Name;//琛ㄥ悕
	private String column_name;//鍒楀悕
	private String data_type;//鏁版嵁绫诲瀷
	private long character_maximum_length;//瀛楃闀垮害
	private long numeric_precision;//鏁板瓧闀垮害
	private long numeric_scale;//灏忔暟闀垮害
	private String is_nullable;//鍏佽绌�
	private int increment;//鏄惁鑷
	private String column_default;//榛樿鍊�
	private String column_comment;//澶囨敞
	private boolean is_show;
	public boolean getIs_show() {
		return is_show;
	}
	public void setIs_show(boolean is_show) {
		this.is_show = is_show;
	}
	public String getTable_Name() {
		return table_Name;
	}
	public void setTable_Name(String tableName) {
		table_Name = tableName;
	}
	public String getColumn_name() {
		return column_name.toLowerCase();
	}
	public void setColumn_name(String columnName) {
		column_name = columnName.toLowerCase();
	}
	public String getData_type() {
		return data_type;
	}
	public void setData_type(String dataType) {
		data_type = dataType;
	}
	public long getCharacter_maximum_length() {
		return character_maximum_length;
	}
	public void setCharacter_maximum_length(long characterMaximumLength) {
		character_maximum_length = characterMaximumLength;
	}
	public long getNumeric_precision() {
		return numeric_precision;
	}
	public void setNumeric_precision(int numericPrecision) {
		numeric_precision = numericPrecision;
	}
	public long getNumeric_scale() {
		return numeric_scale;
	}
	public void setNumeric_scale(int numericScale) {
		numeric_scale = numericScale;
	}
	public String getIs_nullable() {
		return is_nullable;
	}
	public void setIs_nullable(String isNullable) {
		is_nullable = isNullable;
	}
	public int getIncrement() {
		return increment;
	}
	public void setIncrement(int increment) {
		this.increment = increment;
	}
	public String getColumn_default() {
		return column_default;
	}
	public void setColumn_default(String columnDefault) {
		column_default = columnDefault;
	}
	public String getColumn_comment() {
		return column_comment;
	}
	public void setColumn_comment(String columnComment) {
		column_comment = columnComment;
	}
	
	
}
