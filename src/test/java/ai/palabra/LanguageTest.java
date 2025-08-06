package ai.palabra;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Language enum.
 */
class LanguageTest {
    
    @Test
    void testLanguageCodes() {
        assertEquals("en-us", Language.EN_US.getCode());
        assertEquals("es-mx", Language.ES_MX.getCode());
        assertEquals("fr", Language.FR.getCode());
        assertEquals("de", Language.DE.getCode());
    }
    
    @Test
    void testLanguageDisplayNames() {
        assertEquals("English (US)", Language.EN_US.getDisplayName());
        assertEquals("Spanish (Mexico)", Language.ES_MX.getDisplayName());
        assertEquals("French", Language.FR.getDisplayName());
        assertEquals("German", Language.DE.getDisplayName());
    }
    
    @Test
    void testFromCodeSuccess() {
        assertEquals(Language.EN_US, Language.fromCode("en-us"));
        assertEquals(Language.ES_MX, Language.fromCode("es-mx"));
        assertEquals(Language.FR, Language.fromCode("fr"));
        
        // Test case insensitive
        assertEquals(Language.EN_US, Language.fromCode("EN-US"));
        assertEquals(Language.ES_MX, Language.fromCode("Es-Mx"));
    }
    
    @Test
    void testFromCodeFailure() {
        assertThrows(IllegalArgumentException.class, () -> 
            Language.fromCode("invalid-code"));
        assertThrows(IllegalArgumentException.class, () -> 
            Language.fromCode(""));
        assertThrows(IllegalArgumentException.class, () -> 
            Language.fromCode(null));
    }
    
    @Test
    void testToString() {
        String result = Language.EN_US.toString();
        assertTrue(result.contains("English (US)"));
        assertTrue(result.contains("en-us"));
    }
    
    @Test
    void testAllLanguagesHaveCodesAndNames() {
        for (Language lang : Language.values()) {
            assertNotNull(lang.getCode(), "Language " + lang + " has null code");
            assertNotNull(lang.getDisplayName(), "Language " + lang + " has null display name");
            assertFalse(lang.getCode().isEmpty(), "Language " + lang + " has empty code");
            assertFalse(lang.getDisplayName().isEmpty(), "Language " + lang + " has empty display name");
        }
    }
}
