package org.qubership.cloud.disableapi.quarkus;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AntPathMatcherTest {

    @Test
    void testSingleChar() {
        AntPathMatcher antPathMatcher = new AntPathMatcher("/?e?t");
        Assertions.assertTrue(antPathMatcher.matches("/test"));
        Assertions.assertTrue(antPathMatcher.matches("/feet"));
        Assertions.assertFalse(antPathMatcher.matches("/fleet"));
        Assertions.assertFalse(antPathMatcher.matches("/test/t"));
    }

    @Test
    void testSingleWildcard() {
        AntPathMatcher antPathMatcher = new AntPathMatcher("/*");
        Assertions.assertTrue(antPathMatcher.matches("/path1.html"));
        Assertions.assertFalse(antPathMatcher.matches("/path1/path2.html"));
        Assertions.assertFalse(antPathMatcher.matches("/path1/path2/path3.html"));
    }

    @Test
    void testDoubleWildcard() {
        AntPathMatcher antPathMatcher = new AntPathMatcher("/**");
        Assertions.assertTrue(antPathMatcher.matches("/path1/path2/path3.html"));
        Assertions.assertTrue(antPathMatcher.matches("/path1/path2.html"));
        Assertions.assertTrue(antPathMatcher.matches("/path1.html"));
        Assertions.assertTrue(antPathMatcher.matches("/"));
    }

    @Test
    void testSingleWildcardAndExtensions() {
        AntPathMatcher antPathMatcher = new AntPathMatcher("/*.js");
        Assertions.assertTrue(antPathMatcher.matches("/path1.js"));
        Assertions.assertTrue(antPathMatcher.matches("/path2.js"));
        Assertions.assertFalse(antPathMatcher.matches("/path1/path3.js"));
        Assertions.assertFalse(antPathMatcher.matches("/path1.html"));
        Assertions.assertFalse(antPathMatcher.matches("/"));
    }

    @Test
    void testDoubleWildcardAndExtensions() {
        AntPathMatcher antPathMatcher = new AntPathMatcher("/**.js");
        Assertions.assertTrue(antPathMatcher.matches("/path1.js"));
        Assertions.assertTrue(antPathMatcher.matches("/path2.js"));
        Assertions.assertTrue(antPathMatcher.matches("/path1/path3.js"));
        Assertions.assertTrue(antPathMatcher.matches("/path1/path2/path3.js"));
        Assertions.assertFalse(antPathMatcher.matches("/path1.html"));
        Assertions.assertFalse(antPathMatcher.matches("/"));
    }

    @Test
    void testComposite() {
        AntPathMatcher antPathMatcher = new AntPathMatcher("/path?/**/path4/{param1}*{param2:[a-z]+}*{param3:\\d+}");
        Assertions.assertTrue(antPathMatcher.matches("/path1/p/path4/value1---value---23"));
        Assertions.assertTrue(antPathMatcher.matches("/path2/path2/path4/value1---value---23"));
        Assertions.assertTrue(antPathMatcher.matches("/path3/path2/path3/path4/value1---value---23"));
        Assertions.assertFalse(antPathMatcher.matches("/path1/path2/path3/path4/value1---23---value"));
        Assertions.assertFalse(antPathMatcher.matches("/path3/path2/path3/path5/value1---value---23"));
    }

    @Test
    void testCompositeRegex() {
        AntPathMatcher antPathMatcher = new AntPathMatcher("/path?/**/path4/{param1:t?est}*{param2:[a-z?*\\d]+}");
        Assertions.assertTrue(antPathMatcher.matches("/path1/p/path4/test---value?"));
        Assertions.assertTrue(antPathMatcher.matches("/path2/path2/path4/est---value*"));
        Assertions.assertTrue(antPathMatcher.matches("/path3/path2/path3/path4/est---value2"));
        Assertions.assertFalse(antPathMatcher.matches("/path3/path2/path3/path4/est---value!"));
        Assertions.assertFalse(antPathMatcher.matches("/path3/path2/path3/path4/tes---value"));
    }

    @Test
    void testEquals() {
        AntPathMatcher antPathMatcher1_1 = new AntPathMatcher("/1");
        AntPathMatcher antPathMatcher1_2 = new AntPathMatcher("/1");
        AntPathMatcher antPathMatcher2_1 = new AntPathMatcher("/2");
        AntPathMatcher antPathMatcher2_2 = new AntPathMatcher("/2");
        Assertions.assertEquals(antPathMatcher1_1, antPathMatcher1_2);
        Assertions.assertEquals(antPathMatcher1_2, antPathMatcher1_1);
        Assertions.assertEquals(antPathMatcher2_1, antPathMatcher2_2);
        Assertions.assertEquals(antPathMatcher2_2, antPathMatcher2_1);
        Assertions.assertNotEquals(antPathMatcher1_1, antPathMatcher2_1);
        Assertions.assertNotEquals(antPathMatcher1_1, antPathMatcher2_2);
        Assertions.assertNotEquals(antPathMatcher1_2, antPathMatcher2_1);
        Assertions.assertNotEquals(antPathMatcher1_2, antPathMatcher2_2);
        Assertions.assertNotEquals(antPathMatcher2_1, antPathMatcher1_1);
        Assertions.assertNotEquals(antPathMatcher2_1, antPathMatcher1_2);
        Assertions.assertNotEquals(antPathMatcher2_2, antPathMatcher1_1);
        Assertions.assertNotEquals(antPathMatcher2_2, antPathMatcher1_2);
    }
    @Test
    void testHashcode() {
        AntPathMatcher antPathMatcher1_1 = new AntPathMatcher("/1");
        AntPathMatcher antPathMatcher1_2 = new AntPathMatcher("/1");
        AntPathMatcher antPathMatcher2_1 = new AntPathMatcher("/2");
        AntPathMatcher antPathMatcher2_2 = new AntPathMatcher("/2");
        Assertions.assertEquals(antPathMatcher1_1.hashCode(), antPathMatcher1_2.hashCode());
        Assertions.assertEquals(antPathMatcher2_1.hashCode(), antPathMatcher2_2.hashCode());
    }
}