package abacCore;

public class IntOrString {
	private int i;
	private String s;
	private boolean isInt;
	public IntOrString (String input) {
		try {
			i = Integer.parseInt(input);
			isInt = true;
			s = null;
		}catch(NumberFormatException e) {
			s = input;
			isInt = false;
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
			if(i == other.getI())
				return true;
			return false;
		}
		if (!(isInt) && !(other.isInt())) {
			if (s.equals(other.getS()))
				return true;
			return false;
		}throw new Exception ("invalide operation \"equal\": attrebuts type don't match");
	}
	public boolean supequal(Object obj) throws Exception {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntOrString other = (IntOrString) obj;
		if (isInt && other.isInt()) {
			if(i >= other.getI())
				return true;
			return false;
		}throw new Exception ("invalide operation \"supequal\": attrebuts type not integer");
	}
	public boolean infequal(Object obj) throws Exception {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntOrString other = (IntOrString) obj;
		if (isInt && other.isInt()) {
			if(i <= other.getI())
				return true;
			return false;
		}throw new Exception ("invalide operation \"infequal\": attrebuts type not integer");
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
			if(i < other.getI())
				return true;
			return false;
		}throw new Exception ("invalide operation \"inf\": attrebuts type not integer");
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
			if(i > other.getI())
				return true;
			return false;
		}throw new Exception ("invalide operation \"sup\": attrebuts type not integer");
	}
	public boolean isInt() {
		return isInt;
	}
	public void setInt(boolean isInt) {
		this.isInt = isInt;
	}
	@Override
	public String toString() {
		if(isInt) 
			return String.valueOf(i);
		return s;
	}
}
