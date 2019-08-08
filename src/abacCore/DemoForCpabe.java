package abacCore;

import co.junwei.cpabe.*;

public class DemoForCpabe {
	final static boolean DEBUG = true;

	static String pubfile = "Resource/pub_key";
	static String mskfile = "Resource/master_key";
	static String prvfile = "Resource/prv_key";

	static String inputfile = "Resource/input.txt";
	static String encfile = "Resource/input.pdf.cpabe";
	static String decfile = "Resource/input.new.txt";

	static String[] attr = { "Doc", "Active", "foo" };
	static String[] another_attr = { "baf1", "fim1", "foo" };
	static String policy = "foo Doc Active 2of3 baf1 1of2";

	static String student_attr = "objectClass:inetOrgPerson "
		+ "objectClass:organizationalPerson sn:student3 cn:student2 "
		+ "uid:student2 userPassword:student2 ou:idp o:computer "
		+ "mail:student2@sdu.edu.cn title:student";

	static String student_policy = "sn:student3 cn:student2 "
		+ "uid:student2 3of3";

	public static void main(String[] args) throws Exception {
		String attr_str;
		// policy = student_policy;
		// attr_str = array2Str(student_attr);

		attr_str = student_attr;
		policy = student_policy;

		Cpabe test = new Cpabe();
		println("//start to setup");
		test.setup(pubfile, mskfile);
		println("//end to setup");

		println("//start to keygen");
		test.keygen(pubfile, prvfile, mskfile, attr_str);
		println("//end to keygen");

		println("//start to enc");
		test.enc(pubfile, policy, inputfile, encfile);
		println("//end to enc");

		println("//start to dec");
		test.dec(pubfile, prvfile, encfile, decfile);
		println("//end to dec");
	}

	/* connect element of array with blank */
	public static String array2Str(String[] arr) {
		int len = arr.length;
		String str = arr[0];

		for (int i = 1; i < len; i++) {
			str += " ";
			str += arr[i];
		}

		return str;
	}

	private static void println(Object o) {
		if (DEBUG)
			System.out.println(o);
	}
}
