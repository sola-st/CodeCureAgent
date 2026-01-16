// Define new classes or helper methods for encapsulating the repeated logic

class SentenceUtils {

    public static boolean hasSameFeatureValue(List<NLGElement> elements, String feature) {
        boolean allEqual = true;
        Object expectedValue = elements.get(0).getFeature(feature);

        for (NLGElement element : elements) {
            allEqual &= expectedValue.equals(element.getFeature(feature));
        }

        return allEqual;
    }

    public static boolean haveAllElementsFeature(List<NLGElement> elements, String feature, boolean value) {
        for (NLGElement element : elements) {
            if (element.getFeatureAsBoolean(feature) != value) {
                return false;
            }
        }
        return true;
    }

    public static boolean elementsHaveSameHead(List<NLGElement> elements) {
        boolean equal = true;
        NLGElement head1 = elements.get(0).getFeatureAsElement(InternalFeature.HEAD);

        for (NLGElement element : elements) {
            NLGElement head2 = element.getFeatureAsElement(InternalFeature.HEAD);
            equal &= (head1 != null && head2 != null && head1.equals(head2));
        }
        return equal;
    }

    public static boolean elementsHaveSameListFeature(List<NLGElement> elements, String feature) {
        List<NLGElement> expectedList = elements.get(0).getFeatureAsElementList(feature);
        for (NLGElement element : elements) {
            List<NLGElement> currentList = element.getFeatureAsElementList(feature);
            if (!expectedList.equals(currentList)) {
                return false;
            }
        }
        return true;
    }
}

// Refactor the original PhraseChecker class to use helper methods and classes

public abstract class PhraseChecker {
    // ... existing methods ...

    // Just show the refactored method as an example
    // Refactor similar repetitive logic in other methods as well

    public static boolean sameVPHead(NLGElement... sentences) {
        List<NLGElement> vps = getVerbPhrases(sentences);
        return SentenceUtils.elementsHaveSameHead(vps);
    }

    private static List<NLGElement> getVerbPhrases(NLGElement... sentences) {
        List<NLGElement> vps = new ArrayList<NLGElement>();
        for (NLGElement sentence : sentences) {
            vps.add(sentence.getFeatureAsElement(InternalFeature.VERB_PHRASE));
        }
        return vps;
    }

    // Apply similar refactoring for other methods...

    // Example refactoring for `sameVPModifiers` method
    public static boolean sameVPModifiers(NLGElement... sentences) {
        List<NLGElement> vps = getVerbPhrases(sentences);
        boolean samePostModifiers = SentenceUtils.elementsHaveSameListFeature(vps, InternalFeature.POSTMODIFIERS);
        boolean samePreModifiers = SentenceUtils.elementsHaveSameListFeature(vps, InternalFeature.PREMODIFIERS);
        return samePostModifiers && samePreModifiers;
    }

    // Additional example refactoring for `allActive` and `allPassive`
    public static boolean allActive(NLGElement... sentences) {
        return SentenceUtils.haveAllElementsFeature(Arrays.asList(sentences), Feature.PASSIVE, false);
    }

    public static boolean allPassive(NLGElement... sentences) {
        return SentenceUtils.haveAllElementsFeature(Arrays.asList(sentences), Feature.PASSIVE, true);
    }
}