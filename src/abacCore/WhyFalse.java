package abacCore;

public class WhyFalse {
	private boolean res;
	private String why;
	public WhyFalse() {
		res = false;
		why = "";
	}
	public WhyFalse(boolean r) {
		res = r;
		why = "";
	}
	public WhyFalse(boolean r, String w) {
		res = r;
		why = w;
	}
	public boolean isRes() {
		return res;
	}
	public void setRes(boolean res) {
		this.res = res;
	}
	public String getWhy() {
		return why;
	}
	public void setWhy(String why) {
		this.why = why;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WhyFalse other = (WhyFalse) obj;
		if (res != other.res)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "" + res + "";
	}
	

}
