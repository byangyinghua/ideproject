package utils;

import java.lang.reflect.Field;

public class FieldAndCount {
		
	
		public Field getField() {
		return field;
	}
	public void setField(Field field) {
		this.field = field;
	}
		private Field field;
		private int count;
		
		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
}
