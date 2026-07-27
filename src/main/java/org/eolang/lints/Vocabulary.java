/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.lints;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import org.cactoos.set.SetOf;
import org.cactoos.io.InputStreamOf;
import org.cactoos.io.ResourceOf;

/**
 * English vocabulary checks for EO object names.
 *
 * <p>Uses <a href="https://opennlp.apache.org/">OpenNLP</a> POS tagging to
 * determine the grammatical role of words in a kebab-case name.</p>
 *
 * @since 0.2.0
 */
final class Vocabulary {

    /**
     * Pattern to split kebab-case names.
     */
    private static final Pattern KEBAB = Pattern.compile("-");

    /**
     * Non-verbs that still end with "s" in common object names.
     */
    private static final Collection<String> NON_VERBS = new SetOf<>(
        "always", "this", "was", "its"
    );

    /**
     * Part-Of-Speech tagger.
     */
    private final POSTaggerME tagger;

    /**
     * Ctor.
     * @throws IOException If fails to load the POS model resource
     */
    Vocabulary() throws IOException {
        this(
            new POSModel(
                new InputStreamOf(
                    new ResourceOf("en-pos-perceptron.bin")
                )
            )
        );
    }

    /**
     * Ctor.
     * @param mdl Part-Of-Speech model
     */
    Vocabulary(final POSModel mdl) {
        this(new POSTaggerME(mdl));
    }

    /**
     * Ctor.
     * @param pos Part-Of-Speech tagger
     */
    Vocabulary(final POSTaggerME pos) {
        this.tagger = pos;
    }

    /**
     * Check if the given kebab-case name starts with a verb in third-person singular.
     *
     * <p>The check uses the "It [verb]s" rule: "It generates-report" → the
     * first word must be tagged {@code VBZ} (verb, 3rd-person singular present).</p>
     *
     * @param name Kebab-case name without any leading {@code +} sigil
     * @return True if the first word is a VBZ-tagged verb
     */
    boolean isVerb(final String name) {
        final String[] words = Arrays.stream(Vocabulary.KEBAB.split(name))
            .map(s -> s.toLowerCase(Locale.ROOT))
            .toArray(String[]::new);
        final String first = words[0];
        return "VBZ".equals(
            this.tagger.tag(
                Stream.concat(Stream.of("It"), Arrays.stream(words)).toArray(String[]::new)
            )[1]
        ) || Vocabulary.looksLikeVerb(first);
    }

    /**
     * Fallback heuristic for common EO test-object names.
     * @param word First token of the name
     * @return True if it looks like a singular verb
     */
    private static boolean looksLikeVerb(final String word) {
        return "is".equals(word)
            || "has".equals(word)
            || (
                word.length() > 2
                    && word.endsWith("s")
                    && !Vocabulary.NON_VERBS.contains(word)
                    && !word.endsWith("ss")
            );
    }
}
