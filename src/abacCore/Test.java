package abacCore;

import java.util.HashMap;
import java.util.Map;

public class Test {

	public static void main(String[] args) {

		try {

			Core core = new Core();
			core.init();
			/*------------------------------Add new policy-----------------------------*/
			Map<String, String> m = new HashMap<>();
			m.put("IDLOCATION", "231649");
			m.put("STIME", "2000-01-01 12:12:12");
			m.put("ETIME", "2001-01-01 12:12:12");
			m.put("MODEL", "TimeRes");
			m.put("R_W", "read");
			m.put("OBJECT", "261640");

			Map<String, String> m1 = new HashMap<>();
			m1.put("USERROLE", "owner");
			m1.put("MODEL", "Lock");
			m1.put("R_W", "read");
			m1.put("OBJECT", "261640");

			Map<String, String> m2 = new HashMap<>();
			m2.put("docname", "omar");
			m2.put("docSpeciality", "cardiologe");

//			core.removePolicy(m);
//			core.removePolicy(m1);

//			core.addPolicy(m);
//			core.addPolicy(m1);
//			System.out.println(core.getActiveRules());

			// System.out.println("==>" + Permission(doc.getDocumentElement(), connection,
			// "231649", "261640", "read"));

//			core.addEHR(Common.suckFile("Resource/input.txt"), m2);
//			Common.spitFile("Resource/out.txt", core.getEHR("Resource/1", "a:aa b:b c:c d:dd"));
//	          
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}