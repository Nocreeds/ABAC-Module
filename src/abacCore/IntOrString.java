package abacCore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class IntOrString {
	private int i;
	private String s;
	private boolean isInt;
	private Date date;
	private boolean isDate;

	public IntOrString(String input) {
		try {
			i = (int) Float.parseFloat(input);
			isInt = true;
			isDate = false;
			s = null;
			date = null;
		} catch (NumberFormatException e) {
			try {
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				date = dateFormat.parse(input);
				isDate = true;
				isInt = false;
				s = null;
			} catch (ParseException e1) {
				s = input;
				isInt = false;
				isDate = false;
				date = null;
			}
		}
	}

	public int getI() {
		return i;
	}

	public void setI(int i) {
		this.i = i;
	}

	public String getS() {
		return s;
	}

	public void setS(String s) {
		this.s = s;
	}

	public boolean equal(Object obj) throws Exception {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntOrString other = (IntOrString) obj;
		if (isInt && other.isInt()) {
			if (i == other.getI())
				return true;
			return false;
		} else if (isDate && other.isDate()) {
			if (date.equals(other.getDate()))
				return true;
			return false;
		} else if (!isInt && !other.isInt() && !isDate && !other.isDate()) {
			if (s.equals(other.getS()))
				return true;
			return false;
		}
		throw new Exception("invalide operation \"equal\": attrebuts type don't match");
	}

	public Date getDate() {
		return date;
	}

	public boolean isDate() {
		return isDate;
	}

	public boolean supequal(Object obj) throws Exception {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntOrString other = (IntOrString) obj;
		if (isInt && other.isInt()) {
			if (i >= other.getI())
				return true;
			return false;
		} else if (isDate && other.isDate()) {
			if (date.equals(other.getDate()) || date.after(other.getDate()))
				return true;
			return false;
		}
		throw new Exception("invalide operation \"supequal\": attrebuts type not integer or Date");
	}

	public boolean infequal(Object obj) throws Exception {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntOrString other = (IntOrString) obj;
		if (isInt && other.isInt()) {
			if (i <= other.getI())
				return true;
			return false;
		} else if (isDate && other.isDate()) {
			if (date.equals(other.getDate()) || date.before(other.getDate()))
				return true;
			return false;
		}
		throw new Exception("invalide operation \"infequal\": attrebuts type not integer");
	}

	public boolean inf(Object obj) throws Exception {
		if (this == obj)
			return false;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntOrString other = (IntOrString) obj;
		if (isInt && other.isInt()) {
			if (i < other.getI())
				return true;
			return false;
		} else if (isDate && other.isDate()) {
			if (date.before(other.getDate()))
				return true;
			return false;
		}
		throw new Exception("invalide operation \"inf\": attrebuts type not integer");
	}

	public boolean sup(Object obj) throws Exception {
		if (this == obj)
			return false;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntOrString other = (IntOrString) obj;
		if (isInt && other.isInt()) {
			if (i > other.getI())
				return true;
			return false;
		} else if (isDate && other.isDate()) {
			if (date.after(other.getDate()))
				return true;
			return false;
		}
		throw new Exception("invalide operation \"sup\": attrebuts type not integer");
	}

	public boolean isInt() {
		return isInt;
	}

	public void setInt(boolean isInt) {
		this.isInt = isInt;
	}

	@Override
	public String toString() {
		if (isInt)
			return String.valueOf(i);
		if (isDate)
			return date.toString();
		return s;
	}
}
