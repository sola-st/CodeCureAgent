```java
package org.nlpcn.commons.lang.tire.splitWord;

import org.junit.Test;
import org.nlpcn.commons.lang.tire.SmartGetWord;
import org.nlpcn.commons.lang.tire.domain.SmartForest;
import org.nlpcn.commons.lang.util.StringUtil;

import java.util.logging.Logger;

/**
 * by ansj on 3/30/14.
 */
public class SmartGetWordTest {

	private static final Logger logger = Logger.getLogger(SmartGetWordTest.class.getName());

	@Test
	public void test() {
		/**
		 * 词典的构造.一行一个词后面是参数.可以从文件读取.可以是read流.
		 */
		long start = System.currentTimeMillis();
		SmartForest<Integer> forest = new SmartForest<Integer>();

		forest.add("中国", 3);

		forest.add("android", 3);

		forest.add("java", 3);

		forest.add("中国人", 3);

		String content = " Android-java-中国人";
		
		
		forest.remove("中国人") ;
		
		content = StringUtil.rmHtmlTag(content);

		for (int i = 0; i < 1; i++) {
			SmartGetWord<Integer> udg = forest.getWord(content.toLowerCase().toCharArray());

			String temp;
			while ((temp = udg.getFrontWords()) != null) {
				logger.info(temp + "\t" + udg.getParam());
			}
		}
		logger.info(String.valueOf(System.currentTimeMillis() - start));
	}

}
