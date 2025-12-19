package tests.frequencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.stanford.nlp.ling.TaggedWord;
import fuzzyOntologyMiner.frequencies.NaiveTermsExtractor;
import fuzzyOntologyMiner.postag.PosTag;
import fuzzyOntologyMiner.postag.PosTagPattern;

public class NaiveTermsExtractorTest {

    private NaiveTermsExtractor extractor;

    @BeforeEach
    public void setUp() {
        extractor = new NaiveTermsExtractor();
    }

    @Test
    public void testExtractWithNoPatterns() {
        // Given
        List<TaggedWord> taggedWords = Arrays.asList(
            new TaggedWord("hello", "UH"),
            new TaggedWord("world", "NN")
        );

        // When
        Set<String> result = extractor.extract(taggedWords);

        // Then
        Set<String> expected = new HashSet<>(Arrays.asList("hello", "world"));
        assertEquals(expected, result);
    }

    @Test
    public void testExtractWithMatchingPattern() {
        // Given
        PosTagPattern pattern = new PosTagPattern(PosTag.NOUN, PosTag.NOUN);
        extractor.compile(Arrays.asList(pattern));

        List<TaggedWord> taggedWords = Arrays.asList(
            new TaggedWord("machine", "NN"),
            new TaggedWord("learning", "NN"),
            new TaggedWord("is", "VBZ")
        );

        // When
        Set<String> result = extractor.extract(taggedWords);

        // Then
        assertTrue(result.contains("machine learning"));
        assertTrue(result.contains("is"));
    }

    @Test
    public void testExtractWithNonMatchingPattern() {
        // Given
        PosTagPattern pattern = new PosTagPattern(PosTag.ADJECTIVE, PosTag.NOUN);
        extractor.compile(Arrays.asList(pattern));

        List<TaggedWord> taggedWords = Arrays.asList(
            new TaggedWord("machine", "NN"),
            new TaggedWord("learning", "NN")
        );

        // When
        Set<String> result = extractor.extract(taggedWords);

        // Then
        Set<String> expected = new HashSet<>(Arrays.asList("machine", "learning"));
        assertEquals(expected, result);
    }

    @Test
    public void testExtractWithMultiplePatterns() {
        // Given
        PosTagPattern pattern1 = new PosTagPattern(PosTag.NOUN, PosTag.NOUN);
        PosTagPattern pattern2 = new PosTagPattern(PosTag.ADJECTIVE, PosTag.NOUN);
        extractor.compile(Arrays.asList(pattern1, pattern2));

        List<TaggedWord> taggedWords = Arrays.asList(
            new TaggedWord("big", "JJ"),
            new TaggedWord("data", "NN"),
            new TaggedWord("machine", "NN"),
            new TaggedWord("learning", "NN")
        );

        // When
        Set<String> result = extractor.extract(taggedWords);

        // Then
        assertTrue(result.contains("big data"));
        assertTrue(result.contains("machine learning"));
    }

    @Test
    public void testExtractEmptyList() {
        // Given
        List<TaggedWord> taggedWords = Arrays.asList();

        // When
        Set<String> result = extractor.extract(taggedWords);

        // Then
        assertTrue(result.isEmpty());
    }
}