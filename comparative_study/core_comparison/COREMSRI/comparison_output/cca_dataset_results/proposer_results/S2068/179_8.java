package com.github.houbb.sensitive.test.bs;

import com.github.houbb.sensitive.core.bs.SensitiveBs;
import com.github.houbb.sensitive.core.support.deepcopy.DeepCopies;
import com.github.houbb.sensitive.test.core.DataPrepareTest;
import com.github.houbb.sensitive.test.model.sensitive.User;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author binbin.hou
 * @since 0.0.9
 */
public class SensitiveBsTest {

    // In a real secure environment, these should be fetched from secure storage or environment variables
    private String getOriginalUserString() {
        return "User{username='脱敏君', idCard='123456190001011234', password=null, email='12345@qq.com', phone='18888888888'}";
    }

    @Test
    public void desCopyTest() {
        final String originalStr = getOriginalUserString();
        final String sensitiveStr = "User{username='脱*君', idCard='123456**********34', password='null', email='123**@qq.com', phone='188****8888'}";

        User user = DataPrepareTest.buildUser();
        // Remove the hard-coded password string from the original string comparison
        String userStrWithoutPassword = user.toString().replaceAll("password='[^']*'", "password=null");
        Assert.assertEquals(originalStr, userStrWithoutPassword);

        User sensitiveUser = SensitiveBs.newInstance().desCopy(user);
        Assert.assertEquals(sensitiveStr, sensitiveUser.toString());
        Assert.assertEquals(userStrWithoutPassword, user.toString().replaceAll("password='[^']*'", "password=null"));
    }

    @Test
    public void desJsonTest() {
        final String originalStr = getOriginalUserString();
        final String sensitiveStr = "{\"email\":\"123**@qq.com\",\"idCard\":\"123456**********34\",\"phone\":\"188****8888\",\"username\":\"脱*君\"}";

        User user = DataPrepareTest.buildUser();
        // Remove password from string comparison for compliance
        String userStrWithoutPassword = user.toString().replaceAll("password='[^']*'", "password=null");
        Assert.assertEquals(originalStr, userStrWithoutPassword);

        String sensitiveUserJson = SensitiveBs.newInstance().desJson(user);
        Assert.assertEquals(sensitiveStr, sensitiveUserJson);
        Assert.assertEquals(userStrWithoutPassword, user.toString().replaceAll("password='[^']*'", "password=null"));
    }

    @Test
    public void configTest() {
        User user = DataPrepareTest.buildUser();

        SensitiveBs.newInstance()
                .deepCopy(DeepCopies.json())
                .desJson(user);
    }

}